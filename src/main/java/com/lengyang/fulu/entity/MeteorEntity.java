package com.lengyang.fulu.entity;

import java.util.UUID;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 陨石：从高空**垂直砸下**的巨大火球。
 *
 * <p>视觉由 {@code MeteorRenderer} 画（加色混合的火球 + 旋转环），
 * 坠落轨迹的浓烟/岩浆粒子与落地伤害由 {@code MeteorStrike} 负责。
 *
 * <p>落地判定：Y 坐标降到 {@code targetY} 时就引爆 —— 不依赖方块碰撞，
 * 因为陨石速度快，逐刻的方块检测容易穿模。
 */
public class MeteorEntity extends Entity {

    private static final EntityDataAccessor<Float> DATA_SIZE =
            SynchedEntityData.defineId(MeteorEntity.class, EntityDataSerializers.FLOAT);

    private UUID caster;
    private double targetY;
    /** 每刻下落速度（格）。 */
    private static final double FALL_SPEED = 1.6D;

    public MeteorEntity(EntityType<? extends MeteorEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_SIZE, 1.0F);
    }

    public void setup(Player owner, Vec3 pos, double targetY, float size) {
        this.caster = owner.getUUID();
        this.setPos(pos.x, pos.y, pos.z);
        this.targetY = targetY;
        this.entityData.set(DATA_SIZE, size);
    }

    public float getSize() {
        return this.entityData.get(DATA_SIZE);
    }

    public UUID getCasterId() {
        return this.caster;
    }

    /** 用来给渲染器做旋转动画。 */
    public float spin() {
        return (this.tickCount * 9.0F) % 360.0F;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        // 垂直下落：**精确停在目标高度**
        //
        // 之前是"先落 1.5 格、再看有没有到"，导致实际停在地表**下方 0.4 格**，
        // 所有爆炸粒子/碎块都生成在方块内部 —— 看起来就是"什么都没有"。
        double nextY = this.getY() - FALL_SPEED;
        if (nextY <= this.targetY) {
            this.setPos(this.getX(), this.targetY, this.getZ());   // 停在地表
            this.tickTrail();
            com.lengyang.fulu.refining.MeteorStrike.impact(
                    (ServerLevel) this.level(), this);
            this.discard();
            return;
        }
        this.setPos(this.getX(), nextY, this.getZ());
        this.tickTrail();
    }

    /** 坠落拖尾：黑烟 + 岩浆 + 火苗。 */
    private void tickTrail() {
        if (this.level() instanceof ServerLevel server) {
            server.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE,
                    this.getX(), this.getY() + 1.0D, this.getZ(), 4, 0.6D, 1.2D, 0.6D, 0.02D);
            server.sendParticles(net.minecraft.core.particles.ParticleTypes.LAVA,
                    this.getX(), this.getY(), this.getZ(), 3, 0.5D, 0.8D, 0.5D, 0.0D);
            server.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME,
                    this.getX(), this.getY() + 0.5D, this.getZ(), 4, 0.6D, 0.8D, 0.6D, 0.02D);
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
