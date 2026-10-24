package com.lengyang.fulu.client.render;

import com.lengyang.fulu.entity.DebrisEntity;
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
 * 陨石碎块渲染：**翻滚的小方块**（用方块的地图颜色上色）。
 *
 * <p>手绘六个面 + 每刻随机旋转 —— 看起来就是"被炸飞的一块土石"。
 * 不依赖贴图、不依赖原版实体逻辑，所以**必定能看见**。
 */
public class DebrisRenderer extends EntityRenderer<DebrisEntity> {

    private static final ResourceLocation TEX =
            new ResourceLocation("fulu", "textures/entity/blood_wave.png");

    public DebrisRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public ResourceLocation getTextureLocation(DebrisEntity entity) {
        return TEX;
    }

    @Override
    public void render(DebrisEntity entity, float yaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int packedLight) {
        int color = entity.getColor();
        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;
        float s = entity.getSize() * 0.5F;
        int tick = entity.tickCount;

        // 淡出：最后 6 刻渐隐
        float alpha = tick > DebrisEntity.LIFE - 6 ? (DebrisEntity.LIFE - tick) / 6.0F : 1.0F;
        if (alpha <= 0.02F) {
            return;
        }

        pose.pushPose();
        // 翻滚
        pose.mulPose(Axis.YP.rotationDegrees(tick * 23.0F + entity.getId() * 37.0F));
        pose.mulPose(Axis.XP.rotationDegrees(tick * 17.0F + entity.getId() * 11.0F));

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Matrix4f m = pose.last().pose();
        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = tess.getBuilder();
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        // 六个面（顶面亮一点，底面暗一点）
        quad(buf, m, -s, -s, s, s, -s, s, s, s, s, -s, s, s, r, g, b, alpha);          // 前
        quad(buf, m, s, -s, -s, -s, -s, -s, -s, s, -s, s, s, -s,
                r * 0.7F, g * 0.7F, b * 0.7F, alpha);                                    // 后
        quad(buf, m, -s, -s, -s, -s, -s, s, -s, s, s, -s, s, -s,
                r * 0.85F, g * 0.85F, b * 0.85F, alpha);                                 // 左
        quad(buf, m, s, -s, s, s, -s, -s, s, s, -s, s, s, s,
                r * 0.85F, g * 0.85F, b * 0.85F, alpha);                                 // 右
        quad(buf, m, -s, s, s, s, s, s, s, s, -s, -s, s, -s,
                Math.min(1.0F, r * 1.25F), Math.min(1.0F, g * 1.25F),
                Math.min(1.0F, b * 1.25F), alpha);                                       // 顶
        quad(buf, m, -s, -s, -s, s, -s, -s, s, -s, s, -s, -s, s,
                r * 0.5F, g * 0.5F, b * 0.5F, alpha);                                    // 底

        BufferUploader.drawWithShader(buf.end());

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        pose.popPose();

        super.render(entity, yaw, partialTick, pose, buffers, packedLight);
    }

    private void quad(BufferBuilder buf, Matrix4f m,
                      float x0, float y0, float z0, float x1, float y1, float z1,
                      float x2, float y2, float z2, float x3, float y3, float z3,
                      float r, float g, float b, float a) {
        buf.vertex(m, x0, y0, z0).color(r, g, b, a).endVertex();
        buf.vertex(m, x1, y1, z1).color(r, g, b, a).endVertex();
        buf.vertex(m, x2, y2, z2).color(r, g, b, a).endVertex();
        buf.vertex(m, x3, y3, z3).color(r, g, b, a).endVertex();
    }
}
