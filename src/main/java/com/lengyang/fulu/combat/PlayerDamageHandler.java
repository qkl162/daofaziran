package com.lengyang.fulu.combat;

import com.lengyang.fulu.FuLu;
import com.lengyang.fulu.block.FormationEyeBlockEntity;
import com.lengyang.fulu.block.TalismanKind;
import com.lengyang.fulu.effect.ResistanceUpEffect;
import com.lengyang.fulu.registry.ModMobEffects;
import com.lengyang.fulu.standin.StandInData;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 玩家受伤时的全部逻辑，**顺序严格**（用户写的顺序）：
 *
 * <ol>
 *   <li><b>次数盾</b>（七）：有点数就完全挡下这一下、点数 -4，攻击者照样吃反弹；</li>
 *   <li><b>抗性提升减伤</b>（二）：按等级减伤，V 级起就是完全免疫；VII/VIII 带反弹；</li>
 *   <li><b>替死</b>（六）：身上有替身标记、并且这一下**会致命**，才触发。</li>
 * </ol>
 *
 * <p>为什么三件事必须在同一个处理器里：它们互相依赖顺序。分开写就得靠事件注册顺序，
 * 而那个顺序是隐式的 —— 今天对、明天加一个类就可能反了，而且错了的表现是
 * "偶尔不免疫"，极难查。
 *
 * <p>⚠️ 全部逻辑只对**玩家**生效，所以无头测试覆盖不到（专用服务器没有玩家）。
 */
@Mod.EventBusSubscriber(modid = FuLu.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class PlayerDamageHandler {

    /** 调试开关：把每一步算出来的数字记到控制台。默认关。 */
    private static final boolean DEBUG_LOG = false;

    /** 抗性 VIII 期间打玩家的人，被禁疗多久（刻）。 */
    private static final long NO_HEAL_TICKS = 200L;      // 10 秒
    /** 禁疗的持久数据 key。 */
    private static final String K_NO_HEAL = "FuluNoHealUntil";
    private static final String K_NO_HEAL_HP = "FuluNoHealLastHp";

    /** 动作栏提示重复多少刻（用户要求显示 3~5 秒：4 秒 = 80 刻）。 */
    private static final int ACTION_BAR_REPEAT_TICKS = 80;

    private PlayerDamageHandler() {
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        float incoming = event.getAmount();
        if (incoming <= 0.0F) {
            return;
        }

        // ---------------------------------------------- 1. 次数盾（最先）
        int shield = StandInData.hitShield(player);
        if (shield > 0) {
            event.setAmount(0.0F);                      // 这次不扣玩家血
            // 抗性提升 VIII 期间**不消耗**次数盾（用户要求）：盾在、VIII 也在，就一直满着
            MobEffectInstance resist = player.getEffect(ModMobEffects.RESISTANCE_UP.get());
            boolean maxResistance = resist != null
                    && resist.getAmplifier() + 1 >= StandInData.STAND_IN_RESISTANCE_LEVEL;
            int left = shield;
            // 1 秒内多次伤害算一次（用户要求）：距上次扣点不足 1000ms 就不再扣
            long nowMs = System.currentTimeMillis();
            long lastMs = StandInData.lastHitMs(player);
            boolean sameBurst = lastMs != 0L && nowMs - lastMs < 1000L;
            if (!maxResistance && !sameBurst) {
                left = shield - StandInData.SHIELD_COST_PER_HIT;
                StandInData.setHitShield(player, left);
            }
            StandInData.setLastHitMs(player, nowMs);
            if (left <= 0) {
                // 最后一下：盾碎了 —— 金属破碎声 + 绿色粒子（用户要求）
                player.level().playSound(null, player.blockPosition(),
                        SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.6F, 1.6F);
                spawnNearby(player, ParticleTypes.HAPPY_VILLAGER, 24, 0.6D, 0.7D, 0.6D, 0.05D);
            }
            // 次数盾在的时候，攻击者**照样**吃 80% 反弹（用户第八条）
            reflect(player, event, incoming, true);
            if (DEBUG_LOG) {
                FuLu.LOGGER.info("[standin] shield absorbed {} (left {})", incoming, left);
            }
            return;
        }

        // ---------------------------------------------- 2. 抗性提升减伤
        MobEffectInstance instance = player.getEffect(ModMobEffects.RESISTANCE_UP.get());
        if (instance != null) {
            int level = instance.getAmplifier() + 1;
            float reduction = ResistanceUpEffect.reductionFor(level);
            float remaining = incoming * (1.0F - reduction);
            if (remaining < 0.0F) {
                remaining = 0.0F;                       // 160% 之类：负数夹到 0 = 免疫
            }
            // 减伤之后的伤害再走次数盾的接缝（现在原样返回，等规则）
            remaining = HitCountShield.absorb(player, remaining);
            event.setAmount(remaining);
            // VII/VIII 才反弹（第八条后半段：盾破后 30 秒仍免疫并反弹，靠的就是这个效果）
            reflect(player, event, incoming, ResistanceUpEffect.reflects(level));
            if (DEBUG_LOG) {
                FuLu.LOGGER.info("[resistance] lv{} {} -> {}", level, incoming, remaining);
            }
            if (remaining <= 0.0F) {
                return;                                 // 完全免疫了，谈不上替死
            }
        }

        // ---------------------------------------------- 3. 替死（必须致命才触发）
        if (!StandInData.hasStandIn(player)) {
            return;
        }
        if (event.getAmount() < player.getHealth()) {
            return;                                     // 不致命：不触发，标记保留
        }
        triggerStandIn(player);
        event.setAmount(0.0F);                          // 这一下免疫
    }

    /**
     * 80% 反弹（用户要求：无视无敌帧）。
     *
     * @param always 次数盾挡下时传 true（不管等级都反射）；减伤路径传
     *               {@code reflects(level)}，只有 VII/VIII 才反射
     */
    private static void reflect(Player player, LivingHurtEvent event, float original, boolean always) {
        if (!always) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker) || attacker == player) {
            return;
        }
        // 抗性提升 VIII 期间：攻击者**完全无法回血**（用户要求）
        if (player.hasEffect(ModMobEffects.RESISTANCE_UP.get())) {
            MobEffectInstance rt = player.getEffect(ModMobEffects.RESISTANCE_UP.get());
            boolean maxResist = rt != null
                    && rt.getAmplifier() + 1 >= StandInData.STAND_IN_RESISTANCE_LEVEL;
            if (maxResist) {
                attacker.getPersistentData().putLong(K_NO_HEAL,
                        attacker.level().getGameTime() + NO_HEAL_TICKS);
            }
        }

        // **无视护甲与格挡**：不调用 hurt()，直接改血量。
        // hurt() 会被护甲减免、被盾牌格挡（LivingEntity.isDamageSourceBlocked），
        // 而用户要求反伤"护甲和格挡都拦不住"，所以只能绕过 hurt()：
        //   * 手动置 hurtTime（红色的受击闪一下）
        //   * 血量归零时手动调用 die()（否则不会真的死，因为死亡只在 hurt 里触发）
        float reflectDmg = original * ResistanceUpEffect.reflectRatio();
        float before = attacker.getHealth();
        float after = Math.max(0.0F, before - reflectDmg);
        attacker.hurtTime = 10;
        attacker.setHealth(after);
        if (after <= 0.0F) {
            // 直接死亡：走真实伤害类型，掉落与统计正常
            attacker.die(attacker.damageSources().magic());
        } else {
            // 非致死：清无敌帧，让攻击者自己的下一击不被吞
            attacker.invulnerableTime = 0;
        }
        // 反弹命中：给攻击者身上来一声打击音（用户要求）
        attacker.level().playSound(null, attacker.blockPosition(),
                SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 0.7F, 1.0F);
    }

    /**
     * 替死：免疫、满血、抗性提升 VIII 30 秒、次数盾 20、碎裂图案、血腥提示、阵眼碎掉。
     *
     * <p>"一个阵法只能替死一次"是靠**把阵眼的符弄没**实现的：符消失了，
     * 这一只阵眼下一 tick 就不再给标记，所以也不可能再替死。
     */
    /** 触发替死（供阵魔必杀技复用：伤害被别的模组拦掉时，替身阵照样要能救）。 */
    public static void triggerStandIn(Player player) {
        // 1. 回满血
        player.setHealth(player.getMaxHealth());
        // 2. 抗性提升 VIII，30 秒（amplifier 是 7 = VIII 级）
        player.addEffect(new MobEffectInstance(ModMobEffects.RESISTANCE_UP.get(),
                StandInData.STAND_IN_RESISTANCE_TICKS,
                StandInData.STAND_IN_RESISTANCE_LEVEL - 1,
                false, true, true));
        // 3. 次数盾 20 点（5 下）
        StandInData.setHitShield(player, StandInData.SHIELD_FULL);
        // 4. 替身阵抗性标记（第五部分要求的三样之一）
        StandInData.setResistance(player, true);
        // 5. 碎裂标记 → 客户端播全屏图案（40 刻后清）
        StandInData.triggerShatter(player, 40);
        // 6. 血红色动作栏："替身阵已碎裂，你逃过一劫"，重发到 4 秒
        StandInData.setActionBarTicks(player, ACTION_BAR_REPEAT_TICKS);
        player.displayClientMessage(actionBarMessage(), true);
        // 7. 低沉心跳（"替死"那一下，比碎裂声更抓人）+ 碎裂声 + 金色粒子
        player.level().playSound(null, player.blockPosition(),
                SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 1.0F, 0.8F);
        player.level().playSound(null, player.blockPosition(),
                SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0F, 0.7F);
        spawnShatterParticles(player, 1.0F);
        // 8. 阵眼碎掉：符消失、不返还
        shatterEye(player);
        // 9. 替身标记清掉（这一次已经用掉了）
        StandInData.setStandIn(player, false, null);

        if (DEBUG_LOG) {
            FuLu.LOGGER.info("[standin] triggered for {}", player.getName().getString());
        }
    }

    /** 把给这个玩家挂标记的阵眼清空（符就此消失，不给回玩家）。 */
    private static void shatterEye(Player player) {
        BlockPos eye = StandInData.eyePos(player);
        if (eye == null) {
            return;
        }
        if (player.level().getBlockEntity(eye) instanceof FormationEyeBlockEntity be
                && be.talismanKind() == TalismanKind.STAND_IN) {
            // setTalisman 会同步方块状态（外观变回空的），并且顺手清掉这只阵眼发出的标记
            be.setTalisman(TalismanKind.EMPTY);
            player.level().playSound(null, eye, SoundEvents.GLASS_BREAK,
                    SoundSource.BLOCKS, 1.0F, 0.6F);
            // 阵眼位置爆开一圈金色粒子（用户要求）
            if (player.level() instanceof ServerLevel server) {
                sendNearby(server, eye.getX() + 0.5D, eye.getY() + 0.6D, eye.getZ() + 0.5D,
                        ParticleTypes.TOTEM_OF_UNDYING, 60, 1.2D, 0.4D, 1.2D, 0.25D);
            }
        }
    }

    /** 金色粒子（原版"不死图腾"那套），只发给附近玩家。 */
    private static void spawnShatterParticles(Player player, float intensity) {
        if (!(player.level() instanceof ServerLevel server)) {
            return;
        }
        sendNearby(server, player.getX(), player.getY() + 1.0D, player.getZ(),
                ParticleTypes.TOTEM_OF_UNDYING, (int) (40 * intensity), 0.6D, 0.8D, 0.6D, 0.3D);
    }

    /** 只发给附近玩家（32 格内），而不是整个维度广播。 */
    private static void spawnNearby(Player player, net.minecraft.core.particles.ParticleOptions type,
                                    int count, double dx, double dy, double dz, double speed) {
        if (player.level() instanceof ServerLevel server) {
            sendNearby(server, player.getX(), player.getY() + 1.0D, player.getZ(),
                    type, count, dx, dy, dz, speed);
        }
    }

    private static void sendNearby(ServerLevel level, double x, double y, double z,
                                   net.minecraft.core.particles.ParticleOptions type,
                                   int count, double dx, double dy, double dz, double speed) {
        double rangeSqr = 32.0D * 32.0D;
        for (ServerPlayer nearby : level.players()) {
            if (nearby.distanceToSqr(x, y, z) <= rangeSqr) {
                level.sendParticles(nearby, type, true, x, y, z, count, dx, dy, dz, speed);
            }
        }
    }

    /**
     * 登录 / 换维度时补一次同步。
     *
     * <p>没有这一步的话：数据只在"变化时"推，退出重进客户端就显示成 0 点 —
     * 用户明确要求"退出重进，客户端重新从服务端拉一次当前点数"。
     */
    @SubscribeEvent
    public static void onPlayerLoggedIn(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            StandInData.syncIfServer(player);
        }
    }

    /** 血红色（§4）动作栏文案。 */
    private static Component actionBarMessage() {
        return Component.literal("§4替身阵已碎裂，你逃过一劫");
    }

    /**
     * 每刻维护服务端那些会过期的状态。
     *
     * <p>三件事：动作栏重发（原版一次只显示约 2 秒，用户要 3~5 秒）、
     * 碎裂标记倒计时、抗性提升没了就把"替身阵抗性"标记一起清掉。
     */
    /**
     * 禁疗：被抗性 VIII 玩家"反伤"过的生物，一段时间内**完全不能回血**。
     *
     * <p>原版没有"治疗事件"，所以比对上一刻血量：涨了就扣回去（0%）。
     * 同时摘掉「再生」效果，免得它每刻都在尝试回血。
     */
    @SubscribeEvent
    public static void onLivingTickNoHeal(net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent event) {
        net.minecraft.world.entity.LivingEntity e = event.getEntity();
        if (!(e.level() instanceof net.minecraft.server.level.ServerLevel sl)) {
            return;
        }
        long until = e.getPersistentData().getLong(K_NO_HEAL);
        if (until <= sl.getGameTime()) {
            return;                                      // 没被禁疗
        }
        // 摘掉再生
        if (e.hasEffect(net.minecraft.world.effect.MobEffects.REGENERATION)) {
            e.removeEffect(net.minecraft.world.effect.MobEffects.REGENERATION);
        }
        // 血量涨了就扣回
        if (e.getPersistentData().contains(K_NO_HEAL_HP)) {
            float last = e.getPersistentData().getFloat(K_NO_HEAL_HP);
            if (e.getHealth() > last + 0.01F) {
                e.setHealth(last);
            }
        }
        e.getPersistentData().putFloat(K_NO_HEAL_HP, e.getHealth());
        // 视觉：黑色粒子（表示它在被禁疗）
        if (e.tickCount % 10 == 0) {
            sl.sendParticles(net.minecraft.core.particles.ParticleTypes.SQUID_INK,
                    e.getX(), e.getY() + e.getBbHeight() * 0.7D, e.getZ(),
                    3, 0.25D, 0.25D, 0.25D, 0.0D);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        int actionBar = StandInData.actionBarTicks(player);
        if (actionBar > 0) {
            player.displayClientMessage(actionBarMessage(), true);
            StandInData.setActionBarTicks(player, actionBar - 1);
        }
        int shatter = StandInData.shatterTicks(player);
        if (shatter > 0) {
            StandInData.setShatterTicks(player, shatter - 1);
            if (shatter - 1 == 0) {
                StandInData.syncIfServer(player);       // 通知客户端"图案该收了"
            }
        }
        if (StandInData.hasResistance(player)
                && player.getEffect(ModMobEffects.RESISTANCE_UP.get()) == null) {
            StandInData.setResistance(player, false);   // 30 秒到了
        }
        // 有替身标记时，身上冒淡淡的金色粒子（每 10 刻一次，不刷屏）
        if (StandInData.hasStandIn(player) && player.tickCount % 10 == 0) {
            if (player.level() instanceof ServerLevel server) {
                sendNearby(server, player.getX(), player.getY() + 1.2D, player.getZ(),
                        ParticleTypes.TOTEM_OF_UNDYING, 2, 0.25D, 0.35D, 0.25D, 0.01D);
            }
        }
    }
}
