package com.lengyang.fulu.block;

import javax.annotation.Nullable;

import com.lengyang.fulu.registry.ModItems;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 阵眼里装的是哪种符。
 *
 * <p>第九步时这里还是一个 boolean（{@code hasTalisman}）—— "有符 / 没符"。
 * 现在符有两种，boolean 不够用了，于是换成这个枚举。换了之后：
 * <ul>
 *   <li><b>方块状态</b>从"两种"变成"三种"（{@code talisman=empty|chaos|sword}），
 *       每种对应一套外观；</li>
 *   <li><b>方块实体</b>存的是种类而不是真假；</li>
 *   <li>取符时知道该还哪一种；</li>
 *   <li>每刻逻辑只对<b>乱神符</b>生效（飞剑符现在只是能装上去，效果另说）。</li>
 * </ul>
 *
 * <p>实现 {@link StringRepresentable} 是为了让方块状态里写的是
 * {@code talisman=chaos} 这种小写名字，而不是 {@code CHAOS}。
 */
public enum TalismanKind implements StringRepresentable {

    /** 空的。 */
    EMPTY("empty"),
    /** 乱神符：阵内的怪物互相攻击。 */
    CHAOS("chaos"),
    /** 飞剑符：每秒射出一轮飞剑。 */
    SWORD("sword"),
    /** 反重力符：阵内的怪物浮空。 */
    ANTIGRAVITY("antigravity"),
    /** 替身符：阵内的玩家获得"替身"标记，致命伤替他死一次。 */
    STAND_IN("stand_in"),
    /** 嫁祸符：阵内的生物受伤时，一半伤害转给阵内另一只随机生物。 */
    SHIFT_BLAME("shift_blame"),
    /** 招魂符：阵内怪物死亡时生成"魂"，魂自动攻击其他怪物并形成连锁。 */
    SUMMON("summon"),
    /** 滑地符：阵内怪物像踩在冰上，保留惯性、转弯困难、被击退滑更远。 */
    SLIPPERY("slippery"),
    /** 无明符：阵内怪物几乎失明，只能靠声音找玩家。 */
    BLIND("blind"),
    /** 闭环符：阵内怪物走到边界就被传送到对面，永远走不出去。 */
    LOOP("loop"),
    /** 咫尺符：锁住怪物与玩家的距离，怪物永远到不了身边。 */
    DISTANCE("distance"),
    /**
     * 水墨符：不改变阵内的战斗规则，改变的是<b>阵壁的观感</b>。
     *
     * <p>装上之后半球屏障从纯色星空换成半透明的水墨山水画，
     * 内外双面可见，墨迹缓慢晕开（渲染在 {@code InkBarrierLayer}）。
     * 它是第一个"只管看、不管打"的符，所以 {@code FormationFieldEffects}
     * 那边不需要为它加战斗逻辑。
     */
    INK("ink"),
    /**
     * 时缓符：阵内的**怪物**时间变慢（移动 -50%、攻击间隔拉长），投射物被拖慢；
     * 阵内的**玩家**不受影响（挂"时缓免疫"标记，减速逻辑只对 Mob 生效）。
     */
    TIME_SLOW("time_slow"),
    /**
     * 炼妖符：阵内怪物死亡后凝聚**妖丹**，玩家食用妖丹获得该怪物的能力（15 秒）。
     */
    REFINING("refining");

    private final String name;

    TalismanKind(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    /** 这种符对应的物品；空手状态没有物品（返回 null）。 */
    @Nullable
    public Item item() {
        return switch (this) {
            case EMPTY -> null;
            case CHAOS -> ModItems.CHAOS_TALISMAN.get();
            case SWORD -> ModItems.FLYING_SWORD_TALISMAN.get();
            case ANTIGRAVITY -> ModItems.ANTI_GRAVITY_TALISMAN.get();
            case STAND_IN -> ModItems.STAND_IN_TALISMAN.get();
            case SHIFT_BLAME -> ModItems.SHIFT_BLAME_TALISMAN.get();
            case SUMMON -> ModItems.SUMMON_TALISMAN.get();
            case SLIPPERY -> ModItems.SLIPPERY_TALISMAN.get();
            case BLIND -> ModItems.BLIND_TALISMAN.get();
            case LOOP -> ModItems.LOOP_TALISMAN.get();
            case DISTANCE -> ModItems.DISTANCE_TALISMAN.get();
            case INK -> ModItems.INK_TALISMAN.get();
            case TIME_SLOW -> ModItems.TIME_SLOW_TALISMAN.get();
            case REFINING -> ModItems.REFINING_TALISMAN.get();
        };
    }

    /**
     * 手里拿的东西是哪种符；不是符就是 {@link #EMPTY}。
     *
     * <p><b>这里故意不写 if 链，而是回头问 {@link #item()}</b> —— 这是踩过的坑：
     * 原来这里是
     * <pre>
     *   if (stack.is(乱神符)) return CHAOS;
     *   if (stack.is(飞剑符)) return SWORD;
     *   return EMPTY;
     * </pre>
     * 加反重力符时，{@code item()} 是 {@code switch}（编译器会报"漏了一个分支"，
     * 所以我改了），而这个 if 链**编译器不管**，于是漏了 —— 手里拿着反重力符
     * 被当成"没拿符"，右键阵眼自然装不上去。
     *
     * <p>现在只有一处列举符的种类（{@link #item()}），这里只是反过来查一遍：
     * 以后再加第四种、第五种符，只改 {@code item()} 就不可能再对不上。
     */
    public static TalismanKind of(ItemStack stack) {
        if (stack.isEmpty()) {
            return EMPTY;
        }
        for (TalismanKind kind : values()) {
            Item item = kind.item();
            if (item != null && stack.is(item)) {
                return kind;
            }
        }
        return EMPTY;
    }

    /** 从存档里的名字找回来（认不出来就当空的，别让旧存档炸掉）。 */
    public static TalismanKind byName(String name) {
        for (TalismanKind kind : values()) {
            if (kind.name.equals(name)) {
                return kind;
            }
        }
        return EMPTY;
    }

    /** "阵法状态"那一行用的文案 key。 */
    public String stateKey() {
        return "msg.fulu.formation_eye.state." + name;
    }
}
