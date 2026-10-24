package com.lengyang.fulu.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * 阵仆：阵魔召唤出来的小型阵魔。
 *
 * <p>20 血、会打玩家、也会砸阵眼（阵眼破坏由 {@code DemonFormationInteraction} 统一处理）。
 */
public class FormationMinionEntity extends Monster
        implements software.bernie.geckolib.animatable.GeoEntity {

    /** GeckoLib 动画缓存。 */
    private final software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache geckoCache =
            software.bernie.geckolib.util.GeckoLibUtil.createInstanceCache(this);

    @Override
    public software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geckoCache;
    }

    @Override
    public void registerControllers(
            software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new software.bernie.geckolib.core.animation.AnimationController<>(
                this, "main", 3, this::pickAnimation));
    }

    /** 阵仆动作很简单：受伤 → hurt，在动 → walk，否则 idle。 */
    private software.bernie.geckolib.core.object.PlayState pickAnimation(
            software.bernie.geckolib.core.animation.AnimationState<FormationMinionEntity> state) {
        if (this.hurtTime > 0) {
            return state.setAndContinue(
                    software.bernie.geckolib.core.animation.RawAnimation.begin().thenPlay("hurt"));
        }
        boolean moving = this.getDeltaMovement().horizontalDistanceSqr() > 1.0E-4D
                || this.walkAnimation.speed() > 0.05F;
        return state.setAndContinue(
                software.bernie.geckolib.core.animation.RawAnimation.begin()
                        .thenLoop(moving ? "walk" : "idle"));
    }


    private FormationDemonEntity owner;

    public FormationMinionEntity(EntityType<? extends FormationMinionEntity> type, Level level) {
        super(type, level);
        this.xpReward = 5;
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.FOLLOW_RANGE, 24.0D);
    }

    public void setOwner(FormationDemonEntity owner) {
        this.owner = owner;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.1D, false));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        ServerLevel level = (ServerLevel) this.level();
        // 主人死了/消失了，自己也散掉
        if (this.owner != null && !this.owner.isAlive()) {
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.SQUID_INK,
                    this.getX(), this.getY() + 0.5D, this.getZ(), 8, 0.3D, 0.3D, 0.3D, 0.02D);
            this.discard();
            return;
        }
        if (this.tickCount % 10 == 0) {
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE,
                    this.getX(), this.getY() + 0.6D, this.getZ(), 2, 0.25D, 0.2D, 0.25D, 0.0D);
        }
        this.tickSelfDestruct();
    }

    /** 贴近玩家就自爆（用户要求：阵仆会自爆或砸阵眼）。 */
    private void tickSelfDestruct() {
        if (this.tickCount < 40) {
            return;
        }
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        for (Player p : level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(2.2D))) {
            if (p.distanceTo(this) > 2.0D) {
                continue;
            }
            level.playSound(null, this.blockPosition(), SoundEvents.GENERIC_EXPLODE,
                    SoundSource.HOSTILE, 1.2F, 1.2F);
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER,
                    this.getX(), this.getY() + 0.6D, this.getZ(), 2, 0.3D, 0.2D, 0.3D, 0.0D);
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.SQUID_INK,
                    this.getX(), this.getY() + 0.5D, this.getZ(), 20, 0.6D, 0.4D, 0.6D, 0.02D);
            for (Player victim : level.getEntitiesOfClass(Player.class,
                    this.getBoundingBox().inflate(3.0D))) {
                victim.hurt(this.damageSources().mobAttack(this), 6.0F);
            }
            this.discard();
            return;
        }
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        if (this.random.nextFloat() < 0.25F) {
            this.spawnAtLocation(com.lengyang.fulu.registry.ModItems.DEMON_SHARD.get());
        }
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }
}
