package com.lengyang.fulu.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * 嫁祸标记：**站在嫁祸阵里的生物**身上的记号（怪物和玩家都算）。
 *
 * <p>纯标记，不带任何每刻逻辑 —— 真正干活的是
 * {@code com.lengyang.fulu.combat.ShiftBlameHandler}（监听受伤事件，把一半伤害转给别人）。
 * 和"内讧""反重力"是同一个分工：效果当标记，事件干活。
 *
 * <p>为什么用状态效果而不是玩家数据：这个标记要同时挂在**怪物和玩家**身上，
 * 状态效果天然两端都有、也会自动同步；玩家数据只覆盖玩家。而且它不需要显示给谁看，
 * 所以 {@code showIcon=false}、{@code ambient=true}（不占状态栏位置）。
 */
public class ShiftBlameMarkEffect extends MobEffect {

    public ShiftBlameMarkEffect() {
        // NEUTRAL：它既不是增益也不是减益 —— 对站在阵里的人是好是坏取决于谁在打谁
        super(MobEffectCategory.NEUTRAL, 0x8A4FB5);      // 紫罗兰色，和粒子的颜色一致
    }
}
