package com.lengyang.fulu.combat;

import com.lengyang.fulu.FuLu;
import com.lengyang.fulu.block.FormationEyeBlockEntity;
import com.lengyang.fulu.entity.SoulEntity;
import com.lengyang.fulu.registry.ModEntities;
import com.lengyang.fulu.registry.ModMobEffects;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/**
 * 招魂阵：怪物死在阵法里 → 在原地生成一个"魂"。
 *
 * <p>形成连锁的方式很自然：魂打死的怪物**也是死在阵法里**、也被招魂阵标记着，
 * 所以它的死亡事件会再生成一个魂 —— 用户要的"杀一只、魂出来杀下一只"不用额外写。
 *
 * <p>上限 5 个：超过就把**最早生成的那个**赶走（用户要求）。
 */
@Mod.EventBusSubscriber(modid = FuLu.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SummonSoulHandler {

    /** 同时存在的魂上限。 */
    public static final int MAX_SOULS = 10;

    private static final boolean DEBUG_LOG = false;

    private SummonSoulHandler() {
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Mob dead)) {
            return;                                      // 只对怪物生效（玩家死了不召唤）
        }
        if (!(dead.level() instanceof ServerLevel level)) {
            return;
        }
        boolean marked = dead.getEffect(ModMobEffects.SUMMON.get()) != null
                || com.lengyang.fulu.combat.FormationMarks.isMarked(
                        level, dead, com.lengyang.fulu.combat.FormationMarks.SUMMON);
        if (!marked) {
            return;                                      // 没挂招魂标记：说明不是死在阵法里
        }

        spawnSoul(level, dead.getX(), dead.getY() + 0.6D, dead.getZ());
    }

    /** 在指定位置生成一个魂（会做上限处理）。 */
    public static SoulEntity spawnSoul(ServerLevel level, double x, double y, double z) {
        // 上限：把最早的赶走
        List<SoulEntity> existing = new ArrayList<>(
                level.getEntitiesOfClass(SoulEntity.class, new net.minecraft.world.phys.AABB(
                        x - 48, y - 48, z - 48, x + 48, y + 48, z + 48)));
        if (existing.size() >= MAX_SOULS) {
            SoulEntity oldest = existing.get(0);
            for (SoulEntity s : existing) {
                if (s.tickCount > oldest.tickCount) {
                    oldest = s;
                }
            }
            oldest.discard();
        }

        SoulEntity soul = ModEntities.SOUL.get().create(level);
        if (soul == null) {
            return null;
        }
        soul.moveTo(x, y, z, 0.0F, 0.0F);
        level.addFreshEntity(soul);

        // 生成：一圈淡青粒子 + 低沉的呜咽
        level.sendParticles(ParticleTypes.SOUL, x, y, z, 24, 0.5D, 0.5D, 0.5D, 0.02D);
        level.playSound(null, net.minecraft.core.BlockPos.containing(x, y, z),
                SoundEvents.SOUL_ESCAPE, SoundSource.HOSTILE, 0.9F, 0.55F);
        if (DEBUG_LOG) {
            FuLu.LOGGER.info("[summon] soul spawned at {},{},{}", x, y, z);
        }
        return soul;
    }
}
