package com.lengyang.fulu.client.render;

import com.lengyang.fulu.entity.MeteorEntity;
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
 * 陨石渲染：**全部走手绘 Tesselator 路径**（这个项目里验证过的唯一可靠路子）。
 *
 * <p>为什么不再用 {@code MultiBufferSource + RenderType}：
 * 上一版把"批处理（RenderType）"和"手绘（Tesselator + 立即 draw）"混在同一个渲染方法里，
 * 两套渲染状态互相破坏 —— 结果**渲染器被调用了、但屏幕上什么都没有**（日志已证实）。
 *
 * <p>现在：一次设置状态 → 画 6 片贴图火焰 → 叠 3 层手绘光球 → 恢复状态。
 */
public class MeteorRenderer extends EntityRenderer<MeteorEntity> {

    private static final int FRAMES = 8;
    private static final int FRAME_TICKS = 2;

    private static final float[][] PLATES = {
            {0.0F, 0.0F},
            {30.0F, 22.0F},
            {60.0F, -22.0F},
            {90.0F, 0.0F},
            {120.0F, 22.0F},
            {150.0F, -22.0F},
    };

    private static final ResourceLocation[] TEX = new ResourceLocation[FRAMES];

    static {
        for (int i = 0; i < FRAMES; i++) {
            TEX[i] = new ResourceLocation("fulu", "textures/entity/fireball/fire_" + i + ".png");
        }
    }

    public MeteorRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public ResourceLocation getTextureLocation(MeteorEntity entity) {
        return TEX[frameOf(entity)];
    }

    private static int frameOf(MeteorEntity entity) {
        return (entity.tickCount / FRAME_TICKS) % FRAMES;
    }

    @Override
    public void render(MeteorEntity entity, float yaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int packedLight) {
        float size = 3.2F * entity.getSize();
        int tick = entity.tickCount;

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE);
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);

        // 1) 贴图火焰片 ×6
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, TEX[frameOf(entity)]);
        for (int p = 0; p < PLATES.length; p++) {
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(PLATES[p][0] + tick * 3.0F));
            pose.mulPose(Axis.XP.rotationDegrees(PLATES[p][1]));
            texturedPlate(pose.last().pose(), size, p, tick);
            pose.popPose();
        }

        // 2) 手绘光球 ×3（不依赖贴图）
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(tick * 6.0F));
        ball(pose, size * 0.62F, 1.0F, 0.30F, 0.05F, 0.50F);
        ball(pose, size * 0.46F, 1.0F, 0.62F, 0.16F, 0.72F);
        ball(pose, size * 0.28F, 1.0F, 0.96F, 0.72F, 0.95F);
        pose.popPose();

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();

        super.render(entity, yaw, partialTick, pose, buffers, packedLight);
    }

    /** 一片带贴图的火焰四边形（位置带旋转/抖动扰动）。 */
    private void texturedPlate(Matrix4f m, float side, int seed, int tick) {
        float h = side * 0.5F;
        float phase = seed * 1.7F;
        float wob = (float) Math.sin(tick * 0.45F + phase) * 0.06F * side;
        float sk = (float) Math.cos(tick * 0.35F + phase) * 0.05F * side;
        float z = (float) Math.sin(tick * 0.5F + phase) * 0.04F * side;

        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = tess.getBuilder();
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

        buf.vertex(m, -h - wob * 0.5F, -h + sk, z).uv(0.0F, 1.0F)
                .color(255, 255, 255, 235).endVertex();
        buf.vertex(m, h + wob * 0.5F, -h + sk * 0.5F, -z).uv(1.0F, 1.0F)
                .color(255, 255, 255, 235).endVertex();
        buf.vertex(m, h, h - sk, -z).uv(1.0F, 0.0F)
                .color(255, 255, 255, 235).endVertex();
        buf.vertex(m, -h, h - sk * 0.5F, z).uv(0.0F, 0.0F)
                .color(255, 255, 255, 235).endVertex();

        BufferUploader.drawWithShader(buf.end());
    }

    /** 纬度环球面（加色）。 */
    private void ball(PoseStack pose, float radius, float r, float g, float b, float a) {
        Matrix4f mat = pose.last().pose();
        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = tess.getBuilder();
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        int rings = 6;
        int segs = 14;
        for (int ring = 0; ring < rings; ring++) {
            double lat0 = Math.PI * (ring / (double) rings) - Math.PI / 2.0D;
            double lat1 = Math.PI * ((ring + 1) / (double) rings) - Math.PI / 2.0D;
            float y0 = (float) (Math.sin(lat0) * radius);
            float y1 = (float) (Math.sin(lat1) * radius);
            float r0 = (float) (Math.cos(lat0) * radius);
            float r1 = (float) (Math.cos(lat1) * radius);
            for (int i = 0; i < segs; i++) {
                double t0 = Math.PI * 2.0D * i / segs;
                double t1 = Math.PI * 2.0D * (i + 1) / segs;
                buf.vertex(mat, (float) (Math.cos(t0) * r0), y0, (float) (Math.sin(t0) * r0))
                        .color(r, g, b, a).endVertex();
                buf.vertex(mat, (float) (Math.cos(t1) * r0), y0, (float) (Math.sin(t1) * r0))
                        .color(r, g, b, a).endVertex();
                buf.vertex(mat, (float) (Math.cos(t1) * r1), y1, (float) (Math.sin(t1) * r1))
                        .color(r, g, b, a * 0.6F).endVertex();
                buf.vertex(mat, (float) (Math.cos(t0) * r1), y1, (float) (Math.sin(t0) * r1))
                        .color(r, g, b, a * 0.6F).endVertex();
            }
        }
        BufferUploader.drawWithShader(buf.end());
    }
}
