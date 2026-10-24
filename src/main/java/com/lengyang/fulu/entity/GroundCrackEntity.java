package com.lengyang.fulu.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 地面金色裂纹（贴地 Decal）：剑气掠过地面留下的发光裂痕。
 *
 * <p>它就是一条**贴地的狭长四边形带**，画在 y+0.02 的高度上，
 * 用加色混合 + 一条"抖动的折线"模拟裂纹。没有贴图，纯几何，
 * 所以不用担心 Decal 的 UV / 深度问题。
 *
 * <p>寿命 60 刻（3 秒），前 6 刻淡入、最后 15 刻淡出。
 */
public class GroundCrackEntity extends Entity {

    public static final int LIFE = 60;

    private static final EntityDataAccessor<Float> DATA_LENGTH =
            SynchedEntityData.defineId(GroundCrackEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_SEED =
            SynchedEntityData.defineId(GroundCrackEntity.class, EntityDataSerializers.INT);

    public GroundCrackEntity(EntityType<? extends GroundCrackEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_LENGTH, 6.0F);
        this.entityData.define(DATA_SEED, 0);
    }

    public void setup(Vec3 pos, float yawDeg, float length, int seed) {
        this.setPos(pos.x, pos.y, pos.z);
        this.setYRot(yawDeg);
        this.yRotO = yawDeg;
        this.entityData.set(DATA_LENGTH, length);
        this.entityData.set(DATA_SEED, seed);
    }

    public float getLength() {
        return this.entityData.get(DATA_LENGTH);
    }

    public int getSeed() {
        return this.entityData.get(DATA_SEED);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide && this.tickCount >= LIFE) {
            this.discard();
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
