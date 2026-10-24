package com.lengyang.fulu.combat;

import com.lengyang.fulu.FuLu;
import com.lengyang.fulu.refining.RefiningCast;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 施法读条的"推进"与"打断"。
 *
 * <ul>
 *   <li>每刻推进 {@link RefiningCast#tickCasting}；</li>
 *   <li>读条期间**被任何伤害打中** → {@link RefiningCast#interrupt}（失败、短冷却、提示）。</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = FuLu.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CastTickHandler {

    private CastTickHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (event.player.level().isClientSide) {
            return;
        }
        if (event.player instanceof ServerPlayer sp && RefiningCast.isCasting(sp)) {
            RefiningCast.tickCasting(sp);
        }
    }

    /** 泥沼每 tick 处理（服务端、tick 末尾：这时怪已经走完这一 tick）。 */
    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.level.isClientSide) {
            return;
        }
        if (event.level instanceof net.minecraft.server.level.ServerLevel level) {
            com.lengyang.fulu.refining.SwampZoneHandler.tick(level);
            // 剑气推进 + 断魂到期清理（都按 UUID 追踪，不遍历整个世界）
            com.lengyang.fulu.refining.BloodSlash.tick(level);
            com.lengyang.fulu.refining.LightWingUltimate.tick(level);
            com.lengyang.fulu.refining.MeteorStrike.tick(level);
            // 假身：每刻末尾抢一次仇恨（放在最后，保证覆盖怪物 AI 同刻的重选）
            com.lengyang.fulu.entity.DecoyEntity.enforceAggro();
            com.lengyang.fulu.refining.MiasmaField.tick(level);
            com.lengyang.fulu.refining.NetherfireState.tick(level);
        }
    }

    /** **阴火反伤**：身上有阴火的怪物，每次攻击都会伤到自己。 */
    @SubscribeEvent
    public static void onAttackRecoil(net.minecraftforge.event.entity.living.LivingAttackEvent event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof net.minecraft.world.entity.LivingEntity attacker)) {
            return;
        }
        if (com.lengyang.fulu.refining.NetherfireState.isMarked(attacker)) {
            attacker.invulnerableTime = 0;
            attacker.hurt(attacker.level().damageSources().magic(), 3.0F);
        }
    }

    /** 泥沼里被打：**必定暴击**（额外 50% 伤害）。 */
    @SubscribeEvent
    public static void onSwampCrit(net.minecraftforge.event.entity.living.LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof net.minecraft.world.entity.player.Player)) {
            return;
        }
        if (com.lengyang.fulu.refining.SwampZoneHandler.isInSwamp(event.getEntity())) {
            event.setAmount(event.getAmount() * 1.5F);
            if (event.getEntity().level() instanceof net.minecraft.server.level.ServerLevel sl) {
                sl.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT,
                        event.getEntity().getX(),
                        event.getEntity().getY() + event.getEntity().getBbHeight() * 0.6D,
                        event.getEntity().getZ(), 12, 0.3D, 0.3D, 0.3D, 0.2D);
            }
        }
    }

    /** 断魂：受到的所有伤害 +20%（包括阵法伤害）。 */
    @SubscribeEvent
    public static void onHurtBonus(LivingHurtEvent event) {
        if (com.lengyang.fulu.refining.BloodSlash.isCursed(event.getEntity())) {
            // 冥界诅咒：受到的伤害额外 +20%
            event.setAmount(event.getAmount() * 1.2F);
        }
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }
        if (event.getEntity() instanceof ServerPlayer sp && RefiningCast.isCasting(sp)) {
            RefiningCast.interrupt(sp);
        }
    }
}
