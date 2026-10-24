package com.lengyang.fulu.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * 内讧（负面效果）—— <b>一个纯标记，本身不掉血、不改属性</b>。
 *
 * <p>它存在的意义是"能看得出来"：站在阵法里的怪物身上会挂着这个图标，
 * 走出来的怪物身上就没有。玩家一眼就知道哪些怪已经被阵眼影响了，
 * 别的模组/逻辑也可以用 {@code hasEffect(INFIGHTING)} 来问"这只怪在内讧吗"。
 *
 * <p>为什么单独建一个文件而不是在别处 new 一个 {@code MobEffect}：
 * <ul>
 *   <li>效果是一个<b>要注册的东西</b>（有 id、有图标、有语言文件条目），
 *       它的"身份"和阵眼是两回事，混在一起以后加第二个效果就要拆；</li>
 *   <li>以后要给它加行为（比如"内讧中的怪物互相攻击伤害提高"），
 *       改这一个文件就够了 —— 不用去动阵眼那份很长的逻辑。</li>
 * </ul>
 *
 * <p>⚠️ 关于"不掉血"：{@code MobEffect} 有两个会被游戏<b>周期性调用</b>的钩子
 * （{@code applyEffectTick} / {@code isDurationEffectTick}），
 * 喝生命恢复、中毒就是在那里面扣血/回血的。这里<b>一个都不覆盖</b>，
 * 所以它每刻被调用时什么也不做 —— "不掉血"不是靠参数调出来的，是靠**没写那段代码**。
 *
 * <p>颜色用紫色（{@code 0x8B2FC9}），和乱神符那套配色一致；
 * 分类是 {@link MobEffectCategory#HARMFUL}（负面），所以状态栏边框是红的 ——
 * "红框 + 紫图标"正好是你要的红色或紫色。
 */
public class InfightingEffect extends MobEffect {

    /** 图标的颜色（ARGB 里省掉 alpha，只写 RGB）。紫色，和乱神符同色系。 */
    private static final int COLOR = 0x8B2FC9;

    public InfightingEffect() {
        super(MobEffectCategory.HARMFUL, COLOR);
    }

    // 这里是"故意留空"的地方：
    //
    //   @Override public void applyEffectTick(...)      // 没写 = 每刻不做任何事
    //   @Override public boolean isDurationEffectTick() // 没写 = 根本不会被调用
    //
    // 想让它"其实也掉一点血"的时候，才需要回来覆盖这两个方法之一。
}
