package com.lengyang.fulu.client.render;

import com.lengyang.fulu.FuLu;
import com.lengyang.fulu.entity.BloodPoolEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

/**
 * 血泊的渲染器：一张**平铺在地上**的贴图。
 *
 * <p>为什么不用模型：血泊就是地上的一滩，一个四边形足够 —— 建模反倒要做一圈厚度，
 * 而厚度在 16×16 的贴图上根本看不出来。飞剑那种需要骨骼动画的东西才值得建模。
 *
 * <p>两个容易错的地方：
 * <ol>
 *   <li>{@code mulPose(Axis.XP.rotationDegrees(-90))}：四边形先在 XY 平面上画，
 *       转 -90 度才躺到地面上（法线朝上）。转 +90 就朝下了，配合不剔除背面的渲染类型
 *       看着一样，但光照会是错的（地面上的血泊发黑）；</li>
 *   <li>抬高 0.011 格：不抬会和地面 z-fighting（一闪一闪的条纹）。</li>
 * </ol>
 */
public class BloodPoolRenderer extends EntityRenderer<BloodPoolEntity> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(FuLu.MOD_ID, "textures/entity/blood_pool.png");

    /** 血泊的边长（格）。贴图和它 1:1，稍微铺开到方块边缘之外一点，看起来才像淌出来的。 */
    private static final float SIZE = 1.05F;

    /** 抬高一点，避免和地面 z-fighting。 */
    private static final double LIFT = 0.011D;

    public BloodPoolRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;             // 平的东西不该有阴影
    }

    @Override
    public ResourceLocation getTextureLocation(BloodPoolEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(BloodPoolEntity entity, float entityYaw, float partialTick,
                       PoseStack pose, MultiBufferSource buffer, int packedLight) {
        pose.pushPose();
        pose.translate(0.0D, LIFT, 0.0D);
        pose.mulPose(Axis.XP.rotationDegrees(-90.0F));

        // entityTranslucent：贴图边缘是半透明的，不能用不透明类型；
        // 也不剔除背面 —— 万一朝向算反了，至少还看得见（配合上面的光照说明）。
        VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(TEXTURE));
        Matrix4f matrix = pose.last().pose();
        float h = SIZE * 0.5F;

        // 逆时针一圈（从 +Y 往下看），UV 铺满 0..1；z 都是 0（四边形先在 XY 平面上画）
        vertex(matrix, vc, -h, -h, 0.0F, 0.0F, packedLight);
        vertex(matrix, vc, h, -h, 1.0F, 0.0F, packedLight);
        vertex(matrix, vc, h, h, 1.0F, 1.0F, packedLight);
        vertex(matrix, vc, -h, h, 0.0F, 1.0F, packedLight);

        pose.popPose();
    }

    /**
     * 一个顶点。
     *
     * <p>1.20.1 的 {@link VertexConsumer} **没有**"矩阵 + 颜色 + UV + 光照"一把梭的重载；
     * 带矩阵的那个只有 {@code vertex(Matrix4f, x, y, z)}，其余属性要用流式 API 挨个写，
     * 最后 {@code endVertex()} 收尾。少写一个属性不会报错，只会画成白的/黑的 —— 所以写全。
     */
    private static void vertex(Matrix4f matrix, VertexConsumer vc,
                               float x, float y, float u, float v, int light) {
        vc.vertex(matrix, x, y, 0.0F)
                .color(1.0F, 1.0F, 1.0F, 1.0F)          // 白色，颜色全在贴图里
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(0.0F, 1.0F, 0.0F)               // 法线朝上
                .endVertex();
    }
}
