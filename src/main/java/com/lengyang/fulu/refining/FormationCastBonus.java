package com.lengyang.fulu.refining;

import com.lengyang.fulu.block.FormationEyeBlock;
import com.lengyang.fulu.block.TalismanKind;
import com.lengyang.fulu.client.render.FormationBarrierRenderer;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * **阵法联动**：妖丹技能在特定阵法里有加成。
 *
 * <p>判定方式：取客户端同步过来的阵眼列表（`FormationBarrierRenderer.eyesView()`），
 * 逐个比对该阵眼的符种类与玩家的距离（10 格内）。
 *
 * <p>四种联动：
 * <ul>
 *   <li><b>炼妖阵</b>：技能冷却**减半**；</li>
 *   <li><b>飞剑阵</b>：火球爆炸时**额外放出飞剑**；</li>
 *   <li><b>咫尺天涯阵</b>：瞬移**距离翻倍**，并在原地留下假身干扰仇恨；</li>
 *   <li><b>替身阵</b>：自爆**不会伤到自己</b>。</li>
 * </ul>
 */
public final class FormationCastBonus {

    /** 阵眼生效半径（和阵法一致）。 */
    private static final double RADIUS_SQ = 10.0D * 10.0D;

    private FormationCastBonus() {
    }

    /** 玩家附近有没有某种符的阵眼。 */
    public static boolean near(ServerPlayer player, TalismanKind kind) {
        for (BlockPos pos : FormationBarrierRenderer.eyesView()) {
            BlockState st = player.level().getBlockState(pos);
            if (!(st.getBlock() instanceof FormationEyeBlock)) {
                continue;
            }
            if (st.getValue(FormationEyeBlock.TALISMAN) != kind) {
                continue;
            }
            if (player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D)
                    <= RADIUS_SQ) {
                return true;
            }
        }
        return false;
    }

    /** 冷却倍率：炼妖阵内减半。 */
    public static double cooldownMultiplier(ServerPlayer player) {
        return near(player, TalismanKind.REFINING) ? 0.5D : 1.0D;
    }

    /** 瞬移距离倍率：咫尺天涯阵内翻倍。 */
    public static double teleportMultiplier(ServerPlayer player) {
        return near(player, TalismanKind.DISTANCE) ? 2.0D : 1.0D;
    }

    /** 在飞剑阵内施法。 */
    public static boolean inSwordFormation(ServerPlayer player) {
        return near(player, TalismanKind.SWORD);
    }

    /** 在替身阵内施法。 */
    public static boolean inStandInFormation(ServerPlayer player) {
        return near(player, TalismanKind.STAND_IN);
    }

    /**
     * 在 {@code at} 位置生成一具**假身**，并把附近怪物的仇恨转过去。
     *
     * <p>假身是一个真实体（{@code DecoyEntity}）：无敌、无 AI、5 秒后消失。
     * 怪物被 {@code setTarget(假身)} 之后会真的**跑过去砍它**，
     * 而不是像"清仇恨"那样下一秒又锁回玩家。
     */
    public static void leaveDecoyAt(ServerLevel level, Vec3 at) {
        var decoy = new com.lengyang.fulu.entity.DecoyEntity(
                com.lengyang.fulu.registry.ModEntities.DECOY.get(), level);
        decoy.moveTo(at.x, at.y, at.z, level.random.nextFloat() * 360.0F, 0.0F);
        decoy.setInvulnerable(true);
        level.addFreshEntity(decoy);

        // 把 16 格内**正在追玩家**的怪改成追假身
        int pulled = 0;
        for (var mob : level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class,
                decoy.getBoundingBox().inflate(16.0D))) {
            if (mob instanceof com.lengyang.fulu.entity.DecoyEntity) {
                continue;
            }
            if (mob.getTarget() instanceof net.minecraft.world.entity.player.Player) {
                mob.setTarget(decoy);
                pulled++;
            }
        }

        level.sendParticles(net.minecraft.core.particles.ParticleTypes.PORTAL,
                at.x, at.y + 1.0D, at.z, 60, 0.35D, 0.7D, 0.35D, 0.02D);
        level.playSound(null, net.minecraft.core.BlockPos.containing(at),
                net.minecraft.sounds.SoundEvents.ENDERMAN_AMBIENT,
                net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 0.7F);
        com.lengyang.fulu.FuLu.LOGGER.info("[cast] decoy spawned, {} mob(s) retargeted", pulled);
    }
}
