package com.lengyang.fulu.registry;

import com.lengyang.fulu.FuLu;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * 创造模式物品栏。
 *
 * <p>教程的六步里没写这一步，但不写的话符纸只能靠 {@code /give fulu:talisman_paper}
 * 拿到，创造模式里翻遍所有页签都找不到它 —— 对"让游戏里出现符纸"这个目标来说，
 * 加一个自己的页签才算真的"出现"了。
 *
 * <p><b>两处地方都要加</b>，这是踩过的坑：
 * <ol>
 *   <li>自己的页签（{@link #MAIN}）—— 这里列全，包括<b>反重力符</b>；</li>
 *   <li>原版的页签（{@link #onBuildContents}）—— 材料进"材料"页、方块进"建筑方块"页、
 *       符和图鉴进"工具与实用物品"页。</li>
 * </ol>
 * 只做第 1 条时，玩家如果在原版那些页签里找（那是绝大多数人的习惯），
 * 会得出"这个模组在创造模式里没有物品"的结论 —— 东西其实都在，只是不在他看的那一页。
 */
@Mod.EventBusSubscriber(modid = FuLu.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, FuLu.MOD_ID);

    /** 本模组自己的页签："符箓"。 */
    public static final RegistryObject<CreativeModeTab> MAIN = TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.fulu.main"))
                    .icon(() -> new ItemStack(ModItems.TALISMAN_PAPER.get()))
                    .displayItems((params, output) -> {
                        // 材料
                        output.accept(ModItems.TALISMAN_PAPER.get());
                        output.accept(ModItems.CINNABAR.get());
                        output.accept(ModItems.BEAST_BLOOD.get());
                        // 三种符 + 替身符
                        output.accept(ModItems.CHAOS_TALISMAN.get());
                        output.accept(ModItems.FLYING_SWORD_TALISMAN.get());
                        output.accept(ModItems.ANTI_GRAVITY_TALISMAN.get());
                        output.accept(ModItems.STAND_IN_TALISMAN.get());
                        output.accept(ModItems.SHIFT_BLAME_TALISMAN.get());
                        output.accept(ModItems.SUMMON_TALISMAN.get());
                        output.accept(ModItems.SLIPPERY_TALISMAN.get());
                        output.accept(ModItems.BLIND_TALISMAN.get());
                        output.accept(ModItems.LOOP_TALISMAN.get());
                        output.accept(ModItems.DISTANCE_TALISMAN.get());
                        output.accept(ModItems.INK_TALISMAN.get());
                        output.accept(ModItems.TIME_SLOW_TALISMAN.get());
                        output.accept(ModItems.REFINING_TALISMAN.get());
                        output.accept(ModItems.FORMATION_DEMON_SPAWN_EGG.get());
                        // 工具
                        output.accept(ModItems.FORMATION_CODEX.get());
                        // 方块
                        output.accept(ModBlocks.FORMATION_BASE_ITEM.get());
                        output.accept(ModBlocks.FORMATION_EYE_ITEM.get());
                    })
                    .build());

    /** 单独的"妖丹"页签（用户要求：妖丹单独一个页签）。 */
    public static final RegistryObject<CreativeModeTab> ESSENCES = TABS.register("essences",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.fulu.essences"))
                    .icon(() -> new ItemStack(ModItems.ESSENCE_ENDERMAN.get()))
                    .displayItems((params, output) -> {
                        output.accept(ModItems.ESSENCE_BOX.get());
                        output.accept(ModItems.ESSENCE_ZOMBIE.get());
                        output.accept(ModItems.ESSENCE_SKELETON.get());
                        output.accept(ModItems.ESSENCE_CREEPER.get());
                        output.accept(ModItems.ESSENCE_SPIDER.get());
                        output.accept(ModItems.ESSENCE_ENDERMAN.get());
                        output.accept(ModItems.ESSENCE_BLAZE.get());
                        output.accept(ModItems.ESSENCE_WITCH.get());
                        output.accept(ModItems.ESSENCE_SLIME.get());
                        output.accept(ModItems.ESSENCE_WITHER_SKELETON.get());
                        output.accept(ModItems.ESSENCE_GHAST.get());
                    })
                    .build());

    private ModCreativeTabs() {
    }

    public static void register(IEventBus modBus) {
        TABS.register(modBus);
    }

    /**
     * 把本模组的物品也放进**原版**那几个页签里。
     *
     * <p>为什么必须做：玩家翻创造模式时先看的几乎总是"材料 / 工具 / 建筑方块"，
     * 不会想到去看最后一页多出来的那个陌生页签。只放在自己的页签里，
     * 很容易被当成"这个模组没有物品"。
     *
     * <p>分类照原版的习惯来：
     * <ul>
     *   <li>符纸、朱砂 → 材料；</li>
     *   <li>三种符、阵法图鉴 → 工具与实用物品；</li>
     *   <li>阵眼、阵基 → 建筑方块。</li>
     * </ul>
     */
    @SubscribeEvent
    public static void onBuildContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(ModItems.TALISMAN_PAPER.get());
            event.accept(ModItems.CINNABAR.get());
            event.accept(ModItems.BEAST_BLOOD.get());
        } else if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ModItems.CHAOS_TALISMAN.get());
            event.accept(ModItems.FLYING_SWORD_TALISMAN.get());
            event.accept(ModItems.ANTI_GRAVITY_TALISMAN.get());
            event.accept(ModItems.STAND_IN_TALISMAN.get());
            event.accept(ModItems.SHIFT_BLAME_TALISMAN.get());
            event.accept(ModItems.SUMMON_TALISMAN.get());
            event.accept(ModItems.SLIPPERY_TALISMAN.get());
            event.accept(ModItems.BLIND_TALISMAN.get());
            event.accept(ModItems.LOOP_TALISMAN.get());
            event.accept(ModItems.DISTANCE_TALISMAN.get());
            event.accept(ModItems.INK_TALISMAN.get());
            event.accept(ModItems.REFINING_TALISMAN.get());
            event.accept(ModItems.FORMATION_CODEX.get());
        } else if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(ModBlocks.FORMATION_BASE_ITEM.get());
            event.accept(ModBlocks.FORMATION_EYE_ITEM.get());
        }
    }
}
