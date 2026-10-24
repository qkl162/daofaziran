package com.lengyang.fulu.registry;

import com.lengyang.fulu.FuLu;
import com.lengyang.fulu.menu.CodexMenu;

import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 容器界面（菜单）登记表。
 *
 * <p>界面的"身份"也要注册 —— 服务端开界面时发的是这个 id，客户端凭它决定
 * 该开哪一个 {@code Screen}。没注册的表现是"右键之后什么都没发生"。
 *
 * <p>用 {@link IForgeMenuType#create} 而不是原版的 {@code new MenuType(...)}：
 * 它允许客户端工厂多收一个 {@code FriendlyByteBuf}，也就是**开界面时可以带数据**。
 * 这里带的就是"这次是图鉴还是阵眼"。
 */
public final class ModMenus {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, FuLu.MOD_ID);

    public static final RegistryObject<MenuType<CodexMenu>> CODEX =
            MENUS.register("codex", () -> IForgeMenuType.create(CodexMenu::new));

    /** 妖丹匣：6 个只放妖丹的槽位。 */
    public static final RegistryObject<MenuType<com.lengyang.fulu.menu.EssenceBoxMenu>> ESSENCE_BOX =
            MENUS.register("essence_box",
                    () -> IForgeMenuType.create(com.lengyang.fulu.menu.EssenceBoxMenu::new));

    private ModMenus() {
    }

    public static void register(IEventBus modBus) {
        MENUS.register(modBus);
    }
}
