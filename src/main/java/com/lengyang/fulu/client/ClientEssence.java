package com.lengyang.fulu.client;

/**
 * 客户端记的"当前选中的妖丹格 + 本地冷却"。
 *
 * <p>只用于显示（轮盘上把冷却中的格子画灰、施法键发哪个格子）。
 * 真正的判定在服务端 {@code RefiningCast}，客户端的数字改不了服务端。
 */
public final class ClientEssence {

    private static int activeSlot = 0;
    /** 当前妖丹选中的子技能下标。 */
    private static int subSkill = 0;
    /** 本地冷却结束时间（毫秒，System.currentTimeMillis）。 */
    private static final long[] COOLDOWN_UNTIL = new long[48];
    /** 这次冷却的**总时长**（毫秒），用于算扇形比例。 */
    private static final long[] COOLDOWN_TOTAL = new long[48];

    private ClientEssence() {
    }

    /** 本地切槽（轮盘确认时立刻生效，不等服务端回包）。 */
    public static void setActiveSlotLocal(int slot) {
        if (slot >= 0 && slot < 6) {
            activeSlot = slot;
        }
    }

    public static int subSkill() {
        return subSkill;
    }

    public static void setSubSkill(int index) {
        subSkill = Math.max(0, index);
    }

    public static int activeSlot() {
        return activeSlot;
    }

    public static void setActiveSlot(int slot) {
        if (slot >= 0 && slot < 6) {
            activeSlot = slot;
        }
    }

    /** 剩余冷却比例（1 = 刚进冷却，0 = 已好），HUD 画扇形用。 */
    /** 冷却数组下标（格子 × 8 + 技能）。 */
    private static int cdIndex(int slot, int sub) {
        return Math.max(0, Math.min(5, slot)) * 8 + Math.max(0, Math.min(7, sub));
    }

    public static float cooldownFraction(int slot, int sub) {
        int idx = cdIndex(slot, sub);
        if (COOLDOWN_TOTAL[idx] <= 0L) {
            return 0.0F;
        }
        long left = COOLDOWN_UNTIL[idx] - System.currentTimeMillis();
        if (left <= 0L) {
            return 0.0F;
        }
        return Math.max(0.0F, Math.min(1.0F, left / (float) COOLDOWN_TOTAL[idx]));
    }

    /** 服务端同步来的冷却（刻）。 */
    public static void startCooldownTicks(int slot, int sub, int ticks) {
        int idx = cdIndex(slot, sub);
        long ms = Math.max(50L, ticks * 50L);
        COOLDOWN_UNTIL[idx] = System.currentTimeMillis() + ms;
        COOLDOWN_TOTAL[idx] = ms;
    }

    /** 这一格还要冷却多少秒（向上取整）；0 = 可用。 */
    public static int cooldownSeconds(int slot, int sub) {
        int idx = cdIndex(slot, sub);
        long left = COOLDOWN_UNTIL[idx] - System.currentTimeMillis();
        return left <= 0L ? 0 : (int) Math.ceil(left / 1000.0D);
    }

    public static int cooldownSeconds(int slot) {
        if (slot < 0 || slot > 5) {
            return 0;
        }
        long left = COOLDOWN_UNTIL[slot] - System.currentTimeMillis();
        return left <= 0 ? 0 : (int) Math.ceil(left / 1000.0D);
    }

    public static void startCooldown(int slot, int seconds) {
        startCooldown(slot, subSkill, seconds);
    }

    public static void startCooldown(int slot, int sub, int seconds) {
        startCooldownTicks(slot, sub, seconds * 20);
    }

    // ---------------------------------------------------------------- 妖丹匣查询

    /** 背包里的妖丹匣；没有就返回 EMPTY。 */
    public static net.minecraft.world.item.ItemStack findBox() {
        var player = net.minecraft.client.Minecraft.getInstance().player;
        if (player == null) {
            return net.minecraft.world.item.ItemStack.EMPTY;
        }
        for (net.minecraft.world.item.ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof com.lengyang.fulu.item.EssenceBoxItem) {
                return stack;
            }
        }
        return net.minecraft.world.item.ItemStack.EMPTY;
    }

    /** 匣里有没有这种妖丹。 */
    public static boolean hasEssence(com.lengyang.fulu.refining.EssenceKind kind) {
        return slotOf(kind) >= 0;
    }

    /** 匣里第 slot 格装的是什么妖丹；空或不是妖丹返回 null。 */
    public static com.lengyang.fulu.refining.EssenceKind kindAt(int slot) {
        var box = findBox();
        if (box.isEmpty() || slot < 0 || slot >= 6) {
            return null;
        }
        var container = new com.lengyang.fulu.refining.EssenceBoxContainer(box);
        return com.lengyang.fulu.refining.EssenceKind.of(container.getItem(slot));
    }

    /** 这种妖丹在匣里的第几格；没有返回 -1。 */
    public static int slotOf(com.lengyang.fulu.refining.EssenceKind kind) {
        var box = findBox();
        if (box.isEmpty() || kind == null) {
            return -1;
        }
        var container = new com.lengyang.fulu.refining.EssenceBoxContainer(box);
        for (int i = 0; i < 6; i++) {
            if (kind == com.lengyang.fulu.refining.EssenceKind.of(container.getItem(i))) {
                return i;
            }
        }
        return -1;
    }
}
