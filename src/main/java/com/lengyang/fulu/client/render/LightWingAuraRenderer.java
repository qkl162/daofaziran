package com.lengyang.fulu.client.render;

import com.lengyang.fulu.entity.LightWingAuraEntity;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

/**
 * 「光翼」渲染：背后两片金色光翼 + 蓄力时剑尖的聚光。
 *
 * <p>翅膀由 **12 根羽刃**组成（每侧 6 根），展开度由实体的
 * {@code getUnfold()} 控制：0 = 全部收在背后，1 = 完全张开。
 *
 * <p>全部加色混合（SRC_ALPHA, ONE），所以叠起来会自然发光。
 */
public class LightWingAuraRenderer extends EntityRenderer<LightWingAuraEntity> {

    private static final ResourceLocation TEX =
            new ResourceLocation("fulu", "textures/entity/blood_wave.png");

    public LightWingAuraRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public ResourceLocation getTextureLocation(LightWingAuraEntity entity) {
        return TEX;
    }

    @Override
    public void render(LightWingAuraEntity entity, float yaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int packedLight) {
        // **第一人称时不要画自己的光翼**：它就贴在镜头上，会挡满视野
        if (entity.isCamera()) {
            return;                                    // 相机实体不画任何东西
        }
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player != null && entity.getCasterId() != null
                && mc.player.getUUID().equals(entity.getCasterId())
                && mc.options.getCameraType().isFirstPerson()) {
            return;
        }

        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(entity.getYRot()));
        pose.translate(0.0F, 1.0F, -0.55F);             // 背部、稍靠后

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE);
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        float unfold = entity.getUnfold();
        // 蓄力段（前 8 刻）在剑尖位置聚一团光
        if (entity.tickCount < 9) {
            float k = entity.tickCount / 9.0F;
            glow(pose, 0.55F + k * 0.45F, (0.4F + 0.6F * k));
        }

        // 光翼：收拢时贴身，展开时向后上方大张
        if (unfold > 0.02F) {
            wing(pose, 1.0F, unfold);
            wing(pose, -1.0F, unfold);
        }

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        pose.popPose();

        super.render(entity, yaw, partialTick, pose, buffers, packedLight);
    }

    /** 一侧翅膀：6 根羽刃，展开度控制张开角度与长度。 */
    private void wing(PoseStack pose, float side, float unfold) {
        float alpha = Math.min(1.0F, unfold * 1.4F);
        for (int i = 0; i < 6; i++) {
            pose.pushPose();
            // 展开：向后两侧张开
            float spread = 18.0F + i * 12.0F;
            float lift = 8.0F + i * 7.0F;
            pose.mulPose(Axis.YP.rotationDegrees(side * spread * unfold));
            pose.mulPose(Axis.ZP.rotationDegrees(-lift * unfold));
            pose.translate(0.0F, i * 0.16F, 0.0F);
            float len = (1.6F + i * 0.5F) * (0.25F + 0.75F * unfold);
            float w = 0.16F + i * 0.04F;
            feather(pose, len, w, 1.0F, 0.84F, 0.42F, alpha * 0.55F);
            feather(pose, len * 0.92F, w * 0.5F, 1.0F, 0.98F, 0.86F, alpha * 0.9F);
            pose.popPose();
        }
    }

    /** 单根羽刃：向 -Z 伸展、两头尖。 */
    private void feather(PoseStack pose, float length, float width,
                         float r, float g, float b, float a) {
        Matrix4f mat = pose.last().pose();
        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = tess.getBuilder();
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        int seg = 8;
        for (int i = 0; i < seg; i++) {
            float t0 = i / (float) seg;
            float t1 = (i + 1) / (float) seg;
            float wA = width * (float) Math.sin(t0 * Math.PI);
            float wB = width * (float) Math.sin(t1 * Math.PI);
            float zA = -t0 * length;
            float zB = -t1 * length;
            buf.vertex(mat, -wA, 0.0F, zA).color(r, g, b, a * 0.8F).endVertex();
            buf.vertex(mat, wA, 0.0F, zA).color(r, g, b, a * 0.8F).endVertex();
            buf.vertex(mat, wB, 0.0F, zB).color(r, g, b, a).endVertex();
            buf.vertex(mat, -wB, 0.0F, zB).color(r, g, b, a).endVertex();
        }
        BufferUploader.drawWithShader(buf.end());
    }

    /** 剑尖聚光：两片互相垂直的亮片。 */
    private void glow(PoseStack pose, float size, float alpha) {
        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = tess.getBuilder();
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (int plane = 0; plane < 2; plane++) {
            pose.pushPose();
            if (plane == 1) {
                pose.mulPose(Axis.ZP.rotationDegrees(90.0F));
            }
            Matrix4f m = pose.last().pose();
            float s = size;
            buf.vertex(m, -s, -s, 0.0F).color(1.0F, 0.92F, 0.6F, alpha).endVertex();
            buf.vertex(m, s, -s, 0.0F).color(1.0F, 0.92F, 0.6F, alpha).endVertex();
            buf.vertex(m, s, s, 0.0F).color(1.0F, 0.92F, 0.6F, alpha).endVertex();
            buf.vertex(m, -s, s, 0.0F).color(1.0F, 0.92F, 0.6F, alpha).endVertex();
            pose.popPose();
        }
        BufferUploader.drawWithShader(buf.end());
    }
}
