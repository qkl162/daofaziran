package com.lengyang.fulu.refining;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * 「阴火」——**状态**，不是 Buff。
 *
 * <p>为什么不用 MobEffect：
 * 原版 MobEffect 会被 `canBeAffected` / 抗性 / 清除效果等手段挡掉，
 * 对 boss 和模组怪并不可靠。这里用**持久数据**强行标记，
 * 同时额外挂一个同名 MobEffect **只用于显示图标**（挂不上也不影响生效）。
 *
 * <p>效果：
 * <ul>
 *   <li>每秒受到 **3% 最大生命**的真实伤害（清无敌帧，必定结算）；</li>
 *   <li>身上有阴火时**每次攻击反伤自己**（见 {@code CastTickHandler}）。</li>
 * </ul>
 */
public final class NetherfireState {

    /** 状态键（持久数据里的到期时刻）。 */
    public static final String TAG = "FuluNetherfire";

    /** 每刻记录一份（用于周期结算）。 */
    private static final List<LivingEntity> TRACKED = new ArrayList<>();

    private NetherfireState() {
    }

    /** 强行施加（无视免疫/抗性）。 */
    public static void apply(LivingEntity e, int ticks) {
        e.getPersistentData().putLong(TAG, e.level().getGameTime() + ticks);
        // 只为显示图标；万一挂不上也不影响状态本身
        e.addEffect(new MobEffectInstance(
                com.lengyang.fulu.registry.ModMobEffects.NETHERFLAME.get(), ticks, 0,
                false, false, true));
        if (!TRACKED.contains(e)) {
            TRACKED.add(e);
        }
    }

    /** 是否处于阴火状态。 */
    public static boolean isMarked(LivingEntity e) {
        return e.getPersistentData().getLong(TAG) > e.level().getGameTime();
    }

    /** 每刻推进：每秒结算 3% 最大生命。 */
    public static void tick(ServerLevel level) {
        if (TRACKED.isEmpty()) {
            return;
        }
        TRACKED.removeIf(e -> e == null || !e.isAlive() || e.level() != level);

        for (LivingEntity e : TRACKED) {
            long expire = e.getPersistentData().getLong(TAG);
            if (expire <= level.getGameTime()) {
                continue;                                  // 到期：等清理
            }
            if (level.getGameTime() % 20 != 0) {
                continue;
            }
            // 3% 最大生命的真实伤害
            float dmg = Math.max(1.0F, e.getMaxHealth() * 0.03F);
            e.invulnerableTime = 0;
            e.hurt(level.damageSources().magic(), dmg);
            if (!e.isAlive()) {
                e.getPersistentData().putBoolean("FuluDoubleDrop", false);
            }
            // 黑紫色阴火粒子
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    e.getX(), e.getY() + e.getBbHeight() * 0.6D, e.getZ(),
                    3, 0.25D, 0.35D, 0.25D, 0.01D);
            level.sendParticles(ParticleTypes.SQUID_INK,
                    e.getX(), e.getY() + e.getBbHeight() * 0.5D, e.getZ(),
                    2, 0.2D, 0.3D, 0.2D, 0.0D);
            com.lengyang.fulu.FuLu.LOGGER.debug("[netherfire] tick {} dmg={}",
                    e.getType(), dmg);
        }
    }
}
