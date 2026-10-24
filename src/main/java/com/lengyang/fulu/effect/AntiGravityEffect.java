package com.lengyang.fulu.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * 反重力：**纯粹的负面标记效果**。
 *
 * <p>按用户要求，它是负面效果（{@link MobEffectCategory#HARMFUL}）—— 在阵法的
 * 判定里它算"对怪物不利"。它本身不掉血、不改属性，只是"你正在反重力阵里"的记号，
 * 另外两件事按它来做事：
 * <ul>
 *   <li>{@code AntiGravityTickHandler}（服务端每刻）看到标记就抬它升空、按住移动、保留攻击；</li>
 *   <li>{@code AntiGravityRenderHandler}（客户端渲染）看到标记就把模型上下翻 180 度。</li>
 * </ul>
 *
 * <p><b>这个类里一行逻辑都没有</b>，是刻意的：阵法内的时长是**无限**（{@code duration = -1}），
 * 而原版对无限时长的效果不保证每刻调用 {@code applyEffectTick} ——
 * 把"每刻要做的事"挂在一个可能不 tick 的东西上太脆，所以全部搬到上面那两个事件里。
 * 效果只负责"存在"。
 */
public class AntiGravityEffect extends MobEffect {

    public AntiGravityEffect() {
        // HARMFUL = 负面效果（原版把"对生物不利"的效果归在这一类，
        // /effect 的提示颜色、能否被牛奶解除等都按它来）
        super(MobEffectCategory.HARMFUL, 0xB48CF0);      // 淡紫，和粒子、图标一致
    }
}
