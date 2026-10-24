package com.lengyang.fulu.combat;

import com.lengyang.fulu.FuLu;
import com.lengyang.fulu.registry.ModMobEffects;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.CaveSpider;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Stray;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * "怪物打怪物"这件事，单独放一个文件处理。
 *
 * <p>做什么：<b>身上挂着"内讧"的怪物，打中别人时会把"它本来会带给玩家的负面效果"
 * 原样传给被打的那只怪物</b>。尸壳打僵尸 → 僵尸也饥饿；洞穴蜘蛛打僵尸 → 僵尸也中毒；
 * 凋灵骷髅打僵尸 → 僵尸也凋零。
 *
 * <p>不做的事（都很重要）：
 * <ul>
 *   <li><b>没内讧的怪物完全不管</b> —— 原版怎么打就怎么打，我们连碰都不碰；</li>
 *   <li><b>不会把"内讧"传染出去</b>。这里传的是"攻击附带的效果"，
 *       不是"攻击者身上的效果"。内讧只能由阵眼发，这一条是刻意的：
 *       内讧如果能靠打架传播，全世界的怪迟早都会互相打起来；</li>
 *   <li><b>内讧的怪物打玩家不掉血</b>（见下面 {@link #onLivingHurt}）。</li>
 * </ul>
 *
 * <p>⚠️ 为什么要一张表：原版把"打玩家附带什么效果"是<b>写死在每个怪物类里的</b>
 * （{@code Husk#doHurtTarget} 里 new 一个饥饿效果、{@code CaveSpider#doHurtTarget}
 * 里 new 一个中毒效果……），<b>没有任何接口可以问"你打人会附带什么"</b>。
 * 所以我们只能照着原版抄一份表。数值都和原版一致，
 * 加新怪物时照着它的 {@code doHurtTarget} 抄进来即可。
 */
@Mod.EventBusSubscriber(modid = FuLu.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class InfightingCombatHandler {

    /**
     * 调试开关：把每次"内讧怪打中别人"记到控制台。<b>默认关</b>。
     *
     * <p>正常玩法里不需要（事件本来就安静地生效）；开发和自动化测试时临时改成
     * true，就能从服务端日志里看出事件有没有触发、判定走的是哪条分支。
     */
    private static final boolean DEBUG_COMBAT_LOG = false;

    /**
     * 只处理"生物被打"这一件事。事件里同时有加害者和受害者，正好够用。
     *
     * <p>为什么用这个事件而不是 {@code LivingAttackEvent}：那个更早、更底层，
     * 但我们只需要"打中了"这个时机，而且这个事件允许改伤害值/取消，
     * 正好用来实现"内讧怪打不动玩家"。
     */
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        // 调试（默认关）：先记下"确实收到了一次受伤事件"，再看下面走哪条分支。
        // 放在最前面是有意的 —— 否则"事件根本没注册"和"这一下不该管"看起来一模一样。
        if (DEBUG_COMBAT_LOG) {
            FuLu.LOGGER.info("[combat] hurt: source={} victim={}",
                    event.getSource().getEntity(), event.getEntity());
        }
        // 加害者必须是一只怪物。玩家、箭、岩浆之类的一律放过。
        if (!(event.getSource().getEntity() instanceof Monster attacker)) {
            return;
        }
        // 身上没有"内讧" → 原版逻辑，什么都不做。
        if (!attacker.hasEffect(ModMobEffects.INFIGHTING.get())) {
            return;
        }

        LivingEntity victim = event.getEntity();

        // ① 内讧的怪物打玩家：不造成伤害。
        //    （它还是会"挥"一下，只是这一下不算数 —— 用取消事件实现最稳，
        //    不会出现"伤害算了一半"的情况。）
        if (victim instanceof Player) {
            event.setAmount(0.0F);
            event.setCanceled(true);
            return;
        }

        // ② 打的是怪物：把"它打玩家时会附带的负面效果"原样加过去。
        //    注意判断的是<b>受害者</b>是不是怪物 —— 村民、动物、盔甲架都跳过
        //    （想让它们也吃到效果，把这一行放宽成 LivingEntity 即可）。
        if (!(victim instanceof Monster)) {
            return;
        }
        MobEffectInstance attackEffect = attackEffectOf(attacker);
        if (attackEffect == null) {
            if (DEBUG_COMBAT_LOG) {
                FuLu.LOGGER.info("[combat] {} has no attack effect -> nothing to pass on",
                        attacker.getType());
            }
            return;                 // 这只怪本来就没有攻击附带效果 → 不加
        }
        // 复制一份再加：同一个 MobEffectInstance 对象不能同时挂在两只怪身上
        // （它内部有"还剩多久"的计时状态，共用会互相干扰）。
        victim.addEffect(new MobEffectInstance(attackEffect));
        if (DEBUG_COMBAT_LOG) {
            FuLu.LOGGER.info("[combat] passed {} on to {}",
                    attackEffect.getEffect(), victim.getType());
        }
    }

    /**
     * 这只怪物"打玩家时会附带什么负面效果"，没有就返回 {@code null}。
     *
     * <p>下面四条都是照原版对应类里的 {@code doHurtTarget} 抄的，包括时长：
     * <pre>
     *   Husk           饥饿   140 刻（7 秒）
     *   CaveSpider     中毒   普通 7 秒 / 困难 15 秒（原版按难度给）
     *   WitherSkeleton 凋零   200 刻（10 秒）
     *   Stray          缓慢   600 刻（30 秒）
     * </pre>
     * 其余怪物（僵尸、骷髅、苦力怕、蜘蛛、末影人……）原版打人就不附带效果，
     * 所以这里返回 null，什么也不会发生 —— 这正是"没有就跳过"。
     */
    private static MobEffectInstance attackEffectOf(Monster attacker) {
        if (attacker instanceof Husk) {
            return new MobEffectInstance(MobEffects.HUNGER, 140);
        }
        if (attacker instanceof CaveSpider) {
            // 原版：普通难度中毒 7 秒，困难 15 秒，简单不加
            int seconds = switch (attacker.level().getDifficulty()) {
                case HARD -> 15;
                case NORMAL -> 7;
                default -> 0;
            };
            return seconds == 0 ? null : new MobEffectInstance(MobEffects.POISON, seconds * 20);
        }
        if (attacker instanceof WitherSkeleton) {
            return new MobEffectInstance(MobEffects.WITHER, 200);
        }
        if (attacker instanceof Stray) {
            return new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 600);
        }
        return null;
    }

    private InfightingCombatHandler() {
    }
}
