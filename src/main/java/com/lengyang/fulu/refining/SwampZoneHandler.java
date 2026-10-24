package com.lengyang.fulu.refining;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.lengyang.fulu.FuLu;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 粘液沼泽（史莱姆妖丹）：视线前方一片**半径 3.5、持续 32 秒**的泥沼。
 *
 * <p>怪物在里面：
 * <ul>
 *   <li><b>水平速度被死死拉住</b>（移速属性 -90%，比每刻乘 0.1 更稳）；</li>
 *   <li><b>跳不起来</b>：每 tick 结束把正向 Y 速度清零；</li>
 *   <li><b>偶尔"拔不出腿"</b>：每 10 刻有 1 刻把速度整体清零；</li>
 *   <li><b>寻路瘫痪</b>：每 tick `navigation.stop()`，只会原地打转；</li>
 *   <li><b>窒息</b>：连续待满 60 刻后每 40 刻（2 秒）受 **4 点真实伤害**（魔法伤害，无视护甲）。</li>
 * </ul>
 *
 * <p>玩家在里面：
 * <ul>
 *   <li>不潜行 → 移速 **-40%**（深一脚浅一脚）；</li>
 *   <li>按着 Shift → **无视阻力**，并提示「正在施展轻功水上漂」+ 脚下白色水纹。</li>
 * </ul>
 *
 * <p><b>为什么用属性而不是每刻乘速度</b>：{@code LivingTickEvent} 在 {@code aiStep} 之前，
 * 那里改速度会被 AI 立刻覆盖（这个坑在时缓阵上踩过一次）。
 * 属性修饰符与时序无关，位移则放到 tick 末尾处理。
 */
public final class SwampZoneHandler {

    /** 区域半径（格）。 */
    public static final double RADIUS = 3.5D;
    /** 持续时间（刻）：32 秒。 */
    public static final int DURATION = 640;

    /** 怪物移速乘数（-90%）。 */
    private static final double MOB_SLOW = -0.90D;
    /** 玩家移速乘数（-40%）。 */
    private static final double PLAYER_SLOW = -0.40D;

    /** 窒息开始时间 / 间隔（刻）。 */
    private static final int SUFFOCATE_AFTER = 60;
    private static final int SUFFOCATE_EVERY = 40;
    private static final float SUFFOCATE_DAMAGE = 4.0F;

    private static final UUID MOB_ID = UUID.fromString("2a71c9e0-6b14-4f4a-9b3b-1c7a5d2e8f10");
    private static final UUID PLAYER_ID = UUID.fromString("8d4f3c21-9a7e-4b62-b1d5-6e0f3a2c7b44");

    /** 每个维度里的泥沼。 */
    private static final Map<ResourceKey<Level>, List<Zone>> ZONES = new HashMap<>();

    /** 一个泥沼：中心 + 结束时刻。 */
    private record Zone(Vec3 center, long endTick) {
    }

    private SwampZoneHandler() {
    }

    /** 生成一片泥沼（在视线前方的地面上）。 */
    public static void spawn(ServerLevel level, Vec3 center) {
        ZONES.computeIfAbsent(level.dimension(), k -> new ArrayList<>())
                .add(new Zone(center, level.getGameTime() + DURATION));
        // **可见范围**：生成一个泥沼光环实体（绿色地面盘 + 亮边界环）
        var aura = new com.lengyang.fulu.entity.FireFieldEntity(
                com.lengyang.fulu.registry.ModEntities.FIRE_FIELD.get(), level);
        aura.setPos(center.x, center.y + 0.05D, center.z);
        aura.setup(1, DURATION);
        level.addFreshEntity(aura);

        // 落地"吧唧"一声
        level.playSound(null, net.minecraft.core.BlockPos.containing(center),
                SoundEvents.SLIME_BLOCK_PLACE, SoundSource.PLAYERS, 1.4F, 0.6F);
        level.sendParticles(ParticleTypes.ITEM_SLIME,
                center.x, center.y + 0.2D, center.z, 40, 1.2D, 0.2D, 1.2D, 0.02D);
        FuLu.LOGGER.info("[swamp] spawned at {} for {} ticks", center, DURATION);
    }

    /** 这个实体是否在任意泥沼里（供暴击判定）。 */
    public static boolean isInSwamp(net.minecraft.world.entity.Entity entity) {
        var zones = ZONES.get(entity.level().dimension());
        return zones != null && inside(zones, entity.position());
    }

    /** 某个位置是否在任意泥沼里。 */
    private static boolean inside(List<Zone> zones, Vec3 pos) {
        for (Zone z : zones) {
            if (Math.abs(pos.y - z.center().y) > 2.0D) {
                continue;
            }
            double dx = pos.x - z.center().x;
            double dz = pos.z - z.center().z;
            if (dx * dx + dz * dz <= RADIUS * RADIUS) {
                return true;
            }
        }
        return false;
    }

    /** 每 tick 结束处理（服务端）。 */
    public static void tick(ServerLevel level) {
        List<Zone> zones = ZONES.get(level.dimension());
        if (zones == null) {
            return;
        }
        zones.removeIf(z -> level.getGameTime() >= z.endTick());
        if (zones.isEmpty()) {
            ZONES.remove(level.dimension());
            return;
        }

        for (Zone z : zones) {
            Vec3 c = z.center();
            // **泥沼粒子**（加强版）：每刻都撒，多层、多色
            // 1) 脚踝高度的深绿气泡（泥在咕嘟咕嘟冒泡）
            for (int i = 0; i < 12; i++) {
                double a = level.random.nextDouble() * Math.PI * 2.0D;
                double r = Math.sqrt(level.random.nextDouble()) * RADIUS;
                level.sendParticles(ParticleTypes.ITEM_SLIME,
                        c.x + Math.cos(a) * r, c.y + 0.12D, c.z + Math.sin(a) * r,
                        1, 0.0D, 0.02D, 0.0D, 0.0D);
            }
            // 2) 上升的绿色雾团（有体积感）
            for (int i = 0; i < 5; i++) {
                double a = level.random.nextDouble() * Math.PI * 2.0D;
                double r = Math.sqrt(level.random.nextDouble()) * RADIUS;
                level.sendParticles(ParticleTypes.ENTITY_EFFECT,
                        c.x + Math.cos(a) * r, c.y + 0.4D + level.random.nextDouble() * 1.2D,
                        c.z + Math.sin(a) * r,
                        1, 0.0D, 0.04D, 0.0D, 0.0D);
            }
            // 3) 边界上的亮点（让范围边界一直在"流动"）
            if (level.getGameTime() % 2 == 0) {
                for (int i = 0; i < 16; i++) {
                    double a = Math.PI * 2.0D * i / 16.0D
                            + level.getGameTime() * 0.02D;
                    level.sendParticles(ParticleTypes.GLOW,
                            c.x + Math.cos(a) * RADIUS, c.y + 0.15D,
                            c.z + Math.sin(a) * RADIUS,
                            1, 0.0D, 0.02D, 0.0D, 0.0D);
                }
            }
            // 4) 偶尔溅起泥点
            if (level.getGameTime() % 5 == 0) {
                level.sendParticles(ParticleTypes.SPLASH,
                        c.x, c.y + 0.2D, c.z,
                        8, RADIUS * 0.6D, 0.2D, RADIUS * 0.6D, 0.02D);
            }

            AABB box = AABB.ofSize(c, RADIUS * 2.0D, 4.0D, RADIUS * 2.0D);
            for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box)) {
                applyTo(level, entity, zones);
            }
        }
    }

    private static void applyTo(ServerLevel level, LivingEntity entity, List<Zone> zones) {
        boolean in = inside(zones, entity.position());

        if (entity instanceof Player player) {
            AttributeInstance attr = player.getAttribute(Attributes.MOVEMENT_SPEED);
            boolean sneaking = player.isShiftKeyDown();
            if (attr != null) {
                boolean has = attr.getModifier(PLAYER_ID) != null;
                if (in && !sneaking && !has) {
                    attr.addTransientModifier(new AttributeModifier(PLAYER_ID, "fulu_swamp",
                            PLAYER_SLOW, AttributeModifier.Operation.MULTIPLY_TOTAL));
                } else if ((!in || sneaking) && has) {
                    attr.removeModifier(PLAYER_ID);
                }
            }
            // 轻功提示 + 白色水纹
            if (in && sneaking) {
                player.displayClientMessage(
                        Component.translatable("msg.fulu.swamp.walk_on_water"), true);
                if (level.getGameTime() % 2 == 0) {
                    level.sendParticles(ParticleTypes.SPLASH,
                            player.getX(), player.getY() + 0.05D, player.getZ(),
                            3, 0.35D, 0.0D, 0.35D, 0.0D);
                }
            }
            return;
        }

        if (!(entity instanceof Mob mob)) {
            return;
        }

        AttributeInstance attr = mob.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attr != null) {
            boolean has = attr.getModifier(MOB_ID) != null;
            if (in && !has) {
                attr.addTransientModifier(new AttributeModifier(MOB_ID, "fulu_swamp",
                        MOB_SLOW, AttributeModifier.Operation.MULTIPLY_TOTAL));
            } else if (!in && has) {
                attr.removeModifier(MOB_ID);
            }
        }

        if (!in) {
            mob.getPersistentData().putInt("FuluSwampTicks", 0);
            return;
        }

        // 寻路瘫痪
        mob.getNavigation().stop();

        Vec3 v = mob.getDeltaMovement();
        // **重力坍缩**：额外向下速度（约三倍重力）
        if (v.y <= 0.0D) {
            mob.setDeltaMovement(v.x, v.y - 0.16D, v.z);
        } else {
            // 允许起跳，但起跳瞬间标记"摔伤"
            mob.getPersistentData().putBoolean("FuluSwampJumped", true);
        }
        // 拔不出腿：每 10 刻卡住 1 刻
        if (level.getGameTime() % 10 == 0) {
            mob.setDeltaMovement(0.0D, mob.getDeltaMovement().y, 0.0D);
        }

        // **跳起来落地摔伤**
        if (mob.onGround() && mob.getPersistentData().getBoolean("FuluSwampJumped")
                && mob.fallDistance > 0.5F) {
            mob.getPersistentData().putBoolean("FuluSwampJumped", false);
            mob.invulnerableTime = 0;
            mob.hurt(level.damageSources().fall(), 6.0F);
        }

        // 窒息计时
        int ticks = mob.getPersistentData().getInt("FuluSwampTicks") + 1;
        mob.getPersistentData().putInt("FuluSwampTicks", ticks);
        if (ticks >= SUFFOCATE_AFTER && (ticks - SUFFOCATE_AFTER) % SUFFOCATE_EVERY == 0) {
            mob.invulnerableTime = 0;
            mob.hurt(level.damageSources().magic(), SUFFOCATE_DAMAGE);
            level.sendParticles(ParticleTypes.SQUID_INK,
                    mob.getX(), mob.getY() + mob.getBbHeight() + 0.3D, mob.getZ(),
                    10, 0.25D, 0.15D, 0.25D, 0.01D);
            level.playSound(null, mob.blockPosition(), SoundEvents.SLIME_SQUISH_SMALL,
                    SoundSource.HOSTILE, 0.7F, 0.6F);
        }
    }
}
