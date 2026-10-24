package com.lengyang.fulu.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** 「阴火」标记：被阴火缠身的怪物，**每次攻击都会反伤自己**（判定在 LivingAttackEvent）。 */
public class NetherflameEffect extends MobEffect {

    public NetherflameEffect() {
        super(MobEffectCategory.HARMFUL, 0x7A3FA8);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return false;
    }
}
