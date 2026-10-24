package com.lengyang.fulu.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * 抗性提升：**玩家身上的减伤效果**，支持 I ~ VIII 级。
 *
 * <p>等级越高减伤越高，VII 和 VIII 还额外带 80% 伤害反弹。具体数字和反弹逻辑
 * 不在这个类里 —— 效果本身只负责"存在"，真正改伤害的是
 * {@code com.lengyang.fulu.combat.ResistanceUpHandler}（监听 {@code LivingHurtEvent}）。
 *
 * <p>为什么不在效果里算：{@link MobEffect} 没有"受伤时"的钩子，
 * 只有"每刻 tick"；而减伤必须在**伤害结算的那一刻**改数值。
 * 这条分工和反重力那个效果是同一种思路：效果当标记，事件干活。
 *
 * <p>分类是 {@link MobEffectCategory#BENEFICIAL}（正面 ✓），
 * 所以它和原版的力量、抗性一样显示在状态栏右侧，倒计时和悬停提示由原版自动处理
 * （名字和等级来自语言文件 key {@code effect.fulu.resistance_up}）。
 */
public class ResistanceUpEffect extends MobEffect {

    /** 最高支持的等级（VIII）。超出按 VIII 算。 */
    public static final int MAX_LEVEL = 8;

    /**
     * 持续时长：30 秒 = 600 刻（用户要求）。
     *
     * <p>注意时长是**施加时**写进 {@code MobEffectInstance} 的，效果类自己改不了它。
     * 这个常量存在是为了让"以后施加它的人"（替身符 / 替身阵）用同一个数，
     * 而不是各自写 600。
     */
    public static final int DURATION_TICKS = 600;

    public ResistanceUpEffect() {
        // 淡青绿：和反重力的淡紫区分开，看着像"护盾"
        super(MobEffectCategory.BENEFICIAL, 0x59C8B0);
    }

    /**
     * 某一级减多少伤害（0.20 = 减 20%）。按用户给的表：
     *
     * <pre>
     *   等级   减伤    实际受伤
     *   0      0%      100%
     *   I      20%     80%
     *   II     40%     60%
     *   III    60%     40%
     *   IV     80%     20%
     *   V      100%    0%        ← 从这一级起就是完全免疫
     *   VI     120%    0%
     *   VII    140%    0%
     *   VIII   160%    0%
     * </pre>
     *
     * <p>就是"每级 +20%"这一条直线，写成乘法只有一处数字 —— 用 switch 写八个分支反而
     * 容易漏一个（这个项目里"手写清单漏一项"已经出过两次事故）。
     */
    public static float reductionFor(int level) {
        return 0.20F * level;
    }

    /** VII、VIII 带 80% 反弹。 */
    public static boolean reflects(int level) {
        return level >= 7;
    }

    /** 反弹比例（用户指定 80%）。 */
    public static float reflectRatio() {
        return 0.80F;
    }
}
