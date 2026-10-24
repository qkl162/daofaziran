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
 * 诛仙血阵的视觉实体（**纯视觉**，伤害由 {@code BloodSlash} 结算）。
 *
 * <p>两种形态：
 * <ul>
 *   <li><b>刀光</b>（默认）：向前推进的五道锥形刀光；</li>
 *   <li><b>星爆</b>（{@link #setStar}）：施法瞬间在原地爆开的**放射星形光刃**，
 *       仿《王者荣耀》李信·光信大招「炽霜斩」的形态。</li>
 * </ul>
 */
public class BloodWaveEntity extends Entity {

    /** 剑形态存活刻数：以 1.5 格/刻飞行，20 刻可覆盖 30 格。 */
    public static final int LIFE = 20;
    /** 星爆形态的存活刻数：要撑到剑气飞到最远处（17 刻）之后再淡出。 */
    public static final int STAR_LIFE = 26;
    /** 生长阶段刻数：从根部蔓延到最远需要这么久。 */
    public static final int GROW_TICKS = 17;

    private static final EntityDataAccessor<Float> DATA_SCALE =
            SynchedEntityData.defineId(BloodWaveEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_STAR =
            SynchedEntityData.defineId(BloodWaveEntity.class, EntityDataSerializers.BOOLEAN);
    /** 生长进度 0~1：剑刃从根部延伸出去的比例。 */
    private static final EntityDataAccessor<Float> DATA_GROW =
            SynchedEntityData.defineId(BloodWaveEntity.class, EntityDataSerializers.FLOAT);

    private Vec3 dir = Vec3.ZERO;
    private int life = LIFE;

    public BloodWaveEntity(EntityType<? extends BloodWaveEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_SCALE, 1.0F);
        this.entityData.define(DATA_STAR, false);
        this.entityData.define(DATA_GROW, 0.0F);
    }

    /** 刀光形态。 */
    public void setup(Vec3 dir, Vec3 pos) {
        this.dir = dir;
        this.setPos(pos.x, pos.y, pos.z);
        this.setYRot((float) (Math.toDegrees(Math.atan2(dir.x, dir.z))));
        this.yRotO = this.getYRot();
    }

    /** 星爆形态：停在原地，只负责"炸一下"。 */
    public void setupStar(Vec3 pos, float yawDeg) {
        this.entityData.set(DATA_STAR, true);
        this.life = STAR_LIFE;
        this.setPos(pos.x, pos.y, pos.z);
        this.setYRot(yawDeg);
        this.yRotO = yawDeg;
    }

    public boolean isStar() {
        return this.entityData.get(DATA_STAR);
    }

    /** 生长进度（0~1）。 */
    public float getGrow() {
        return this.entityData.get(DATA_GROW);
    }

    public void setGrow(float grow) {
        this.entityData.set(DATA_GROW, Math.max(0.0F, Math.min(1.0F, grow)));
    }

    public float getScale() {
        return this.entityData.get(DATA_SCALE);
    }

    public void setScale(float scale) {
        this.entityData.set(DATA_SCALE, scale);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide) {
            // 半月剑气**向前飞行**（1.5 格/刻）
            if (!isStar() && !this.dir.equals(Vec3.ZERO)) {
                this.setPos(this.getX() + this.dir.x * 1.5D,
                        this.getY() + this.dir.y * 1.5D,
                        this.getZ() + this.dir.z * 1.5D);
            }
            int max = isStar() ? STAR_LIFE : LIFE;
            if (this.tickCount >= max) {
                this.discard();
            }
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
