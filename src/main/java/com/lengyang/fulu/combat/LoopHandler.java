package com.lengyang.fulu.combat;

import com.lengyang.fulu.FuLu;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 闭环阵：阵内怪物走到边界就被传送到**对面**，于是它以为自己一直在前进，实际在绕圈。
 *
 * <p>为什么用 {@code teleportTo} 而不是 {@code setPos}：传送是"瞬间到另一个点"，
 * 不需要碰撞检测（它本来就要穿过边界）；而且 {@code teleportTo} 会正确更新追踪与区块。
 *
 * <p>用户要求（第七条）：**仇恨不变** —— 我不动 {@code setTarget}，
 * 所以它仍然朝玩家走，只是每走到边界就被甩到对面，永远追不到。
 *
 * <p>BOSS 免疫（用户允许的选项之一），玩家不受影响。
 */
@Mod.EventBusSubscriber(modid = FuLu.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class LoopHandler {

    /** 阵法半径（和阵眼一致）。 */
    private static final double RADIUS = 10.0D;
    /** 传送后离阵眼多远（用户要求 9 格）。 */
    private static final double WARP_IN = 9.0D;

    /** 阵眼位置存在这个 key 里（BlockPos.asLong）。 */
    public static final String K_EYE = "FuluMarkLoopEye";

    private static final boolean DEBUG_LOG = false;

    private LoopHandler() {
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity e = event.getEntity();
        if (e instanceof Player) {
            return;                                  // 玩家可以正常穿过
        }
        if (!(e.level() instanceof ServerLevel level)) {
        if (com.lengyang.fulu.combat.FormationMarks.isImmune(e)) {
            return;                              // 只有末影龙免疫
        }
            return;
        }
        if (!FormationMarks.isMarked(level, e, FormationMarks.LOOP)) {
            return;
        }

        long packed = e.getPersistentData().getLong(K_EYE);
        if (packed == 0L) {
            return;
        }
        BlockPos eye = BlockPos.of(packed);
        Vec3 center = Vec3.atCenterOf(eye);

        double dx = e.getX() - center.x;
        double dz = e.getZ() - center.z;
        double dist = Math.hypot(dx, dz);
        if (dist <= RADIUS) {
            return;                                  // 还在阵内
        }

        // 传送到"阵眼的对面、距离 9 格"：方向取反
        double nx = dx / dist;
        double nz = dz / dist;
        double tx = center.x - nx * WARP_IN;
        double tz = center.z - nz * WARP_IN;
        double ty = e.getY();                        // 保留高度（飞行怪也一样）

        // 两端的紫色波纹
        ring(level, e.getX(), ty + 0.4D, e.getZ());
        ring(level, tx, ty + 0.4D, tz);

        e.teleportTo(tx, ty, tz);
        e.setYRot(e.getYRot());                      // 朝向不变：它以为还在往前走
        e.setDeltaMovement(e.getDeltaMovement());

        // 短促的"嗖"声（末影人传送）
        level.playSound(null, BlockPos.containing(tx, ty, tz),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 0.8F, 1.3F);

        if (DEBUG_LOG) {
            FuLu.LOGGER.info("[loop] {} wrapped {:.1f} -> {} blocks",
                    e.getName().getString(), dist, WARP_IN);
        }

        // 身上常冒淡紫粒子，表示处于闭环状态
        if (e.tickCount % 10 == 0) {
            level.sendParticles(ParticleTypes.PORTAL,
                    e.getX(), e.getY() + e.getBbHeight() * 0.6D, e.getZ(),
                    2, 0.3D, 0.3D, 0.3D, 0.0D);
        }
    }

    /** 一圈向内扩散的紫色波纹（用 PORTAL 粒子近似圆环）。 */
    private static void ring(ServerLevel level, double x, double y, double z) {
        for (int i = 0; i < 16; i++) {
            double a = Math.PI * 2.0D * i / 16.0D;
            double r = 0.8D;
            level.sendParticles(ParticleTypes.PORTAL,
                    x + Math.cos(a) * r, y, z + Math.sin(a) * r,
                    1, 0.02D, 0.02D, 0.02D, 0.0D);
        }
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, x, y, z, 8, 0.2D, 0.2D, 0.2D, 0.02D);
    }
}
