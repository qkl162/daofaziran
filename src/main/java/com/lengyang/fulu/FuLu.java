package com.lengyang.fulu;

import com.lengyang.fulu.registry.ModBlockEntities;
import com.lengyang.fulu.registry.ModBlocks;
import com.lengyang.fulu.registry.ModEntities;
import com.lengyang.fulu.registry.ModMenus;
import com.lengyang.fulu.registry.ModMobEffects;
import com.lengyang.fulu.registry.ModParticles;
import com.lengyang.fulu.network.ModNetwork;
import com.lengyang.fulu.registry.ModCreativeTabs;
import com.lengyang.fulu.registry.ModItems;
import com.mojang.logging.LogUtils;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * 符箓 (FuLu) —— 模组主入口。
 *
 * <p>这就是教程里说的"模组主文件"：游戏启动时 Forge 会调用这个类的构造方法，
 * 我们在构造方法里把各个"登记表"挂到模组事件总线上，
 * 挂上之后登记表里的东西才会被游戏真正创建出来。
 *
 * <p>注意这里的分工，是本模组所有内容统一的写法：
 * <pre>
 *   registry/ModItems          声明"有哪些物品"，本身不碰事件总线
 *   registry/ModBlocks         声明"有哪些方块 + 方块物品"，同样不碰总线
 *   registry/ModBlockEntities  声明"有哪些方块实体类型"
 *   registry/ModCreativeTabs   声明"创造模式物品栏"
 *   FuLu（本文件）              唯一调用 register(modBus) 的地方
 * </pre>
 * 如果只在 ModItems 里写了 ITEMS.register(...) 却忘了在这里挂总线，
 * 编译不会报错，但进游戏后 {@code /give} 什么都拿不到 —— 这是最常见的新手坑。
 */
@Mod(FuLu.MOD_ID)
public class FuLu {

    /** 模组的命名空间：所有资源路径都写作 {@code fulu:xxx}。 */
    public static final String MOD_ID = "fulu";

    public static final Logger LOGGER = LogUtils.getLogger();

    public FuLu() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        // 让游戏加载"物品登记文件"
        ModItems.register(modBus);
        // 让游戏加载"方块登记文件"（方块本体 + 对应的方块物品两张表）
        ModBlocks.register(modBus);
        // 让游戏加载"方块实体登记文件"（阵眼的运行逻辑靠它绑到方块上）
        ModBlockEntities.register(modBus);
        // 让游戏加载"粒子登记文件"
        ModParticles.register(modBus);
        // 让游戏加载"实体登记文件"（飞剑）
        ModEntities.register(modBus);
        // 让游戏加载"状态效果登记文件"（内讧就是这个表里的）
        ModMobEffects.register(modBus);
        // 让游戏加载"界面登记文件"（图鉴 GUI 的菜单类型）
        ModMenus.register(modBus);
        ModCreativeTabs.register(modBus);
        // 网络通道（替身阵的数据要同步到客户端，才能画血条和碎裂图案）
        ModNetwork.register();
    }
}
