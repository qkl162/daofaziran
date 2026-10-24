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
 * 命中爆点：金色爆闪 + **环形冲击波 Mesh**（视觉实体）。
 *
 * <p>寿名 14 刻：前 8 刻环快速扩张，之后淡出。
 */
public class HitBurstEntity extends Entity {

    public static final int LIFE = 14;

    private static final EntityDataAccessor<Float> DATA_SCALE =
            SynchedEntityData.defineId(HitBurstEntity.class, EntityDataSerializers.FLOAT);

    public HitBurstEntity(EntityType<? extends HitBurstEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_SCALE, 1.0F);
    }

    public void setup(Vec3 pos, float scale) {
        this.setPos(pos.x, pos.y, pos.z);
        this.entityData.set(DATA_SCALE, scale);
    }

    public float getScale() {
        return this.entityData.get(DATA_SCALE);
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
