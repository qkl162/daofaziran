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

/**
 * 「光翼」气场：跟着施法者走的**光翼 + 蓄力光晕**视觉（纯客户端表现）。
 *
 * <p>生命周期 40 刻（2 秒），按时间分三段：
 * <ol>
 *   <li>{@code 0~8} 刻：蓄力 —— 剑尖聚光、光粒向中心汇聚；</li>
 *   <li>{@code 8~14} 刻：光翼展开；</li>
 *   <li>{@code 14~32} 刻：光翼保持（挥剑阶段）；</li>
 *   <li>{@code 32~40} 刻：光翼收拢、光尘消散。</li>
 * </ol>
 */
public class LightWingAuraEntity extends Entity {

    public static final int LIFE = 40;

    private static final EntityDataAccessor<Float> DATA_UNFOLD =
            SynchedEntityData.defineId(LightWingAuraEntity.class, EntityDataSerializers.FLOAT);
    /** true = 这个实体是"第四视角"相机，不画光翼。 */
    private static final EntityDataAccessor<Boolean> DATA_CAMERA =
            SynchedEntityData.defineId(LightWingAuraEntity.class, EntityDataSerializers.BOOLEAN);

    private UUID caster;

    public LightWingAuraEntity(EntityType<? extends LightWingAuraEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_UNFOLD, 0.0F);
        this.entityData.define(DATA_CAMERA, false);
    }

    public void setCaster(Player player) {
        this.caster = player.getUUID();
        this.setPos(player.getX(), player.getY(), player.getZ());
        this.setYRot(player.getYRot());
    }

    public boolean isCamera() {
        return this.entityData.get(DATA_CAMERA);
    }

    /**
     * 作为**后拉第三人称**相机：站在玩家**身后**，和玩家朝同一个方向看。
     *
     * <p>相当于把第三人称镜头再往后推几格、视野更宽，而不是绕到正面看自己。
     */
    public void setupCamera(Player player, double distance) {
        this.entityData.set(DATA_CAMERA, true);
        this.caster = player.getUUID();
        this.follow(player, distance);
    }

    /** 每刻把相机贴到玩家身后（跟随移动与转头）。 */
    public void follow(Player player, double distance) {
        double rad = Math.toRadians(player.getYRot());
        double fx = -Math.sin(rad);
        double fz = Math.cos(rad);
        this.setPos(player.getX() - fx * distance,
                player.getY() + 1.55D,
                player.getZ() - fz * distance);
        this.setYRot(player.getYRot());
        this.yRotO = player.getYRot();
        this.setXRot(player.getXRot() * 0.5F);
        this.xRotO = this.getXRot();
    }

    /** 施法者的 UUID（客户端用来判断"这是我自己的光翼"）。 */
    public java.util.UUID getCasterId() {
        return this.caster;
    }

    /** 0 = 收拢，1 = 完全展开。 */
    public float getUnfold() {
        return this.entityData.get(DATA_UNFOLD);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        ServerLevel server = (ServerLevel) this.level();
        Player p = this.caster == null ? null : server.getPlayerByUUID(this.caster);
        if (p == null || !p.isAlive() || this.tickCount >= LIFE) {
            this.discard();
            return;
        }
        if (isCamera()) {
            this.follow(p, 5.5D);                   // 后拉第三人称：始终在身后 5.5 格
        } else {
            this.setPos(p.getX(), p.getY(), p.getZ());
            this.setYRot(p.getYRot());
        }

        // 展开曲线
        float unfold;
        if (this.tickCount < 8) {
            unfold = 0.0F;                                  // 蓄力：未展开
        } else if (this.tickCount < 14) {
            unfold = (this.tickCount - 8) / 6.0F;           // 0.4~0.7 秒展开
        } else if (this.tickCount < 32) {
            unfold = 1.0F;                                  // 挥剑阶段保持
        } else {
            unfold = 1.0F - (this.tickCount - 32) / 8.0F;   // 收拢
        }
        this.entityData.set(DATA_UNFOLD, Math.max(0.0F, Math.min(1.0F, unfold)));
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
