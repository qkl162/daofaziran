package com.lengyang.fulu.combat;

import com.lengyang.fulu.registry.ModMobEffects;
import com.lengyang.fulu.registry.ModParticles;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 反重力阵的**每刻效果**：升空 + 不能走（但保留攻击）。
 *
 * <p>用户要求是"<b>升到空中 + 倒立 + 不能走但能攻击</b>"，三件事分别落在三处：
 * <ul>
 *   <li>标记：{@code fulu:antigravity} 效果（阵眼挂，阵法内**无限时长**）；</li>
 *   <li>倒立：{@code AntiGravityRenderHandler}（纯客户端渲染）；</li>
 *   <li>升空 + 不能走：**本类**（服务端每刻）。</li>
 * </ul>
 *
 * <p><b>为什么升空放在这里而不是效果里</b>：无限时长的效果（{@code duration = -1}）
 * 在原版里不一定会每刻调 {@code applyEffectTick} —— 把"每刻都在做的事"挂在
 * 一个"可能不 tick"的东西上太脆。放在事件里就没这个问题：只要生物身上有标记，
 * 每刻一定会走到这里。
 *
 * <p>上升靠每刻重设竖直速度，位置每刻正好走 RISE 格（见 {@link #RISE} 的注释）。
 * 不寻路、不跳跃、不攻击目标清空 —— 保留攻击，见下。
 *
 * <p>上升上限来自阵眼写进生物持久数据的天花板（阵眼 y + 5）。
 * 保留攻击目标 —— 一旦清掉，怪物就彻底忘了玩家，近战也不会打了。
 */
@Mod.EventBusSubscriber(modid = "fulu")
public final class AntiGravityTickHandler {

    /**
     * 每刻上升多少格 —— 就是**位置**每刻上升这么多，直接照用户说的 0.02。
     *
     * <p>这里和"设速度"的直觉不一样，日志量过：原版 {@code travel()} 是
     * <b>先按当前速度移动、再把重力扣在速度上</b>，而这个事件在 travel 之前触发、
     * 每刻都把速度重设一次 —— 所以**重力根本参与不了位置更新**，
     * 位置每刻正好走 RISE 这么多。
     *
     * <p>（我一开始写 0.10、注释说"抵掉重力净升 0.02"，那是**错的**：
     * 实测每秒 20 刻升了 1.6 格 = 0.08/刻，正好等于当时设的值。）
     */
    private static final double RISE = 0.02D;

    /** 到天花板之后竖直速度设 0：每刻移动 0，就是悬在空中。 */
    private static final double HOVER = 0.0D;

    /** 阵眼没写天花板时的默认高度（阵眼上方 5 格 = 这里用自身 +5）。 */
    private static final double DEFAULT_CEILING = 5.0D;

    /** 阵眼写进来的天花板（绝对 y）。 */
    public static final String CEILING_KEY = "FuluFloatCeiling";

    /** 每几刻出一次粒子（每刻都出太密）。 */
    private static final int PARTICLE_INTERVAL = 2;

    /** 临时打开看数字用（默认关）。 */
    private static final boolean DEBUG_LOG = false;

    private AntiGravityTickHandler() {
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Mob mob)) {
            return;
        }
        if (mob.level().isClientSide()) {
            return;                                        // 位置和移动都是服务端说了算
        }
        if (mob.getEffect(ModMobEffects.ANTIGRAVITY.get()) == null) {
            return;                                        // 没有标记：什么都不做
        }
        if (!FormationTarget.canInvert(mob)) {
            return;                                        // 飞行怪 / BOSS 不受影响
        }
        if (!(mob.level() instanceof ServerLevel server)) {
            return;
        }

        // ---- 1 + 2. 升空 + 不能走（一次把速度定死）----
        //
        // 这里是本类最容易踩的坑：事件在**实体自己 travel 之前**触发，所以
        //   * 只调 move() 把位置往上挪一点 —— 紧接着实体自己那一步会被重力拖回去，
        //     净效果是往下沉，表现就是"完全没升起来"（试过，600 刻全在地面）；
        //   * 只把速度设成 0.02 —— 抵不过重力（约 0.08），同样是往下沉。
        // 正确做法：把**速度**设成一个能抵掉重力还有富余的值（RISE），
        // 让实体自己那一步去完成上升；到天花板后换成 HOVER，正好与重力平衡，
        // 看起来就是悬在那里。水平速度同时清零 —— 那就是"不能走"。
        CompoundTag data = mob.getPersistentData();
        double ceiling = data.contains(CEILING_KEY)
                ? data.getDouble(CEILING_KEY)
                : mob.getY() + DEFAULT_CEILING;
        double vy = mob.getY() < ceiling ? RISE : HOVER;
        mob.setDeltaMovement(0.0D, vy, 0.0D);
        // 落体距离**不清**：离开阵法掉下来时照常摔伤（用户要的就是这个）

        // 不寻路、不跳跃
        mob.getNavigation().stop();
        mob.setJumping(false);
        // 注意：**不碰 setTarget** —— 攻击能力要留着，见类注释

        // ---- 3. 粒子：脚底往上飘（升空期间才有） ----
        if (DEBUG_LOG && mob.tickCount % 20 == 0) {
            com.lengyang.fulu.FuLu.LOGGER.info("[antigravity-tick] y={} ceiling={} hasKey={} vy={}",
                    String.format("%.2f", mob.getY()), String.format("%.2f", ceiling),
                    data.contains(CEILING_KEY), String.format("%.3f", vy));
        }

        if (mob.tickCount % PARTICLE_INTERVAL == 0) {
            server.sendParticles(ModParticles.ANTI_GRAVITY.get(),
                    mob.getX(), mob.getY() + 0.05D, mob.getZ(),
                    2, 0.3D, 0.05D, 0.3D, 0.02D);
        }
    }
}
