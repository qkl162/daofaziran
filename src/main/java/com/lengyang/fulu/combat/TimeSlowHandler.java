package com.lengyang.fulu.combat;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.lengyang.fulu.FuLu;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 时缓阵的减速逻辑。
 *
 * <p>两件事：
 * <ol>
 *   <li><b>移动变慢</b>：给怪物挂一个**瞬时**属性修饰符（MULTIPLY_TOTAL -50%）。
 *       用瞬时（transient）修饰符而不是状态效果，是因为模组 boss 常重写
 *       {@code canBeAffected} 把效果拒掉，而属性修饰符管不了那么远；
 *       另外它不会被存档写进去，区块卸载也不会留下"永久变慢"的脏数据。</li>
 *   <li><b>攻击变慢</b>：监听 {@link LivingAttackEvent}，带时缓标记的生物
 *       每 {@link #ATTACK_INTERVAL} 刻才允许造成一次伤害 —— 其余攻击直接取消。
 *       原版没有通用的"延长攻击前摇"接口（每种怪的挥击时长写死在 AI 里），
 *       所以用"降低出手频率"来达到同样手感。</li>
 * </ol>
 *
 * <p>玩家不受影响：减速只对 {@link Mob} 生效，攻击限速只对带标记的生物生效，
 * 而阵眼只给怪物挂标记。
 */
@Mod.EventBusSubscriber(modid = FuLu.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TimeSlowHandler {

    /** 移动速度修饰符 id（固定值，方便反复增删）。 */
    private static final UUID SLOW_ID = UUID.fromString("7b1e6f42-3c5a-4d21-9f0e-2a8d5c4b1e33");
    private static final String SLOW_NAME = "fulu_time_slow";

    /** 普通怪物：移动速度 -50%。 */
    private static final double MOVE_FACTOR = -0.50D;

    /**
     * Boss 级单位的移速乘数：**-40%**。
     *
     * <p>用户要求：boss 也减速，幅度是普通怪的 80%
     * （普通怪 -50% 的 80% = -40%）。
     *
     * <p>为什么用属性修饰符而不是"每刻乘 deltaMovement"：
     * {@code LivingEvent.LivingTickEvent} 触发在 {@code aiStep()} **之前**，
     * 那时 AI 还没设速度，乘完会被立刻覆盖 —— 实测确实不生效。
     * 属性修饰符不依赖时序，是唯一可靠的方式。
     * 早前出现过"抽搐"，根因是边界反复加/删修饰符（已用 4 格滞回修掉），
     * 不是属性本身的问题。
     */
    private static final double MOVE_FACTOR_BOSS = -0.40D;

    /**
     * 【已停用】位移回拉比例。保留常量仅供以后参考：
     * 每刻 setPos 回拉会让 boss 出现"前进-拽回"的锯齿抖动（用户实测抽搐），
     * 所以改回只吃属性修饰符。
     *
     * <p>用户选的是「B + C」：
     * <ul>
     *   <li><b>B</b>：回拉放宽 —— 60% → **75%**，不要拽得太狠；</li>
     *   <li><b>C</b>：冲刺/dash 类位移**不回拉**（见 {@link #DASH_SKIP_DISTANCE}），
     *       只对走路生效，避免"冲出去被拽回来"。</li>
     * </ul>
     *
     * <p>为什么必须用位移回拉：灾变 boss 的移动不经过 {@code MOVEMENT_SPEED} 属性，
     * 属性修饰符加上了也毫无效果（日志里加得上、游戏里不动）。
     */
    private static final double BOSS_KEEP_RATIO = 0.75D;

    /**
     * 单 tick 位移超过这个距离就当作"冲刺/位移技能"，**不回拉**。
     *
     * <p>参考量级：boss 走路每 tick 约 0.1~0.3 格；冲刺/跳跃位移通常在 0.5 格以上。
     */
    private static final double DASH_SKIP_DISTANCE = 0.45D;

    // boss 的位移回拉方案已回滚（会造成抽搐），现在 boss 只吃属性修饰符。

    /** 灾变模组的实体包名前缀（不硬依赖：只用来分级，不加载它的类）。 */
    private static final String[] BOSS_PACKAGES = {
            "com.github.L_Ender.cataclysm",
            "com.github.lender.cataclysm",
    };

    /** 普通怪物攻击间隔（刻）：40 刻 = 2 秒一次，明显比正常慢。 */
    private static final int ATTACK_INTERVAL = 40;

    /**
     * Boss 攻击间隔（刻）：60 刻 = 3 秒一次。
     *
     * <p>比普通怪宽松一点，因为 boss 的多段技能如果被砍得太碎，
     * 会出现"动画在播、伤害没出"的空转感。这个数可以单独调。
     */
    private static final int BOSS_ATTACK_INTERVAL = 60;

    /** 上次出手的刻（清理时按时间丢弃，防止无限增长）。 */
    private static final Map<UUID, Long> LAST_ATTACK = new HashMap<>();

    private TimeSlowHandler() {
    }

    /**
     * 是不是"Boss 级"单位 —— 需要**温和减速、且不能打断攻击**。
     *
     * <p>两条判定，任一成立即可：
     * <ul>
     *   <li>类名在灾变模组的包下（按包名前缀判断，**不加载它的类**，没装也不会崩）；</li>
     *   <li>最大生命 ≥ 100（原版绝大多数怪都到不了这个数）。</li>
     * </ul>
     */
    public static boolean isBossTier(Mob mob) {
        if (mob.getMaxHealth() >= 100.0F) {
            return true;
        }
        String cn = mob.getClass().getName();
        for (String pkg : BOSS_PACKAGES) {
            if (cn.startsWith(pkg)) {
                return true;
            }
        }
        return false;
    }

    /** 阵眼每刻调用：给这只怪上慢速 + 标记 + 沙粒。 */
    public static void applyTo(ServerLevel level, Mob mob) {
        if (mob == null || !mob.isAlive()) {
            return;
        }
        FormationMarks.mark(level, mob, FormationMarks.TIME_SLOW);
        AttributeInstance attr = mob.getAttribute(Attributes.MOVEMENT_SPEED);
        boolean boss = isBossTier(mob);
        if (attr != null && attr.getModifier(SLOW_ID) == null) {
            attr.addTransientModifier(new AttributeModifier(
                    SLOW_ID, SLOW_NAME, boss ? MOVE_FACTOR_BOSS : MOVE_FACTOR,
                    AttributeModifier.Operation.MULTIPLY_TOTAL));
            if (boss && !mob.getPersistentData().getBoolean("FuluTimeSlowLogged")) {
                mob.getPersistentData().putBoolean("FuluTimeSlowLogged", true);
                FuLu.LOGGER.info("[time_slow] boss-tier -40%: {} (hp={})",
                        mob.getClass().getSimpleName(), mob.getMaxHealth());
            }
        }
        // 淡金色"沙粒"：WAX_ON 是原版唯一的金色小光点
        level.sendParticles(ParticleTypes.WAX_ON,
                mob.getX(), mob.getY() + mob.getBbHeight() * 0.6D, mob.getZ(),
                1, 0.28D, 0.35D, 0.28D, 0.0D);
    }

    /** 标记过期就把修饰符摘掉（离开阵法 / 取走符之后生效）。 */
    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity == null) {
            return;
        }
        if (entity.level().isClientSide || !(entity instanceof Mob)) {
            return;
        }
        AttributeInstance attr = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        boolean marked = FormationMarks.isMarked(entity.level(), entity, FormationMarks.TIME_SLOW);

        if (attr == null || attr.getModifier(SLOW_ID) == null) {
            return;
        }
        if (!marked) {
            attr.removeModifier(SLOW_ID);
        }
    }

    /** 攻击限速：带时缓标记的生物每 2 秒最多打一下。 */
    @SubscribeEvent
    public static void onAttack(LivingAttackEvent event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) {
            return;
        }
        if (!FormationMarks.isMarked(attacker.level(), attacker, FormationMarks.TIME_SLOW)) {
            return;
        }
        // 用户要求：**boss 和普通怪物都干预攻击频率**。
        // 注意：多段技能的 boss 被取消个别攻击时，可能出现"动作在播、伤害没出"的空转，
        // 如果实测抽搐，把 BOSS_ATTACK_INTERVAL 放宽（下面单独留了常量）。
        long now = attacker.level().getGameTime();
        int interval = (attacker instanceof Mob m && isBossTier(m))
                ? BOSS_ATTACK_INTERVAL : ATTACK_INTERVAL;
        Long last = LAST_ATTACK.get(attacker.getUUID());
        if (last != null) {
            if (now - last < interval) {
                event.setCanceled(true);      // 这一下"抬手太慢"，没打出去
                return;
            }
            if (now - last > 600L) {
                LAST_ATTACK.remove(attacker.getUUID());   // 顺手清理冷数据
            }
        }
        LAST_ATTACK.put(attacker.getUUID(), now);
    }
}
