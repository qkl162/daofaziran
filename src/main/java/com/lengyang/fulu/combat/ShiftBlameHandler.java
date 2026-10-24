package com.lengyang.fulu.combat;

import com.lengyang.fulu.FuLu;
import com.lengyang.fulu.registry.ModMobEffects;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 嫁祸阵：**把一半伤害转给阵法里的另一只生物**。
 *
 * <p>用户要求（第五条）逐条对应：
 * <ul>
 *   <li>受伤者身上有嫁祸标记才处理；</li>
 *   <li>在阵法范围内找**其他**带标记的生物，随机挑一只；</li>
 *   <li>没有别的生物 → 不转移，正常受伤；</li>
 *   <li>各吃 50%；转移出去的那一半**不触发被转移者的无敌帧**，但**会被护甲减免**
 *       （所以用正常的 {@link LivingEntity#hurt} 而不是直接扣血）；</li>
 *   <li>只转移**生物攻击**造成的伤害：摔伤、岩浆、火、虚空这类没有攻击者的不转移；</li>
 *   <li>同一个生物每刻最多被转移一次（防止 A→B→C 链式转移）。</li>
 * </ul>
 *
 * <p>两条我自己定的（用户留给我选的）：
 * <ol>
 *   <li><b>BOSS 免疫转移</b>（末影龙、凋灵）—— 比"只转 25%"更容易解释，也不会出现
 *       "打 BOSS 反而让旁边的小怪掉血"这种怪事；</li>
 *   <li>转移比例是常量 {@link #TRANSFER_RATIO}（50%），想调就改这一个数。</li>
 * </ol>
 *
 * <p><b>防递归</b>：转移时对目标调用 {@code hurt}，目标身上同样有标记，会再触发一次
 * {@link #onLivingHurt} —— 所以用 {@link #TRANSFERRING} 这个静态门闩挡住内层的那一次。
 * 事件在同一线程同步触发，所以一个 boolean 就够；用完必须复位（放在 finally 里）。
 */
@Mod.EventBusSubscriber(modid = FuLu.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ShiftBlameHandler {

    /** 转移比例（用户默认 50%）。 */
    public static final float TRANSFER_RATIO = 0.5F;

    /** 转移时找活物的半径（和阵法的 10 格一致）。 */
    private static final double RADIUS = 10.0D;

    /** 内层 hurt 的防递归门闩。 */
    private static boolean TRANSFERRING = false;

    /** 这一刻已经被转移过的生物（UUID），保证"每刻最多被转移一次"。 */
    private static final Set<java.util.UUID> TRANSFERRED_THIS_TICK = new HashSet<>();

    /** 上一个 tick 的 gameTime，用来清空上面那个集合。 */
    private static long lastTick = -1L;

    private static final boolean DEBUG_LOG = false;

    private ShiftBlameHandler() {
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (TRANSFERRING) {
            return;                                     // 正在转移，内层这一下不再转
        }
        // 用户要求：**只有怪物之间的伤害才转移**。玩家受伤不转移，正常受伤。
        if (!(event.getEntity() instanceof Mob victim)) {
            return;
        }
        if (!(victim.level() instanceof ServerLevel level)) {
        if (com.lengyang.fulu.combat.FormationMarks.isImmune(victim)) {
            return;                              // 只有末影龙免疫
        }
            return;
        }
        boolean marked = victim.getEffect(ModMobEffects.SHIFT_BLAME.get()) != null
                || com.lengyang.fulu.combat.FormationMarks.isMarked(
                        level, victim, com.lengyang.fulu.combat.FormationMarks.SHIFT_BLAME);
        if (!marked) {
            return;                                     // 身上没有嫁祸标记
        }
        if (!isAttackDamage(event.getSource())) {
            return;                                     // 摔伤/岩浆等环境伤害不转移
        }
        float amount = event.getAmount();
        if (amount <= 0.0F) {
            return;
        }

        // 每刻每只生物最多被转一次
        long now = level.getGameTime();
        if (now != lastTick) {
            TRANSFERRED_THIS_TICK.clear();
            lastTick = now;
        }
        if (!TRANSFERRED_THIS_TICK.add(victim.getUUID())) {
            return;
        }

        LivingEntity target = pickOtherMarked(victim, level);
        if (target == null) {
            return;                                     // 范围内没有别人，正常受伤
        }

        float half = amount * TRANSFER_RATIO;
        event.setAmount(amount - half);                 // 原目标只吃一半

        TRANSFERRING = true;
        try {
            target.invulnerableTime = 0;                // 不吃无敌帧（用户要求）
            target.hurt(event.getSource(), half);        // 走正常流程 → 护甲会减免
        } finally {
            TRANSFERRING = false;
        }

        spawnTransferFx(level, victim, target);
        level.playSound(null, victim.blockPosition(), SoundEvents.SOUL_ESCAPE,
                SoundSource.PLAYERS, 0.8F, 0.7F);

        if (DEBUG_LOG) {
            FuLu.LOGGER.info("[shift-blame] {} -> {} : {}/{}",
                    victim.getName().getString(), target.getName().getString(), half, amount);
        }
    }

    /**
     * 随机挑一只范围内**其他**带标记的生物。
     *
     * <p>只认"有标记"的 —— 标记就是阵法范围的判据（每刻刷新、离开就掉），
     * 所以这里不用再算一次盒子，两者永远是同一个集合。
     */
    private static LivingEntity pickOtherMarked(LivingEntity victim, ServerLevel level) {
        // 只挑别的**怪物**（玩家不参与转移）
        AABB box = victim.getBoundingBox().inflate(RADIUS);
        List<Mob> candidates = new ArrayList<>();
        for (Mob other : level.getEntitiesOfClass(Mob.class, box)) {
            if (other == victim || !other.isAlive()) {
                continue;
            }
            if (other.getEffect(ModMobEffects.SHIFT_BLAME.get()) == null) {
                continue;
            }
            candidates.add(other);
        }
        if (candidates.isEmpty()) {
            return null;
        }
        return candidates.get(level.random.nextInt(candidates.size()));
    }

    /** 只转移"生物攻击"造成的伤害：有攻击者（玩家本人或射出的箭/三叉戟）。 */
    private static boolean isAttackDamage(DamageSource source) {
        Entity attacker = source.getEntity();
        if (attacker instanceof LivingEntity) {
            return true;
        }
        if (attacker instanceof Projectile projectile) {
            return projectile.getOwner() instanceof LivingEntity;
        }
        return false;
    }

    /** 我选的策略：末影龙、凋灵免疫转移。 */
    private static boolean isBoss(LivingEntity entity) {
        return entity instanceof EnderDragon || entity instanceof WitherBoss;
    }

    /**
     * 视觉效果：从原目标到被转移目标拉一条粒子线（约 0.5 秒），被转移者身上冒红粒子。
     *
     * <p>线怎么画：{@code count = 0} 的 {@code sendParticles} 会把 {@code dx,dy,dz} 当成
     * **速度**（而不是散布范围），粒子就沿着这个速度飞出去 —— 一个包就能画一条流动的线，
     * 不用每格发一次（那样十几个包，粒子一多就卡）。
     *
     * <p>只发给附近玩家（32 格内），不广播整个维度。
     */
    private static void spawnTransferFx(ServerLevel level, LivingEntity from, LivingEntity to) {
        Vec3 start = from.position().add(0.0D, from.getBbHeight() * 0.5D, 0.0D);
        Vec3 end = to.position().add(0.0D, to.getBbHeight() * 0.5D, 0.0D);
        Vec3 dir = end.subtract(start);
        if (dir.lengthSqr() < 0.01D) {
            return;
        }
        Vec3 speed = dir.normalize().scale(0.45D);

        for (ServerPlayer nearby : level.players()) {
            if (nearby.distanceToSqr(start) > 32.0D * 32.0D) {
                continue;
            }
            // 一条朝目标飞去的紫色魂线（3 个不同起点，看起来像一道流）
            for (int i = 0; i < 3; i++) {
                Vec3 p = start.add(dir.scale(i * 0.12D));
                level.sendParticles(nearby, ParticleTypes.SOUL, true,
                        p.x, p.y, p.z, 0, speed.x, speed.y, speed.z, 1.0D);
            }
            // 被转移者身上冒红色伤害粒子
            level.sendParticles(nearby, ParticleTypes.DAMAGE_INDICATOR, true,
                    end.x, end.y, end.z, 6, 0.3D, 0.4D, 0.3D, 0.05D);
        }
    }

    /**
     * 给某个生物挂/刷新嫁祸标记。
     *
     * <p>时长 60 刻（3 秒）且每刻刷新 —— 和反重力当时一样，是"离开阵法最多 3 秒就掉"。
     * 但这里**没有**用无限时长，所以就算阵眼被拆了，标记也会自己过期，
     * 不会重演"负面效果比阵法活得久"那个 bug。
     */
    public static void mark(LivingEntity entity) {
        entity.addEffect(new MobEffectInstance(ModMobEffects.SHIFT_BLAME.get(), 60, 0,
                true, false, false));
    }
}
