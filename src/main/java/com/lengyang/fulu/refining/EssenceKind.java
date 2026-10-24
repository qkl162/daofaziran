package com.lengyang.fulu.refining;

import com.lengyang.fulu.registry.ModItems;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 六种妖丹的统一定义：物品、名字、颜色、冷却。
 *
 * <p>轮盘、施法、冷却、界面提示全都从这里查，避免"加一种妖丹要改五个地方"。
 */
public enum EssenceKind {

    ZOMBIE("zombie", 10, 0.42F, 0.72F, 0.25F),
    SKELETON("skeleton", 10, 0.90F, 0.92F, 0.95F),
    CREEPER("creeper", 12, 0.42F, 0.85F, 0.32F),
    SPIDER("spider", 8, 0.55F, 0.35F, 0.70F),
    ENDERMAN("enderman", 10, 0.62F, 0.22F, 0.80F),
    BLAZE("blaze", 10, 1.00F, 0.62F, 0.18F),
    /** 女巫：诡计毒瓶（抛出滞留毒瓶，毒每秒打 4 次）。 */
    WITCH("witch", 10, 0.35F, 0.55F, 0.25F),
    /** 史莱姆：粘液沼泽（地面泥沼，怪被拽住、玩家可轻功）。 */
    SLIME("slime", 12, 0.55F, 0.85F, 0.45F),
    /** 凋零骷髅：凋零断魂斩（扇形剑气，破甲 + 处决）。 */
    WITHER_SKELETON("wither_skeleton", 12, 0.30F, 0.22F, 0.38F),
    /** 恶魂：陨石天降（延迟 1.5 秒的毁灭性范围打击，冷却 30 秒）。 */
    GHAST("ghast", 30, 0.85F, 0.85F, 0.90F);

    private final String id;
    private final int cooldownSeconds;
    private final float r;
    private final float g;
    private final float b;

    EssenceKind(String id, int cooldownSeconds, float r, float g, float b) {
        this.id = id;
        this.cooldownSeconds = cooldownSeconds;
        this.r = r;
        this.g = g;
        this.b = b;
    }

    public String id() {
        return id;
    }

    /**
     * 这个妖丹有几个技能（子技能 id 数组）。
     *
     * <p>目前只有恶魂是**多技能**：陨石 / 飞行；其余妖丹只有一个技能。
     */
    public String[] skills() {
        if (this == GHAST) {
            return new String[]{"meteor", "flight"};
        }
        if (this == SPIDER) {
            // 蜘蛛：盘丝天牢（新）+ 蛛行（旧）
            return new String[]{"web", "climb"};
        }
        return new String[]{"default"};
    }

    /** 读条时长（刻）：凋零骷髅是"拔剑聚气"1 秒，其余 0.5 秒。 */
    public int windupTicks() {
        return this == WITHER_SKELETON ? 20 : 10;
    }

    /** 读条期间的移速惩罚：凋零骷髅 -80%，其余 -50%。 */
    public double castSlow() {
        return this == WITHER_SKELETON ? -0.80D : -0.50D;
    }

    /** 冷却秒数（四舍五入给界面显示用）。 */
    public int cooldownSeconds() {
        return cooldownSeconds;
    }

    /**
     * 冷却刻数：**统一 2 刻（0.1 秒）**（用户要求：所有技能冷却都改成 0.1 秒）。
     */
    public int cooldownTicks() {
        return cooldownTicks(0);
    }

    /**
     * **按技能**取冷却（不同技能各算各的）。
     *
     * @param sub 该妖丹的第几个技能
     */
    public int cooldownTicks(int sub) {
        if (this == GHAST) {
            // 恶魂：陨石 30 秒；飞行只是开关，给 1 秒防连按
            return sub == 1 ? 20 : 600;
        }
        if (this == SPIDER) {
            // 蜘蛛：盘丝天牢 15 秒；蛛行开关 1 秒
            return sub == 0 ? 300 : 20;
        }
        return 2;
    }

    public float red() {
        return r;
    }

    public float green() {
        return g;
    }

    public float blue() {
        return b;
    }

    public String nameKey() {
        return "item.fulu." + id + "_essence";
    }

    public Item item() {
        return switch (this) {
            case ZOMBIE -> ModItems.ESSENCE_ZOMBIE.get();
            case SKELETON -> ModItems.ESSENCE_SKELETON.get();
            case CREEPER -> ModItems.ESSENCE_CREEPER.get();
            case SPIDER -> ModItems.ESSENCE_SPIDER.get();
            case ENDERMAN -> ModItems.ESSENCE_ENDERMAN.get();
            case BLAZE -> ModItems.ESSENCE_BLAZE.get();
            case WITCH -> ModItems.ESSENCE_WITCH.get();
            case SLIME -> ModItems.ESSENCE_SLIME.get();
            case WITHER_SKELETON -> ModItems.ESSENCE_WITHER_SKELETON.get();
            case GHAST -> ModItems.ESSENCE_GHAST.get();
        };
    }

    public static EssenceKind of(int idx) {
        EssenceKind[] all = values();
        return (idx >= 0 && idx < all.length) ? all[idx] : null;
    }

    /** 这个物品是不是妖丹；是就返回种类，不是返回 null。 */
    public static EssenceKind of(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        for (EssenceKind kind : values()) {
            if (stack.is(kind.item())) {
                return kind;
            }
        }
        return null;
    }
}
