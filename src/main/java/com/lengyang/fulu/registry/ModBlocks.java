package com.lengyang.fulu.registry;

import java.util.function.Supplier;

import com.lengyang.fulu.FuLu;
import com.lengyang.fulu.block.FormationEyeBlock;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 方块登记表（教程第二步里"专门用来登记方块的文件"）。
 *
 * <p>方块比物品多一层手续：<b>方块和"方块物品"是两样东西</b>。
 * {@code BLOCKS} 里登记的方块只存在于世界里，玩家背包放不下它；
 * 想拿在手里、放进物品栏，还得再登记一个 {@link BlockItem} 指回这个方块。
 * 这就是教程里"同时登记一个对应的方块物品"那一步。
 *
 * <p>方块物品和普通物品用同一个注册表（{@code minecraft:item}），
 * 所以这里另开一个 {@code BLOCK_ITEMS} 而不是塞进 {@link ModItems}：
 * 两个 {@code DeferredRegister} 指向同一个注册表是允许的，
 * 这样方块物品跟着方块一起在 {@code ModBlocks} 里闭环，
 * 也不会出现"ModItems 初始化时 ModBlocks 还没加载"的先后顺序问题。
 */
public final class ModBlocks {

    /** 方块本体登记表。 */
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, FuLu.MOD_ID);

    /** 方块物品登记表（和 {@link ModItems#ITEMS} 是同一个注册表，分开写只是好读）。 */
    public static final DeferredRegister<Item> BLOCK_ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, FuLu.MOD_ID);

    // ===================== 阵法 =====================

    /**
     * 阵眼：一座阵法的核心，代号 {@code formation_eye}，中文名"阵眼"。
     *
     * <p>属性上它是一个**普通完整方块**：能放在任意面上、要镐子才挖得快、
     * 抗爆性比石头高一点，并且自发光（阵眼是活的，暗处能看见它在亮）。
     * 不想让它发光就删掉 {@code lightLevel} 那一行，其余不用改。
     *
     * <p>第三步起它不再是裸的 {@code new Block(...)}，而是
     * {@link FormationEyeBlock} —— 因为要给它挂方块实体和每刻逻辑。
     * 注意方块本身的名字、模型、贴图、语言 key 全都不用改：
     * 加方块实体是代码侧的事，不影响任何资源路径。
     */
    public static final RegistryObject<Block> FORMATION_EYE = BLOCKS.register("formation_eye",
            () -> new FormationEyeBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(3.5F, 9.0F)
                    .lightLevel(state -> 7)
                    .sound(SoundType.DEEPSLATE_TILES)
                    .requiresCorrectToolForDrops()));

    /**
     * 阵眼的方块物品 —— 有它才能拿在手里、放进物品栏、被 {@code /give} 出来。
     *
     * <p>注意名字必须和方块一模一样（都是 {@code formation_eye}），
     * 否则方块和物品会变成两个不相干的东西：能放下的方块拿不起来，
     * 能拿起来的物品放不下去。
     */
    public static final RegistryObject<Item> FORMATION_EYE_ITEM =
            registerBlockItem("formation_eye", FORMATION_EYE);

    /**
     * 阵基：阵法占地用的底座，代号 {@code formation_base}，中文名"阵基"。
     *
     * <p>它是**纯粹的装饰/结构方块**，和阵眼完全不同：
     * <ul>
     *   <li>没有自己的类 —— 就是一个 {@code new Block(...)}，因为它不需要任何行为；</li>
     *   <li>没有方块实体、没有 tick、没有方块状态属性（连 {@code facing} 都没有）；</li>
     *   <li>但它<b>必须有掉落表</b>（{@code data/fulu/loot_tables/blocks/formation_base.json}）
     *       —— 没有掉落表的方块挖掉之后什么都不掉，这是新手最常踩的一个坑
     *       （方块能放、能看见，就是挖不出来东西）。</li>
     * </ul>
     * 属性按"石砖"抄：一样的硬度、一样的挖掘等级，镐子挖得快、徒手慢。
     */
    public static final RegistryObject<Block> FORMATION_BASE = BLOCKS.register("formation_base",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(1.5F, 6.0F)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()));

    /** 阵基的方块物品。名字必须和方块一致（都是 {@code formation_base}）。 */
    public static final RegistryObject<Item> FORMATION_BASE_ITEM =
            registerBlockItem("formation_base", FORMATION_BASE);

    private ModBlocks() {
    }

    /** 给方块配一个同名方块物品。 */
    private static RegistryObject<Item> registerBlockItem(String name, Supplier<? extends Block> block) {
        return BLOCK_ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    /** 由模组主文件 FuLu 调用，把两张登记表挂到模组事件总线上。 */
    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        BLOCK_ITEMS.register(modBus);
    }
}
