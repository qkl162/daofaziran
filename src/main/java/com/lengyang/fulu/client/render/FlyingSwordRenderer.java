package com.lengyang.fulu.client.render;

import com.lengyang.fulu.FuLu;
import com.lengyang.fulu.client.model.FlyingSwordModel;
import com.lengyang.fulu.entity.FlyingSwordEntity;
import com.lengyang.fulu.registry.ModModelLayers;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * 飞剑的渲染器：把 {@link FlyingSwordModel} 画到世界里。
 *
 * <p><b>每把剑都是独立的</b>：模型的形状是启动时烘焙一次、所有实体共用的，所以
 * "这一把剑朝哪儿、在什么位置"**只能通过 {@link PoseStack} 施加**，绝不能去改
 * {@code ModelPart} 的字段 —— 那样十几把剑会互相串姿态（一把剑转了，满天的剑都跟着转）。
 * {@link FlyingSwordModel#setupAnim} 因此是空的。
 *
 * <p>模型来自 Blockbench（{@code ref/mohei_xuanjian.bbmodel}）：柄在原点、剑锋朝上，
 * 全剑 48 单位 = 3 格。渲染时做三件事：
 * <ol>
 *   <li>按实体的 yaw/pitch 转（每把剑各自的朝向）；</li>
 *   <li>沿剑轴下移 1.5 格，让**剑的中段**落在实体位置上
 *       （否则剑会整把悬在实体前方 3 格）；</li>
 *   <li>放倒（模型 +Y 转到 +Z），让剑锋指向飞行方向。</li>
 * </ol>
 */
public class FlyingSwordRenderer extends EntityRenderer<FlyingSwordEntity> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(FuLu.MOD_ID, "textures/entity/flying_sword.png");

    /** 模型 48 单位 = 3 格；缩到一半，一把 1.5 格的玄剑。 */
    private static final float MODEL_SCALE = 0.5F;

    /** 剑尖到剑柄的距离（格），符文环绕要用同一个数。 */
    public static final float BLADE_LENGTH = 3.0F * MODEL_SCALE;

    private final FlyingSwordModel model;

    public FlyingSwordRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new FlyingSwordModel(context.bakeLayer(ModModelLayers.FLYING_SWORD));
        this.shadowRadius = 0.0F;
    }

    @Override
    public ResourceLocation getTextureLocation(FlyingSwordEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(FlyingSwordEntity entity, float entityYaw, float partialTick,
                       PoseStack pose, MultiBufferSource buffer, int packedLight) {
        // ★ 必须**自己**调用 setupAnim！继承 EntityRenderer（而不是
        //   LivingEntityRenderer）的话，没有任何人会替你调它 ——
        //   不调的话模型永远停在初始姿态，动画一行都不会生效，而且不报错。
        this.model.setupAnim(entity, 0.0F, 0.0F, entity.animAge(), 0.0F, 0.0F);

        pose.pushPose();

        // 朝向：按**速度方向**转整把剑（动画只管自转和浮动，朝向在这里）。
        // 用的是"这一小段位移"的方向，所以剑尖始终指着它正在飞的方向。
        double mx = entity.getX() - entity.xo;
        double my = entity.getY() - entity.yo;
        double mz = entity.getZ() - entity.zo;
        float yaw;
        float pitch;
        if (mx * mx + my * my + mz * mz > 1.0E-6D) {
            double horizontal = Math.sqrt(mx * mx + mz * mz);
            yaw = (float) Math.toDegrees(Math.atan2(mz, mx)) - 90.0F;
            pitch = (float) (-Math.toDegrees(Math.atan2(my, horizontal)));
        } else {
            // 这一小段没动（刚生成、或者停住了）：沿用实体记着的朝向
            yaw = entity.getYRot();
            pitch = entity.getXRot();
        }

        pose.mulPose(Axis.YP.rotationDegrees(-yaw));
        pose.mulPose(Axis.XP.rotationDegrees(pitch));
        // 让剑的中段（而不是柄）落在实体位置
        pose.translate(0.0D, BLADE_LENGTH * 0.5D, 0.0D);
        // 放倒：模型的 +Y（剑锋方向）转到 +Z（飞行方向）
        pose.mulPose(Axis.XP.rotationDegrees(90.0F));
        pose.scale(MODEL_SCALE, MODEL_SCALE, MODEL_SCALE);

        VertexConsumer vertices = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        this.model.renderToBuffer(pose, vertices, packedLight, OverlayTexture.NO_OVERLAY,
                1.0F, 1.0F, 1.0F, 1.0F);

        pose.popPose();
        super.render(entity, entityYaw, partialTick, pose, buffer, packedLight);
    }
}
