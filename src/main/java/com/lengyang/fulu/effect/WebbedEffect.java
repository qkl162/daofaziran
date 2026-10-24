package com.lengyang.fulu.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * 「被困」：盘丝天牢把怪物吊起来时挂在它身上的标记效果。
 *
 * <p>它**没有数值作用**（伤害/破甲都由 {@code WebPrisonEntity} 直接结算），
 * 存在的唯一目的是在怪物状态栏里显示一个**白丝网图标**，让玩家一眼看出谁被吊住了。
 *
 * <p>为什么需要单独一个类：{@code MobEffect} 的构造器是 `protected`，
 * 不能在注册表里直接 `new`，必须继承。
 */
public class WebbedEffect extends MobEffect {

    public WebbedEffect() {
        super(MobEffectCategory.HARMFUL, 0xF2F2F2);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return false;                       // 不做任何周期结算
    }
}
