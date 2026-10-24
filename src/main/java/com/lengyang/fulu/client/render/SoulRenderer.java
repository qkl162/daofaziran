package com.lengyang.fulu.client.render;

import com.lengyang.fulu.entity.SoulEntity;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * 魂的渲染：一张朝向相机的半透明幽灵贴图。
 *
 * <p>为什么用广告牌（billboard）而不是实体模型：魂本来就该"没有实体感"，
 * 一张会飘、会淡出的贴图比一个方块模型更像幽灵；而且不吃模型面数。
 *
 * <p>三层叠加：外层淡青柔光（大，40%）、中层本体（中，60%）、眼睛（小，不透明白）。
 */
public class SoulRenderer extends EntityRenderer<SoulEntity> {

    private static final ResourceLocation TEX =
            new ResourceLocation("fulu", "textures/entity/soul.png");

    public SoulRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public ResourceLocation getTextureLocation(SoulEntity entity) {
        return TEX;
    }

    @Override
    public void render(SoulEntity entity, float yaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int packedLight) {
        pose.pushPose();
        // 上下浮动
        double bob = Math.sin((entity.tickCount + partialTick) * 0.15D) * 0.08D;
        pose.translate(0.0D, 0.45D + bob, 0.0D);
        // 朝向相机
        pose.mulPose(this.entityRenderDispatcher.cameraOrientation());
        pose.mulPose(Axis.YP.rotationDegrees(180.0F));

        float glow = entity.glow();
        VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucent(TEX));
        var matrix = pose.last().pose();
        var normal = pose.last().normal();

        // 外层柔光
        quad(vc, matrix, normal, 1.35F + glow * 0.3F, 0.6F, 0.95F, 1.0F, 0.38F);
        // 本体
        quad(vc, matrix, normal, 1.0F + glow * 0.2F, 1.0F, 1.0F, 1.0F, 0.85F);

        pose.popPose();
        super.render(entity, yaw, partialTick, pose, buffers, packedLight);
    }

    /** 一个朝向相机的四边形（中心在原点，向 Z 稍微偏一点避免 z-fighting）。 */
    private void quad(VertexConsumer vc, Matrix4f m, Matrix3f n,
                      float scale, float r, float g, float b, float a) {
        float h = scale * 0.5F;
        vertex(vc, m, n, -h, -h, r, g, b, a, 0.0F, 1.0F);
        vertex(vc, m, n, h, -h, r, g, b, a, 1.0F, 1.0F);
        vertex(vc, m, n, h, h, r, g, b, a, 1.0F, 0.0F);
        vertex(vc, m, n, -h, h, r, g, b, a, 0.0F, 0.0F);
    }

    private void vertex(VertexConsumer vc, Matrix4f m, Matrix3f n,
                        float x, float y, float r, float g, float b, float a,
                        float u, float v) {
        vc.vertex(m, x, y, 0.0F)
                .color(r, g, b, a)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(0xF000F0)                       // 自发光，不受光照影响
                .normal(n, 0.0F, 1.0F, 0.0F)
                .endVertex();
    }
}
