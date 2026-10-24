package com.lengyang.fulu.client;

import com.lengyang.fulu.FuLu;
import com.lengyang.fulu.client.model.FlyingSwordModel;
import com.lengyang.fulu.combat.AntiGravityRenderHandler;
import com.lengyang.fulu.client.particle.AntiGravityMoteParticle;
import com.lengyang.fulu.client.particle.RuneMoteParticle;
import com.lengyang.fulu.client.particle.SpiritMoteParticle;
import com.lengyang.fulu.client.particle.SwordBurstParticle;
import com.lengyang.fulu.client.StandInOverlay;
import com.lengyang.fulu.client.render.BloodPoolRenderer;
import com.lengyang.fulu.client.render.FormationBarrierRenderer;
import com.lengyang.fulu.client.render.SoulRenderer;
import com.lengyang.fulu.registry.ModEntities;
import com.lengyang.fulu.client.render.FlyingSwordRenderer;
import com.lengyang.fulu.client.screen.CodexScreen;
import com.lengyang.fulu.registry.ModEntities;
import com.lengyang.fulu.registry.ModMenus;
import com.lengyang.fulu.registry.ModModelLayers;
import com.lengyang.fulu.registry.ModParticles;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * 客户端专属的登记。
 *
 * <p>{@code @Mod.EventBusSubscriber} 上的 {@code value = Dist.CLIENT} 是关键：
 * 专用服务端（dedicated server）里根本没有贴图、没有渲染管线，
 * 这个类连同 {@link SpiritMoteParticle} 都不该被加载。加上这个参数之后，
 * Forge 只在客户端注册这个事件监听器，服务端连类都不会碰 ——
 * 少写它，服务端启动时会直接 {@code NoClassDefFoundError}。
 *
 * <p>{@code bus = Bus.MOD} 是因为粒子 provider 属于"加载期登记"，
 * 走的是模组事件总线，不是游戏事件总线。
 */
@Mod.EventBusSubscriber(modid = FuLu.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ModClientSetup {

    private ModClientSetup() {
    }

    /** 把 id 和"怎么画"接起来；不接就是服务端发了包、客户端画不出来。 */
    @SubscribeEvent
    public static void onRegisterParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.SPIRIT_MOTE.get(), SpiritMoteParticle.Provider::new);
        // 命中爆发的小星星
        event.registerSpriteSet(ModParticles.SWORD_BURST.get(), SwordBurstParticle.Provider::new);
        // 绕着飞剑转的符文
        event.registerSpriteSet(ModParticles.RUNE_MOTE.get(), RuneMoteParticle.Provider::new);
        // 反重力：每刻只出反重力粒子（升空/按住移动在 AntiGravityTickHandler）
        event.registerSpriteSet(ModParticles.ANTI_GRAVITY.get(), AntiGravityMoteParticle.Provider::new);
    }

    /**
     * 实体渲染器 + 模型层：**实体要在世界里显示出来，这两样缺一不可**。
     *
     * <p>少了渲染器：飞剑实体存在、会飞、会打人，但屏幕上什么都没有
     * （而且不报错，最难查的那种）。
     */
    /** 实体的属性注册（血量/护甲/伤害）。漏了会 NPE 或血量变成 0。 */
    @SubscribeEvent
    public static void onEntityAttributes(
            net.minecraftforge.event.entity.EntityAttributeCreationEvent event) {
        event.put(ModEntities.FORMATION_DEMON.get(),
                com.lengyang.fulu.entity.FormationDemonEntity.createAttributes().build());
        event.put(ModEntities.FORMATION_MINION.get(),
                com.lengyang.fulu.entity.FormationMinionEntity.createAttributes().build());
        // 假身：**必须**注册默认属性，否则构造 LivingEntity 时 AttributeSupplier 为 null 直接崩
        // （crash-2026-10-22_14.22.56-server.txt 就是这个）
        event.put(ModEntities.DECOY.get(),
                com.lengyang.fulu.entity.DecoyEntity.createAttributes().build());
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.FLYING_SWORD.get(), FlyingSwordRenderer::new);
        // 血泊：贴地的一张四边形，不需要模型层（所以下面只给飞剑注册了 LayerDefinition）
        event.registerEntityRenderer(ModEntities.BLOOD_POOL.get(), BloodPoolRenderer::new);
        // 招魂阵的魂：广告牌幽灵。**漏了这条会崩** —— EntityRenderDispatcher 找不到渲染器就 NPE。
        event.registerEntityRenderer(ModEntities.SOUL.get(), SoulRenderer::new);
        // 阵魔与阵仆
        event.registerEntityRenderer(ModEntities.FORMATION_DEMON.get(),
                com.lengyang.fulu.client.render.FormationDemonGeoRenderer::new);
        // 盘丝天牢
        event.registerEntityRenderer(ModEntities.WEB_PRISON.get(),
                com.lengyang.fulu.client.render.WebPrisonRenderer::new);
        // 陨石火海
        event.registerEntityRenderer(ModEntities.FIRE_FIELD.get(),
                com.lengyang.fulu.client.render.FireFieldRenderer::new);
        // 陨石碎块
        event.registerEntityRenderer(ModEntities.DEBRIS.get(),
                com.lengyang.fulu.client.render.DebrisRenderer::new);
        // 陨石
        event.registerEntityRenderer(ModEntities.METEOR.get(),
                com.lengyang.fulu.client.render.MeteorRenderer::new);
        // 光翼气场 / 地面裂纹 / 命中爆点
        event.registerEntityRenderer(ModEntities.LIGHT_WING_AURA.get(),
                com.lengyang.fulu.client.render.LightWingAuraRenderer::new);
        event.registerEntityRenderer(ModEntities.GROUND_CRACK.get(),
                com.lengyang.fulu.client.render.GroundCrackRenderer::new);
        event.registerEntityRenderer(ModEntities.HIT_BURST.get(),
                com.lengyang.fulu.client.render.HitBurstRenderer::new);
        // 诛仙血阵剑气：三角形网格月牙
        event.registerEntityRenderer(ModEntities.BLOOD_WAVE.get(),
                com.lengyang.fulu.client.render.BloodWaveRenderer::new);
        // 假身：不可见实体（空渲染器，但**必须**注册，否则客户端会崩）
        event.registerEntityRenderer(ModEntities.DECOY.get(),
                com.lengyang.fulu.client.render.DecoyRenderer::new);
        // 阵仆改用 GeckoLib 模型（复用阵魔骨骼，缩到 0.42 倍）
        event.registerEntityRenderer(ModEntities.FORMATION_MINION.get(),
                com.lengyang.fulu.client.render.FormationMinionGeoRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ModModelLayers.FLYING_SWORD, FlyingSwordModel::createBodyLayer);
        event.registerLayerDefinition(ModModelLayers.FORMATION_DEMON,
                com.lengyang.fulu.client.model.FormationDemonModel::createBodyLayer);
        // 假身用"玩家形状"的模型：HumanoidModel 的 UV 布局和玩家皮肤完全一致
        event.registerLayerDefinition(com.lengyang.fulu.client.render.DecoyRenderer.LAYER,
                () -> net.minecraft.client.model.geom.builders.LayerDefinition.create(
                        net.minecraft.client.model.HumanoidModel.createMesh(
                                net.minecraft.client.model.geom.builders.CubeDeformation.NONE, 0.0F),
                        64, 64));
        event.registerLayerDefinition(ModModelLayers.FORMATION_MINION,
                com.lengyang.fulu.client.model.FormationDemonModel::createBodyLayer);
    }

    /**
     * 客户端启动：图鉴界面的接线 + 反重力倒立回调的显式注册。
     *
     * <p>把"菜单类型"和"界面的类"接起来 —— 这一行就是图鉴 GUI 的最后一环。
     *
     * <p>服务端开界面时发的是菜单类型的注册 id；客户端收到之后凭这个 id 来这里查
     * 该 new 哪个 {@code Screen}。漏了这一行，表现是"右键之后界面一闪、什么都不开"
     * 或者干脆没反应（而且日志里有 "Unknown menu type" 之类的抱怨）。
     */
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // enqueueWork：MenuScreens.register 必须在客户端主线程上做
        event.enqueueWork(() -> MenuScreens.register(ModMenus.CODEX.get(), CodexScreen::new));
        event.enqueueWork(() -> MenuScreens.register(ModMenus.ESSENCE_BOX.get(),
                com.lengyang.fulu.client.screen.EssenceBoxScreen::new));
        // 反重力"倒立"的渲染回调：显式注册，不依赖注解扫描（扫描是静默失败的，
        // 没命中就什么都不发生 —— 而"怪物没倒立"排查到最后只剩这一环可疑）
        AntiGravityRenderHandler.register();
        // 替身阵的 HUD（附加血条 + 全屏碎裂图案）：同样显式注册，理由同上
        StandInOverlay.register();
        // 阵魔的华丽血条（自定义贴图，替代原版 BossBar）
        DemonBossBarOverlay.register();
        // ???????????????????????
        FormationBarrierRenderer.register();
        // 三阶段天空压暗
        com.lengyang.fulu.client.render.DemonSkyDarkener.register();
        // 招魂阵的魂
    }
}

