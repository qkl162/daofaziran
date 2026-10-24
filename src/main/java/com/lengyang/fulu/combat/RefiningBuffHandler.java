package com.lengyang.fulu.combat;

import com.lengyang.fulu.FuLu;
import com.lengyang.fulu.registry.ModMobEffects;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

import java.util.UUID;

/**
 * 炼妖 Buff 的实际行为。
 *
 * <p>六个 Buff 里四个不需要按键：
 * <ul>
 *   <li><b>腐化之力</b>（僵尸）：你的攻击附带 3 秒饥饿；</li>
 *   <li><b>精准</b>（骷髅）：你射出的箭会追踪最近的敌人；</li>
 *   <li><b>蛛行</b>（蜘蛛）：移动速度 +20%，并且能贴墙爬；</li>
 *   <li><b>烈火</b>（烈焰人）：免疫火焰伤害，攻击点燃目标。</li>
 * </ul>
 * 另两个（爆炸狂怒、空间跳跃）需要按键，在 {@code RefiningKeyHandler} 里。
 */
@Mod.EventBusSubscriber(modid = FuLu.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RefiningBuffHandler {

    /** 蛛行的移速修饰符。 */
    private static final UUID SPIDER_SPEED_ID = UUID.fromString("9c2f1a54-6b3d-4e77-8f21-77b0c2d51a44");
    private static final double SPIDER_SPEED_BONUS = 0.20D;

    /** 爬墙时每刻往上给的推力。 */
    private static final double CLIMB_PUSH = 0.22D;

    /** 精准：水平追踪半径（格）。用户要求 20 格。 */
    private static final double HOMING_RADIUS = 20.0D;

    /** 精准：垂直容差（格）。上下各 5 格。 */
    private static final double HOMING_VERTICAL = 5.0D;

    private RefiningBuffHandler() {
    }

    /** 玩家匣里有没有这种妖丹（能力来源统一改成匣内妖丹）。 */
    private static boolean has(Player player, com.lengyang.fulu.refining.EssenceKind kind) {
        return com.lengyang.fulu.refining.RefiningCast.hasEssence(player, kind);
    }

    // ---------------------------------------------------------------- 攻击附加
    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        // 烈火：匣里有烈焰妖丹 -> 免疫火焰伤害
        LivingEntity victim = event.getEntity();
        if (victim instanceof Player p
                && has(p, com.lengyang.fulu.refining.EssenceKind.BLAZE)
                && event.getSource().is(DamageTypeTags.IS_FIRE)) {
            event.setCanceled(true);
            return;
        }

        // 攻击方是带 Buff 的玩家 —— 处理"攻击附带"
        if (!(event.getSource().getEntity() instanceof Player attacker)) {
            return;
        }
        LivingEntity target = event.getEntity();

        boolean zombie = has(attacker, com.lengyang.fulu.refining.EssenceKind.ZOMBIE);
        boolean blaze = has(attacker, com.lengyang.fulu.refining.EssenceKind.BLAZE);
        if (zombie) {
            // 腐化之力：3 秒饥饿
            target.addEffect(new MobEffectInstance(MobEffects.HUNGER, 60, 0), attacker);
        }
        if (blaze) {
            // 烈火：点燃 4 秒
            target.setSecondsOnFire(4);
        }

        if (attacker.level() instanceof ServerLevel sl && (zombie || blaze)) {
            float[] c = zombie
                    ? new float[]{0.42F, 0.72F, 0.25F}
                    : new float[]{1.00F, 0.62F, 0.18F};
            sl.sendParticles(new DustParticleOptions(new Vector3f(c[0], c[1], c[2]), 1.0F),
                    target.getX(), target.getY() + target.getBbHeight() * 0.6D, target.getZ(),
                    6, 0.3D, 0.3D, 0.3D, 0.01D);
        }
    }

    // ---------------------------------------------------------------- 每刻：爬墙 / 速度 / 箭追踪
    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.level.isClientSide) {
            return;
        }
        if (!(event.level instanceof ServerLevel level)) {
            return;
        }

        for (Player player : level.players()) {
            handleSpider(player);
            handleHomingArrows(level, player);
        }
    }

    /** 蛛行：+20% 移速（瞬时修饰符）与贴墙攀爬。 */
    private static void handleSpider(Player player) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        // 现在蛛行是**开关式效果**（Z 键切换），所以判定看效果而不是看匣
        boolean active = player.hasEffect(ModMobEffects.SPIDER_WALK.get());
        if (speed != null) {
            boolean hasMod = speed.getModifier(SPIDER_SPEED_ID) != null;
            if (active && !hasMod) {
                speed.addTransientModifier(new AttributeModifier(
                        SPIDER_SPEED_ID, "fulu_spider_walk", SPIDER_SPEED_BONUS,
                        AttributeModifier.Operation.MULTIPLY_TOTAL));
            } else if (!active && hasMod) {
                speed.removeModifier(SPIDER_SPEED_ID);
            }
        }
        // 爬墙动作交给客户端（服务端改玩家速度无效），这里只保留移速加成
    }

    /** 精准：玩家射出的箭会拐向最近的敌对生物。 */
    private static void handleHomingArrows(ServerLevel level, Player owner) {
        if (!has(owner, com.lengyang.fulu.refining.EssenceKind.SKELETON)) {
            return;
        }
        // 范围放到 64 格（约等于渲染距离），并只追**玩家视野内**的生物
        for (AbstractArrow arrow : level.getEntitiesOfClass(AbstractArrow.class,
                owner.getBoundingBox().inflate(64.0D))) {
            if (!arrow.isAlive() || arrow.getOwner() != owner) {
                continue;
            }
            Vec3 look = owner.getLookAngle();

            // ---- 追踪模式：照无尽贪婪「天堂陨落长弓」的追踪箭（TraceArrowEntity）
            //
            // 关键差别：**开弓时锁定一次目标，之后一直追**，而不是每刻重新挑最近的。
            // 这样箭的飞行轨迹是一条稳定的曲线，不会在两个目标之间来回摆。
            // 锁定的目标存在箭的持久数据里（`FuluHomingTarget`）。
            java.util.UUID targetId = arrow.getPersistentData().hasUUID("FuluHomingTarget")
                    ? arrow.getPersistentData().getUUID("FuluHomingTarget") : null;
            LivingEntity best = null;
            if (targetId != null && level.getEntity(targetId) instanceof LivingEntity locked
                    && locked.isAlive() && locked instanceof Enemy) {
                best = locked;                        // 锁定的目标还活着，继续追
            }

            if (best == null) {
                // 还没锁定（或目标死了）：以箭为中心 20 格内挑"最接近箭前进方向"的那只
                Vec3 dir = arrow.getDeltaMovement().normalize();
                double bestScore = -2.0D;
                for (LivingEntity cand : level.getEntitiesOfClass(LivingEntity.class,
                        arrow.getBoundingBox().inflate(HOMING_RADIUS))) {
                    if (cand == owner || !(cand instanceof Enemy) || !cand.isAlive()) {
                        continue;
                    }
                    Vec3 to = cand.position().add(0.0D, cand.getBbHeight() * 0.5D, 0.0D)
                            .subtract(arrow.position());
                    double dist = to.length();
                    if (dist > HOMING_RADIUS || dist < 0.6D) {
                        continue;
                    }
                    // 用"方向相似度"排序：飞得最正的那只优先
                    double score = to.normalize().dot(dir);
                    if (score <= bestScore) {
                        continue;
                    }
                    // 视线被方块挡住的不锁
                    net.minecraft.world.phys.HitResult line = level.clip(
                            new net.minecraft.world.level.ClipContext(
                                    arrow.position(),
                                    cand.position().add(0.0D, cand.getBbHeight() * 0.5D, 0.0D),
                                    net.minecraft.world.level.ClipContext.Block.COLLIDER,
                                    net.minecraft.world.level.ClipContext.Fluid.NONE, arrow));
                    if (line.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
                        continue;
                    }
                    bestScore = score;
                    best = cand;
                }
                if (best != null) {
                    arrow.getPersistentData().putUUID("FuluHomingTarget", best.getUUID());
                }
            }
            if (best == null) {
                continue;
            }

            // 每刻把速度对准目标身体中心；速度保底 **3.0**（用户要求）
            Vec3 body = best.position().add(0.0D, best.getBbHeight() * 0.5D, 0.0D);
            Vec3 want = body.subtract(arrow.position()).normalize();
            double speed = Math.max(3.0D, arrow.getDeltaMovement().length());
            arrow.setDeltaMovement(want.scale(speed));
            arrow.hurtMarked = true;
            arrow.setNoGravity(true);

            level.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANTED_HIT,
                    arrow.getX(), arrow.getY(), arrow.getZ(), 1, 0.05D, 0.05D, 0.05D, 0.0D);
        }
    }
}
