package com.lengyang.fulu.refining;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

import com.lengyang.fulu.FuLu;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 「冥界诅咒」的**禁疗**与到期清理。
 *
 * <p>禁疗用 {@code LivingHealEvent} 把治疗量直接清零 —— 这样：
 * <ul>
 *   <li>自然回血（饱食度）被挡；</li>
 *   <li>药水/金苹果/治疗法术**全部**被挡（它们最终都走这个事件）；</li>
 *   <li>不依赖 MobEffect 的"能不能被免疫"，Boss 也吃这一套。</li>
 * </ul>
 *
 * <p>诅咒到期后把护甲修饰符摘掉，并把实体从追踪表里移除。
 */
@Mod.EventBusSubscriber(modid = FuLu.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BloodCurseHandler {

    private static final Set<UUID> TRACKED = new HashSet<>();

    private BloodCurseHandler() {
    }

    /** 记入追踪表（由 {@link BloodSlash#applyCurse} 调用）。 */
    public static void track(LivingEntity entity) {
        TRACKED.add(entity.getUUID());
    }

    /** 禁疗：诅咒期间一切治疗为 0。 */
    @SubscribeEvent
    public static void onHeal(LivingHealEvent event) {
        // 冥界诅咒 与 绝仙瘴气 都让治疗量归零
        if (BloodSlash.isCursed(event.getEntity())
                || MiasmaField.isNoHeal(event.getEntity())) {
            event.setAmount(0.0F);
        }
    }

    /** 到期清理。 */
    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.level.isClientSide) {
            return;
        }
        if (TRACKED.isEmpty() || !(event.level instanceof ServerLevel level)) {
            return;
        }
        Iterator<UUID> it = TRACKED.iterator();
        while (it.hasNext()) {
            UUID id = it.next();
            Entity ent = level.getEntity(id);
            if (!(ent instanceof LivingEntity le) || !le.isAlive()) {
                it.remove();
                continue;
            }
            if (!BloodSlash.isCursed(le)) {
                BloodSlash.clearArmorBreak(le);
                it.remove();
            }
        }
    }
}
