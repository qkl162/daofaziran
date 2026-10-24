package com.lengyang.fulu.combat;

import com.lengyang.fulu.FuLu;
import com.lengyang.fulu.registry.ModMobEffects;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 无明阵（强化版）：失明 + 攻击失准 + 互相误伤 + 恐惧 + 黑暗侵蚀 + 玩家增益 + 感知混乱。
 *
 * <p><b>关于"视野"</b>：Minecraft 没有"视野半径"这个参数，怪物寻敌是 goal 驱动的。
 * 所以这里的做法是每刻检查仇恨目标、超出 1 格就 {@code setTarget(null)} ——
 * 效果上等价于"几乎全瞎"。
 *
 * <p><b>关于"听觉"</b>：声音是自己记的。玩家走路/跑步/攻击/使用物品时登记一个声音事件，
 * 带无明标记的怪如果听得见，就寻路过去（30% 会走错方向）。
 *
 * <p>玩家侧的规则（潜行加速、+20% 伤害、30% 闪避）靠阵眼给玩家挂的 {@code BLIND_ZONE} 标记判断。
 */
@Mod.EventBusSubscriber(modid = FuLu.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BlindHandler {

    // ---- 失明强度
    /** 普通怪物失明后的视野（格）：几乎全瞎。 */
    private static final double SIGHT_BLIND = 1.0D;

    // ---- 声音半径（强化后缩小）
    private static final double NOISE_WALK = 5.0D;
    private static final double NOISE_RUN = 8.0D;
    private static final double NOISE_ATTACK = 8.0D;
    private static final double NOISE_USE = 5.0D;

    // ---- 失准 / 误伤 / 恐惧 / 侵蚀
    private static final float MISS_CHANCE = 0.50F;
    private static final float FRIENDLY_FIRE_CHANCE = 0.50F;
    private static final float FEAR_HP_RATIO = 0.30F;
    private static final int FEAR_TICKS = 60;            // 3 秒
    private static final int ERODE_INTERVAL = 60;        // 每 3 秒 1 点
    private static final float ERODE_DAMAGE = 1.0F;
    /** 阵内生物的回血只保留 10%。 */
    private static final float HEAL_KEEP = 0.10F;

    // ---- 玩家增益
    private static final float SNEAK_SPEED_BONUS = 0.20F;   // +20%
    private static final float PLAYER_DAMAGE_BONUS = 1.20F; // +20%
    private static final float DODGE_CHANCE = 0.30F;

    // ---- 感知混乱
    private static final float WRONG_DIR_CHANCE = 0.30F;

    private record Noise(Vec3 pos, double radius, long time) {
    }

    private static final Map<Level, Noise> LAST_NOISE = new HashMap<>();

    private static final boolean DEBUG_LOG = true;     // 临时打开，确认到底有没有在跑

    private BlindHandler() {
    }

    // ================================================================ 声音登记
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }
        if (player.isSpectator()) {
            return;
        }
        // 潜行不发声音
        if (player.isShiftKeyDown()) {
            // 潜行时移动加速（无明阵内 +20%）
            if (player.hasEffect(ModMobEffects.BLIND.get())) {
                player.addEffect(new MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED, 40, 0,
                        true, false, false));
            }
            return;
        }
        Vec3 motion = player.getDeltaMovement();
        double speed = Math.hypot(motion.x, motion.z);
        if (speed < 0.02D) {
            return;
        }
        boolean running = speed > 0.13D;
        register(player.level(), player.position(), running ? NOISE_RUN : NOISE_WALK);
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();

        // ---------------- 玩家攻击带无明标记的怪：+20% 伤害
        if (event.getSource().getEntity() instanceof ServerPlayer attacker
                && victim instanceof Mob && victim.hasEffect(ModMobEffects.BLIND.get())
                && attacker.hasEffect(ModMobEffects.BLIND.get())) {
            event.setAmount(event.getAmount() * PLAYER_DAMAGE_BONUS);
        }

        // ---------------- 玩家闪避：30%
        if (victim instanceof ServerPlayer player
                && player.hasEffect(ModMobEffects.BLIND.get())
                && event.getSource().getEntity() instanceof Mob
                && player.getRandom().nextFloat() < DODGE_CHANCE) {
            event.setCanceled(true);
            if (player.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.CLOUD,
                        player.getX(), player.getY() + 1.0D, player.getZ(),
                        12, 0.3D, 0.5D, 0.3D, 0.05D);
            }
            return;
        }

        // ---------------- 怪物失准 + 误伤：只对"带无明标记的近战怪打玩家"生效
        if (event.getSource().getEntity() instanceof Mob attacker
                && victim instanceof ServerPlayer
                && attacker.hasEffect(ModMobEffects.BLIND.get())
                && attacker.getRandom().nextFloat() < MISS_CHANCE) {
            event.setCanceled(true);                     // 挥空
            ServerLevel sl = (ServerLevel) attacker.level();
            // 落空：一大团黑烟 + 一声"呼"的空挥声 + 动作栏提示（让它能被看见）
            sl.sendParticles(ParticleTypes.LARGE_SMOKE,
                    victim.getX(), victim.getY() + victim.getBbHeight() * 0.6D, victim.getZ(),
                    18, 0.45D, 0.45D, 0.45D, 0.03D);
            sl.sendParticles(ParticleTypes.SMOKE,
                    victim.getX(), victim.getY() + 0.1D, victim.getZ(),
                    10, 0.4D, 0.05D, 0.4D, 0.01D);
            sl.playSound(null, victim.blockPosition(),
                    net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_SWEEP,
                    net.minecraft.sounds.SoundSource.HOSTILE, 1.0F, 0.6F);
            if (victim instanceof ServerPlayer sp && sp.getRandom().nextFloat() < 0.5F) {
                sp.displayClientMessage(
                        net.minecraft.network.chat.Component.translatable("msg.fulu.blind.miss"),
                        true);
            }

            // 误伤：旁边有别的怪就打它
            if (attacker.getRandom().nextFloat() < FRIENDLY_FIRE_CHANCE) {
                List<Mob> others = sl.getEntitiesOfClass(Mob.class, attacker.getBoundingBox().inflate(2.5D));
                for (Mob other : others) {
                    if (other != attacker && other.isAlive()) {
                        other.hurt(attacker.damageSources().mobAttack(attacker), event.getAmount());
                        sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                                other.getX(), other.getY() + other.getBbHeight() * 0.5D, other.getZ(),
                                6, 0.2D, 0.2D, 0.2D, 0.05D);
                        sl.sendParticles(ParticleTypes.SMOKE,
                                other.getX(), other.getY() + other.getBbHeight() * 0.5D, other.getZ(),
                                6, 0.2D, 0.2D, 0.2D, 0.02D);
                        break;
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onRightClick(PlayerInteractEvent.RightClickItem event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            register(player.level(), player.position(), NOISE_USE);
        }
    }

    /**
     * 远程射偏：失明的怪物射出的箭，50% 几率被"打歪"。
     *
     * <p>为什么在 {@code EntityJoinLevelEvent} 里做：{@code ArrowLooseEvent} 只是"拉弓"，
     * 这时箭实体还没生成；而箭一进世界就会有速度，在它刚加入时改速度最直接。
     */
    @SubscribeEvent
    public static void onProjectileJoin(net.minecraftforge.event.entity.EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof net.minecraft.world.entity.projectile.Projectile proj)) {
            return;
        }
        net.minecraft.world.entity.Entity owner = proj.getOwner();
        if (!(owner instanceof Mob shooter)) {
            return;
        }
        if (shooter.getEffect(ModMobEffects.BLIND.get()) == null) {
            return;
        }
        if (shooter.getRandom().nextFloat() >= MISS_CHANCE) {
            return;                                      // 这一发没偏
        }
        // 打歪：方向随机偏 20~70 度，并随机偏上下
        Vec3 v = proj.getDeltaMovement();
        double len = v.length();
        if (len < 0.01D) {
            return;
        }
        double yaw = Math.atan2(v.z, v.x);
        double pitch = Math.asin(Math.max(-1.0D, Math.min(1.0D, v.y / len)));
        yaw += (shooter.getRandom().nextDouble() - 0.5D) * Math.toRadians(140.0D);
        pitch += (shooter.getRandom().nextDouble() - 0.5D) * Math.toRadians(30.0D);
        pitch = Math.max(Math.toRadians(-60.0D), Math.min(Math.toRadians(60.0D), pitch));
        double nx = Math.cos(yaw) * Math.cos(pitch) * len;
        double ny = Math.sin(pitch) * len;
        double nz = Math.sin(yaw) * Math.cos(pitch) * len;
        proj.setDeltaMovement(nx, ny, nz);
        if (shooter.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.SMOKE,
                    proj.getX(), proj.getY(), proj.getZ(), 6, 0.15D, 0.15D, 0.15D, 0.02D);
        }
    }

    private static void register(Level level, Vec3 pos, double radius) {
        LAST_NOISE.put(level, new Noise(pos, radius, level.getGameTime()));
    }

    // ================================================================ 怪物每刻
    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        // **除玩家以外的所有生物**（用户要求）：
        //   * 玩家：排除（玩家视野正常，只吃增益）
        //   * 自己的魂：排除（它是我们的实体，不该被侵蚀）
        //   * 其它一切 LivingEntity（怪物、动物、BOSS、盔甲架……）
        // getEntity() 的返回类型本来就是 LivingEntity，不需要再 instanceof
        LivingEntity living = event.getEntity();
        // 玩家排除（玩家只吃增益）。注意：SoulEntity 是 Entity 不是 LivingEntity，
        // 所以它根本进不到这里，不用显式排除。
        if (living instanceof ServerPlayer) {
            return;
        }
        if (!(living.level() instanceof ServerLevel level)) {
        if (com.lengyang.fulu.combat.FormationMarks.isImmune(living)) {
            return;                              // 只有末影龙免疫
        }
            return;
        }
        // 标记有两种来源：状态效果（普通怪）或持久数据（对效果免疫的 BOSS）
        boolean marked = living.getEffect(ModMobEffects.BLIND.get()) != null
                || com.lengyang.fulu.combat.FormationMarks.isMarked(
                        level, living, com.lengyang.fulu.combat.FormationMarks.BLIND);
        if (!marked) {
            return;
        }
        // 只有 Mob 才有寻路和目标；非 Mob（比如盔甲架）不参与失明/听觉那部分
        Mob mob = living instanceof Mob m ? m : null;

        if (DEBUG_LOG && living.tickCount % 20 == 0) {
            FuLu.LOGGER.info("[blind] processing {} (marked={})",
                    living.getName().getString(), true);
        }

        double sight = SIGHT_BLIND;
        // 阵魔：失明效果减半 —— 视野 8 格而不是 1 格
        if (living instanceof com.lengyang.fulu.entity.FormationDemonEntity) {
            sight = 8.0D;
        }

        // ---------- 恐惧：血量低于 30% 时 20% 几率逃跑
        if (mob.getHealth() < mob.getMaxHealth() * FEAR_HP_RATIO
                && !mob.hasEffect(ModMobEffects.FEAR.get())) {            // 绝对触发：血量一低就恐惧
            mob.addEffect(new MobEffectInstance(ModMobEffects.FEAR.get(), FEAR_TICKS, 0,
                    true, false, false));
            level.sendParticles(ParticleTypes.WITCH,
                    mob.getX(), mob.getY() + mob.getBbHeight() + 0.4D, mob.getZ(),
                    10, 0.3D, 0.3D, 0.3D, 0.02D);
        }
        if (mob.hasEffect(ModMobEffects.FEAR.get())) {
            // 随机方向逃
            if (mob.tickCount % 5 == 0) {
                double a = mob.getRandom().nextDouble() * Math.PI * 2.0D;
                mob.getNavigation().moveTo(mob.getX() + Math.cos(a) * 8.0D, mob.getY(),
                        mob.getZ() + Math.sin(a) * 8.0D, 1.4D);
            }
            level.sendParticles(ParticleTypes.WITCH,
                    mob.getX(), mob.getY() + mob.getBbHeight() + 0.4D, mob.getZ(),
                    2, 0.2D, 0.2D, 0.2D, 0.01D);
        }

        // ---------- 回血压制：**只对怪物**（含 BOSS）生效，且不削减
        // 玩家到不了这里（上面 instanceof Mob 已经挡住了），所以玩家的回血是正常的。
        // BOSS 也是 Mob，所以末影龙、凋灵一样只保留 10% 回血。
        // 原版没有"治疗事件"可以直接拦，所以比对上一刻的血量：涨了就把 90% 扣回去。
        CompoundTag hd = mob.getPersistentData();
        if (hd.contains("FuluBlindLastHp")) {
            float last = hd.getFloat("FuluBlindLastHp");
            float nowHp = mob.getHealth();
            if (nowHp > last + 0.01F) {
                float gain = nowHp - last;
                mob.setHealth(last + gain * HEAL_KEEP);
            }
            hd.putFloat("FuluBlindLastHp", mob.getHealth());
        } else {
            hd.putFloat("FuluBlindLastHp", mob.getHealth());
        }
        // 再生效果也直接掐掉
        if (mob.hasEffect(net.minecraft.world.effect.MobEffects.REGENERATION)) {
            mob.removeEffect(net.minecraft.world.effect.MobEffects.REGENERATION);
        }

        // ---------- 黑暗侵蚀：每 3 秒 1 点，无视护甲（magic）
        if (mob.tickCount % ERODE_INTERVAL == 0) {
            mob.hurt(level.damageSources().magic(), ERODE_DAMAGE);
            level.sendParticles(ParticleTypes.SMOKE,
                    mob.getX(), mob.getY() + mob.getBbHeight() * 0.5D, mob.getZ(),
                    4, 0.3D, 0.3D, 0.3D, 0.01D);
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    mob.getX(), mob.getY() + 0.3D, mob.getZ(),
                    2, 0.2D, 0.1D, 0.2D, 0.0D);
        }

        // ---------- 视野：目标超出范围就丢仇恨
        if (mob.getTarget() != null && mob.getTarget().isAlive()) {
            if (mob.distanceTo(mob.getTarget()) > sight) {
                mob.setTarget(null);
            }
        }

        // ---------- 听觉：朝声音摸过去（30% 走错方向）
        Noise noise = LAST_NOISE.get(level);
        if (noise != null && level.getGameTime() - noise.time() < 200L) {
            double d = Math.sqrt(noise.pos().distanceToSqr(mob.position()));
            if (d <= noise.radius() && d > sight) {
                double tx = noise.pos().x;
                double tz = noise.pos().z;
                if (mob.getRandom().nextFloat() < WRONG_DIR_CHANCE) {
                    double a = mob.getRandom().nextDouble() * Math.PI * 2.0D;
                    tx += Math.cos(a) * 6.0D;
                    tz += Math.sin(a) * 6.0D;
                }
                double dToNoise = Math.sqrt(
                        noise.pos().distanceToSqr(mob.position()));
                CompoundTag pd = mob.getPersistentData();
                int spin = pd.getInt("FuluBlindSpin");
                if (spin > 0) {
                    // 转圈中：原地转头、不寻路、不攻击
                    pd.putInt("FuluBlindSpin", spin - 1);
                    mob.setYRot(mob.getYRot() + 12.0F);
                    mob.setTarget(null);
                } else if (dToNoise < 2.5D && mob.getRandom().nextFloat() < 0.02F) {
                    // 到了声音位置：50% 左右会原地转 3 秒
                    pd.putInt("FuluBlindSpin", 60);
                } else {
                    mob.getNavigation().moveTo(tx, noise.pos().y, tz, 1.0D);
                }
            }
        }

        // ---------- 头顶符号：黑烟（"?"）和亮点（"！"）交替，每 1 秒换一次
        boolean showBang = (mob.tickCount / 20) % 2 == 1 || mob.getTarget() != null;
        if (mob.tickCount % 8 == 0) {
            if (showBang) {
                level.sendParticles(ParticleTypes.END_ROD,
                        mob.getX(), mob.getY() + mob.getBbHeight() + 0.55D, mob.getZ(),
                        3, 0.08D, 0.10D, 0.08D, 0.0D);
            } else {
                level.sendParticles(ParticleTypes.LARGE_SMOKE,
                        mob.getX(), mob.getY() + mob.getBbHeight() + 0.55D, mob.getZ(),
                        4, 0.22D, 0.14D, 0.22D, 0.0D);
            }
            // 身上的黑烟一直有
            level.sendParticles(ParticleTypes.SMOKE,
                    mob.getX(), mob.getY() + mob.getBbHeight() * 0.5D, mob.getZ(),
                    2, 0.25D, 0.25D, 0.25D, 0.0D);
        }
    }
}
