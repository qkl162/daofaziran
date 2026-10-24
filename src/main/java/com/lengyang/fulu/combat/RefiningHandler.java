package com.lengyang.fulu.combat;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.lengyang.fulu.FuLu;
import com.lengyang.fulu.registry.ModItems;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

/**
 * 炼妖阵：怪物死亡凝聚**妖丹**。
 *
 * <p>三件事：
 * <ol>
 *   <li>死亡时判断身上有没有 {@code REFINING} 标记 —— 有才凝丹；</li>
 *   <li>按怪物类型给出对应妖丹（僵尸/骷髅/苦力怕/蜘蛛/末影人/烈焰人）；</li>
 *   <li>妖丹以**无重力掉落物**的形式悬浮，并缓慢飘向最近的玩家。</li>
 * </ol>
 *
 * <p>妖丹用掉落物而不是自定义实体：拾取、合并、存档、漏斗兼容全都现成，
 * 而且"悬浮 + 飘向玩家"只要关掉重力、每刻改一下速度就够了。
 */
@Mod.EventBusSubscriber(modid = FuLu.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RefiningHandler {

    /** 妖丹向玩家飘的速度（每刻位移）。 */
    private static final double DRIFT_SPEED = 0.055D;

    /** 妖丹开始吸向玩家的半径。 */
    private static final double PULL_RADIUS = 6.0D;

    /** 每种妖丹的颜色（用于粒子和光环）。 */
    private static final Map<String, float[]> COLORS = new HashMap<>();

    static {
        COLORS.put("zombie", new float[]{0.42F, 0.72F, 0.25F});
        COLORS.put("skeleton", new float[]{0.90F, 0.92F, 0.95F});
        COLORS.put("creeper", new float[]{0.42F, 0.85F, 0.32F});
        COLORS.put("spider", new float[]{0.55F, 0.35F, 0.70F});
        COLORS.put("enderman", new float[]{0.62F, 0.22F, 0.80F});
        COLORS.put("blaze", new float[]{1.00F, 0.62F, 0.18F});
        COLORS.put("witch", new float[]{0.35F, 0.55F, 0.25F});
        COLORS.put("slime", new float[]{0.55F, 0.85F, 0.45F});
        COLORS.put("wither_skeleton", new float[]{0.30F, 0.22F, 0.38F});
        COLORS.put("ghast", new float[]{0.85F, 0.85F, 0.90F});
    }

    private RefiningHandler() {
    }

    /** 这只怪对应哪种妖丹；不是可炼的怪就返回 null。 */
    private static String kindOf(LivingEntity entity) {
        if (entity instanceof Zombie) {
            return "zombie";
        }
        if (entity instanceof net.minecraft.world.entity.monster.WitherSkeleton) {
            return "wither_skeleton";               // 必须先判凋零骷髅
        }
        if (entity instanceof AbstractSkeleton) {
            return "skeleton";
        }
        if (entity instanceof Creeper) {
            return "creeper";
        }
        if (entity instanceof Spider) {
            return "spider";
        }
        if (entity instanceof EnderMan) {
            return "enderman";
        }
        if (entity instanceof Blaze) {
            return "blaze";
        }
        if (entity instanceof net.minecraft.world.entity.monster.Witch) {
            return "witch";
        }
        if (entity instanceof net.minecraft.world.entity.monster.Slime) {
            return "slime";
        }
        if (entity instanceof net.minecraft.world.entity.monster.Ghast) {
            return "ghast";
        }
        return null;
    }

    /** 供【诛仙血阵】调用：按怪物类型直接给一颗妖丹（炼妖阵双倍掉落用）。 */
    public static void grantEssence(net.minecraft.server.level.ServerLevel level,
                                    net.minecraft.world.entity.LivingEntity dead) {
        String kind = kindOf(dead);
        if (kind == null) {
            return;
        }
        Item item = itemOf(kind);
        if (item == null) {
            return;
        }
        var drop = new net.minecraft.world.entity.item.ItemEntity(level,
                dead.getX(), dead.getY() + 0.5D, dead.getZ(),
                new net.minecraft.world.item.ItemStack(item));
        drop.setNoGravity(true);
        drop.setPickUpDelay(10);
        level.addFreshEntity(drop);
    }

    private static Item itemOf(String kind) {
        return switch (kind) {
            case "zombie" -> ModItems.ESSENCE_ZOMBIE.get();
            case "skeleton" -> ModItems.ESSENCE_SKELETON.get();
            case "creeper" -> ModItems.ESSENCE_CREEPER.get();
            case "spider" -> ModItems.ESSENCE_SPIDER.get();
            case "enderman" -> ModItems.ESSENCE_ENDERMAN.get();
            case "blaze" -> ModItems.ESSENCE_BLAZE.get();
            case "witch" -> ModItems.ESSENCE_WITCH.get();
            case "slime" -> ModItems.ESSENCE_SLIME.get();
            case "wither_skeleton" -> ModItems.ESSENCE_WITHER_SKELETON.get();
            case "ghast" -> ModItems.ESSENCE_GHAST.get();
            default -> null;
        };
    }

    /** 死亡：凝丹。 */
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide || !(entity.level() instanceof ServerLevel level)) {
            return;
        }
        if (entity instanceof Player) {
            return;                                     // 玩家不凝丹
        }
        if (!FormationMarks.isMarked(level, entity, FormationMarks.REFINING)) {
            return;
        }
        String kind = kindOf(entity);
        if (kind == null) {
            return;
        }
        Item item = itemOf(kind);
        if (item == null) {
            return;
        }

        float[] c = COLORS.getOrDefault(kind, new float[]{1.0F, 0.5F, 0.5F});

        // 身上爆开一圈粒子（聚拢感：从外向内）
        DustParticleOptions dust = new DustParticleOptions(new Vector3f(c[0], c[1], c[2]), 1.2F);
        for (int i = 0; i < 28; i++) {
            double a = Math.PI * 2.0D * i / 28.0D;
            double r = 1.1D;
            level.sendParticles(dust,
                    entity.getX() + Math.cos(a) * r,
                    entity.getY() + 0.6D,
                    entity.getZ() + Math.sin(a) * r,
                    1, -Math.cos(a) * 0.25D, 0.08D, -Math.sin(a) * 0.25D, 0.0D);
        }

        // 生成悬浮妖丹
        ItemEntity drop = new ItemEntity(level,
                entity.getX(), entity.getY() + 1.1D, entity.getZ(),
                new ItemStack(item));
        drop.setNoGravity(true);
        drop.setDeltaMovement(0.0D, 0.06D, 0.0D);
        drop.setPickUpDelay(10);
        drop.getPersistentData().putString("FuluEssenceKind", kind);
        level.addFreshEntity(drop);

        // 炼丹炉"咕噜"声
        level.playSound(null, entity.blockPosition(), SoundEvents.BREWING_STAND_BREW,
                SoundSource.NEUTRAL, 1.0F, 1.2F);

        FuLu.LOGGER.info("[refining] {} -> {} essence", entity.getType(), kind);
    }

    /** 悬浮 + 飘向玩家 + 环绕粒子。 */
    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.level.isClientSide) {
            return;
        }
        if (!(event.level instanceof ServerLevel level)) {
            return;
        }
        // 只扫玩家附近的掉落物，避免遍历整个世界
        for (Player player : level.players()) {
            for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class,
                    player.getBoundingBox().inflate(PULL_RADIUS))) {
                String kind = item.getPersistentData().getString("FuluEssenceKind");
                if (kind.isEmpty()) {
                    continue;
                }
                float[] c = COLORS.getOrDefault(kind, new float[]{1.0F, 0.5F, 0.5F});

                // 飘向玩家
                Vec3 to = player.position().add(0.0D, 0.8D, 0.0D).subtract(item.position());
                if (to.lengthSqr() > 0.25D) {
                    Vec3 step = to.normalize().scale(DRIFT_SPEED);
                    item.setDeltaMovement(step.x, Math.max(0.0D, step.y) + 0.01D, step.z);
                    item.hurtMarked = true;
                }

                // 环绕的细粒子
                if (level.getGameTime() % 3 == 0) {
                    double a = (level.getGameTime() * 0.4D) % (Math.PI * 2.0D);
                    level.sendParticles(
                            new DustParticleOptions(new Vector3f(c[0], c[1], c[2]), 0.7F),
                            item.getX() + Math.cos(a) * 0.35D,
                            item.getY() + 0.15D,
                            item.getZ() + Math.sin(a) * 0.35D,
                            1, 0.0D, 0.0D, 0.0D, 0.0D);
                }
            }
        }
    }
}
