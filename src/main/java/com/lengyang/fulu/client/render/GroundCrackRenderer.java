package com.lengyang.fulu.client.render;

import com.lengyang.fulu.entity.GroundCrackEntity;
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
 * 地面金色裂纹（贴地 Decal）。
 *
 * <p>用**抖动的折线**模拟裂痕：从原点向 +Z 延伸，每一段横向偏移一点点，
 * 拉成一条贴地的四边形带；再叠一条更亮的细芯。
 * 贴地高度 +0.02，不会和方块 z-fighting。
 *
 * <p>颜色青金（外）→ 白金（芯），加色混合。
 */
public class GroundCrackRenderer extends EntityRenderer<GroundCrackEntity> {

    private static final ResourceLocation TEX =
            new ResourceLocation("fulu", "textures/entity/blood_wave.png");

    public GroundCrackRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public ResourceLocation getTextureLocation(GroundCrackEntity entity) {
        return TEX;
    }

    @Override
    public void render(GroundCrackEntity entity, float yaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int packedLight) {
        int tick = entity.tickCount;
        float a;
        if (tick < 6) {
            a = tick / 6.0F;                                     // 淡入
        } else if (tick > GroundCrackEntity.LIFE - 15) {
            a = (GroundCrackEntity.LIFE - tick) / 15.0F;         // 淡出
        } else {
            a = 1.0F;
        }
        if (a <= 0.01F) {
            return;
        }

        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(entity.getYRot()));
        pose.translate(0.0F, 0.02F, 0.0F);

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE);
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        crack(pose, entity.getLength(), entity.getSeed(), 0.55F, 1.0F, 0.82F, 0.5F, a);
        crack(pose, entity.getLength() * 0.95F, entity.getSeed() + 7, 0.22F, 1.0F, 0.95F, 0.2F, a);

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        pose.popPose();

        super.render(entity, yaw, partialTick, pose, buffers, packedLight);
    }

    /** 一条抖动的裂纹带。 */
    private void crack(PoseStack pose, float length, int seed,
                       float r, float g, float b, float width, float alpha) {
        Matrix4f mat = pose.last().pose();
        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = tess.getBuilder();
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        int seg = 14;
        java.util.Random rnd = new java.util.Random(seed);
        float prevX = 0.0F;
        for (int i = 0; i < seg; i++) {
            float t0 = i / (float) seg;
            float t1 = (i + 1) / (float) seg;
            float z0 = t0 * length;
            float z1 = t1 * length;
            // 抖动偏移
            float x0 = prevX;
            float x1 = prevX + (rnd.nextFloat() - 0.5F) * 0.9F;
            prevX = x1;
            // 末端收细
            float w = width * (1.0F - t0 * 0.7F);
            float w1 = width * (1.0F - t1 * 0.7F);

            buf.vertex(mat, x0 - w, 0.0F, z0).color(r, g, b, alpha).endVertex();
            buf.vertex(mat, x0 + w, 0.0F, z0).color(r, g, b, alpha).endVertex();
            buf.vertex(mat, x1 + w1, 0.0F, z1).color(r, g, b, alpha * 0.7F).endVertex();
            buf.vertex(mat, x1 - w1, 0.0F, z1).color(r, g, b, alpha * 0.7F).endVertex();
        }
        BufferUploader.drawWithShader(buf.end());
    }
}
