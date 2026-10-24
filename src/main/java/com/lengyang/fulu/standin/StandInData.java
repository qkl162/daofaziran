package com.lengyang.fulu.standin;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * 玩家身上的「替身阵」数据。
 *
 * <p>四个字段（用户要求三个 + 我自己加了一个）：
 * <ol>
 *   <li>{@code hasStandIn}：有没有"替身"标记（替身阵给的）——<b>用户要求</b>；</li>
 *   <li>{@code hasResistance}：有没有"替身阵抗性"标记 ——<b>用户要求</b>；</li>
 *   <li>{@code hitShield}：次数盾点数，初始 0，每挨一下 -4 ——<b>用户要求</b>；</li>
 *   <li>{@code eyePos}：<b>我加的</b>。替死时要让"那一个"阵眼碎掉，光有布尔值不知道是哪个；
 *       玩家同时站在两个替身阵里时，没有这个字段就会碎错阵或者全碎。</li>
 * </ol>
 *
 * <p>存在 {@link Player#getPersistentData()} 里 —— Forge 会把这块数据跟着玩家一起
 * 存进存档（"退出重进不丢"），而且不需要写 capability 那套样板。
 *
 * <p><b>同步</b>：持久数据只存在服务端，客户端看不到。所以每次改动都要显式调
 * {@link #sync(ServerPlayer)} 推一份给那个玩家（画血条和全屏图案要用）。
 */
public final class StandInData {

    /** 次数盾满值（用户要求 20 点，每挨一下 -4，也就是 5 下）。 */
    public static final int SHIELD_FULL = 20;

    /** 每挨一次攻击扣多少点。 */
    public static final int SHIELD_COST_PER_HIT = 4;

    /** 替死之后挂的抗性提升等级（VIII）。 */
    public static final int STAND_IN_RESISTANCE_LEVEL = 8;

    /** 替死之后抗性提升的时长：30 秒。 */
    public static final int STAND_IN_RESISTANCE_TICKS = 600;

    private static final String K_STAND_IN = "FuluStandIn";
    private static final String K_RESISTANCE = "FuluStandInResistance";
    private static final String K_SHIELD = "FuluHitShield";
    private static final String K_EYE_X = "FuluStandInEyeX";
    private static final String K_EYE_Y = "FuluStandInEyeY";
    private static final String K_EYE_Z = "FuluStandInEyeZ";
    private static final String K_SHATTER = "FuluShatterTicks";
    private static final String K_ACTION_BAR = "FuluActionBarTicks";

    private StandInData() {
    }

    private static CompoundTag tag(Player player) {
        return player.getPersistentData();
    }

    // ---------------------------------------------------------------- 替身标记

    public static boolean hasStandIn(Player player) {
        return tag(player).getBoolean(K_STAND_IN);
    }

    /** 挂上/刷新替身标记，并记住是哪个阵眼给的。 */
    public static void setStandIn(Player player, boolean value, BlockPos eye) {
        boolean before = hasStandIn(player);
        BlockPos beforePos = eyePos(player);
        tag(player).putBoolean(K_STAND_IN, value);
        if (value && eye != null) {
            tag(player).putInt(K_EYE_X, eye.getX());
            tag(player).putInt(K_EYE_Y, eye.getY());
            tag(player).putInt(K_EYE_Z, eye.getZ());
        } else if (!value) {
            tag(player).remove(K_EYE_X);
            tag(player).remove(K_EYE_Y);
            tag(player).remove(K_EYE_Z);
        }
        if (before != value || (value && !java.util.Objects.equals(beforePos, eye))) {
            syncIfServer(player);
        }
    }

    /** 给标记的阵眼位置；没有标记时返回 null。 */
    public static BlockPos eyePos(Player player) {
        CompoundTag t = tag(player);
        if (!t.contains(K_EYE_X) || !t.contains(K_EYE_Y) || !t.contains(K_EYE_Z)) {
            return null;
        }
        return new BlockPos(t.getInt(K_EYE_X), t.getInt(K_EYE_Y), t.getInt(K_EYE_Z));
    }

    // ------------------------------------------------------------ 替身阵抗性

    public static boolean hasResistance(Player player) {
        return tag(player).getBoolean(K_RESISTANCE);
    }

    public static void setResistance(Player player, boolean value) {
        if (hasResistance(player) != value) {
            tag(player).putBoolean(K_RESISTANCE, value);
            syncIfServer(player);
        }
    }

    // ---------------------------------------------------------------- 次数盾

    /** 上次扣次数盾的时间（毫秒）—— 用来把"同一波连击"并成一次。 */
    private static final String K_LAST_HIT_MS = "FuluStandInLastHitMs";

    public static long lastHitMs(Player player) {
        return player.getPersistentData().getLong(K_LAST_HIT_MS);
    }

    public static void setLastHitMs(Player player, long ms) {
        player.getPersistentData().putLong(K_LAST_HIT_MS, ms);
    }

    public static int hitShield(Player player) {
        return tag(player).getInt(K_SHIELD);
    }

    public static void setHitShield(Player player, int points) {
        int clamped = Math.max(0, points);
        if (hitShield(player) != clamped) {
            tag(player).putInt(K_SHIELD, clamped);
            syncIfServer(player);
        }
    }

    // -------------------------------------------------- 全屏碎裂标记（几刻后自动清）

    public static int shatterTicks(Player player) {
        return tag(player).getInt(K_SHATTER);
    }

    /** 触发全屏碎裂图案：给一个倒计时，客户端收到就播一次。 */
    public static void triggerShatter(Player player, int ticks) {
        tag(player).putInt(K_SHATTER, ticks);
        syncIfServer(player);
    }

    public static void setShatterTicks(Player player, int ticks) {
        tag(player).putInt(K_SHATTER, ticks);
    }

    // ------------------------------------------- 动作栏提示的剩余刻数（要显示 3~5 秒）

    public static int actionBarTicks(Player player) {
        return tag(player).getInt(K_ACTION_BAR);
    }

    public static void setActionBarTicks(Player player, int ticks) {
        tag(player).putInt(K_ACTION_BAR, ticks);
    }

    // ---------------------------------------------------------------- 同步

    /** 把当前数据推给客户端（只有服务端玩家能推）。 */
    public static void syncIfServer(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            com.lengyang.fulu.network.ModNetwork.sync(serverPlayer);
        }
    }
}
