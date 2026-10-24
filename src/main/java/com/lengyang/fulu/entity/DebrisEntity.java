package com.lengyang.fulu.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * 陨石落地时的**碎块**（纯视觉）。
 *
 * <p>为什么不用原版 {@code FallingBlockEntity}：
 * <ul>
 *   <li>{@code fall()} 会把方块从世界挖掉；</li>
 *   <li>就算生成到方块上方，它的 {@code tick} 一碰到 {@code onGround} 就自杀，
 *       实测生成了 44 个、屏幕上却是空的；</li>
 *   <li>构造器不是 public，没法"只造实体不动世界"。</li>
 * </ul>
 *
 * <p>所以这里自己写一个最简单的实体：只带一个**颜色**（取自方块的地图颜色），
 * 由 {@code DebrisRenderer} 画成翻滚的小方块 —— 视觉上就是"方块碎块"，
 * 而且**完全可控**。
 */
public class DebrisEntity extends Entity {

    public static final int LIFE = 20;

    /** 方块颜色（0xRRGGBB）。 */
    private static final EntityDataAccessor<Integer> DATA_COLOR =
            SynchedEntityData.defineId(DebrisEntity.class, EntityDataSerializers.INT);
    /** 尺寸（格）。 */
    private static final EntityDataAccessor<Float> DATA_SIZE =
            SynchedEntityData.defineId(DebrisEntity.class, EntityDataSerializers.FLOAT);

    public DebrisEntity(EntityType<? extends DebrisEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;                       // 不参与碰撞，不会被方块挤掉
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_COLOR, 0x808080);
        this.entityData.define(DATA_SIZE, 0.28F);
    }

    public void setup(int color, float size, double vx, double vy, double vz) {
        this.entityData.set(DATA_COLOR, color);
        this.entityData.set(DATA_SIZE, size);
        this.setDeltaMovement(vx, vy, vz);
    }

    public int getColor() {
        return this.entityData.get(DATA_COLOR);
    }

    public float getSize() {
        return this.entityData.get(DATA_SIZE);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        // 简易重力 + 阻力
        var v = this.getDeltaMovement();
        this.setDeltaMovement(v.x * 0.97D, (v.y - 0.05D) * 0.97D, v.z * 0.97D);
        this.setPos(this.getX() + v.x, this.getY() + v.y, this.getZ() + v.z);
        if (this.tickCount >= LIFE) {
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
