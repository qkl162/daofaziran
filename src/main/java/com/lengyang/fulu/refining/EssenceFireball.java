package com.lengyang.fulu.refining;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 烈焰妖丹的火球：**烈焰人的火球外形**（{@link SmallFireball}），
 * 命中生物或方块 -> **10 格范围伤害**，**不破坏方块**，并炸出一圈营火烟余波。
 *
 * <p>为什么用 {@code SmallFireball}：原版烈焰人喷的就是它，模型/拖尾粒子都现成。
 * 但它的 {@code onHitBlock} 会调用 {@code level.explode(...)}（会烧地形），
 * 所以这里**完全覆盖** onHit 系列，不调用父类实现。
 */
public class EssenceFireball extends SmallFireball {

    /** 范围伤害半径（格）。 */
    private static final double AOE_RADIUS = 10.0D;
    /** 中心伤害；越靠边衰减到 40%。 */
    private static final float AOE_DAMAGE = 12.0F;

    public EssenceFireball(Level level, LivingEntity owner, double dx, double dy, double dz) {
        super(level, owner, dx, dy, dz);
        this.setDeltaMovement(dx, dy, dz);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        // 不调 super：父类会走原版燃烧/爆炸逻辑
        detonate();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        // 不调 super：父类会 level.explode(...) 并烧方块
        detonate();
    }

    /** 每刻追踪最近的敌对生物（慢速追踪，转向速度有限）。 */
    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        if (this.tickCount % 2 == 0) {
            LivingEntity best = null;
            double bestD = 18.0D * 18.0D;
            for (LivingEntity e : this.level().getEntitiesOfClass(LivingEntity.class,
                    this.getBoundingBox().inflate(18.0D))) {
                if (e == this.getOwner() || !(e instanceof net.minecraft.world.entity.monster.Enemy)
                        || !e.isAlive()) {
                    continue;
                }
                double d = e.distanceToSqr(this);
                if (d < bestD) {
                    bestD = d;
                    best = e;
                }
            }
            if (best != null) {
                var want = best.position().add(0.0D, best.getBbHeight() * 0.5D, 0.0D)
                        .subtract(this.position()).normalize();
                var cur = this.getDeltaMovement();
                // 慢速追踪：每刻只把速度方向往目标拧一点
                var blended = cur.scale(0.82D).add(want.scale(0.18D * cur.length()));
                this.setDeltaMovement(blended);
                this.hurtMarked = true;
            }
        }
    }

    private void detonate() {
        Level level = this.level();
        if (level instanceof ServerLevel server) {
            Vec3 pos = this.position();
            Entity owner = this.getOwner();

            // ---- 范围伤害
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class,
                    this.getBoundingBox().inflate(AOE_RADIUS))) {
                if (e == owner || !e.isAlive()) {
                    continue;
                }
                double d = e.distanceTo(this);
                if (d > AOE_RADIUS) {
                    continue;
                }
                float factor = (float) (1.0D - 0.6D * (d / AOE_RADIUS));
                float amount = AOE_DAMAGE * factor;
                e.invulnerableTime = 0;                       // 清无敌帧，保证命中必掉血
                boolean hit;
                if (owner instanceof LivingEntity le) {
                    hit = e.hurt(level.damageSources().indirectMagic(this, le), amount);
                } else {
                    hit = e.hurt(level.damageSources().magic(), amount);
                }
                com.lengyang.fulu.FuLu.LOGGER.info(
                        "[blaze] hit {} for {} (applied={})", e.getType(), amount, hit);
                // **阴火状态**：强行施加（无视免疫），每秒 3% 最大生命真伤
                NetherfireState.apply(e, 200);
            }

            // ---- 爆炸中心
            server.sendParticles(ParticleTypes.EXPLOSION_EMITTER, pos.x, pos.y, pos.z,
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
            server.sendParticles(ParticleTypes.FLAME, pos.x, pos.y, pos.z,
                    90, AOE_RADIUS * 0.5D, 1.2D, AOE_RADIUS * 0.5D, 0.06D);

            // ---- **余波圆环**：由内向外扩散的营火烟
            // 逐圈给粒子，半径 2 到 10，每圈 48 个点，形成"冲击波"观感
            for (double ring = 2.0D; ring <= AOE_RADIUS; ring += 1.5D) {
                int points = (int) Math.max(16, ring * 6);
                for (int i = 0; i < points; i++) {
                    double a = Math.PI * 2.0D * i / points;
                    double x = pos.x + Math.cos(a) * ring;
                    double z = pos.z + Math.sin(a) * ring;
                    server.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                            x, pos.y + 0.15D, z, 1,
                            0.0D, 0.02D, 0.0D, 0.002D);
                }
            }
            // 再补一圈向上飘的烟，让"余波"有体积感
            for (int i = 0; i < 60; i++) {
                double a = Math.PI * 2.0D * i / 60.0D;
                double r = AOE_RADIUS * 0.85D;
                server.sendParticles(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE,
                        pos.x + Math.cos(a) * r, pos.y + 0.4D, pos.z + Math.sin(a) * r,
                        1, 0.0D, 0.03D, 0.0D, 0.01D);
            }

            // **点燃蛛网**：火焰类技能命中会引爆附近的盘丝天牢
            com.lengyang.fulu.entity.WebPrisonEntity.annihilateAt(server, pos);
            level.playSound(null, this.blockPosition(), SoundEvents.GENERIC_EXPLODE,
                    SoundSource.PLAYERS, 1.6F, 0.9F);
        }
        this.discard();
    }
}
