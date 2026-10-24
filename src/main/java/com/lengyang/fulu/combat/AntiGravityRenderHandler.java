package com.lengyang.fulu.combat;

import com.lengyang.fulu.FuLu;
import com.lengyang.fulu.registry.ModMobEffects;
import com.lengyang.fulu.registry.ModParticles;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 反重力阵的**倒立**：给挂了 {@code fulu:antigravity} 的怪物，渲染时上下翻 180 度。
 *
 * <p>为什么用渲染事件而不是改模型：这件事必须满足三条，只有渲染事件能同时做到：
 * <ol>
 *   <li><b>只影响外观</b>：翻的是 {@link PoseStack}，碰撞箱、位置、AI 一律不动 ——
 *       所以怪物看起来倒立着，实际还站在原处，打它、它打你的距离都不变；</li>
 *   <li><b>不用给每种怪写模型</b>：原版有几十种怪物模型，逐个改是不可能的；</li>
 *   <li><b>纯客户端</b>：服务端根本不需要知道"倒立"这件事（它只知道那个标记效果）。</li>
 * </ol>
 *
 * <p><b>注册方式：显式注册，不用 {@code @Mod.EventBusSubscriber} 注解。</b>
 * 这里本来用的是注解（{@code value = Dist.CLIENT}），但"倒立没生效"排查到最后，
 * 注解扫描是唯一还可能出问题的一环 —— 而它是**静默失败**：扫描没命中就什么都不发生，
 * 没有报错、没有日志。改成由 {@code ModClientSetup} 显式
 * {@link MinecraftForge#EVENT_BUS}{@code .register(...)}：这条路只有"注册了"和
 * "编译不过"两种结果，没有中间地带。
 *
 * <p><b>只注册一次</b>：所以这里**不能**再带注解 —— 两边都注册会让回调跑两遍，
 * 翻两次等于没翻。自检里有一条断言专门盯着这件事。
 *
 * <p><b>翻转的数学</b>：渲染时 {@link PoseStack} 的原点在怪物**脚底**（调用方已经
 * 把姿态平移到实体位置了），而模型的 y 是往上为负。绕 Z 轴转 180 度会把身体从
 * [-h, 0] 映射到 [0, h]（跑到地面以下），所以先向上平移 h 再转，身体正好回到
 * [-h, 0] —— **原地倒过来**。
 *
 * <p>飞行怪物（幻翼、恶魂）和 BOSS 不翻：阵法本来就不会给它们挂标记，
 * 但万一有人用 {@code /effect} 手动挂上，这里也不翻它们。
 */
public final class AntiGravityRenderHandler {

    /** 上下翻转 180 度。 */
    private static final float FLIP_DEGREES = 180.0F;

    /**
     * 翻转时写一条日志（<b>默认开着</b>）。
     *
     * <p>为什么默认开着：倒立是**纯客户端渲染**，无头测试里没有画面可观察，
     * 我没法自己验证它。这条日志是唯一能让"它到底跑没跑"变成事实的东西：
     * 客户端日志里出现 {@code [antigravity-render]} 就说明回调确实在跑、
     * 翻转代码确实执行了；一条都没有就说明回调根本没被调用。
     *
     * <p>确认没问题之后可以关掉（每 20 刻最多一条，不会刷屏）。
     */
    private static final boolean DEBUG_LOG = true;

    private AntiGravityRenderHandler() {
    }

    /** 由 {@code ModClientSetup} 在客户端启动时调用一次。 */
    public static void register() {
        MinecraftForge.EVENT_BUS.register(AntiGravityRenderHandler.class);
    }

    @SubscribeEvent
    public static void onRenderLiving(RenderLivingEvent.Pre<?, ?> event) {
        LivingEntity entity = event.getEntity();
        if (entity.getEffect(ModMobEffects.ANTIGRAVITY.get()) == null) {
            return;                                        // 没有标记，什么都不做
        }
        if (entity instanceof Mob mob && !FormationTarget.canInvert(mob)) {
            return;                                        // 飞行怪 / BOSS 不翻
        }

        PoseStack pose = event.getPoseStack();
        // ---- 绕身体中心做一次 y 反射（镜像）。为什么是"反射"而不是"旋转"： ----
        //
        // 原版渲染器自己那句 scale(-1,-1,1)，在矩阵上**就等于绕 Z 轴转 180 度**。
        // 我前面几版用的都是某种 180 度旋转，于是：
        //   * 绕 Z 转 → 和原版那句是同一个矩阵，两个叠起来 = 恒等 = **什么都没改**。
        //     这正是"回调在跑（粒子看得见）、画面却纹丝不动"的原因；
        //   * 绕 X 转 → 两个 180 合成"绕 Y 转" = 原地转身，人还是立着的。
        // 原因是原版那句的行列式是 +1（旋转），而"上下颠倒"在数学上是**镜像**
        // （行列式 -1）—— 镜像不可能被一个旋转抵消，所以只有它能真正翻过来。
        //
        // 支点取身体中心（身高的一半），让身体留在原地：y -> 身高 - y。
        //
        // 代价：奇数个负缩放 = 镜像，贴图会左右翻。对称的怪看不出来，带武器的会。
        // 原版实体用 entityCutoutNoCull（不剔除背面），所以镜像后照常可见。
        float half = entity.getBbHeight() * 0.5F;
        pose.translate(0.0D, half, 0.0D);
        pose.scale(1.0F, -1.0F, 1.0F);
        pose.translate(0.0D, -half, 0.0D);

        // ---- 自证：在怪物身上放一粒紫色粒子 ----
        // 为什么要放这个：倒立是**纯客户端的姿态**，无头测试看不到画面，
        // 而"翻没翻"只能靠人眼。日志要翻文件、还容易看漏，粒子是**直接在画面里**
        // 的：看得见粒子 = 这个回调确实执行了（那问题就在变换上）；
        // 看不见粒子 = 回调压根没跑（那问题就不在这个事件上）。
        // 每 20 刻最多一粒，不刷屏。
        if (entity.tickCount % 20 == 0) {
            entity.level().addParticle(ModParticles.ANTI_GRAVITY.get(),
                    entity.getX(), entity.getY() + 1.0D, entity.getZ(),
                    0.0D, 0.02D, 0.0D);
        }

        if (DEBUG_LOG && entity.tickCount % 20 == 0) {
            FuLu.LOGGER.info("[antigravity-render] flipped entity {} (h={})",
                    entity.getId(), String.format("%.2f", entity.getBbHeight()));
        }
    }
}
