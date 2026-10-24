package com.lengyang.fulu.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * 无明标记：站在无明阵里的怪物身上的记号。
 *
 * <p>纯标记。真正干活的是 {@code BlindHandler}：把它的仇恨目标在 2 格外清空、
 * 让它只能靠"声音位置"摸过来。
 */
public class BlindMarkEffect extends MobEffect {

    public BlindMarkEffect() {
        super(MobEffectCategory.NEUTRAL, 0x404050);      // 深灰
    }
}
