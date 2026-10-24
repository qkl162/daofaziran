package com.lengyang.fulu.client.model;

import com.lengyang.fulu.entity.FlyingSwordEntity;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;

/**
 * 飞剑的**动画**：idle / fly / launch / hit 四段，作用在
 * {@link FlyingSwordShape} 生成的三根骨骼上。
 *
 * <p>骨骼（用户规范：剑身是父骨骼，转它整把剑跟着转）：
 * <pre>
 *   blade  剑身（含剑尖）
 *    +- guard  护手
 *    +- hilt   剑柄
 * </pre>
 *
 * <p><b>为什么在 {@code setupAnim} 里改骨骼是安全的</b>：{@code ModelPart} 是烘焙一次、
 * 所有实体共用的，但 {@code setupAnim} 会在**每个实体渲染之前**被调用一次，
 * 而且这里写的全是**绝对值**（不是累加）—— 所以每把剑都拿到自己那一份姿态。
 * 前提是渲染器必须**真的调用**它：原版 {@code LivingEntityRenderer} 会调，
 * 而我们的渲染器继承的是 {@code EntityRenderer}，得自己调
 * （见 {@code FlyingSwordRenderer}，那里有注释说明这个坑）。
 *
 * <p><b>朝向不在这里做</b>：剑尖对准目标是渲染器按速度方向转整把剑；
 * 这里只管剑自身的自转与浮动。两件事分开，互不干扰。
 */
public class FlyingSwordModel extends EntityModel<FlyingSwordEntity> {

    /** launch：0.5 秒，只播一次。 */
    public static final int LAUNCH_TICKS = FlyingSwordEntity.ANIM_LAUNCH_TICKS;
    /** idle：2 秒一轮。 */
    public static final int IDLE_TICKS = FlyingSwordEntity.ANIM_IDLE_TICKS;
    /** fly：1 秒一轮，比 idle 快一倍。 */
    public static final int FLY_TICKS = FlyingSwordEntity.ANIM_FLY_TICKS;
    /** hit：0.3 秒，只播一次，播完就消失。 */
    public static final int HIT_TICKS = FlyingSwordEntity.ANIM_HIT_TICKS;

    /** 浮动幅度：0.05 格 = 0.8 模型单位（用户要求"小一点"）。 */
    private static final float BOB_UNITS = 0.8F;

    /** 剑尖摆动幅度（度）。 */
    private static final float TIP_SWAY_DEGREES = 1.5F;

    /** 护手/剑柄反向旋转的比例。 */
    private static final float COUNTER_SPIN = 0.15F;

    /** hit 的震动表：一下大、一下小（单位：格，正 = 朝剑锋方向）。 */
    private static final float[] HIT_SHAKE = {0.0F, 0.10F, -0.07F, 0.05F, -0.03F, 0.0F};

    /** 一整圈。 */
    private static final float TAU = (float) (Math.PI * 2.0);

    private final ModelPart root;
    private final ModelPart blade;
    private final ModelPart guard;
    private final ModelPart hilt;

    public FlyingSwordModel(ModelPart root) {
        this.root = root;
        this.blade = root.getChild("blade");
        this.guard = this.blade.getChild("guard");
        this.hilt = this.blade.getChild("hilt");
    }

    public static LayerDefinition createBodyLayer() {
        return FlyingSwordShape.createBodyLayer();
    }

    @Override
    public void renderToBuffer(com.mojang.blaze3d.vertex.PoseStack pose,
                               com.mojang.blaze3d.vertex.VertexConsumer buffer,
                               int packedLight, int packedOverlay,
                               float red, float green, float blue, float alpha) {
        this.root.render(pose, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }

    @Override
    public void setupAnim(FlyingSwordEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        reset();

        int age = entity.animAge();
        switch (entity.animState()) {
            case FlyingSwordEntity.ANIM_LAUNCH -> animLaunch(age);
            case FlyingSwordEntity.ANIM_FLY -> animFly(age);
            case FlyingSwordEntity.ANIM_HIT -> animHit(age);
            default -> animIdle(age);
        }
    }

    /**
     * 每帧先归零。
     *
     * <p>必须写**绝对值**而不是累加：这个模型是所有实体共用的，
     * 上一把剑的姿态会留在字段里。
     */
    private void reset() {
        this.root.x = 0.0F;
        this.root.y = 0.0F;
        this.root.z = 0.0F;
        this.root.xRot = 0.0F;
        this.root.yRot = 0.0F;
        this.root.zRot = 0.0F;
        this.root.xScale = 1.0F;
        this.root.yScale = 1.0F;
        this.root.zScale = 1.0F;

        this.blade.xRot = 0.0F;
        this.blade.yRot = 0.0F;
        this.blade.zRot = 0.0F;
        this.blade.xScale = 1.0F;
        this.blade.yScale = 1.0F;
        this.blade.zScale = 1.0F;

        this.guard.yRot = 0.0F;
        this.hilt.yRot = 0.0F;
    }

    /**
     * idle 待机：2 秒一轮。
     * <ul>
     *   <li>剑身绕长轴 0 - 180 - 360 度，慢慢自转；</li>
     *   <li>整把剑上下浮动 +-0.05 格（0.5 秒在上、1.5 秒在下）；</li>
     *   <li>剑尖左右摆一点点（1.5 度）。</li>
     * </ul>
     */
    private void animIdle(int age) {
        float t = (age % IDLE_TICKS) / (float) IDLE_TICKS;
        this.blade.yRot = t * TAU;
        // Java 模型 y 向下，所以"向上浮"是负的；sin 正好给出 0.5s 在上、1.5s 在下
        this.root.y = -BOB_UNITS * (float) Math.sin(t * TAU);
        // 剑尖摆动：绕侧向轴，频率是自转的两倍，看起来才像"甩"而不是"翻"
        this.blade.zRot = (float) Math.toRadians(TIP_SWAY_DEGREES)
                * (float) Math.sin(t * TAU * 2.0D);
    }

    /**
     * fly 飞行：1 秒一圈，比 idle 快一倍，像在钻。
     * 整把剑**不浮动**（飞行要利落），护手和剑柄轻微反向旋转增加层次感。
     */
    private void animFly(int age) {
        float t = (age % FLY_TICKS) / (float) FLY_TICKS;
        float spin = t * TAU;
        this.blade.yRot = spin;
        this.guard.yRot = -spin * COUNTER_SPIN;
        this.hilt.yRot = -spin * COUNTER_SPIN;
    }

    /**
     * launch 发射：0.5 秒只播一次 —— 绕长轴 0 - 720 度（0.25 秒）- 1080 度（0.5 秒），
     * 整把剑 0.5 - 1.2 - 1.0 倍，像被弹出去。
     */
    private void animLaunch(int age) {
        float half = LAUNCH_TICKS * 0.5F;
        float clamped = Math.min(age, LAUNCH_TICKS);
        float degrees = clamped < half
                ? (clamped / half) * 720.0F
                : 720.0F + ((clamped - half) / half) * 360.0F;
        this.blade.yRot = (float) Math.toRadians(degrees);

        float t = clamped / (float) LAUNCH_TICKS;
        float scale;
        if (t < 0.5F) {
            scale = 0.5F + (t / 0.5F) * (1.2F - 0.5F);            // 0.5 -> 1.2
        } else {
            scale = 1.2F - ((t - 0.5F) / 0.5F) * (1.2F - 1.0F);   // 1.2 -> 1.0
        }
        this.root.xScale = scale;
        this.root.yScale = scale;
        this.root.zScale = scale;
    }

    /**
     * hit 命中：0.3 秒只播一次 —— 沿剑轴前后震两下（第二下更小），
     * 剑身瞬间放大到 1.3 倍再缩回。播完飞剑就被移除了。
     */
    private void animHit(int age) {
        if (age >= 0 && age < HIT_SHAKE.length) {
            // 正数 = 朝剑锋方向；模型里剑锋在 -Y，所以取负
            this.root.y = -HIT_SHAKE[age] * 16.0F;
        }
        float t = Math.min(age, HIT_TICKS) / (float) HIT_TICKS;
        float scale = 1.0F + 0.3F * Math.max(0.0F, 1.0F - t * 2.0F);   // 1.3 -> 1.0
        this.blade.xScale = scale;
        this.blade.yScale = scale;
        this.blade.zScale = scale;
    }
}
