package com.lengyang.fulu.refining;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.entity.player.Player;

/**
 * 玩家的"炼化"数据（**永久存**在玩家身上）。
 *
 * <p>两项：
 * <ul>
 *   <li><b>已炼化列表</b>：吃过哪些妖丹（永久，重复吃不会重复记录）；</li>
 *   <li><b>当前激活槽位</b>：轮盘里选中的是第几颗（0~5）。</li>
 * </ul>
 *
 * <p><b>为什么用 {@code getPersistentData()} 而不是自己建 attachment</b>：
 * 玩家持久数据本来就会随存档保存、随玩家实体一起加载，不需要额外注册和迁移；
 * 而且它能被 {@code /data} 命令读写，测试里可以直接看。
 */
public final class RefiningPlayerData {

    private static final String KEY_LEARNED = "FuluEssenceLearned";
    private static final String KEY_ACTIVE = "FuluEssenceActive";

    private RefiningPlayerData() {
    }

    private static CompoundTag tag(Player player) {
        return player.getPersistentData();
    }

    /** 记录"已经炼化过这种妖丹"。 */
    public static void learn(Player player, EssenceKind kind) {
        if (kind == null) {
            return;
        }
        CompoundTag root = tag(player);
        ListTag list = root.getList(KEY_LEARNED, 8);      // 8 = StringTag
        for (int i = 0; i < list.size(); i++) {
            if (kind.id().equals(list.getString(i))) {
                return;                                    // 已经记过
            }
        }
        list.add(StringTag.valueOf(kind.id()));
        root.put(KEY_LEARNED, list);
    }

    public static boolean hasLearned(Player player, EssenceKind kind) {
        if (kind == null) {
            return false;
        }
        ListTag list = tag(player).getList(KEY_LEARNED, 8);
        for (int i = 0; i < list.size(); i++) {
            if (kind.id().equals(list.getString(i))) {
                return true;
            }
        }
        return false;
    }

    /** 已炼化的种类（按枚举顺序）。 */
    public static List<EssenceKind> learned(Player player) {
        List<EssenceKind> out = new ArrayList<>();
        for (EssenceKind kind : EssenceKind.values()) {
            if (hasLearned(player, kind)) {
                out.add(kind);
            }
        }
        return out;
    }

    public static int activeSlot(Player player) {
        return tag(player).getInt(KEY_ACTIVE);
    }

    public static void setActiveSlot(Player player, int slot) {
        if (slot < 0 || slot > 5) {
            return;
        }
        tag(player).putInt(KEY_ACTIVE, slot);
    }
}
