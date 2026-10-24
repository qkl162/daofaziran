package com.lengyang.fulu.client.render;

import com.lengyang.fulu.FuLu;
import com.lengyang.fulu.client.model.FormationDemonModel;
import com.lengyang.fulu.entity.FormationDemonEntity;
import com.lengyang.fulu.registry.ModModelLayers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * 阵魔的渲染。
 *
 * <p>两条路径同时开着，用来排查"看不见"卡在哪一层：
 * <ol>
 *   <li><b>模型</b>（{@link FormationDemonModel}）—— 正式的有骨骼模型；</li>
 *   <li><b>保底广告牌</b> —— 一张朝向相机的大四边形，用同一张贴图、半透明。
 *       如果模型坏了，至少这张纸片会出现，就能区分"模型坏了"和"渲染压根没跑"。</li>
 * </ol>
 *
 * <p>每 100 次渲染打一行日志，带"离相机多远"：区分"没渲染"和"渲染了但太远"。
 */
public class FormationDemonRenderer
        extends MobRenderer<FormationDemonEntity, FormationDemonModel<FormationDemonEntity>> {

    private static final ResourceLocation TEX =
            new ResourceLocation("fulu", "textures/entity/formation_demon.png");

    private static int renderCount;

    public FormationDemonRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new FormationDemonModel<>(ctx.bakeLayer(ModModelLayers.FORMATION_DEMON)), 1.4F);
    }

    @Override
    public ResourceLocation getTextureLocation(FormationDemonEntity entity) {
        return TEX;
    }

    @Override
    public void render(FormationDemonEntity entity, float yaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int packedLight) {
        renderCount++;
        if (renderCount == 1) {
            FuLu.LOGGER.info("[demon] renderer drew the model for the first time");
        }
        super.render(entity, yaw, partialTick, pose, buffers, packedLight);
    }

    private void billboard(VertexConsumer vc, Matrix4f m, Matrix3f n,
                           float x0, float y0, float x1, float y1, float a) {
        v(vc, m, n, x0, y0, 1.0F, 1.0F, 1.0F, a, 0.0F, 1.0F);
        v(vc, m, n, x1, y0, 1.0F, 1.0F, 1.0F, a, 1.0F, 1.0F);
        v(vc, m, n, x1, y1, 1.0F, 1.0F, 1.0F, a, 1.0F, 0.0F);
        v(vc, m, n, x0, y1, 1.0F, 1.0F, 1.0F, a, 0.0F, 0.0F);
    }

    private void v(VertexConsumer vc, Matrix4f m, Matrix3f n, float x, float y,
                   float r, float g, float b, float alpha, float u, float v) {
        vc.vertex(m, x, y, 0.0F)
                .color(r, g, b, alpha)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(0xF000F0)
                .normal(n, 0.0F, 1.0F, 0.0F)
                .endVertex();
    }
}
