package com.lengyang.fulu.registry;

import com.lengyang.fulu.FuLu;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;

/**
 * 模型层登记：给"烘焙好的模型"起个名字。
 *
 * <p>实体模型不是每次渲染都重新搭一遍，而是启动时**烘焙**一次，
 * 之后靠 {@code ModelLayerLocation} 这个名字去取（见
 * {@link com.lengyang.fulu.client.render.FlyingSwordRenderer} 里的 {@code bakeLayer}）。
 *
 * <p>⚠️ 这个类和它引用的客户端类型是**纯客户端**的：专用服务端不能碰。
 * 所以引用它的地方只有 {@code ModClientSetup}（那里有 {@code Dist.CLIENT} 把关）。
 * 常量本身只是两个字符串，放在公共包里不会导致加载客户端类 ——
 * 但 {@code ModelLayerLocation} 这个类型本身是客户端的，所以这个类只在客户端被加载。
 */
public final class ModModelLayers {

    /** 飞剑模型那一层。 */
    /** 阵魔模型那一层。 */
    public static final ModelLayerLocation FORMATION_DEMON =
            new ModelLayerLocation(new ResourceLocation(FuLu.MOD_ID, "formation_demon"), "main");

    /** 阵仆复用同一个模型形状，但单独烘焙一层，方便以后单独调整。 */
    public static final ModelLayerLocation FORMATION_MINION =
            new ModelLayerLocation(new ResourceLocation(FuLu.MOD_ID, "formation_minion"), "main");

    public static final ModelLayerLocation FLYING_SWORD =
            new ModelLayerLocation(new ResourceLocation(FuLu.MOD_ID, "flying_sword"), "main");

    private ModModelLayers() {
    }
}
