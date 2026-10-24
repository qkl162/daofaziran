package com.lengyang.fulu.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * 陨石留下的**火海**：一个持续 8 秒的燃烧区域实体。
 *
 * <p>为什么做成实体而不是纯粒子：
 * 粒子在某些光影/粒子设置下会被压到几乎不可见（用户实测"完全没有火海"）。
 * 做成实体后，{@code FireFieldRenderer} 会用**火球贴图画一圈跳动的火焰**，
 * 和粒子系统完全无关 —— 只要实体能同步就一定能看见。
 *
 * <p>伤害逻辑（每刻给范围内生物点火）也在服务端的 tick 里。
 */
public class FireFieldEntity extends Entity {

    /** 半径（格）。 */
    public static final float RADIUS = 6.0F;
    /** 持续时间（刻）：**15 秒**（用户觉得 8 秒太短）。 */
    public static final int LIFE = 300;

    /** 视觉模式：0 = 火海，1 = 泥沼区域光环。 */
    private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> DATA_MODE =
            net.minecraft.network.syncher.SynchedEntityData.defineId(
                    FireFieldEntity.class, net.minecraft.network.syncher.EntityDataSerializers.INT);
    /** 存在时长（刻，按模式不同）。 */
    private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> DATA_LIFE =
            net.minecraft.network.syncher.SynchedEntityData.defineId(
                    FireFieldEntity.class, net.minecraft.network.syncher.EntityDataSerializers.INT);

    /** 0 = 火海，1 = 泥沼光环。 */
    public int getMode() {
        return this.entityData.get(DATA_MODE);
    }

    public int getLife() {
        return this.entityData.get(DATA_LIFE);
    }

    /** 设置模式与寿命（火海 300 刻 / 泥沼 640 刻）。 */
    public void setup(int mode, int life) {
        this.entityData.set(DATA_MODE, mode);
        this.entityData.set(DATA_LIFE, life);
    }

    public FireFieldEntity(EntityType<? extends FireFieldEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_MODE, 0);
        this.entityData.define(DATA_LIFE, LIFE);
    }

    /** 燃烧判定 + 粒子（服务端）。 */
    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        ServerLevel level = (ServerLevel) this.level();
        if (this.tickCount >= getLife()) {
            this.discard();
            return;
        }
        // 泥沼光环只负责视觉，灼烧逻辑交给 SwampZoneHandler
        if (getMode() == 1) {
            return;
        }
        // 范围内生物持续着火
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(this.position(), this.position()).inflate(RADIUS, 4.0D, RADIUS))) {
            if (e.isAlive()) {
                e.setSecondsOnFire(2);
            }
        }
        // 少量粒子做点缀（不是主要视觉）
        if (this.tickCount % 4 == 0) {
            level.sendParticles(ParticleTypes.LARGE_SMOKE,
                    this.getX(), this.getY() + 0.8D, this.getZ(),
                    3, RADIUS * 0.5D, 0.3D, RADIUS * 0.5D, 0.01D);
        }
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }
}
