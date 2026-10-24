package com.lengyang.fulu.combat;

import com.lengyang.fulu.FuLu;
import com.lengyang.fulu.block.TalismanKind;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 阵法「领域」效果：站在阵法里的生物会受到的持续影响。
 *
 * <p>对应用户第四轮扩展里那些**只加手感、不改核心机制**的条目：
 * <ul>
 *   <li>18 / 30 范围内的**怪物减速**（像穿过一层水膜）；</li>
 *   <li>29 范围内的**玩家稍微加速**；</li>
 *   <li>31 范围内的**玩家获得夜视**；</li>
 *   <li>28 范围内的**玩家缓慢回血**（用户标注"看你要不要"，这里默认开）；</li>
 *   <li>20 **怪物进入阵法时**，阵法里的玩家在动作栏收到提示。</li>
 * </ul>
 *
 * <p><b>没有做的</b>：33（阵法内不自然刷怪）—— 这个 Forge 版本里没能定位到
 * {@code LivingSpawnEvent}，我不拿没验证过的 API 硬猜，留到确认了再做。
 *
 * <p>为什么这些放在一个类里而不是塞进阵眼：它们是**所有阵法共有**的，和"装了什么符"无关。
 * 每刻由阵眼调用一次，只有真正在跑的阵眼才有领域效果。
 *
 * <p>时长都用 40 刻（2 秒）且每刻刷新 —— 和嫁祸标记同一个理由：**不用无限时长**，
 * 这样阵眼被拆了效果也会自己过期。
 */
@Mod.EventBusSubscriber(modid = FuLu.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FormationFieldEffects {

    /**
     * 已激活阵眼的位置（按维度分组）。
     *
     * <p>为什么要这张表：刷怪判定（{@link MobSpawnEvent.PositionCheck}）每刻可能触发很多次，
     * 如果每次都去扫 20 多格范围内的方块实体（上千次查找），服务器会明显掉帧。
     * 阵眼在 {@code onLoad} / {@code setRemoved} 里登记与注销，刷怪判定只遍历这张小表。
     */
    private static final Map<ResourceKey<Level>, Set<BlockPos>> EYES = new HashMap<>();

    /** 阵眼加载时登记。 */
    public static void registerEye(Level level, BlockPos pos) {
        EYES.computeIfAbsent(level.dimension(), k -> new HashSet<>()).add(pos.immutable());
    }

    /** 阵眼卸载/被拆时注销。 */
    public static void unregisterEye(Level level, BlockPos pos) {
        Set<BlockPos> set = EYES.get(level.dimension());
        if (set != null) {
            set.remove(pos);
        }
    }

    /**
     * 33：阵法范围内不自然刷怪，但**刷怪笼除外**（用户要求）。
     *
     * <p>用的是 Forge 1.20.1 真正存在的事件 {@code MobSpawnEvent.PositionCheck}
     * —— 上一轮我按 {@code LivingSpawnEvent} 找，那个名字在这个版本里不存在。
     */
    @SubscribeEvent
    public static void onPositionCheck(MobSpawnEvent.PositionCheck event) {
        if (event.getSpawnType() == MobSpawnType.SPAWNER) {
            return;                                     // 刷怪笼：放行
        }
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (insideAnyFormation(level, new Vec3(event.getX(), event.getY(), event.getZ()))) {
            event.setResult(Event.Result.DENY);
        }
    }

    /** 某个位置是否落在任意一个**已激活**阵法的范围内。 */
    public static boolean insideAnyFormation(ServerLevel level, Vec3 pos) {
        Set<BlockPos> eyes = EYES.get(level.dimension());
        if (eyes == null || eyes.isEmpty()) {
            return false;
        }
        for (BlockPos p : eyes) {
            if (!(level.getBlockEntity(p) instanceof com.lengyang.fulu.block.FormationEyeBlockEntity eye)) {
                continue;
            }
            if (!(level.getBlockState(p).getBlock() instanceof com.lengyang.fulu.block.FormationEyeBlock)) {
                continue;
            }
            if (level.getBlockState(p).getValue(com.lengyang.fulu.block.FormationEyeBlock.TALISMAN)
                    == TalismanKind.EMPTY) {
                continue;
            }
            if (eye.formationContains(pos)) {
                return true;
            }
        }
        return false;
    }

    /** 效果刷新时长（刻）：2 秒。 */
    private static final int REFRESH_TICKS = 40;

    /** 怪物身上的"在阵法里"标记 key（用来判断"刚进来"）。 */
    private static final String IN_FORMATION = "FuluInFormation";

    private static final boolean DEBUG_LOG = false;

    private FormationFieldEffects() {
    }

    /**
     * 由阵眼每刻调用：给范围内生物挂领域效果。
     *
     * @param box     阵法范围（和阵眼自己算的一致）
     * @param eyePos  阵眼位置（只用于日志）
     * @param kind    这个阵眼装的符类型
     */
    public static void applyAura(ServerLevel level, AABB box, BlockPos eyePos, TalismanKind kind) {
        // 20：怪物第一次进入 → 提醒阵法里的玩家
        // 18 / 30：怪物减速
        for (Mob mob : level.getEntitiesOfClass(Mob.class, box)) {
            mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, REFRESH_TICKS, 0,
                    true, false, false));
            if (!mob.getPersistentData().getBoolean(IN_FORMATION)) {
                mob.getPersistentData().putBoolean(IN_FORMATION, true);
                for (ServerPlayer nearby : level.getEntitiesOfClass(ServerPlayer.class, box)) {
                    nearby.displayClientMessage(
                            Component.translatable("msg.fulu.formation.field.alarm", mob.getName()),
                            true);
                }
                if (DEBUG_LOG) {
                    FuLu.LOGGER.info("[field] {} entered formation at {}",
                            mob.getName().getString(), eyePos);
                }
            }
        }
        // 离开范围的怪把标记清掉（下次进来还能再提醒一次）
        for (Mob mob : level.getEntitiesOfClass(Mob.class, box.inflate(24.0D))) {
            if (!box.contains(mob.position()) && mob.getPersistentData().getBoolean(IN_FORMATION)) {
                mob.getPersistentData().putBoolean(IN_FORMATION, false);
            }
        }

        // 28 / 29 / 31：玩家回血 + 加速 + 夜视
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, box)) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, REFRESH_TICKS, 0,
                    true, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0,
                    true, false, false));
            if (player.tickCount % 40 == 0 && player.getHealth() < player.getMaxHealth()) {
                player.heal(1.0F);
            }
        }
    }
}
