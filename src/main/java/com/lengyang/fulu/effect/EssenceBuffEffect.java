package com.lengyang.fulu.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * 炼妖 Buff 的通用效果类。
 *
 * <p>这六个 Buff 的行为都写在 {@code RefiningBuffHandler} 里（监听事件时读效果判断），
 * 所以效果本身只需要"是个 BUFF、有颜色、有图标"。
 */
public class EssenceBuffEffect extends MobEffect {

    public EssenceBuffEffect(int color) {
        super(MobEffectCategory.BENEFICIAL, color);
    }
}
