package com.lengyang.fulu.combat;

import com.lengyang.fulu.FuLu;
import com.lengyang.fulu.registry.ModMobEffects;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 滑地阵：阵内怪物像踩在冰上。
 *
 * <p><b>Minecraft 没有"每实体摩擦力"这种东西</b>，所以不能真的改摩擦。这里的做法是：
 * 在实体每刻（{@link LivingEvent.LivingTickEvent}，已经跑完 AI 和移动）把**上一刻的位移**
 * 当成惯性再推一次 —— 于是：
 * <ul>
 *   <li>它往前走会**冲过头**（位移被重复施加）；</li>
 *   <li>它想转弯时**转不回来**（旧方向还在推）；</li>
 *   <li>被击退时**滑得更远**（击退本身也是位移）。</li>
 * </ul>
 * 这比"改 AI 参数"更省事，也不会和怪物自己的寻路打架。
 *
 * <p>排除：**飞行怪**（用户要求无影响）、**玩家**（只对怪物生效）、
 * **BOSS 只受一半**（末影龙、凋灵）。
 */
@Mod.EventBusSubscriber(modid = FuLu.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SlipperyHandler {

    /** 滑行速度的保持率（越大越滑）。0.90 ≈ 摩擦降到 10%。 */
    private static final double KEEP = 0.90D;
    /** 每次叠加的位移比例。 */
    private static final double GAIN = 0.90D;
    /** 最大滑行速度，避免飞出地图。 */
    private static final double MAX_SLIDE = 0.9D;

    private static final String K_X = "FuluSlideX";
    private static final String K_Y = "FuluSlideY";
    private static final String K_Z = "FuluSlideZ";
    private static final String K_VX = "FuluSlideVX";
    private static final String K_VZ = "FuluSlideVZ";
    private static final String K_INIT = "FuluSlideInit";
    private static final String K_HIT_CD = "FuluSlideHitCd";

    private static final boolean DEBUG_LOG = false;

    private SlipperyHandler() {
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Mob mob)) {
            return;                                      // 只对怪物生效（玩家、盔甲架都不管）
        }
        if (!(mob.level() instanceof ServerLevel level)) {
        if (com.lengyang.fulu.combat.FormationMarks.isImmune(mob)) {
            return;                              // 只有末影龙免疫
        }
            return;
        }
        boolean marked = mob.getEffect(ModMobEffects.SLIPPERY.get()) != null
                || com.lengyang.fulu.combat.FormationMarks.isMarked(
                        level, mob, com.lengyang.fulu.combat.FormationMarks.SLIPPERY);
        if (!marked) {
            // 离开阵法：清掉惯性，免得下次进来还带着旧速度
            CompoundTag d0 = mob.getPersistentData();
            if (d0.getBoolean(K_INIT)) {
                d0.remove(K_INIT);
                d0.putDouble(K_VX, 0.0D);
                d0.putDouble(K_VZ, 0.0D);
            }
            return;
        }
        // 飞行怪不受影响
        if (mob instanceof FlyingMob || mob.isNoGravity()) {
            return;
        }

        CompoundTag data = mob.getPersistentData();
        Vec3 now = mob.position();
        if (!data.getBoolean(K_INIT)) {
            data.putBoolean(K_INIT, true);
            data.putDouble(K_X, now.x);
            data.putDouble(K_Y, now.y);
            data.putDouble(K_Z, now.z);
            return;
        }

        Vec3 delta = now.subtract(data.getDouble(K_X), data.getDouble(K_Y), data.getDouble(K_Z));
        double vx = data.getDouble(K_VX) * KEEP + delta.x * GAIN;
        double vz = data.getDouble(K_VZ) * KEEP + delta.z * GAIN;

        // 限速
        double speed = Math.hypot(vx, vz);
        if (speed > MAX_SLIDE) {
            vx = vx / speed * MAX_SLIDE;
            vz = vz / speed * MAX_SLIDE;
            speed = MAX_SLIDE;
        }

        // BOSS 只受一半影响
        double scale = (mob instanceof EnderDragon || mob instanceof WitherBoss) ? 0.5D : 1.0D;

        if (speed > 0.01D) {
            // **必须用 move() 而不是 setPos()**：
            // setPos 是"硬设坐标"，不做任何碰撞检查 —— 滑行的位移会直接穿过墙。
            // move() 是原版的碰撞感知移动，会沿墙滑动、撞上就停，并且会置位
            // horizontalCollision（下面的撞墙伤害正是靠它判断）。
            // 阵魔：滑地效果减半（会滑，但不会失控）
            double half = (mob instanceof com.lengyang.fulu.entity.FormationDemonEntity) ? 0.5D : 1.0D;
            mob.move(net.minecraft.world.entity.MoverType.SELF,
                    new Vec3(vx * scale * half, 0.0D, vz * scale * half));
        }

        // ---- 撞到方块：按滑行速度造成伤害 ----
        // horizontalCollision 是原版在移动被方块挡住时置位的标志，正好拿来用。
        int cd = data.getInt(K_HIT_CD);
        if (cd > 0) {
            data.putInt(K_HIT_CD, cd - 1);
        }
        if (mob.horizontalCollision && speed > 0.15D && cd <= 0) {
            float dmg = (float) ((speed - 0.15D) * 10.0D);       // 0.9 速 ≈ 7.5 点
            if (dmg >= 0.5F) {
                mob.hurt(mob.damageSources().flyIntoWall(), dmg);
                data.putInt(K_HIT_CD, 10);                        // 0.5 秒内不再重复撞
                // 撞击效果：冰屑 + 一声闷响
                level.sendParticles(ParticleTypes.ITEM_SNOWBALL,
                        mob.getX(), mob.getY() + 0.4D, mob.getZ(),
                        10, 0.35D, 0.3D, 0.35D, 0.05D);
                level.playSound(null, mob.blockPosition(),
                        net.minecraft.sounds.SoundEvents.GLASS_BREAK,
                        net.minecraft.sounds.SoundSource.HOSTILE, 0.5F, 1.4F);
                // 撞完把惯性打掉大半，不然会一直贴墙受伤
                vx *= 0.25D;
                vz *= 0.25D;
                speed *= 0.25D;
            }
        }

        // 记录本刻位置（用于下刻算位移）
        data.putDouble(K_X, mob.getX());
        data.putDouble(K_Y, mob.getY());
        data.putDouble(K_Z, mob.getZ());
        data.putDouble(K_VX, vx * scale);
        data.putDouble(K_VZ, vz * scale);

        // 冰霜轨迹：滑得越快粒子越多（站着不动没有）
        if (speed > 0.05D) {
            int n = Math.min(6, 1 + (int) (speed * 8.0D));
            level.sendParticles(ParticleTypes.SNOWFLAKE,
                    mob.getX(), mob.getY() + 0.05D, mob.getZ(),
                    n, mob.getBbWidth() * 0.4D, 0.02D, mob.getBbWidth() * 0.4D, 0.0D);
            level.sendParticles(ParticleTypes.ITEM_SNOWBALL,
                    mob.getX(), mob.getY() + 0.03D, mob.getZ(),
                    1, 0.1D, 0.0D, 0.1D, 0.0D);
        }

        if (DEBUG_LOG && mob.tickCount % 20 == 0) {
            FuLu.LOGGER.info("[slippery] {} slide speed {}", mob.getName().getString(), speed);
        }
    }
}
