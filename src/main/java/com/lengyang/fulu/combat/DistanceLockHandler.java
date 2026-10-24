package com.lengyang.fulu.combat;

import com.lengyang.fulu.FuLu;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 咫尺天涯阵：锁住"怪物与玩家"的距离 —— 它一直在走，但永远到不了你身边。
 *
 * <p>做法：怪物锁定玩家时，把**当时的水平距离**记下来（存进它的持久数据）；
 * 之后每刻检查，如果距离变了，就把它**拉回那条线上、距离不变的位置**。
 *
 * <p>为什么不用 {@code setPos}：这里就是纯粹的"空间锁定"，不需要碰撞；
 * 用 {@code teleportTo} 才能正确更新服务端的追踪与寻路状态，避免它"卡在方块里"。
 *
 * <p>BOSS 免疫（用户允许的选项），玩家自己不受影响。
 */
@Mod.EventBusSubscriber(modid = FuLu.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DistanceLockHandler {

    /** 记住锁定距离的 key。 */
    private static final String K_LOCKED = "FuluDistanceLocked";

    /** 投射物锁定：固定停在这个距离（格）。 */
    private static final double PROJ_LOCK = 5.0D;
    /** 投射物身上记的 key（记"锁定距离"）。 */
    private static final String K_PROJ = "FuluDistanceProjLock";

    /** 正在被锁住的投射物（每个维度一份）。 */
    private static final java.util.Map<net.minecraft.world.level.Level,
            java.util.Set<net.minecraft.world.entity.Entity>> LOCKED_PROJECTILES =
            new java.util.concurrent.ConcurrentHashMap<>();
    /** 距离容差：小于这个值就不动它，避免每刻抖动。 */
    private static final double TOLERANCE = 0.15D;
    /** 锁定距离的范围（太近或太远都不合理）。 */
    private static final double MIN_LOCK = 2.0D;
    private static final double MAX_LOCK = 12.0D;

    private static final boolean DEBUG_LOG = false;

    private DistanceLockHandler() {
    }

    /**
     * 投射物一进世界就判断：是不是**带咫尺标记的怪物**射的？
     *
     * <p>是的话，记下它此刻到最近玩家的距离，然后把它加进"被锁住"的表里 ——
     * 之后每刻由 {@link #onLevelTick} 把它拉回那个距离。
     */
    /**
     * 范围攻击也挡住：被锁住的怪物对"它锁定的那个玩家"造成的**任何伤害**都取消。
     *
     * <p>覆盖近战、投射物、爆炸、AOE —— 因为"咫尺天涯"的语义就是**永远碰不到**。
     * 玩家自己打它、它打别的玩家，都不受影响。
     */
    @SubscribeEvent
    public static void onLockedDamage(net.minecraftforge.event.entity.living.LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        // 攻击者（含爆炸/投射物的来源）
        net.minecraft.world.entity.Entity src = event.getSource().getEntity();
        if (!(src instanceof Mob mob)) {
            return;
        }
        if (!FormationMarks.isMarked(level, mob, FormationMarks.DISTANCE)) {
            return;
        }
        // 它锁定的对象就是自己吗？（有目标且目标是自己）
        if (mob.getTarget() != player) {
            return;
        }
        event.setCanceled(true);
        level.sendParticles(ParticleTypes.PORTAL,
                player.getX(), player.getY() + 1.0D, player.getZ(),
                8, 0.3D, 0.4D, 0.3D, 0.02D);
    }

    @SubscribeEvent
    public static void onProjectileJoin(net.minecraftforge.event.entity.EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof net.minecraft.world.entity.projectile.Projectile proj)) {
            return;
        }
        if (!(proj.getOwner() instanceof Mob shooter)) {
            return;                                  // 玩家自己射的不管
        }
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (!FormationMarks.isMarked(level, shooter, FormationMarks.DISTANCE)) {
            return;                                  // 射手不在阵法里
        }
        // 找最近的玩家
        ServerPlayer nearest = null;
        double best = Double.MAX_VALUE;
        for (ServerPlayer p : level.players()) {
            double d = p.distanceToSqr(proj);
            if (d < best) {
                best = d;
                nearest = p;
            }
        }
        if (nearest == null) {
            return;
        }
        double dist = Math.sqrt(best);
        double locked = Math.max(2.0D, Math.min(PROJ_LOCK, dist));
        proj.getPersistentData().putDouble(K_PROJ, locked);
        LOCKED_PROJECTILES.computeIfAbsent(level, k -> java.util.concurrent.ConcurrentHashMap.newKeySet())
                .add(proj);
        proj.setDeltaMovement(0.0D, 0.0D, 0.0D);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL,
                proj.getX(), proj.getY(), proj.getZ(), 10, 0.3D, 0.3D, 0.3D, 0.02D);
    }

    /** 被锁住的投射物每刻：拉回固定距离 + 抖动 + 不许命中。 */
    @SubscribeEvent
    public static void onLevelTick(net.minecraftforge.event.TickEvent.LevelTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) {
            return;
        }
        if (!(event.level instanceof ServerLevel level)) {
            return;
        }
        var set = LOCKED_PROJECTILES.get(level);
        if (set == null || set.isEmpty()) {
            return;
        }
        set.removeIf(p -> !p.isAlive());

        for (net.minecraft.world.entity.Entity e : set) {
            ServerPlayer nearest = null;
            double best = Double.MAX_VALUE;
            for (ServerPlayer p : level.players()) {
                double d = p.distanceToSqr(e);
                if (d < best) {
                    best = d;
                    nearest = p;
                }
            }
            if (nearest == null) {
                continue;
            }
            double locked = e.getPersistentData().getDouble(K_PROJ);
            double dx = e.getX() - nearest.getX();
            double dz = e.getZ() - nearest.getZ();
            double dist = Math.hypot(dx, dz);
            double nx = dist < 0.01D ? 1.0D : dx / dist;
            double nz = dist < 0.01D ? 0.0D : dz / dist;
            // 抖动：每刻一点点随机位移，像在挣扎
            double jx = (level.random.nextDouble() - 0.5D) * 0.06D;
            double jz = (level.random.nextDouble() - 0.5D) * 0.06D;
            e.teleportTo(nearest.getX() + nx * locked + jx, e.getY(), nearest.getZ() + nz * locked + jz);
            e.setDeltaMovement(0.0D, 0.0D, 0.0D);     // 它"飞不动"，只是悬着
            if (e.tickCount % 4 == 0) {
                level.sendParticles(ParticleTypes.PORTAL,
                        e.getX(), e.getY(), e.getZ(), 2, 0.1D, 0.1D, 0.1D, 0.0D);
            }
        }
    }

    /** 被锁住的投射物不许命中任何东西。 */
    @SubscribeEvent
    public static void onImpact(net.minecraftforge.event.entity.ProjectileImpactEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)) {
            return;
        }
        var set = LOCKED_PROJECTILES.get(level);
        if (set != null && set.contains(event.getProjectile())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity e = event.getEntity();
        if (e instanceof Player) {
            return;                                  // 玩家不受影响
        }
        if (!(e instanceof Mob mob)) {
            return;
        }
        if (!(mob.level() instanceof ServerLevel level)) {
            return;
        }
        if (FormationMarks.isImmune(mob)) {
            return;                                  // 只有末影龙免疫
        }
        if (!FormationMarks.isMarked(level, mob, FormationMarks.DISTANCE)) {
            // 离开阵法：忘掉锁定距离，下次重新锁
            mob.getPersistentData().remove(K_LOCKED);
            return;
        }
        // BOSS 免疫
        // 只对"正在锁玩家"的怪生效
        if (!(mob.getTarget() instanceof Player player)) {
            mob.getPersistentData().remove(K_LOCKED);
            return;
        }

        double dx = mob.getX() - player.getX();
        double dz = mob.getZ() - player.getZ();
        double dist = Math.hypot(dx, dz);

        double locked;
        if (mob.getPersistentData().contains(K_LOCKED)) {
            locked = mob.getPersistentData().getDouble(K_LOCKED);
        } else {
            // 第一次锁定：用当前距离
            locked = Math.max(MIN_LOCK, Math.min(MAX_LOCK, dist));
            mob.getPersistentData().putDouble(K_LOCKED, locked);
            level.sendParticles(ParticleTypes.REVERSE_PORTAL,
                    mob.getX(), mob.getY() + 1.0D, mob.getZ(), 12, 0.4D, 0.4D, 0.4D, 0.03D);
        }

        if (Math.abs(dist - locked) <= TOLERANCE) {
            particles(level, mob, player);
            return;
        }

        // 把怪物拉回"玩家 -> 怪物"这条线上、距离 = locked 的位置（高度保持不变）
        double nx = dist < 0.01D ? 1.0D : dx / dist;
        double nz = dist < 0.01D ? 0.0D : dz / dist;
        double tx = player.getX() + nx * locked;
        double tz = player.getZ() + nz * locked;
        mob.teleportTo(tx, mob.getY(), tz);

        // 视觉：两点之间一串淡紫粒子（表示"咫尺天涯"）
        particles(level, mob, player);

        if (DEBUG_LOG && mob.tickCount % 40 == 0) {
            FuLu.LOGGER.info("[distance] {} locked at {} (now {})",
                    mob.getName().getString(), String.format("%.1f", locked),
                    String.format("%.1f", dist));
        }
    }

    /** 怪物脚下 + 怪物与玩家之间的连线粒子。 */
    private static void particles(ServerLevel level, Mob mob, Player player) {
        if (mob.tickCount % 4 != 0) {
            return;
        }
        level.sendParticles(ParticleTypes.PORTAL,
                mob.getX(), mob.getY() + 0.1D, mob.getZ(),
                3, 0.25D, 0.05D, 0.25D, 0.0D);
        // 连线：从怪物到玩家插 8 个点
        Vec3 a = mob.position().add(0.0D, mob.getBbHeight() * 0.6D, 0.0D);
        Vec3 b = player.position().add(0.0D, 1.0D, 0.0D);
        for (int i = 1; i < 8; i++) {
            double t = (double) i / 8.0D;
            level.sendParticles(ParticleTypes.END_ROD,
                    a.x + (b.x - a.x) * t,
                    a.y + (b.y - a.y) * t,
                    a.z + (b.z - a.z) * t,
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }
}
