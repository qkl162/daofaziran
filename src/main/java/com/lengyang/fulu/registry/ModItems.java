package com.lengyang.fulu.registry;

import com.lengyang.fulu.FuLu;
import com.lengyang.fulu.item.FormationCodexItem;

import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 物品登记表（教程第一步里"专门用来登记物品的文件"）。
 *
 * <p>三件事必须对上，否则游戏里会出现紫黑格或者干脆没有这个物品：
 * <ol>
 *   <li>这里 register 的名字 = {@code talisman_paper}；
 *   <li>模型文件放 {@code assets/fulu/models/item/talisman_paper.json}；
 *   <li>贴图放 {@code assets/fulu/textures/item/talisman_paper.png}（16×16）。
 * </ol>
 *
 * <p>另外语言文件里的 key 是 {@code item.fulu.talisman_paper}，
 * 也就是 {@code item.<模组id>.<物品名>}，写错了只会在游戏里显示成
 * "item.fulu.talisman_paper" 这串字，不会崩游戏。
 */
public final class ModItems {

    /** 所有物品都通过这个登记表注册，命名空间固定为 fulu。 */
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, FuLu.MOD_ID);

    // ===================== 制符材料 =====================

    /**
     * 符纸：空白的黄纸，画上朱砂才成符。
     *
     * <p>代号 {@code talisman_paper}，中文名"符纸"写在语言文件里
     * （见 {@code assets/fulu/lang/zh_cn.json}）。
     */
    public static final RegistryObject<Item> TALISMAN_PAPER = ITEMS.register("talisman_paper",
            () -> new Item(new Item.Properties()));

    /**
     * 乱神符：成品的符，画好朱砂、能装到阵眼上。
     *
     * <p>和符纸的区别：符纸是<b>空白材料</b>，乱神符是<b>能用的成品符</b>
     * （第十步第三部分开始，拿着它右键阵眼就能装上去）。
     *
     * <p>它还没有自己的类 —— 只是一个普通物品。等到它需要有"自己的行为"
     * （比如右键天上放个法术），那时再给它建一个 {@code extends Item} 的类，
     * 就像阵眼从"只是个方块"变成"有自己大脑的方块实体"那样。
     */
    public static final RegistryObject<Item> CHAOS_TALISMAN = ITEMS.register("chaos_talisman",
            () -> new Item(new Item.Properties()));

    /**
     * 飞剑符：第二种符，装进阵眼用。
     *
     * <p>它现在**只是能装上去、能取下来**，还没有效果 ——
     * 阵眼的每刻逻辑只认乱神符（{@link com.lengyang.fulu.block.TalismanKind}）。
     * 它的效果等你说，跟当初乱神符先做"能装"、再做"效果"是同一个节奏。
     */
    public static final RegistryObject<Item> FLYING_SWORD_TALISMAN =
            ITEMS.register("flying_sword_talisman", () -> new Item(new Item.Properties()));

    /**
     * 反重力符：装到阵眼上让范围内的怪物浮空。
     *
     * <p>合成：符纸 + 朱砂 + 羽毛 + 潜影壳（潜影壳是"飘"的意象）。
     */
    public static final RegistryObject<Item> ANTI_GRAVITY_TALISMAN =
            ITEMS.register("anti_gravity_talisman", () -> new Item(new Item.Properties()));

    /**
     * 朱砂：红色的矿粉，画符的颜料，也是阵基的料。
     *
     * <p>到这一步它才存在 —— 之前图鉴的配方里用它当材料，但模组里没有这个物品，
     * 所以第十三步先是拿红石粉顶替；现在补上了，配方改回真正的朱砂。
     */
    public static final RegistryObject<Item> CINNABAR = ITEMS.register("cinnabar",
            () -> new Item(new Item.Properties()));

    /**
     * 兽血：暗红的一小瓶，画「乱神符」要用的引子。
     *
     * <p><b>这个物品是这一步新加的。</b>你要的乱神符配方里写着"兽血 ×1"，
     * 但模组里原本没有这个东西（原来只有符纸 / 朱砂 / 乱神符 / 飞剑符 / 反重力符 /
     * 阵眼 / 阵基 / 图鉴）。按"照字面实现"的规矩，我把它做出来了 ——
     * 内部代号 {@code beast_blood}（兽 = beast）。
     *
     * <p><b>它目前没有获取途径</b>：你没说兽血从哪来，我**不自己编**（比如"杀牛掉落"），
     * 所以现在只在创造模式页签里拿得到。要怎么获得，等你定。
     */
    public static final RegistryObject<Item> BEAST_BLOOD = ITEMS.register("beast_blood",
            () -> new Item(new Item.Properties()));

    /**
     * 替身符：第四个阵法「替身阵」的符。
     *
     * <p>合成（有序）：空 符纸 空 / 朱砂 干草块 兽血 / 空 羽毛 空 —— 干草块是"草人"的意象。
     *
     * <p>它跟"抗性提升"效果是一套的：效果本身和减伤/反弹已经做好了
     * （见 {@link com.lengyang.fulu.effect.ResistanceUpEffect} 和
     * {@code ResistanceUpHandler}），**但"这张符怎么把效果给玩家"你还没说** ——
     * 是拿符右键自己？还是装到阵眼里让范围内玩家吃到？等你说，我再接。
     */
    public static final RegistryObject<Item> STAND_IN_TALISMAN =
            ITEMS.register("stand_in_talisman", () -> new Item(new Item.Properties()));

    /**
     * 嫁祸符：第五个阵法「嫁祸阵」的符。
     *
     * <p>合成（有序）：空 符纸 空 / 腐肉 兽血 蜘蛛眼 / 空 朱砂 空。
     * 效果和伤害转移都在 {@code ShiftBlameHandler}，这里只是那张符。
     */
    public static final RegistryObject<Item> SHIFT_BLAME_TALISMAN =
            ITEMS.register("shift_blame_talisman", () -> new Item(new Item.Properties()));

    /**
     * 招魂符：第六个阵法「招魂阵」的符。
     *
     * <p>合成（有序）：空 符纸 空 / 骨头 兽血 灵魂沙 / 空 朱砂 空。
     * 效果在 {@code SummonSoulHandler}（怪物死亡生成魂）。
     */
    public static final RegistryObject<Item> SUMMON_TALISMAN =
            ITEMS.register("summon_talisman", () -> new Item(new Item.Properties()));

    /**
     * 滑地符：第七个阵法「滑地阵」的符。
     *
     * <p>合成（有序）：空 符纸 空 / 蓝冰 兽血 雪球 / 空 朱砂 空。
     * 效果在 {@code SlipperyHandler}（阵内怪物保留惯性、滑出去停不下来）。
     */
    public static final RegistryObject<Item> SLIPPERY_TALISMAN =
            ITEMS.register("slippery_talisman", () -> new Item(new Item.Properties()));

    /**
     * 无明符：第八个阵法「无明阵」的符。
     *
     * <p>合成（有序）：空 符纸 空 / 墨囊 兽血 幻翼膜 / 空 朱砂 空。
     * 效果在 {@code BlindHandler}（阵内怪物几乎失明，只能靠声音找玩家）。
     */
    public static final RegistryObject<Item> BLIND_TALISMAN =
            ITEMS.register("blind_talisman", () -> new Item(new Item.Properties()));

    /**
     * 闭环符：第九个阵法「闭环阵」的符。
     *
     * <p>合成（有序）：空 符纸 空 / 末影珍珠 兽血 紫颂果 / 空 朱砂 空。
     * 效果在 {@code LoopHandler}（阵内怪物走到边界就被传送到对面，永远走不出去）。
     */
    public static final RegistryObject<Item> LOOP_TALISMAN =
            ITEMS.register("loop_talisman", () -> new Item(new Item.Properties()));

    /**
     * 咫尺符：第十个阵法「咫尺天涯阵」的符。
     *
     * <p>合成（有序）：空 符纸 空 / 末影之眼 兽血 紫颂花 / 空 朱砂 空。
     * 效果在 {@code DistanceLockHandler}（锁住怪物与玩家的距离，怪物永远到不了你身边）。
     */
    public static final RegistryObject<Item> DISTANCE_TALISMAN =
            ITEMS.register("distance_talisman", () -> new Item(new Item.Properties()));

    /**
     * 水墨符：第十一个阵法「水墨阵」的符。
     *
     * <p>合成（有序）：空 符纸 空 / 墨囊 兽血 青金石 / 空 朱砂 空。
     * 它和前面十个不一样 —— 别的符管的是"阵内发生什么"，
     * 它改的是<b>阵壁本身长什么样</b>：换上它以后，半球屏障从纯色星空
     * 变成一幅半透明的水墨山水（见 {@code client/render/InkBarrierLayer}）。
     */
    public static final RegistryObject<Item> INK_TALISMAN =
            ITEMS.register("ink_talisman", () -> new Item(new Item.Properties()));

    /** 时缓符：阵内怪物减速、投射物变慢，玩家不受影响。 */
    public static final RegistryObject<Item> TIME_SLOW_TALISMAN =
            ITEMS.register("time_slow_talisman", () -> new Item(new Item.Properties()));

    /** 炼妖符：阵内怪物死亡凝成妖丹。 */
    public static final RegistryObject<Item> REFINING_TALISMAN =
            ITEMS.register("refining_talisman", () -> new Item(new Item.Properties()));

    /** 妖丹匣：收纳 6 颗妖丹，右键打开。 */
    public static final RegistryObject<Item> ESSENCE_BOX =
            ITEMS.register("essence_box", () -> new com.lengyang.fulu.item.EssenceBoxItem(
                    new Item.Properties()));

    // ---- 六种妖丹（可食用，吃掉获得对应怪物能力 15 秒）
    public static final RegistryObject<Item> ESSENCE_ZOMBIE =
            ITEMS.register("zombie_essence", () -> new com.lengyang.fulu.item.EssenceItem(
                    ModMobEffects.ZOMBIE_POWER, "tooltip.fulu.essence.zombie"));
    public static final RegistryObject<Item> ESSENCE_SKELETON =
            ITEMS.register("skeleton_essence", () -> new com.lengyang.fulu.item.EssenceItem(
                    ModMobEffects.PRECISION, "tooltip.fulu.essence.skeleton"));
    public static final RegistryObject<Item> ESSENCE_CREEPER =
            ITEMS.register("creeper_essence", () -> new com.lengyang.fulu.item.EssenceItem(
                    ModMobEffects.BLAST_FURY, "tooltip.fulu.essence.creeper"));
    public static final RegistryObject<Item> ESSENCE_SPIDER =
            ITEMS.register("spider_essence", () -> new com.lengyang.fulu.item.EssenceItem(
                    ModMobEffects.SPIDER_WALK, "tooltip.fulu.essence.spider"));
    public static final RegistryObject<Item> ESSENCE_ENDERMAN =
            ITEMS.register("enderman_essence", () -> new com.lengyang.fulu.item.EssenceItem(
                    ModMobEffects.SPACE_JUMP, "tooltip.fulu.essence.enderman"));
    public static final RegistryObject<Item> ESSENCE_BLAZE =
            ITEMS.register("blaze_essence", () -> new com.lengyang.fulu.item.EssenceItem(
                    ModMobEffects.FLAME_BODY, "tooltip.fulu.essence.blaze"));
    public static final RegistryObject<Item> ESSENCE_WITCH =
            ITEMS.register("witch_essence", () -> new com.lengyang.fulu.item.EssenceItem(
                    ModMobEffects.WITCH_POISON, "tooltip.fulu.essence.witch"));
    public static final RegistryObject<Item> ESSENCE_SLIME =
            ITEMS.register("slime_essence", () -> new com.lengyang.fulu.item.EssenceItem(
                    ModMobEffects.SPIDER_WALK, "tooltip.fulu.essence.slime"));
    public static final RegistryObject<Item> ESSENCE_GHAST =
            ITEMS.register("ghast_essence", () -> new com.lengyang.fulu.item.EssenceItem(
                    ModMobEffects.FLAME_BODY, "tooltip.fulu.essence.ghast"));
    public static final RegistryObject<Item> ESSENCE_WITHER_SKELETON =
            ITEMS.register("wither_skeleton_essence", () -> new com.lengyang.fulu.item.EssenceItem(
                    ModMobEffects.ZOMBIE_POWER, "tooltip.fulu.essence.wither_skeleton"));

    /** 阵魔刷怪蛋。 */
    public static final RegistryObject<Item> FORMATION_DEMON_SPAWN_EGG =
            ITEMS.register("formation_demon_spawn_egg", () -> new net.minecraftforge.common.ForgeSpawnEggItem(
                    com.lengyang.fulu.registry.ModEntities.FORMATION_DEMON,
                    0x2A1F3D, 0xB2222A, new Item.Properties()));

    /** 阵魔核心：阵魔必掉，用于合成高级阵眼/符箓。 */
    public static final RegistryObject<Item> DEMON_CORE =
            ITEMS.register("demon_core", () -> new Item(new Item.Properties()));

    /** 破碎的阵纹：阵魔必掉，用于合成阵基或研究新阵法。 */
    public static final RegistryObject<Item> DEMON_SHARD =
            ITEMS.register("demon_shard", () -> new Item(new Item.Properties()));

    // ===================== 工具 =====================

    /**
     * 阵法图鉴：右键列出"已经解锁的阵法"。
     *
     * <p>它是本模组第一个**有自己类**的物品（{@link FormationCodexItem}）——
     * 因为它要能"右键做事"，光靠 {@code new Item(...)} 做不到。
     * {@code stacksTo(1)}：图鉴是工具，叠一摞没有意义。
     */
    public static final RegistryObject<Item> FORMATION_CODEX = ITEMS.register("formation_codex",
            () -> new FormationCodexItem(new Item.Properties().stacksTo(1)));

    private ModItems() {
    }

    /** 由模组主文件 FuLu 调用，把登记表挂到模组事件总线上。 */
    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }
}
