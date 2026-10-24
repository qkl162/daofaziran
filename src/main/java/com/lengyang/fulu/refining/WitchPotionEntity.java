package com.lengyang.fulu.refining;

import com.lengyang.fulu.registry.ModMobEffects;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 女巫妖丹的**诡计毒瓶**：比原版滞留药水更狠。
 *
 * <p>原版滞留药水在落地时由 {@code ThrownPotion.splash()} 自己生成毒云，
 * 半径 3、持续 200 刻，控制不了。这里覆盖 {@code onHit}：
 * <ul>
 *   <li>落地瞬间先来一次 **6 格爆发伤害**（砸脸惩罚）；</li>
 *   <li>再生成一片**自己控制**的毒云：半径 5、持续 350 刻；</li>
 *   <li>云里同时挂**缓慢**和**反胃**（"诡计"的感觉）；</li>
 *   <li>最后仍然调用父类，让药水本身的瓶碎效果正常播放。</li>
 * </ul>
 */
public class WitchPotionEntity extends ThrownPotion {

    /** 落地爆发半径。 */
    private static final double BURST_RADIUS = 6.0D;
    /** 落地爆发伤害。 */
    private static final float BURST_DAMAGE = 8.0F;

    public WitchPotionEntity(Level level, LivingEntity owner) {
        super(level, owner);
    }

    @Override
    protected void onHit(HitResult result) {
        Level level = this.level();
        if (level instanceof ServerLevel server) {
            Vec3 pos = result.getLocation();

            // ---- 1. 落地爆发
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class,
                    this.getBoundingBox().inflate(BURST_RADIUS))) {
                if (e == this.getOwner() || !e.isAlive()) {
                    continue;
                }
                if (e.distanceToSqr(pos.x, pos.y, pos.z) > BURST_RADIUS * BURST_RADIUS) {
                    continue;
                }
                Entity owner = this.getOwner();
                if (owner instanceof LivingEntity le) {
                    e.hurt(level.damageSources().indirectMagic(this, le), BURST_DAMAGE);
                } else {
                    e.hurt(level.damageSources().magic(), BURST_DAMAGE);
                }
            }
            server.sendParticles(ParticleTypes.ITEM_SLIME, pos.x, pos.y + 0.2D, pos.z,
                    60, 1.2D, 0.4D, 1.2D, 0.06D);

            // ---- 2. 自己控制的毒云（比原版大、久、还带减速与反胃）
            AreaEffectCloud cloud = new AreaEffectCloud(level, pos.x, pos.y, pos.z);
            cloud.setRadius(5.0F);
            cloud.setDuration(350);
            cloud.setRadiusPerTick(-0.002F);
            cloud.setParticle(ParticleTypes.ITEM_SLIME);
            cloud.addEffect(new MobEffectInstance(ModMobEffects.WITCH_POISON.get(), 300, 0));
            cloud.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 1));
            cloud.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 0));
            server.addFreshEntity(cloud);

            level.playSound(null, this.blockPosition(), SoundEvents.BREWING_STAND_BREW,
                    SoundSource.PLAYERS, 1.4F, 0.8F);
        }
        super.onHit(result);
    }
}
