package com.lengyang.fulu.combat;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/**
 * 阵法标记（**不依赖状态效果**）。
 *
 * <p><b>为什么需要这个类</b>：原本我用 {@code MobEffect} 当"在阵法里"的标记。
 * 但很多模组 BOSS（比如灾变的焰魔）重写了 {@code canBeAffected}，**拒绝任何状态效果** ——
 * {@code addEffect} 会静默失败，标记根本挂不上，于是整个阵法对这个 BOSS 无效。
 *
 * <p>改用 {@code getPersistentData()}（实体的 NBT）：任何模组都拦不住，
 * 也不需要注册、不需要同步（纯服务端判断）。
 *
 * <p>用法：阵眼每刻调 {@link #mark}，处理器用 {@link #isMarked} 判断。
 * 时间用的是 {@code level.getGameTime()}，所以离开阵法 3 秒后自动失效。
 */
public final class FormationMarks {

    /** 标记持续时长（刻）。 */
    public static final int DURATION = 60;

    private FormationMarks() {
    }

    /**
     * 是否免疫阵法效果。
     *
     * <p><b>只有末影龙</b>（用户要求）。原因：它有自己的一套飞行路径控制，
     * 距离锁定/边界传送会和它的 AI 打架，表现为抽搐或卡在空中。
     * 凋灵、灾变的焰魔等其它 BOSS **照常生效**。
     *
     * <p>注意末影龙受伤时命中的是 {@code EnderDragonPart}（身体各部件），
     * 所以要先取 {@code getParent()} 再判断。
     */
    public static boolean isImmune(net.minecraft.world.entity.Entity entity) {
        if (entity instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon) {
            return true;
        }

        if (entity instanceof net.minecraft.world.entity.boss.EnderDragonPart part) {
            return part.getParent() instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon;
        }
        return false;
    }

    /**
     * 阵魔接受哪些阵法（用户规格：只有这三种对它有效，且效果减半）。
     *
     * <p>闭环 / 咫尺 / 嫁祸 / 浮空 / 乱神对它**完全无效** —— 那些阵法挂不上标记，
     * 处理器自然不会作用到它身上。
     */
    public static boolean allowsDemon(String key) {
        return SUMMON.equals(key) || SLIPPERY.equals(key) || BLIND.equals(key)
                || TIME_SLOW.equals(key);
    }

    /** 带阵法种类的免疫判定：末影龙全免疫；阵魔只接受 {@link #allowsDemon} 里的几种。 */
    public static boolean isImmune(net.minecraft.world.entity.Entity entity, String key) {
        if (isImmune(entity)) {
            return true;
        }
        if (entity instanceof com.lengyang.fulu.entity.FormationDemonEntity) {
            return !allowsDemon(key);
        }
        return false;
    }

    /** 给实体打上某种阵法的标记（刷新时长）。 */
    public static void mark(Level level, LivingEntity entity, String key) {
        if (level == null || entity == null) {
            return;
        }
        if (isImmune(entity, key)) {
            return;                                  // 完全免疫 / 阵魔不吃这个阵法
        }
        entity.getPersistentData().putLong(key, level.getGameTime() + DURATION);
    }

    /** 这个实体现在是否带着该标记。 */
    public static boolean isMarked(Level level, LivingEntity entity, String key) {
        if (level == null || entity == null) {
            return false;
        }
        return entity.getPersistentData().getLong(key) > level.getGameTime();
    }

    /** 清掉标记。 */
    public static void clear(LivingEntity entity, String key) {
        if (entity != null) {
            entity.getPersistentData().remove(key);
        }
    }

    // ---- 各阵法用的 key（互相独立，免得一个阵法的标记被另一个误读）
    public static final String BLIND = "FuluMarkBlind";
    public static final String SLIPPERY = "FuluMarkSlippery";
    public static final String SUMMON = "FuluMarkSummon";
    public static final String SHIFT_BLAME = "FuluMarkShiftBlame";
    public static final String LOOP = "FuluMarkLoop";
    public static final String DISTANCE = "FuluMarkDistance";
    /** 时缓：怪物变得迟缓（移动 -50%、攻击间隔拉长）。 */
    public static final String TIME_SLOW = "FuluMarkTimeSlow";
    /** 时缓免疫：玩家身上的反向标记，确保时间减速只作用于怪物。 */
    public static final String TIME_SLOW_IMMUNE = "FuluMarkTimeSlowImmune";

    /** 炼妖：怪物死亡时凝聚成妖丹。 */
    public static final String REFINING = "FuluMarkRefining";
}
