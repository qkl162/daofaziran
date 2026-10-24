package com.lengyang.fulu.refining;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.lengyang.fulu.FuLu;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * 【陨石天降 / 毁灭火雨】：指定地点、延迟 1.5 秒的毁灭性范围技能。
 *
 * <pre>
 *   按下 Z → 锁定准星/前方 10 格的地面
 *          → 立刻画半径 6 的**红黑预警圈** + 恶魂尖啸（30 刻 = 1.5 秒）
 *          → 正上方 30 格生成陨石，垂直砸下（带浓烟/岩浆拖尾 + 呼啸）
 *          → 落地：30 → 10 点真实伤害（中心向外递减）、强击飞、
 *                  震屏 20 格、碎块飞溅、6 格火海灼烧 6 秒
 * </pre>
 *
 * <p>施法代价：自损 4 点 + 虚弱 2 秒；冷却 30 秒（见 {@code EssenceKind}）。
 */
public final class MeteorStrike {

    /** 预警时长（刻）：1.5 秒。 */
    public static final int WARN_TICKS = 30;
    /** 预警圈半径（格）。 */
    public static final double RADIUS = 6.0D;
    /** 陨石生成高度（预警圈正上方）。 */
    private static final double SPAWN_HEIGHT = 30.0D;
    /** 中心伤害 / 边缘伤害。 */
    private static final float CENTER_DAMAGE = 30.0F;
    private static final float EDGE_DAMAGE = 10.0F;

    /** 一次待落地打击。 */
    private static final class Strike {
        final ServerPlayer caster;
        final Vec3 target;
        int ticksLeft = WARN_TICKS;
        boolean fired;
        boolean fireDone;

        Strike(ServerPlayer caster, Vec3 target) {
            this.caster = caster;
            this.target = target;
        }
    }

    private static final List<Strike> ACTIVE = new ArrayList<>();

    private MeteorStrike() {
    }

    /** 选取落点：准星指着的方块，否者前方 10 格。 */
    public static Vec3 pickTarget(ServerPlayer player) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(40.0D));
        BlockHitResult hit = player.level().clip(new net.minecraft.world.level.ClipContext(
                eye, end, net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE, player));
        if (hit.getType() == HitResult.Type.BLOCK) {
            return hit.getLocation();
        }
        Vec3 forward = player.position().add(player.getLookAngle().scale(10.0D));
        return new Vec3(forward.x, player.getY(), forward.z);
    }

    /** 开始施法（按 Z 时调用）。 */
    public static void cast(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 target = pickTarget(player);
        ACTIVE.add(new Strike(player, target));

        // 恶魂尖啸
        level.playSound(null, BlockPos.containing(target), SoundEvents.GHAST_SCREAM,
                SoundSource.PLAYERS, 3.0F, 0.7F);
        level.playSound(null, player.blockPosition(), SoundEvents.GHAST_WARN,
                SoundSource.PLAYERS, 2.0F, 0.6F);

        // 立刻画出预警圈（红 + 黑）
        ring(level, target, RADIUS, 1.0F, 0.15F, 0.10F, 1.6F);
        ring(level, target, RADIUS * 0.55D, 0.15F, 0.05F, 0.05F, 1.2F);

        // **施法代价**：自损 4 点 + 虚弱 2 秒
        player.invulnerableTime = 0;
        player.hurt(level.damageSources().magic(), 4.0F);
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0));
        player.displayClientMessage(
                net.minecraft.network.chat.Component.translatable("msg.fulu.meteor.cost"), true);

        FuLu.LOGGER.info("[meteor] cast at {}", target);
    }

    /** 每刻推进。 */
    public static void tick(ServerLevel level) {
        // 预警圈：每刻补一层粒子，越接近落地越密
        for (Strike s : new ArrayList<>(ACTIVE)) {
            if (s.caster.level() != level) {
                continue;
            }
            if (!s.fired) {
                float k = 1.0F - s.ticksLeft / (float) WARN_TICKS;
                if (level.getGameTime() % 2 == 0) {
                    ring(level, s.target, RADIUS, 1.0F, 0.15F, 0.10F, 1.2F + k);
                }
                if (s.ticksLeft % 6 == 0) {
                    level.playSound(null, BlockPos.containing(s.target),
                            SoundEvents.GHAST_AMBIENT, SoundSource.PLAYERS, 1.4F, 0.6F);
                }
                if (--s.ticksLeft <= 0) {
                    spawnMeteor(level, s);
                    s.fired = true;
                }
            } else {
                ACTIVE.remove(s);
            }
        }

    }

    /** 生成陨石。 */
    private static void spawnMeteor(ServerLevel level, Strike s) {
        Vec3 spawn = s.target.add(0.0D, SPAWN_HEIGHT, 0.0D);
        var meteor = new com.lengyang.fulu.entity.MeteorEntity(
                com.lengyang.fulu.registry.ModEntities.METEOR.get(), level);
        meteor.setup(s.caster, spawn, s.target.y, 2.2F);
        level.addFreshEntity(meteor);

        level.playSound(null, BlockPos.containing(s.target), SoundEvents.PHANTOM_SWOOP,
                SoundSource.PLAYERS, 3.0F, 0.5F);
        level.playSound(null, BlockPos.containing(s.target), SoundEvents.ENDER_DRAGON_FLAP,
                SoundSource.PLAYERS, 3.0F, 0.4F);
        FuLu.LOGGER.info("[meteor] falling at {}", s.target);
    }

    /** 陨石落地：伤害 + 击飞 + 震屏 + 碎块 + 火海。 */
    public static void impact(ServerLevel level, com.lengyang.fulu.entity.MeteorEntity meteor) {
        Vec3 p = meteor.position();
        ServerPlayer caster = meteor.getCasterId() == null ? null
                : level.getServer().getPlayerList().getPlayer(meteor.getCasterId());
        FuLu.LOGGER.info("[meteor] impact at {}", p);

        // ---- 范围真实伤害（中心 30 -> 边缘 10）
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(p, p).inflate(RADIUS, 6.0D, RADIUS))) {
            if (!e.isAlive()) {
                continue;
            }
            // **施法者免疫自己陨石的爆炸与击飞**（用户反馈：根本跑不出来，会被自己炸死）
            if (caster != null && e.getUUID().equals(caster.getUUID())) {
                continue;
            }
            double d = Math.min(RADIUS, e.position().distanceTo(p));
            float dmg = CENTER_DAMAGE - (CENTER_DAMAGE - EDGE_DAMAGE) * (float) (d / RADIUS);
            e.invulnerableTime = 0;
            e.hurt(level.damageSources().magic(), dmg);
            // 强击飞：向外 + 向上
            Vec3 push = e.position().subtract(p).normalize().scale(1.4D);
            e.push(push.x, 1.15D, push.z);
            e.hurtMarked = true;
        }

        // ---- 震屏（20 格内的玩家）
        var shake = new com.lengyang.fulu.network.ScreenShakePacket(14, 2.4F);
        for (ServerPlayer other : level.players()) {
            if (other.distanceToSqr(p) < 20.0D * 20.0D) {
                com.lengyang.fulu.network.ModNetwork.CHANNEL.send(
                        net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> other),
                        shake);
            }
        }

        // **点燃蛛网**：陨石落地同样会引爆附近的盘丝天牢
        com.lengyang.fulu.entity.WebPrisonEntity.annihilateAt(level, p);

        // ---- 音效：爆炸 + 低频耳鸣
        level.playSound(null, BlockPos.containing(p), SoundEvents.GENERIC_EXPLODE,
                SoundSource.PLAYERS, 4.0F, 0.6F);
        level.playSound(null, BlockPos.containing(p), SoundEvents.WARDEN_HEARTBEAT,
                SoundSource.PLAYERS, 2.0F, 0.5F);

        // ---- 碎块飞溅（第一版：FallingBlockEntity.fall，会真的挖掉方块）
        BlockPos center = BlockPos.containing(p);
        int spawned = 0;
        for (int i = 0; i < 45; i++) {
            int dx = level.random.nextInt(9) - 4;
            int dz = level.random.nextInt(9) - 4;
            int dy = level.random.nextInt(3) - 1;
            BlockPos bp = center.offset(dx, dy, dz);
            BlockState st = level.getBlockState(bp);
            if (st.isAir() || st.getDestroySpeed(level, bp) < 0.0F) {
                continue;
            }
            var fb = net.minecraft.world.entity.item.FallingBlockEntity.fall(level, bp, st);
            fb.dropItem = false;
            fb.push((level.random.nextDouble() - 0.5D) * 1.1D,
                    0.6D + level.random.nextDouble() * 0.9D,
                    (level.random.nextDouble() - 0.5D) * 1.1D);
            fb.hurtMarked = true;
            spawned++;
        }
        FuLu.LOGGER.info("[meteor] debris spawned: {}", spawned);

        // ---- 爆炸粒子
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, p.x, p.y + 1.0D, p.z,
                6, 2.0D, 0.5D, 2.0D, 0.0D);
        level.sendParticles(ParticleTypes.LAVA, p.x, p.y + 0.5D, p.z,
                80, 3.0D, 1.0D, 3.0D, 0.4D);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, p.x, p.y + 1.0D, p.z,
                60, 3.0D, 1.5D, 3.0D, 0.05D);

        // ---- 火海（6 格 / 6 秒）
        // 火海：生成**实体**（贴图渲染，不依赖粒子系统）
        var field = new com.lengyang.fulu.entity.FireFieldEntity(
                com.lengyang.fulu.registry.ModEntities.FIRE_FIELD.get(), level);
        field.setPos(p.x, p.y + 0.1D, p.z);
        level.addFreshEntity(field);
        FuLu.LOGGER.info("[meteor] fire field placed, players shaken, caster={}",
                caster == null ? "?" : caster.getName().getString());
    }

    /** 画一圈粒子（红黑预警 / 火环）。 */
    private static void ring(ServerLevel level, Vec3 center, double radius,
                             float r, float g, float b, float scale) {
        DustParticleOptions dust = new DustParticleOptions(new Vector3f(r, g, b), scale);
        int points = 72;
        for (int i = 0; i < points; i++) {
            double a = Math.PI * 2.0D * i / points;
            level.sendParticles(dust,
                    center.x + Math.cos(a) * radius, center.y + 0.1D,
                    center.z + Math.sin(a) * radius, 1, 0.0D, 0.02D, 0.0D, 0.0D);
        }
    }
}
