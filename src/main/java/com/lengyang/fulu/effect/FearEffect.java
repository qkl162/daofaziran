package com.lengyang.fulu.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * 无明「恐惧」：血量低于 30% 时惊慌逃跑。
 *
 * <p>只是标记；实际逃跑行为（随机方向 + 加速）在 {@code BlindHandler} 里做 ——
 * 因为原版没有"逃跑"的 AI 可以直接调用。
 */
public class FearEffect extends MobEffect {

    public FearEffect() {
        super(MobEffectCategory.HARMFUL, 0xA040E0);      // 紫色
    }
}
