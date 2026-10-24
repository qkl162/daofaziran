package com.lengyang.fulu.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * 滑地标记：站在滑地阵里的怪物身上的记号。
 *
 * <p>纯标记，不带每刻逻辑。真正干活的是 {@code SlipperyHandler}：
 * 监听实体每刻，如果带这个标记，就把"上一刻的位移"当成惯性继续推它 ——
 * 于是它会滑过头、转弯困难、被击退滑得更远。
 *
 * <p>为什么不直接改摩擦力：Minecraft 没有"每实体摩擦力"这种东西。
 * 用"保留位移"来模拟，效果一样而且不跟怪物自己的 AI 打架。
 */
public class SlipperyMarkEffect extends MobEffect {

    public SlipperyMarkEffect() {
        super(MobEffectCategory.NEUTRAL, 0x80D0FF);      // 冰蓝
    }
}
