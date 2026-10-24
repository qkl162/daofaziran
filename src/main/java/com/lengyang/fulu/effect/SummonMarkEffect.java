package com.lengyang.fulu.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * 招魂标记：站在招魂阵里的怪物身上的记号。
 *
 * <p>纯标记，不带每刻逻辑。真正干活的是 {@code SummonSoulHandler}：
 * 监听生物死亡，如果死的怪物带这个标记，就在死亡位置生成一个"魂"。
 */
public class SummonMarkEffect extends MobEffect {

    public SummonMarkEffect() {
        super(MobEffectCategory.NEUTRAL, 0x80E0E0);      // 淡青，和魂同色
    }
}
