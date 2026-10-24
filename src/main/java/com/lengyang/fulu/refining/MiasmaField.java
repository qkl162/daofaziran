package com.lengyang.fulu.refining;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 【绝仙瘴气】：僵尸妖丹的雾。
 *
 * <p>与"中毒"不同，它是**灵力枯竭**：
 * <ul>
 *   <li>每秒 2 点伤害；</li>
 *   <li>**剥离全部增益效果**（每次结算都清一遍）；</li>
 *   <li>**治疗量为 0**（靠 {@code FuluMiasma} 标记，由 {@code BloodCurseHandler} 统一拦截）。</li>
 * </ul>
 */
public final class MiasmaField {

    /** 半径 / 持续时间（刻）。 */
    private static final double RADIUS = 6.0D;
    private static final int LIFE = 160;
    /** 治疗归零标记的键（与 BloodCurseHandler 共用拦截逻辑）。 */
    public static final String NO_HEAL_TAG = "FuluMiasma";

    private static final class Field {
        final Vec3 pos;
        int left = LIFE;

        Field(Vec3 pos) {
            this.pos = pos;
        }
    }

    private static final List<Field> ACTIVE = new ArrayList<>();

    private MiasmaField() {
    }

    /** 生成一片瘴气（附带**原版药水云**视觉）。 */
    public static void spawn(ServerLevel level, Vec3 pos) {
        ACTIVE.add(new Field(pos));

        // **原版药水云**：就是喷溅药水落地那朵云，绿色、半径 6、持续 8 秒
        var cloud = new net.minecraft.world.entity.AreaEffectCloud(level, pos.x, pos.y, pos.z);
        cloud.setRadius(6.0F);
        cloud.setDuration(LIFE);
        cloud.setRadiusPerTick(-0.002F);
        cloud.setParticle(ParticleTypes.ENTITY_EFFECT);
        cloud.setFixedColor(0x4E8C3A);                 // 墨绿
        level.addFreshEntity(cloud);

        // **雾体**：复用 fire_field 实体的 mode 2（绿色雾柱），保证一定看得见
        var fog = new com.lengyang.fulu.entity.FireFieldEntity(
                com.lengyang.fulu.registry.ModEntities.FIRE_FIELD.get(), level);
        fog.setPos(pos.x, pos.y + 0.05D, pos.z);
        fog.setup(2, LIFE);
        level.addFreshEntity(fog);

        level.playSound(null, net.minecraft.core.BlockPos.containing(pos),
                net.minecraft.sounds.SoundEvents.BREWING_STAND_BREW,
                net.minecraft.sounds.SoundSource.PLAYERS, 1.4F, 0.6F);
        level.playSound(null, net.minecraft.core.BlockPos.containing(pos),
                net.minecraft.sounds.SoundEvents.LINGERING_POTION_THROW,
                net.minecraft.sounds.SoundSource.PLAYERS, 1.2F, 0.7F);
        com.lengyang.fulu.FuLu.LOGGER.info("[miasma] spawned at {}", pos);
    }

    /** 每刻推进。 */
    public static void tick(ServerLevel level) {
        if (ACTIVE.isEmpty()) {
            return;
        }
        List<Field> done = new ArrayList<>();
        for (Field f : ACTIVE) {
            if (--f.left <= 0) {
                done.add(f);
                continue;
            }
            // **密集雾粒子**：每刻都撒（之前每 2 刻 6 颗，几乎看不见）
            for (int i = 0; i < 10; i++) {
                double a = level.random.nextDouble() * Math.PI * 2.0D;
                double r = Math.sqrt(level.random.nextDouble()) * RADIUS;
                level.sendParticles(ParticleTypes.ENTITY_EFFECT,
                        f.pos.x + Math.cos(a) * r,
                        f.pos.y + 0.3D + level.random.nextDouble() * 1.6D,
                        f.pos.z + Math.sin(a) * r,
                        1, 0.0D, 0.03D, 0.0D, 0.0D);
            }
            if (level.getGameTime() % 2 == 0) {
                level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                        f.pos.x, f.pos.y + 0.6D, f.pos.z,
                        4, RADIUS * 0.5D, 0.5D, RADIUS * 0.5D, 0.01D);
            }

            boolean second = f.left % 20 == 0;
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class,
                    new AABB(f.pos, f.pos).inflate(RADIUS, 3.0D, RADIUS))) {
                if (!e.isAlive() || e instanceof Player) {
                    continue;
                }
                // 治疗归零标记（持续 40 刻，离开雾也短暂残留）
                e.getPersistentData().putLong(NO_HEAL_TAG,
                        level.getGameTime() + 40);

                if (!second) {
                    continue;
                }
                e.invulnerableTime = 0;
                e.hurt(level.damageSources().magic(), 2.0F);

                // **剥离全部增益**
                for (MobEffectInstance inst : new ArrayList<>(e.getActiveEffects())) {
                    if (inst.getEffect().getCategory() == MobEffectCategory.BENEFICIAL) {
                        e.removeEffect(inst.getEffect());
                    }
                }
                level.sendParticles(ParticleTypes.SQUID_INK,
                        e.getX(), e.getY() + e.getBbHeight() * 0.7D, e.getZ(),
                        6, 0.3D, 0.3D, 0.3D, 0.02D);
            }
        }
        ACTIVE.removeAll(done);
    }

    /** 这个实体是否处于"灵力枯竭"禁疗状态。 */
    public static boolean isNoHeal(net.minecraft.world.entity.Entity e) {
        return e.getPersistentData().getLong(NO_HEAL_TAG) > e.level().getGameTime();
    }
}
