package com.lengyang.fulu.entity;

import com.lengyang.fulu.registry.ModEntities;
import com.lengyang.fulu.registry.ModMobEffects;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

/**
 * 魂：招魂阵里怪物死亡后留下的幽灵。
 *
 * <p>为什么用 {@link Entity} 而不是 {@code Mob}：
 * <ul>
 *   <li>用户要求"**不占方块空间、可以穿过**" —— 普通实体只要不注册碰撞就穿得过；</li>
 *   <li>"**没有血条、打不死**" —— {@link Entity} 默认就没有血量和受击逻辑，天然满足；</li>
 *   <li>"**只攻击怪物**" —— 行为完全自己写，比套 AI 更简单可控。</li>
 * </ul>
 *
 * <p>行为：生成后找最近的、**没有招魂标记**的怪物飘过去；接触后造成 4 点伤害；
 * 打满 3 次或存在 12 秒就消失。魂不攻击玩家，也不理会任何攻击。
 */
public class SoulEntity extends Entity {

    /** 每次伤害 4 点（用户指定）。 */
    private static final float DAMAGE = 4.0F;
    /** 找目标的半径。 */
    private static final double SEARCH = 16.0D;
    /** 攻击冷却（刻）。 */
    private static final int ATTACK_COOLDOWN = 20;
    /** 移动速度（每刻）。 */
    private static final double SPEED = 0.16D;

    private int attacks;
    private int cooldown;
    /** 最近一次攻击的时间，用来做"身体短暂变亮"。 */
    public long lastAttackMs;

    public SoulEntity(EntityType<? extends SoulEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;                       // 穿方块
        this.setNoGravity(true);
    }

    // ---------------------------------------------------------- 必须实现的三件
    @Override
    protected void defineSynchedData() {
        // 没有需要同步的状态（位置由实体跟踪自动同步）
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.attacks = tag.getInt("FuluSoulAttacks");
        this.tickCount = tag.getInt("FuluSoulAge");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("FuluSoulAttacks", this.attacks);
        tag.putInt("FuluSoulAge", this.tickCount);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    // ---------------------------------------------------------- 不可交互
    @Override
    public boolean isPickable() {
        return false;                                // 点不到、打不到
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;                                // 打不死
    }

    // ---------------------------------------------------------- 行为
    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            // 客户端只管飘一飘的视觉：上下轻微浮动
            return;
        }

        // 找目标：优先没有招魂标记的怪物
        Mob target = findTarget();
        if (target == null) {
            // 没目标就原地上下飘
            this.setPos(this.getX(), this.getY() + Math.sin(this.tickCount * 0.1D) * 0.01D, this.getZ());
            return;
        }

        Vec3 to = target.position().add(0.0D, target.getBbHeight() * 0.6D, 0.0D).subtract(this.position());
        double dist = to.length();
        if (dist > 1.4D) {
            Vec3 step = to.normalize().scale(SPEED);
            this.setPos(this.getX() + step.x, this.getY() + step.y + Math.sin(this.tickCount * 0.25D) * 0.012D,
                    this.getZ() + step.z);
        }

        if (--this.cooldown <= 0 && dist <= 1.6D) {
            // 魂越多咬得越快：1 只 20 刻，10 只 4 刻（最多 5 倍速）
            int souls = countSouls();
            this.cooldown = Math.max(4, ATTACK_COOLDOWN - (souls - 1) * 2);
            // 阵魔：魂的伤害减半（用户规格）
            float soulDmg = (target instanceof com.lengyang.fulu.entity.FormationDemonEntity) ? DAMAGE * 0.5F : DAMAGE;
            target.hurt(this.damageSources().magic(), soulDmg);
            this.attacks++;
            this.lastAttackMs = System.currentTimeMillis();
            // 攻击粒子 + 轻击声
            if (this.level() instanceof net.minecraft.server.level.ServerLevel sl) {
                sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                        target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(),
                        10, 0.4D, 0.4D, 0.4D, 0.02D);
            }
            this.level().playSound(null, this.blockPosition(), SoundEvents.SOUL_ESCAPE,
                    SoundSource.HOSTILE, 0.5F, 1.6F);
        }

        // 拖尾
        if (this.level() instanceof net.minecraft.server.level.ServerLevel sl) {
            sl.sendParticles(ParticleTypes.SOUL, this.getX(), this.getY() + 0.3D, this.getZ(),
                    1, 0.05D, 0.05D, 0.05D, 0.0D);
        }
    }

    /** 找最近的怪物：优先没有招魂标记的。 */
    private Mob findTarget() {
        Mob best = null;
        double bestDist = Double.MAX_VALUE;
        for (Mob mob : this.level().getEntitiesOfClass(Mob.class,
                this.getBoundingBox().inflate(SEARCH))) {
            if (!mob.isAlive()) {
                continue;
            }
            // **只打阵法内的怪**：招魂标记就是"在阵法里"的凭据（阵眼每刻刷新，离阵就掉）
            if (mob.getEffect(ModMobEffects.SUMMON.get()) == null) {
                continue;
            }
            double d = mob.distanceToSqr(this);
            if (d < bestDist) {
                best = mob;
                bestDist = d;
            }
        }
        return best;
    }

    /** 附近有多少只魂（用来决定攻击速度）。 */
    private int countSouls() {
        return this.level().getEntitiesOfClass(SoulEntity.class,
                this.getBoundingBox().inflate(24.0D)).size();
    }

    /** 消失：炸成小粒子 + 一声轻叹。 */
    private void vanish() {
        if (this.level() instanceof net.minecraft.server.level.ServerLevel sl) {
            sl.sendParticles(ParticleTypes.SOUL, this.getX(), this.getY() + 0.4D, this.getZ(),
                    14, 0.35D, 0.35D, 0.35D, 0.03D);
        }
        this.level().playSound(null, this.blockPosition(), SoundEvents.SOUL_ESCAPE,
                SoundSource.HOSTILE, 0.4F, 0.7F);
        this.discard();
    }

    // ---------------------------------------------------------- 视觉（客户端）
    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 4096.0D;
    }

    /** 供渲染器用：身体发亮的强度（刚攻击完更亮）。 */
    public float glow() {
        if (lastAttackMs == 0L) {
            return 0.0F;
        }
        float t = (System.currentTimeMillis() - lastAttackMs) / 400.0F;
        return t >= 1.0F ? 0.0F : 1.0F - t;
    }
}
