package com.lengyang.fulu.combat;

import com.lengyang.fulu.FuLu;
import com.lengyang.fulu.block.FormationEyeBlock;
import com.lengyang.fulu.block.FormationEyeBlockEntity;
import com.lengyang.fulu.block.TalismanKind;
import com.lengyang.fulu.entity.FormationDemonEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;

/**
 * 阵魔的召唤：玩家附近放满 4 个以上**激活的**阵法，阵魔就会感应到。
 *
 * <p>流程（用户规格第八条）：感应 → 地面震动冒黑粒子 **10 秒** → 在玩家附近 **10~15 格**钻出
 * → 咆哮。消失后 **10 分钟**冷却。
 *
 * <p>怎么数阵法：阵眼自己在加载时登记进 {@code FormationEyeBlockEntity.LIVE_EYES}，
 * 这里只数"登记过、且 talisman != EMPTY"的，避免扫方块。
 */
@Mod.EventBusSubscriber(modid = FuLu.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DemonSummoner {

    /** 需要几个激活阵法。 */
    private static final int NEEDED = 4;
    /** 统计半径。 */
    private static final double RADIUS = 24.0D;
    /** 感应到之后多久钻出来。 */
    private static final int RITUAL_TICKS = 200;
    /** 冷却（10 分钟）。 */
    private static final long COOLDOWN_TICKS = 20L * 60L * 10L;
    /** 冷却到期时间（按维度记）。 */
    private static final Map<Level, Long> COOLDOWN = new HashMap<>();
    /** 正在进行的仪式：玩家 -> 剩余刻数。 */
    private static final Map<Player, Integer> RITUALS = new HashMap<>();

    private DemonSummoner() {
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (!(event.level instanceof ServerLevel level)) {
            return;
        }
        long now = level.getGameTime();
        Long until = COOLDOWN.get(level);
        if (until != null && now < until) {
            return;                                  // 冷却中
        }

        // ---- 每个玩家独立判断
        for (ServerPlayer player : level.players()) {
            if (RITUALS.containsKey(player)) {
                int left = RITUALS.get(player) - 1;
                ritualParticles(level, player, left);
                if (left <= 0) {
                    RITUALS.remove(player);
                    spawnDemon(level, player);
                    COOLDOWN.put(level, now + COOLDOWN_TICKS);
                } else {
                    RITUALS.put(player, left);
                }
                continue;
            }
            int eyes = countActiveEyes(level, player.blockPosition());
            if (player.tickCount % 100 == 0 && eyes > 0) {
                FuLu.LOGGER.info("[demon] {} active formation(s) near {}",
                        eyes, player.getName().getString());
            }
            if (eyes >= NEEDED) {
                RITUALS.put(player, RITUAL_TICKS);
                level.playSound(null, player.blockPosition(), SoundEvents.WARDEN_ANGRY,
                        SoundSource.HOSTILE, 1.4F, 0.5F);
                player.displayClientMessage(
                        net.minecraft.network.chat.Component.translatable("msg.fulu.demon.summoning"),
                        false);
            }
        }
    }

    /** 10 秒震动：玩家脚下一圈黑粒子 + 低频嗡鸣。 */
    private static void ritualParticles(ServerLevel level, ServerPlayer player, int left) {
        if (left % 10 != 0) {
            return;
        }
        net.minecraft.world.phys.Vec3 p = player.position();
        for (int i = 0; i < 12; i++) {
            double a = Math.PI * 2.0D * i / 12.0D;
            level.sendParticles(ParticleTypes.SQUID_INK,
                    p.x + Math.cos(a) * 3.0D, p.y + 0.1D, p.z + Math.sin(a) * 3.0D,
                    1, 0.05D, 0.02D, 0.05D, 0.0D);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.WARDEN_HEARTBEAT,
                SoundSource.HOSTILE, 1.2F, 0.6F);
    }

    /** 在玩家附近 10~15 格钻出阵魔。 */
    private static void spawnDemon(ServerLevel level, ServerPlayer player) {
        double a = level.random.nextDouble() * Math.PI * 2.0D;
        double dist = 10.0D + level.random.nextDouble() * 5.0D;
        double x = player.getX() + Math.cos(a) * dist;
        double z = player.getZ() + Math.sin(a) * dist;
        int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, (int) x, (int) z);

        FormationDemonEntity demon = com.lengyang.fulu.registry.ModEntities.FORMATION_DEMON.get().create(level);
        if (demon == null) {
            return;
        }
        demon.moveTo(x, y, z, level.random.nextFloat() * 360.0F, 0.0F);
        level.addFreshEntity(demon);

        FuLu.LOGGER.info("[demon] spawned at {},{},{} ({} blocks from player)",
                (int) x, y, (int) z, String.format("%.1f", dist));
        level.playSound(null, demon.blockPosition(), SoundEvents.WARDEN_ROAR,
                SoundSource.HOSTILE, 2.0F, 0.5F);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, x, y + 1.0D, z, 2, 1.0D, 0.3D, 1.0D, 0.0D);
        level.sendParticles(ParticleTypes.SQUID_INK, x, y + 0.5D, z, 60, 1.2D, 0.4D, 1.2D, 0.03D);
    }

    /** 附近有几个"激活的"阵眼。 */
    private static int countActiveEyes(ServerLevel level, BlockPos near) {
        int n = 0;
        for (BlockPos pos : FormationEyeBlockEntity.liveEyes(level)) {
            if (pos.distSqr(near) > RADIUS * RADIUS) {
                continue;
            }
            BlockState st = level.getBlockState(pos);
            if (!(st.getBlock() instanceof FormationEyeBlock)) {
                continue;
            }
            if (st.getValue(FormationEyeBlock.TALISMAN) == TalismanKind.EMPTY) {
                continue;                            // 空阵眼不算
            }
            n++;
        }
        return n;
    }
}
