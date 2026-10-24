package com.lengyang.fulu.registry;

import com.lengyang.fulu.FuLu;
import com.lengyang.fulu.entity.BloodPoolEntity;
import com.lengyang.fulu.entity.FlyingSwordEntity;
import com.lengyang.fulu.entity.FormationDemonEntity;
import com.lengyang.fulu.entity.FormationMinionEntity;
import com.lengyang.fulu.entity.SoulEntity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 实体登记表。
 *
 * <p>实体类型（{@link EntityType}）和方块/物品一样要登记 —— 它决定
 * "存档里的那个 id 对应哪个类"，也是 {@code /summon} 能被认出来的前提。
 *
 * <p>{@link MobCategory#MISC}：飞剑不是生物，不参与刷怪上限、也不该被当成怪。
 * {@code sized(0.6F, 0.6F)} 是它的碰撞箱（一把剑大概这么粗）；
 * {@code clientTrackingRange(8)} 表示客户端在 8 个区块内会收到它的位置更新 ——
 * 对一个飞得很快的小东西来说够用了，拉大只是白费带宽。
 */
public final class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, FuLu.MOD_ID);

    /** 飞剑。 */
    public static final RegistryObject<EntityType<FlyingSwordEntity>> FLYING_SWORD =
            ENTITIES.register("flying_sword", () -> EntityType.Builder
                    // 显式写出类型参数：构造函数收的是 EntityType<? extends ...>，
                    // 让编译器自己推断会因为通配符而失败。
                    .<FlyingSwordEntity>of(FlyingSwordEntity::new, MobCategory.MISC)
                    .sized(0.6F, 0.6F)
                    .clientTrackingRange(8)
                    .updateInterval(2)
                    .build("flying_sword"));

    /**
     * 血泊：动物被击败后留在原地的那一滩，拿玻璃瓶右键能接出兽血。
     *
     * <p>{@code sized(0.9F, 0.05F)}：**薄薄一层** —— 宽度接近一个方块，高度只有 1/20 格，
     * 所以它躺在地上而不是立着。
     *
     * <p>{@code updateInterval(Integer.MAX_VALUE)}：它永远不动，不需要位置同步 ——
     * 原版对静止实体也是这么省的（写 1 就是每刻发包，几十滩血泊白白占带宽）。
     */
    public static final RegistryObject<EntityType<BloodPoolEntity>> BLOOD_POOL =
            ENTITIES.register("blood_pool", () -> EntityType.Builder
                    .<BloodPoolEntity>of(BloodPoolEntity::new, MobCategory.MISC)
                    .sized(0.9F, 0.05F)
                    .clientTrackingRange(6)
                    .updateInterval(Integer.MAX_VALUE)
                    .build("blood_pool"));

    /**
     * 魂：招魂阵里怪物死亡后留下的幽灵。
     *
     * <p>{@code sized(0.6F, 0.6F)} + 实体的 {@code noPhysics} 让它能穿方块；
     * {@code updateInterval(2)} 是因为它一直在飘，需要比较密的位置同步。
     */
    public static final RegistryObject<EntityType<SoulEntity>> SOUL =
            ENTITIES.register("soul", () -> EntityType.Builder
                    .<SoulEntity>of(SoulEntity::new, MobCategory.MISC)
                    .sized(0.6F, 0.6F)
                    .clientTrackingRange(8)
                    .updateInterval(2)
                    .build("soul"));

    /** 阵魔：阵法系 BOSS。300 血、三阶段、会砸阵眼。 */
    public static final RegistryObject<EntityType<FormationDemonEntity>> FORMATION_DEMON =
            ENTITIES.register("formation_demon", () -> EntityType.Builder
                    .<FormationDemonEntity>of(FormationDemonEntity::new, MobCategory.MONSTER)
                    .sized(1.6F, 3.2F)
                    .clientTrackingRange(10)
                    .updateInterval(2)
                    .build("formation_demon"));

    /** 阵仆：阵魔召唤的小型版本。 */
    /** 盘丝天牢（蛛网牢笼）。 */
    public static final RegistryObject<EntityType<com.lengyang.fulu.entity.WebPrisonEntity>> WEB_PRISON =
            ENTITIES.register("web_prison", () -> EntityType.Builder
                    .<com.lengyang.fulu.entity.WebPrisonEntity>of(
                            com.lengyang.fulu.entity.WebPrisonEntity::new,
                            net.minecraft.world.entity.MobCategory.MISC)
                    .sized(6.0F, 5.0F)
                    .clientTrackingRange(12)
                    .noSummon()
                    .build("web_prison"));

    /** 陨石留下的火海（持续燃烧区域）。 */
    public static final RegistryObject<EntityType<com.lengyang.fulu.entity.FireFieldEntity>> FIRE_FIELD =
            ENTITIES.register("fire_field", () -> EntityType.Builder
                    .<com.lengyang.fulu.entity.FireFieldEntity>of(
                            com.lengyang.fulu.entity.FireFieldEntity::new,
                            net.minecraft.world.entity.MobCategory.MISC)
                    .sized(6.0F, 0.5F)
                    .clientTrackingRange(12)
                    .noSummon()
                    .build("fire_field"));

    /** 陨石碎块（纯视觉小方块）。 */
    public static final RegistryObject<EntityType<com.lengyang.fulu.entity.DebrisEntity>> DEBRIS =
            ENTITIES.register("debris", () -> EntityType.Builder
                    .<com.lengyang.fulu.entity.DebrisEntity>of(
                            com.lengyang.fulu.entity.DebrisEntity::new,
                            net.minecraft.world.entity.MobCategory.MISC)
                    .sized(0.6F, 0.6F)
                    .clientTrackingRange(10)
                    .noSummon()
                    .build("debris"));

    /** 陨石（恶魂妖丹）。 */
    public static final RegistryObject<EntityType<com.lengyang.fulu.entity.MeteorEntity>> METEOR =
            ENTITIES.register("meteor", () -> EntityType.Builder
                    .<com.lengyang.fulu.entity.MeteorEntity>of(
                            com.lengyang.fulu.entity.MeteorEntity::new,
                            net.minecraft.world.entity.MobCategory.MISC)
                    .sized(3.0F, 3.0F)
                    .clientTrackingRange(16)
                    .noSummon()
                    .build("meteor"));

    /** 光翼气场（蓄力/展翼/收拢）。 */
    public static final RegistryObject<EntityType<com.lengyang.fulu.entity.LightWingAuraEntity>> LIGHT_WING_AURA =
            ENTITIES.register("light_wing_aura", () -> EntityType.Builder
                    .<com.lengyang.fulu.entity.LightWingAuraEntity>of(
                            com.lengyang.fulu.entity.LightWingAuraEntity::new,
                            net.minecraft.world.entity.MobCategory.MISC)
                    .sized(1.0F, 2.0F)
                    .clientTrackingRange(10)
                    .noSummon()
                    .build("light_wing_aura"));

    /** 地面金色裂纹（贴地 Decal）。 */
    public static final RegistryObject<EntityType<com.lengyang.fulu.entity.GroundCrackEntity>> GROUND_CRACK =
            ENTITIES.register("ground_crack", () -> EntityType.Builder
                    .<com.lengyang.fulu.entity.GroundCrackEntity>of(
                            com.lengyang.fulu.entity.GroundCrackEntity::new,
                            net.minecraft.world.entity.MobCategory.MISC)
                    .sized(0.5F, 0.1F)
                    .clientTrackingRange(10)
                    .noSummon()
                    .build("ground_crack"));

    /** 命中爆点（环形冲击波 Mesh）。 */
    public static final RegistryObject<EntityType<com.lengyang.fulu.entity.HitBurstEntity>> HIT_BURST =
            ENTITIES.register("hit_burst", () -> EntityType.Builder
                    .<com.lengyang.fulu.entity.HitBurstEntity>of(
                            com.lengyang.fulu.entity.HitBurstEntity::new,
                            net.minecraft.world.entity.MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(10)
                    .noSummon()
                    .build("hit_burst"));

    /** 诛仙血阵的剑气（纯视觉实体，三角形网格渲染）。 */
    public static final RegistryObject<EntityType<com.lengyang.fulu.entity.BloodWaveEntity>> BLOOD_WAVE =
            ENTITIES.register("blood_wave", () -> EntityType.Builder
                    .<com.lengyang.fulu.entity.BloodWaveEntity>of(
                            com.lengyang.fulu.entity.BloodWaveEntity::new,
                            net.minecraft.world.entity.MobCategory.MISC)
                    .sized(1.0F, 1.0F)
                    .clientTrackingRange(10)
                    .noSummon()
                    .build("blood_wave"));

    /** 假身：末影妖丹换位后留下的分身（不可见、无敌、吸引仇恨）。 */
    public static final RegistryObject<EntityType<com.lengyang.fulu.entity.DecoyEntity>> DECOY =
            ENTITIES.register("decoy", () -> EntityType.Builder
                    .of(com.lengyang.fulu.entity.DecoyEntity::new, net.minecraft.world.entity.MobCategory.MONSTER)
                    .sized(0.6F, 1.8F)
                    .clientTrackingRange(10)
                    .build("decoy"));

    public static final RegistryObject<EntityType<FormationMinionEntity>> FORMATION_MINION =
            ENTITIES.register("formation_minion", () -> EntityType.Builder
                    .<FormationMinionEntity>of(FormationMinionEntity::new, MobCategory.MONSTER)
                    .sized(0.7F, 1.4F)
                    .clientTrackingRange(8)
                    .updateInterval(2)
                    .build("formation_minion"));

    private ModEntities() {
    }

    public static void register(IEventBus modBus) {
        ENTITIES.register(modBus);
    }
}
