package com.lengyang.fulu.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 「假身」：末影妖丹换位后留在原地、**吸引怪物仇恨**的分身。
 *
 * <p>为什么不能只"清仇恨"：怪只是跟丢了，下一秒又会锁回玩家。
 * 真做法是给怪物一个**可以被攻击的目标** —— 所以这里生成一个实体，
 * 并且让附近怪物把它设为 {@code setTarget(假身)}：
 * <ul>
 *   <li>它没有 AI（{@code setNoAi}），不会乱跑也不会攻击玩家；</li>
 *   <li>它**免疫伤害**（{@code isInvulnerable}）—— 怪会一直砍它，但它不会被打死；</li>
 *   <li>寿命 {@link #LIFETIME} 刻，到点冒烟消失，仇恨自然回到真实玩家身上。</li>
 * </ul>
 *
 * <p>渲染：它是**不可见**的，只靠环绕粒子表示存在（所以渲染器是空的）。
 */
public class DecoyEntity extends Monster {

    /** 存在时长（刻）：**10 秒**。 */
    public static final int LIFETIME = 200;
    /** 消失时的小爆炸：半径 / 伤害。 */
    private static final double BURST_RADIUS = 4.5D;
    private static final float BURST_DAMAGE = 6.0F;

    /** 场上所有假身（每刻末尾统一抢仇恨）。 */
    private static final java.util.List<DecoyEntity> LIVE = new java.util.ArrayList<>();

    /**
     * 每刻末尾**强制**把附近怪物的目标改成假身。
     *
     * <p>为什么要在末尾再抢一次：怪物的 `NearestAttackableTargetGoal` 会
     * 周期性重新扫描玩家，我们只在生成时设一次 target 的话，下一轮扫描就被玩家抢回去了
     * （这就是"假身不吸引仇恨"的原因）。
     */
    public static void enforceAggro() {
        LIVE.removeIf(d -> !d.isAlive());
        for (DecoyEntity d : LIVE) {
            if (!(d.level() instanceof net.minecraft.server.level.ServerLevel level)) {
                continue;
            }
            for (net.minecraft.world.entity.Mob mob : level.getEntitiesOfClass(
                    net.minecraft.world.entity.Mob.class, d.getBoundingBox().inflate(24.0D))) {
                LivingEntity cur = mob.getTarget();
                if (cur == null || cur instanceof net.minecraft.world.entity.player.Player) {
                    mob.setTarget(d);                       // 抢过来
                    mob.getNavigation().moveTo(d, 1.15D);   // 顺便让它真的走过来
                }
            }
        }
    }

    public DecoyEntity(EntityType<? extends DecoyEntity> type, Level level) {
        super(type, level);
        this.xpReward = 0;
        this.setNoAi(true);
        this.setPersistenceRequired();
        this.setInvulnerable(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 200.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D);
    }

    @Override
    protected void registerGoals() {
        // 故意不挂任何 goal：它只是"站在那里当靶子"
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;                                   // 打不死的靶子
    }

    @Override
    public void onAddedToWorld() {
        super.onAddedToWorld();
        if (!this.level().isClientSide) {
            LIVE.add(this);
        }
    }

    @Override
    public void onRemovedFromWorld() {
        super.onRemovedFromWorld();
        LIVE.remove(this);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        ServerLevel level = (ServerLevel) this.level();
        // 用一个"半透明的末影人轮廓"表示假身：每刻少量紫色粒子
        if (this.tickCount % 2 == 0) {
            level.sendParticles(ParticleTypes.PORTAL,
                    this.getX(), this.getY() + 1.0D, this.getZ(), 3, 0.3D, 0.6D, 0.3D, 0.02D);
        }
        if (this.tickCount >= LIFETIME) {
            // **消失时炸一下**：只打敌对生物，不伤玩家；不破坏地形
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                    this.getBoundingBox().inflate(BURST_RADIUS))) {
                if (target == this || !target.isAlive()) {
                    continue;
                }
                if (!(target instanceof net.minecraft.world.entity.monster.Enemy)) {
                    continue;                                   // 只炸怪，不炸玩家/动物
                }
                target.invulnerableTime = 0;
                target.hurt(level.damageSources().magic(), BURST_DAMAGE);
                Vec3 away = target.position().subtract(this.position()).normalize().scale(0.6D);
                target.push(away.x, 0.35D, away.z);
                target.hurtMarked = true;
            }
            level.sendParticles(ParticleTypes.SMOKE,
                    this.getX(), this.getY() + 1.0D, this.getZ(), 20, 0.4D, 0.7D, 0.4D, 0.02D);
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                    this.getX(), this.getY() + 0.8D, this.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
            level.playSound(null, this.blockPosition(), SoundEvents.GENERIC_EXPLODE,
                    SoundSource.HOSTILE, 1.0F, 1.2F);
            level.playSound(null, this.blockPosition(), SoundEvents.ENDERMAN_TELEPORT,
                    SoundSource.HOSTILE, 0.8F, 1.4F);
            this.discard();
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
    }

    /** 不被算作"敌对生物"（免得被阵法/飞剑当成怪打）。 */
    @Override
    public boolean isPushable() {
        return false;
    }
}
