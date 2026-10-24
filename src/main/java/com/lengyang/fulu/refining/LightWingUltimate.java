package com.lengyang.fulu.refining;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.lengyang.fulu.FuLu;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/**
 * 「光翼剑客」终极技能的时间轴调度（总时长 **2 秒 / 40 刻**）：
 *
 * <pre>
 *  0.00 – 0.40s  蓄力：剑尖聚光、光粒汇聚、脚下剑阵亮起
 *  0.40 – 0.70s  光翼展开
 *  0.70 – 1.10s  连续挥剑三次（第 14/18/22 刻），每次一道青金半月剑气
 *  1.10 – 1.60s  剑气飞行（带拖尾、地面裂纹）
 *  1.60 – 2.00s  光翼收拢、光尘消散
 * </pre>
 *
 * <p>职责划分：
 * <ul>
 *   <li>本类只管**节奏**（什么时候挥剑、什么时候震屏）；</li>
 *   <li>伤害仍由 {@link BloodSlash} 结算；</li>
 *   <li>视觉由 {@code LightWingAuraEntity} / {@code BloodWaveEntity} /
 *       {@code GroundCrackEntity} / {@code HitBurstEntity} 四个实体承担。</li>
 * </ul>
 */
public final class LightWingUltimate {

    /** 总时长（刻）。 */
    public static final int TOTAL = 40;
    /** 三次挥剑的时刻（刻）。 */
    private static final int[] SLASH_TICKS = {14, 18, 22};
    /** 三次挥剑的扇形偏角（度）。 */
    private static final float[] SLASH_ANGLES = {-8.0F, 0.0F, 8.0F};

    private static final class State {
        int age;
        int nextSlash;

        State() {
        }
    }

    private static final Map<UUID, State> ACTIVE = new HashMap<>();

    private LightWingUltimate() {
    }

    /** 开始整段技能。 */
    public static void start(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        ACTIVE.put(player.getUUID(), new State());

        // 光翼气场实体（蓄力 + 展翼 + 收拢全由它负责）
        var aura = new com.lengyang.fulu.entity.LightWingAuraEntity(
                com.lengyang.fulu.registry.ModEntities.LIGHT_WING_AURA.get(), level);
        aura.setCaster(player);
        level.addFreshEntity(aura);

        // 视角交给游戏本身：**不再生成相机实体、不再改 FOV / 抖屏**（用户要求）
        // 蓄力音：低沉一记钟声 + 光粒汇聚的"嗡"
        level.playSound(null, player.blockPosition(), SoundEvents.WITHER_SPAWN,
                SoundSource.PLAYERS, 1.8F, 0.5F);
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.PLAYERS, 1.6F, 0.7F);

        // **蓄力慢动作**：把所有其他生物的移动/攻击压慢（8 刻）
        // 真正的全局时间减速改的是 tick 速率，多人环境不可接受；
        // 这里用"极强减速 + 挖掘疲劳"做出同样的体感，只作用于周围 12 格内的其他生物。
        for (var mob : level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class,
                player.getBoundingBox().inflate(12.0D))) {
            mob.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 14, 6));
            mob.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.DIG_SLOWDOWN, 14, 4));
        }

        // 脚下剑阵亮起（一圈青金粒子）
        for (int i = 0; i < 72; i++) {
            double a = Math.PI * 2.0D * i / 72.0D;
            level.sendParticles(ParticleTypes.END_ROD,
                    player.getX() + Math.cos(a) * 3.4D, player.getY() + 0.08D,
                    player.getZ() + Math.sin(a) * 3.4D, 1, 0.0D, 0.01D, 0.0D, 0.0D);
        }
        FuLu.LOGGER.info("[ult] start for {}", player.getName().getString());
    }

    /** 每刻推进。 */
    public static void tick(ServerLevel level) {
        if (ACTIVE.isEmpty()) {
            return;
        }
        try {
            tickInner(level);
        } catch (Throwable t) {
            FuLu.LOGGER.error("[ult] tick failed", t);
        }
    }

    private static void tickInner(ServerLevel level) {
        List<UUID> done = new ArrayList<>();
        for (Map.Entry<UUID, State> e : ACTIVE.entrySet()) {
            ServerPlayer p = level.getServer().getPlayerList().getPlayer(e.getKey());
            State s = e.getValue();
            if (p == null || !p.isAlive()) {
                done.add(e.getKey());
                continue;
            }
            // 只让"玩家所在的那个维度"推进；其他维度跳过，但**不要清掉状态**
            if (p.level() != level) {
                continue;
            }
            s.age++;
            if (s.age % 5 == 0) {
                FuLu.LOGGER.info("[ult] age={} nextSlash={}", s.age, s.nextSlash);
            }

            // 蓄力段：光粒向剑尖汇聚
            if (s.age < 8) {
                Vec3 eye = p.getEyePosition().add(p.getLookAngle().scale(1.2D));
                for (int i = 0; i < 3; i++) {
                    double a = level.random.nextDouble() * Math.PI * 2.0D;
                    double r = 2.5D + level.random.nextDouble() * 1.5D;
                    level.sendParticles(ParticleTypes.END_ROD,
                            eye.x + Math.cos(a) * r, eye.y + (level.random.nextDouble() - 0.5D) * 2.0D,
                            eye.z + Math.sin(a) * r, 1,
                            -Math.cos(a) * 0.25D, (eye.y - (p.getY() + 1.0D)) * 0.0D,
                            -Math.sin(a) * 0.25D, 0.02D);
                }
            }

            // 挥剑时刻
            while (s.nextSlash < SLASH_TICKS.length && s.age >= SLASH_TICKS[s.nextSlash]) {
                float angle = SLASH_ANGLES[s.nextSlash];
                s.nextSlash++;
                boolean isThird = s.nextSlash == SLASH_TICKS.length;
                FuLu.LOGGER.info("[ult] slash #{} at age {} break={}",
                        s.nextSlash, s.age, isThird);
                BloodSlash.castSingle(p, angle, isThird);
                // 命中帧震屏
                sendShake(level, p);
                level.playSound(null, p.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                        SoundSource.PLAYERS, 1.6F, 1.2F);
            }

            // 收尾：光尘消散
            if (s.age == TOTAL - 6) {
                level.sendParticles(ParticleTypes.END_ROD,
                        p.getX(), p.getY() + 1.2D, p.getZ(),
                        40, 1.0D, 0.8D, 1.0D, 0.06D);
            }
            if (s.age >= TOTAL) {
                done.add(e.getKey());
            }
        }
        for (UUID id : done) {
            ACTIVE.remove(id);
        }
    }

    /**
     * 斩击震屏（用户要求恢复）。
     *
     * <p>只抖镜头，**不动 FOV** —— 之前"视角被放大"就是 FOV 那一段造成的。
     */
    private static void sendShake(ServerLevel level, ServerPlayer source) {
        var packet = new com.lengyang.fulu.network.ScreenShakePacket(5, 0.85F);
        for (ServerPlayer other : level.players()) {
            if (other.distanceToSqr(source) < 40.0D * 40.0D) {
                com.lengyang.fulu.network.ModNetwork.CHANNEL.send(
                        net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> other),
                        packet);
            }
        }
    }
}
