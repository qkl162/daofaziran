package com.lengyang.fulu.standin;

/**
 * 客户端侧的替身阵数据缓存。
 *
 * <p>服务端推过来什么就存什么，界面（血条、全屏碎裂图案）只读这里。
 * <b>这个类是纯客户端的</b> —— 服务端不能碰它（会 NoClassDefFoundError），
 * 所以 {@code StandInSyncPacket} 里用 {@code DistExecutor.unsafeRunWhenOn} 间接调用。
 */
public final class ClientStandInData {

    private static boolean hasStandIn;
    private static boolean hasResistance;
    private static int hitShield;

    /** 收到"碎裂"那一刻的本地时间（毫秒），用来播一次动画；0 = 没在播。 */
    private static long shatterStartMillis;

    private ClientStandInData() {
    }

    /** 由网络包调用。 */
    public static void apply(boolean standIn, boolean resistance, int shield, boolean shatter) {
        hasStandIn = standIn;
        hasResistance = resistance;
        hitShield = shield;
        if (shatter) {
            shatterStartMillis = System.currentTimeMillis();
        }
        // 诊断（默认开）：每个同步包都记一行。用户报"绿心不显示"时，这一行能立刻区分
        // "服务端没发" 和 "发了但没画" —— 和反重力倒立那次同一个思路。
        if (DEBUG_LOG) {
            com.lengyang.fulu.FuLu.LOGGER.info(
                    "[standin-sync] received: shield={} standIn={} resistance={} shatter={}",
                    shield, standIn, resistance, shatter);
        }
    }

    /** 诊断开关（默认开：一次同步才一行，不刷屏）。 */
    public static final boolean DEBUG_LOG = true;

    public static boolean hasStandIn() {
        return hasStandIn;
    }

    public static boolean hasResistance() {
        return hasResistance;
    }

    public static int hitShield() {
        return hitShield;
    }

    /**
     * 碎裂动画已经播了多久（秒）；没在播返回 -1。
     *
     * <p>动画总长 1.8 秒（用户要求 1~2 秒），播完自动归零。
     */
    public static float shatterSeconds() {
        if (shatterStartMillis == 0L) {
            return -1.0F;
        }
        float seconds = (System.currentTimeMillis() - shatterStartMillis) / 1000.0F;
        if (seconds > 1.8F) {
            shatterStartMillis = 0L;
            return -1.0F;
        }
        return seconds;
    }
}
