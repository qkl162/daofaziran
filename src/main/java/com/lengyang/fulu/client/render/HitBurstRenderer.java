package com.lengyang.fulu.client.render;

import com.lengyang.fulu.entity.HitBurstEntity;
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
 * 命中爆点：**环形冲击波 Mesh** + 金色爆闪。
 *
 * <p>三个环：贴地水平环、竖直环、斜环；再加两片交叉亮片做爆闪。
 * 环在 8 刻内从 0.5 扩到 3 倍并淡出。
 */
public class HitBurstRenderer extends EntityRenderer<HitBurstEntity> {

    private static final ResourceLocation TEX =
            new ResourceLocation("fulu", "textures/entity/blood_wave.png");

    public HitBurstRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public ResourceLocation getTextureLocation(HitBurstEntity entity) {
        return TEX;
    }

    @Override
    public void render(HitBurstEntity entity, float yaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int packedLight) {
        int tick = entity.tickCount;
        float k = Math.min(1.0F, tick / 8.0F);
        float grow = 0.5F + k * 2.5F;
        float alpha = (1.0F - k) * (1.0F - k);
        if (alpha <= 0.01F) {
            return;
        }
        float scale = entity.getScale();

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE);
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        // 贴地水平环
        pose.pushPose();
        ring(pose, grow * scale, alpha * 0.9F, 0.0F);
        pose.popPose();

        // 竖直环
        pose.pushPose();
        pose.mulPose(Axis.XP.rotationDegrees(90.0F));
        ring(pose, grow * scale * 0.85F, alpha * 0.7F, 0.0F);
        pose.popPose();

        // 斜环
        pose.pushPose();
        pose.mulPose(Axis.ZP.rotationDegrees(45.0F));
        ring(pose, grow * scale * 0.7F, alpha * 0.5F, 0.0F);
        pose.popPose();

        // 爆闪
        if (tick <= 3) {
            flash(pose, scale * (1.2F + tick * 0.6F), (1.0F - tick / 4.0F) * 0.9F);
        }

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();

        super.render(entity, yaw, partialTick, pose, buffers, packedLight);
    }

    private void ring(PoseStack pose, float radius, float alpha, float yOffset) {
        Matrix4f mat = pose.last().pose();
        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = tess.getBuilder();
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        int seg = 48;
        float thick = Math.max(0.08F, radius * 0.18F);
        for (int i = 0; i < seg; i++) {
            double a0 = Math.PI * 2.0D * i / seg;
            double a1 = Math.PI * 2.0D * (i + 1) / seg;
            float x0 = (float) Math.cos(a0) * radius;
            float z0 = (float) Math.sin(a0) * radius;
            float x1 = (float) Math.cos(a1) * radius;
            float z1 = (float) Math.sin(a1) * radius;
            float x0i = (float) Math.cos(a0) * (radius - thick);
            float z0i = (float) Math.sin(a0) * (radius - thick);
            float x1i = (float) Math.cos(a1) * (radius - thick);
            float z1i = (float) Math.sin(a1) * (radius - thick);

            buf.vertex(mat, x0, yOffset, z0).color(1.0F, 0.86F, 0.45F, alpha).endVertex();
            buf.vertex(mat, x1, yOffset, z1).color(1.0F, 0.86F, 0.45F, alpha).endVertex();
            buf.vertex(mat, x1i, yOffset, z1i).color(0.7F, 1.0F, 0.95F, alpha * 0.4F).endVertex();
            buf.vertex(mat, x0i, yOffset, z0i).color(0.7F, 1.0F, 0.95F, alpha * 0.4F).endVertex();
        }
        BufferUploader.drawWithShader(buf.end());
    }

    private void flash(PoseStack pose, float size, float alpha) {
        Matrix4f mat = pose.last().pose();
        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = tess.getBuilder();
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        float s = size;
        buf.vertex(mat, -s, -s, 0.0F).color(1.0F, 0.97F, 0.85F, alpha).endVertex();
        buf.vertex(mat, s, -s, 0.0F).color(1.0F, 0.97F, 0.85F, alpha).endVertex();
        buf.vertex(mat, s, s, 0.0F).color(1.0F, 0.97F, 0.85F, alpha).endVertex();
        buf.vertex(mat, -s, s, 0.0F).color(1.0F, 0.97F, 0.85F, alpha).endVertex();
        BufferUploader.drawWithShader(buf.end());
    }
}
