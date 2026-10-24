package com.lengyang.fulu.effect;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * 诡计毒：**每秒攻击 4 次**（每 5 刻掉一次血）。
 *
 * <p>有两个坑必须处理，否则会出现"有时候不掉血"：
 *
 * <ol>
 *   <li><b>无敌帧</b>：{@code LivingEntity.hurt()} 每次生效后会把
 *       {@code invulnerableTime} 设成 10 刻。而我们每 5 刻就要打一次 ——
 *       不把它清零的话，**每隔一跳就被吞掉**，表现就是"时好时坏"。
 *       所以每次结算前先 {@code entity.invulnerableTime = 0}。</li>
 *   <li><b>双重节流</b>：{@code isDurationEffectTick} 已经按
 *       {@code duration % 5} 把跳伤限制到每秒 4 次了，这里**不要**再加
 *       {@code tickCount % 5} —— 两个计数器相位不同，会随机吞掉一些跳伤。</li>
 * </ol>
 */
public class WitchPoisonEffect extends MobEffect {

    /** 每 5 刻结算一次 = 每秒 4 次。 */
    public static final int TICK_INTERVAL = 5;

    /** 每次伤害（每秒 4 次 = 16 点/秒）。 */
    private static final float DAMAGE_PER_TICK = 4.0F;

    public WitchPoisonEffect() {
        super(MobEffectCategory.HARMFUL, 0x3FA34D);
    }

    /** 只有这一处节流：每秒 4 次。 */
    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration % TICK_INTERVAL == 0;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide) {
            return;
        }
        // 关键：清掉无敌帧，否则每 5 刻一次的毒会被 10 刻的受伤无敌吞掉一半
        entity.invulnerableTime = 0;
        entity.hurt(entity.level().damageSources().magic(),
                DAMAGE_PER_TICK * (amplifier + 1));

        if (entity.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.ITEM_SLIME,
                    entity.getX(), entity.getY() + entity.getBbHeight() * 0.5D, entity.getZ(),
                    2, 0.2D, 0.2D, 0.2D, 0.0D);
        }
    }
}
