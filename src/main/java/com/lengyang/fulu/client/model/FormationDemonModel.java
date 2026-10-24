package com.lengyang.fulu.client.model;

import com.lengyang.fulu.entity.FormationDemonEntity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;

/**
 * 阵魔模型 + 动作库。
 *
 * <p><b>坐标约定</b>（曾写错，记在这里）：模型空间里 <b>y = 24 是脚底</b>，越往上越负。
 *
 * <p><b>动作怎么驱动</b>：服务端把"当前动作编号 / 开始时刻 / 前摇长度"写进同步数据，
 * 客户端在这里读出来，用 {@code t = ageInTicks - animStart} 算进度，再按阶段摆姿势。
 * 动作和伤害判定共用同一条时间轴，所以不会脱节。
 *
 * <p>每个动作都拆五段：预备 → 发力 → 命中 → 跟随 → 收招。
 * 身体联动靠"腰先动、手臂跟上、头单独盯人"的相位差表现。
 */
public class FormationDemonModel<T extends net.minecraft.world.entity.LivingEntity>
        extends EntityModel<T> {

    private static final int HURT_TOTAL = 6;

    private final ModelPart root;
    private final ModelPart legL;
    private final ModelPart legR;
    private final ModelPart body;
    private final ModelPart armL;
    private final ModelPart armR;
    private final ModelPart head;
    private final ModelPart eye;
    private final ModelPart shardA;
    private final ModelPart shardB;
    private final ModelPart shardC;

    public FormationDemonModel(ModelPart root) {
        super(RenderType::entityCutoutNoCull);
        this.root = root;
        this.legL = root.getChild("leg_l");
        this.legR = root.getChild("leg_r");
        this.body = root.getChild("body");
        this.armL = root.getChild("arm_l");
        this.armR = root.getChild("arm_r");
        this.head = root.getChild("head");
        this.eye = this.head.getChild("eye");
        this.shardA = root.getChild("shard_a");
        this.shardB = root.getChild("shard_b");
        this.shardC = root.getChild("shard_c");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("leg_l",
                CubeListBuilder.create().texOffs(0, 32).addBox(-3.5F, 0.0F, -3.5F, 7.0F, 22.0F, 7.0F),
                PartPose.offset(-5.0F, 2.0F, 0.0F));
        root.addOrReplaceChild("leg_r",
                CubeListBuilder.create().texOffs(0, 32).addBox(-3.5F, 0.0F, -3.5F, 7.0F, 22.0F, 7.0F),
                PartPose.offset(5.0F, 2.0F, 0.0F));

        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0).addBox(-10.0F, 0.0F, -5.0F, 20.0F, 16.0F, 10.0F),
                PartPose.offset(0.0F, -14.0F, 0.0F));

        root.addOrReplaceChild("arm_l",
                CubeListBuilder.create().texOffs(28, 32).addBox(-2.5F, 0.0F, -2.5F, 5.0F, 22.0F, 5.0F),
                PartPose.offset(-11.5F, -13.0F, 0.0F));
        root.addOrReplaceChild("arm_r",
                CubeListBuilder.create().texOffs(28, 32).addBox(-2.5F, 0.0F, -2.5F, 5.0F, 22.0F, 5.0F),
                PartPose.offset(11.5F, -13.0F, 0.0F));

        // ---- 旗子：挂在右臂上，所以挥旗时它跟着手臂一起动
        PartDefinition armR = root.getChild("arm_r");
        armR.addOrReplaceChild("flag_pole",
                CubeListBuilder.create().texOffs(0, 56).addBox(-0.6F, 0.0F, -0.6F, 1.2F, 34.0F, 1.2F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        armR.addOrReplaceChild("flag_cloth",
                CubeListBuilder.create().texOffs(8, 56).addBox(-0.6F, 0.0F, -0.6F, 1.2F, 12.0F, 9.0F),
                PartPose.offset(0.0F, 3.0F, 0.0F));

        PartDefinition head = root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(40, 0).addBox(-6.0F, -11.0F, -6.0F, 12.0F, 11.0F, 12.0F),
                PartPose.offset(0.0F, -14.0F, 0.0F));
        head.addOrReplaceChild("eye",
                CubeListBuilder.create().texOffs(50, 32).addBox(-3.0F, -7.0F, -6.6F, 6.0F, 3.0F, 1.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        for (int i = 0; i < 3; i++) {
            root.addOrReplaceChild("shard_" + (char) ('a' + i),
                    CubeListBuilder.create().texOffs(28, 50).addBox(-1.0F, -1.0F, -6.0F, 2.0F, 2.0F, 12.0F),
                    PartPose.offset(0.0F, -20.0F + i * 10.0F, 0.0F));
        }

        return LayerDefinition.create(mesh, 64, 64);
    }

    // ================================================================ 平滑工具
    private static float ease(float p) {
        p = Mth.clamp(p, 0.0F, 1.0F);
        return p * p * (3.0F - 2.0F * p);
    }

    private static float lerp(float a, float b, float p) {
        return a + (b - a) * p;
    }

    // ================================================================ 主入口
    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        // ---- 复位（避免上一帧姿势残留）
        this.body.xRot = 0.0F;
        this.body.yRot = 0.0F;
        this.body.zRot = 0.0F;
        this.body.xScale = 1.0F;
        this.body.yScale = 1.0F;
        this.body.zScale = 1.0F;
        this.armL.xRot = 0.0F;
        this.armL.yRot = 0.0F;
        this.armL.zRot = 0.0F;
        this.armR.xRot = 0.0F;
        this.armR.yRot = 0.0F;
        this.armR.zRot = 0.0F;
        this.legL.xRot = 0.0F;
        this.legR.xRot = 0.0F;

        // ---- 待机：呼吸（周期约 2 秒）+ 手臂轻摆
        float breath = Mth.sin(ageInTicks * 0.157F);
        this.body.yScale = 1.0F + breath * 0.012F;
        this.body.xRot = breath * 0.02F;
        this.armL.zRot = 0.06F;
        this.armR.zRot = -0.06F;
        this.armL.xRot = breath * 0.06F;
        this.armR.xRot = -breath * 0.06F;

        // ---- 走路：腿摆、手臂反相
        this.legL.xRot += Mth.cos(limbSwing * 0.6662F) * 0.75F * limbSwingAmount;
        this.legR.xRot += Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 0.75F * limbSwingAmount;
        this.armL.xRot += Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 0.5F * limbSwingAmount;
        this.armR.xRot += Mth.cos(limbSwing * 0.6662F) * 0.5F * limbSwingAmount;

        // ---- 阵纹：常驻慢转 + 上下浮动
        float shardSpin = ageInTicks * 0.06F;
        this.shardA.yRot = shardSpin;
        this.shardB.yRot = shardSpin + 2.094F;
        this.shardC.yRot = shardSpin + 4.189F;
        this.shardA.y = -20.0F + Mth.sin(ageInTicks * 0.08F) * 2.0F;
        this.shardB.y = -10.0F + Mth.sin(ageInTicks * 0.08F + 1.5F) * 2.0F;
        this.shardC.y = 0.0F + Mth.sin(ageInTicks * 0.08F + 3.0F) * 2.0F;

        // ---- 动作（T 的边界是 LivingEntity，所以要先转成阵魔才能读动画字段）
        int anim = FormationDemonEntity.ANIM_IDLE;
        float t = 0.0F;
        if (entity instanceof FormationDemonEntity demon) {
            anim = demon.getAnim();
            t = ageInTicks - demon.getAnimStart();
        }
        if (t < 0.0F || t > 200.0F) {
            t = 0.0F;
        }
        switch (anim) {
            case FormationDemonEntity.ANIM_MELEE -> this.poseMelee(t, entity);
            case FormationDemonEntity.ANIM_RAY -> this.poseRay(t);
            case FormationDemonEntity.ANIM_DASH -> this.poseDash(t);
            case FormationDemonEntity.ANIM_SUMMON -> this.poseSummon(t);
            case FormationDemonEntity.ANIM_BURST -> this.poseBurst(t);
            case FormationDemonEntity.ANIM_HURT -> this.poseHurt(t);
            default -> {
            }
        }

        // ---- 头始终盯人（身体动、头不动）—— 放在最后，覆盖所有动作
        this.head.yRot = netHeadYaw * ((float) Math.PI / 180.0F);
        this.head.xRot = headPitch * ((float) Math.PI / 180.0F);

        // ---- 眼睛：呼吸 + 受击更亮
        float pulse = 1.0F + Mth.sin(ageInTicks * 0.12F) * 0.08F
                + (entity.hurtTime > 0 ? 0.35F : 0.0F);
        this.eye.xScale = pulse;
        this.eye.yScale = pulse;
    }

    // ================================================================ 近战：预备→发力→命中→跟随→收招
    /**
     * 近战姿势。
     *
     * <p>用哪只手由 {@code entity.getAnimArm()} 决定：
     * <ul>
     *   <li>0 = 右手劈；</li>
     *   <li>1 = 左手劈（整体镜像）；</li>
     *   <li>2 = **双手过顶重砸**：两条手臂一起抬起、一起砸下，身体下沉更多、幅度更大 ——
     *       这是"有分量"的关键，单手挥砍永远像挠痒。</li>
     * </ul>
     */
    private void poseMelee(float t, T entity) {
        int windup = 10;
        int arm = 0;
        if (entity instanceof FormationDemonEntity demon) {
            int len = demon.getAnimLen();
            if (len > 0) {
                windup = len;
            }
            arm = demon.getAnimArm();
        }
        float w = windup;
        boolean both = arm == 2;
        // 出手那条手臂；mirror 表示左右镜像（左手时身体朝反方向拧）
        boolean left = arm == 1;
        float mirror = left ? -1.0F : 1.0F;

        if (t < w) {
            // ---- 预备：身体后仰、腰拧过去、手臂拉到身后、肩膀抬高
            float p = ease(t / w);
            float pull = both ? 0.85F : 1.0F;              // 双手时手臂不用拉那么靠后
            this.body.xRot = lerp(0.0F, -0.26F, p) * (both ? 1.3F : 1.0F);
            this.body.yRot = lerp(0.0F, 0.22F, p) * mirror;
            if (both) {
                this.armL.xRot = lerp(0.0F, -2.55F, p);
                this.armR.xRot = lerp(0.0F, -2.55F, p);
                this.armL.zRot = lerp(0.0F, 0.25F, p);
                this.armR.zRot = lerp(0.0F, -0.25F, p);
            } else if (left) {
                this.armL.xRot = lerp(0.0F, -2.3F, p) * pull;
                this.armL.zRot = lerp(0.0F, 0.35F, p);
                this.armR.xRot = lerp(0.0F, 0.35F, p);
            } else {
                this.armR.xRot = lerp(0.0F, -2.3F, p) * pull;
                this.armR.zRot = lerp(0.0F, -0.35F, p);
                this.armL.xRot = lerp(0.0F, 0.35F, p);
            }
            this.legR.xRot = lerp(0.0F, -0.15F, p);
        } else if (t < w + 2.0F) {
            // ---- 发力：手臂砸下、身体前倾、脚步踏出、身体下沉
            float p = ease((t - w) / 2.0F);
            this.body.xRot = lerp(-0.26F, both ? 0.62F : 0.5F, p);
            this.body.yRot = lerp(0.22F * mirror, -0.25F * mirror, p);
            if (both) {
                this.armL.xRot = lerp(-2.55F, 1.25F, p);
                this.armR.xRot = lerp(-2.55F, 1.25F, p);
                this.body.yScale = lerp(1.0F, 0.92F, p);   // 下蹲压重
            } else if (left) {
                this.armL.xRot = lerp(-2.3F, 1.15F, p);
                this.armL.zRot = lerp(0.35F, -0.15F, p);
            } else {
                this.armR.xRot = lerp(-2.3F, 1.15F, p);
                this.armR.zRot = lerp(-0.35F, 0.15F, p);
            }
            this.legL.xRot = lerp(0.0F, 0.55F, p);
        } else if (t < w + 6.0F) {
            // ---- 跟随：继续往下甩、身体跟着倾
            float p = ease((t - w - 2.0F) / 4.0F);
            this.body.xRot = lerp(both ? 0.62F : 0.5F, both ? 0.45F : 0.36F, p);
            this.body.yRot = lerp(-0.25F * mirror, -0.12F * mirror, p);
            if (both) {
                this.armL.xRot = lerp(1.25F, 1.55F, p);
                this.armR.xRot = lerp(1.25F, 1.55F, p);
                this.body.yScale = lerp(0.92F, 0.96F, p);
            } else if (left) {
                this.armL.xRot = lerp(1.15F, 1.5F, p);
            } else {
                this.armR.xRot = lerp(1.15F, 1.5F, p);
            }
            this.legL.xRot = lerp(0.55F, 0.3F, p);
        } else {
            // ---- 收招：缓慢回位
            float p = ease((t - w - 6.0F) / 10.0F);
            this.body.xRot = lerp(both ? 0.45F : 0.36F, 0.0F, p);
            this.body.yRot = lerp(-0.12F * mirror, 0.0F, p);
            if (both) {
                this.armL.xRot = lerp(1.55F, 0.0F, p);
                this.armR.xRot = lerp(1.55F, 0.0F, p);
                this.body.yScale = lerp(0.96F, 1.0F, p);
            } else if (left) {
                this.armL.xRot = lerp(1.5F, 0.0F, p);
                this.armL.zRot = lerp(-0.15F, 0.0F, p);
            } else {
                this.armR.xRot = lerp(1.5F, 0.0F, p);
                this.armR.zRot = lerp(0.15F, 0.0F, p);
            }
            this.legL.xRot = lerp(0.3F, 0.0F, p);
        }
        // 阵纹被动作带起来
        this.shardA.yRot += this.body.yRot * 0.9F;
        this.shardB.yRot += this.body.yRot * 0.6F;
        this.shardC.yRot += this.body.yRot * 0.35F;
    }

    // ================================================================ 射线：蓄力→发射→持续→收招
    private void poseRay(float t) {
        if (t < 16.0F) {
            float p = ease(t / 16.0F);
            this.armR.xRot = lerp(0.0F, -1.45F, p);
            this.armR.zRot = lerp(0.0F, -0.18F, p);
            this.body.xRot = lerp(0.0F, -0.06F, p);
        } else if (t < 18.0F) {
            float p = ease((t - 16.0F) / 2.0F);
            this.armR.xRot = lerp(-1.45F, -1.7F, p);
            this.body.xRot = lerp(-0.06F, -0.28F, p);
            this.armL.xRot = lerp(0.0F, 0.3F, p);
        } else if (t < 28.0F) {
            float j = Mth.sin(t * 1.7F) * 0.035F;
            this.armR.xRot = -1.7F + j;
            this.body.xRot = -0.28F + j * 0.5F;
            this.armL.xRot = 0.3F;
        } else {
            float p = ease((t - 28.0F) / 6.0F);
            this.armR.xRot = lerp(-1.7F, 0.0F, p);
            this.body.xRot = lerp(-0.28F, 0.0F, p);
            this.armL.xRot = lerp(0.3F, 0.0F, p);
        }
    }

    // ================================================================ 冲刺：预备→冲刺→撞击→停顿
    private void poseDash(float t) {
        if (t < 10.0F) {
            float p = ease(t / 10.0F);
            this.body.xRot = lerp(0.0F, 0.4F, p);
            this.legL.xRot = lerp(0.0F, 0.6F, p);
            this.legR.xRot = lerp(0.0F, 0.6F, p);
            this.armL.xRot = lerp(0.0F, -0.8F, p);
            this.armR.xRot = lerp(0.0F, -0.8F, p);
            this.body.yScale = 1.0F - 0.06F * p;
        } else if (t < 16.0F) {
            float p = ease((t - 10.0F) / 6.0F);
            this.body.xRot = lerp(0.4F, 0.75F, p);
            this.armL.xRot = lerp(-0.8F, 1.1F, p);
            this.armR.xRot = lerp(-0.8F, 1.1F, p);
            this.legL.xRot = lerp(0.6F, 0.2F, p);
            this.legR.xRot = lerp(0.6F, 0.2F, p);
        } else if (t < 18.0F) {
            this.body.xRot = 0.4F;
            this.armL.xRot = -0.4F;
            this.armR.xRot = -0.4F;
            this.body.yScale = 1.05F;
        } else {
            float p = ease((t - 18.0F) / 20.0F);
            this.body.xRot = lerp(0.4F, 0.0F, p);
            this.armL.xRot = lerp(-0.4F, 0.0F, p);
            this.armR.xRot = lerp(-0.4F, 0.0F, p);
            this.body.yScale = lerp(1.05F, 1.0F, p);
        }
    }

    // ================================================================ 召唤：抬手→念咒→下压→收招
    private void poseSummon(float t) {
        if (t < 10.0F) {
            float p = ease(t / 10.0F);
            this.armL.zRot = lerp(0.0F, 1.5F, p);
            this.armR.zRot = lerp(0.0F, -1.5F, p);
            this.armL.xRot = lerp(0.0F, -0.9F, p);
            this.armR.xRot = lerp(0.0F, -0.9F, p);
            this.body.xRot = lerp(0.0F, -0.12F, p);
        } else if (t < 30.0F) {
            float j = Mth.sin(t * 2.3F) * 0.03F;
            this.armL.zRot = 1.5F + j;
            this.armR.zRot = -1.5F - j;
            this.armL.xRot = -0.95F;
            this.armR.xRot = -0.95F;
            this.body.xRot = -0.15F + j;
        } else if (t < 32.0F) {
            float p = ease((t - 30.0F) / 2.0F);
            this.armL.zRot = lerp(1.5F, 0.1F, p);
            this.armR.zRot = lerp(-1.5F, -0.1F, p);
            this.armL.xRot = lerp(-0.9F, 0.7F, p);
            this.armR.xRot = lerp(-0.9F, 0.7F, p);
            this.body.xRot = lerp(-0.15F, 0.25F, p);
        } else {
            float p = ease((t - 32.0F) / 10.0F);
            this.armL.xRot = lerp(0.7F, 0.0F, p);
            this.armR.xRot = lerp(0.7F, 0.0F, p);
            this.armL.zRot = lerp(0.1F, 0.0F, p);
            this.armR.zRot = lerp(-0.1F, 0.0F, p);
            this.body.xRot = lerp(0.25F, 0.0F, p);
        }
    }

    // ================================================================ 阵爆：膨胀→合拢→收招
    private void poseBurst(float t) {
        if (t < 20.0F) {
            float p = ease(t / 20.0F);
            float swell = 1.0F + 0.18F * p;
            this.body.xScale = swell;
            this.body.yScale = swell;
            this.body.zScale = swell;
            this.armL.zRot = lerp(0.0F, 1.25F, p);
            this.armR.zRot = lerp(0.0F, -1.25F, p);
            this.armL.xRot = lerp(0.0F, -0.5F, p);
            this.armR.xRot = lerp(0.0F, -0.5F, p);
        } else if (t < 22.0F) {
            float p = ease((t - 20.0F) / 2.0F);
            this.body.xScale = lerp(1.18F, 1.0F, p);
            this.body.yScale = lerp(1.18F, 1.0F, p);
            this.body.zScale = lerp(1.18F, 1.0F, p);
            this.armL.zRot = lerp(1.25F, 0.05F, p);
            this.armR.zRot = lerp(-1.25F, -0.05F, p);
            this.body.xRot = lerp(0.0F, 0.3F, p);
        } else {
            float p = ease((t - 22.0F) / 20.0F);
            this.body.xRot = lerp(0.3F, 0.0F, p);
            this.armL.zRot = lerp(0.05F, 0.0F, p);
            this.armR.zRot = lerp(-0.05F, 0.0F, p);
        }
    }

    // ================================================================ 受伤：后仰 → 复位
    private void poseHurt(float t) {
        float p = t / HURT_TOTAL;
        if (p > 1.0F) {
            return;
        }
        float s = 1.0F - p;
        this.body.xRot = -0.22F * s;
        this.body.yRot = 0.12F * s;
        this.armL.xRot = -0.35F * s;
        this.armR.xRot = -0.45F * s;
        this.armL.zRot = 0.2F * s;
        this.armR.zRot = -0.2F * s;
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer buffer,
                               int packedLight, int packedOverlay,
                               float r, float g, float b, float a) {
        this.root.render(pose, buffer, packedLight, packedOverlay, r, g, b, a);
    }
}
