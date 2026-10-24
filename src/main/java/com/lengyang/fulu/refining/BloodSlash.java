package com.lengyang.fulu.refining;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.lengyang.fulu.FuLu;
import com.lengyang.fulu.block.TalismanKind;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * 【阿鼻地狱·诛仙血阵】：三道黑色月牙剑气，**无视地形**，穿透一切。
 *
 * <ul>
 *   <li>三道品字（±5 格偏移），合计覆盖 **15 格宽**、**25 格长**；</li>
 *   <li>**无视方块**：没有人造射线检测，墙后与天上的目标都能砍到；</li>
 *   <li>**越砍越强**：每命中一个目标，该道剑气伤害 +10%、视觉变宽；</li>
 *   <li>伤害 = **20 + 目标当前血量 15%**；</li>
 *   <li>**冥界诅咒 8 秒**：护甲清零 + 禁疗（{@link BloodCurseHandler}）；</li>
 *   <li>**恐惧 3 秒**：清目标、背对玩家逃；</li>
 *   <li>**斩杀线 30%**：非玩家目标低血直接黑雾爆开，震退周围并掉灵魂碎片；</li>
 *   <li>**吸血**：造成多少伤害就回多少血，溢出转成吸收（金色额外生命）；</li>
 *   <li>阵法联动：**炼妖阵**内斩杀必掉 2 颗妖丹；**闭环阵**内剑气会反弹一次。</li>
 * </ul>
 */
public final class BloodSlash {

    private static final int WAVES = 3;
    private static final double[] LATERAL = {-5.0D, 0.0D, 5.0D};
    private static final double WAVE_HALF_WIDTH = 2.6D;
    private static final double SPEED = 1.5D;
    private static final int LIFE = 17;
    private static final double DEPTH = 2.5D;
    private static final float BASE_DAMAGE = 20.0F;
    private static final double HEALTH_RATIO = 0.15D;
    public static final int CURSE_TICKS = 160;
    private static final double EXECUTE_RATIO = 0.30D;

    public static final UUID ARMOR_BREAK_ID =
            UUID.fromString("3e7d1c48-5a2b-4f16-8c9d-0b7e4a1f6d22");

    private static final List<Wave> ACTIVE = new ArrayList<>();
    /** 每个施法者这一轮的累计伤害 / 斩杀数（剑气全部结束后统一结算吸血）。 */
    private static final Map<UUID, Round> ROUNDS = new HashMap<>();

    private static final class Round {
        float dealt;
        int kills;
    }

    private static final class Wave {
        final ServerPlayer caster;
        final ServerLevel level;
        Vec3 dir;
        Vec3 origin;
        int ticksLeft = LIFE;
        float bonus;
        boolean bounced;
        /** 这一道是否破盾（第三斩）。 */
        boolean shieldBreak;
        final Set<UUID> hit = new HashSet<>();
        /** 对应的视觉实体（客户端画月牙用）。 */
        com.lengyang.fulu.entity.BloodWaveEntity visual;

        Wave(ServerPlayer caster, ServerLevel level, Vec3 dir, Vec3 origin) {
            this.caster = caster;
            this.level = level;
            this.dir = dir;
            this.origin = origin;
        }
    }

    private BloodSlash() {
    }

    /** 施放。 */
    public static void cast(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 dir = player.getLookAngle().normalize();
        Vec3 right = new Vec3(-dir.z, 0.0D, dir.x).normalize();
        Vec3 eye = player.getEyePosition();
        ROUNDS.put(player.getUUID(), new Round());
        for (double off : LATERAL) {
            Vec3 origin = eye.add(right.scale(off)).add(dir.scale(1.0D));
            Wave w = new Wave(player, level, dir, origin);
            // 生成视觉实体（会自己往前飞，客户端用网格画月牙）
            var visual = new com.lengyang.fulu.entity.BloodWaveEntity(
                    com.lengyang.fulu.registry.ModEntities.BLOOD_WAVE.get(), level);
            visual.setup(dir, origin);
            level.addFreshEntity(visual);
            w.visual = visual;
            ACTIVE.add(w);
        }
        // 施法瞬间：原地爆开一圈放射星形光刃（仿李信·光信大招）
        var star = new com.lengyang.fulu.entity.BloodWaveEntity(
                com.lengyang.fulu.registry.ModEntities.BLOOD_WAVE.get(), level);
        // 放在**脚下**：剑刃从地面长出去，而不是挂在头顶
        star.setupStar(new Vec3(player.getX(), player.getY(), player.getZ()), player.getYRot());
        level.addFreshEntity(star);

        level.playSound(null, player.blockPosition(), SoundEvents.WITHER_SPAWN,
                SoundSource.PLAYERS, 2.0F, 0.45F);
        FuLu.LOGGER.info("[blood] 3 waves cast by {}", player.getName().getString());
    }

    /** 单道剑气（三段斩击里每一斩都调它）。 */
    public static void castSingle(ServerPlayer player, float yawOffsetDeg) {
        castSingle(player, yawOffsetDeg, false);
    }

    /** 单道剑气；shieldBreak = 这一刀能破盾（第三斩）。 */
    public static void castSingle(ServerPlayer player, float yawOffsetDeg, boolean shieldBreak) {
        com.lengyang.fulu.FuLu.LOGGER.info("[ult] castSingle angle={} break={}",
                yawOffsetDeg, shieldBreak);
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 base = player.getLookAngle().normalize();
        double rad = Math.toRadians(yawOffsetDeg);
        Vec3 dir = new Vec3(
                base.x * Math.cos(rad) - base.z * Math.sin(rad),
                base.y,
                base.x * Math.sin(rad) + base.z * Math.cos(rad)).normalize();
        Vec3 origin = player.getEyePosition().add(dir.scale(1.0D));

        ROUNDS.computeIfAbsent(player.getUUID(), k -> new Round());
        Wave w = new Wave(player, level, dir, origin);
        w.shieldBreak = shieldBreak;
        var visual = new com.lengyang.fulu.entity.BloodWaveEntity(
                com.lengyang.fulu.registry.ModEntities.BLOOD_WAVE.get(), level);
        visual.setup(dir, origin);
        level.addFreshEntity(visual);
        w.visual = visual;
        ACTIVE.add(w);

        // 地面裂纹（贴地 Decal）：从脚下向前 25 格
        var crack = new com.lengyang.fulu.entity.GroundCrackEntity(
                com.lengyang.fulu.registry.ModEntities.GROUND_CRACK.get(), level);
        crack.setup(new Vec3(player.getX(), player.getY(), player.getZ()),
                (float) Math.toDegrees(Math.atan2(dir.x, dir.z)), 25.0F,
                level.random.nextInt(9999));
        level.addFreshEntity(crack);
        com.lengyang.fulu.FuLu.LOGGER.info(
                "[ult] wave spawned dir=({}, {}, {}) origin={}",
                String.format("%.2f", dir.x), String.format("%.2f", dir.z),
                String.format("%.2f", dir.y), origin);
    }

    /** 每刻推进。 */
    public static void tick(ServerLevel level) {
        if (ACTIVE.isEmpty()) {
            return;
        }
        List<Wave> done = new ArrayList<>();
        for (Wave w : ACTIVE) {
            if (w.level != level) {
                continue;
            }
            if (!w.caster.isAlive()) {
                done.add(w);
                continue;
            }
            w.origin = w.origin.add(w.dir.scale(SPEED));

            // 闭环阵：碰到 10 格外的阵壁反弹一次
            if (!w.bounced
                    && FormationCastBonus.near(w.caster, TalismanKind.LOOP)
                    && w.caster.distanceToSqr(w.origin) > 100.0D) {
                w.dir = w.dir.reverse();
                w.bounced = true;
            }

            // 视觉：剑刃从根部生长（进度 = 已飞过的比例）
            if (w.visual != null && w.visual.isAlive()) {
                w.visual.setGrow(1.0F - w.ticksLeft / (float) LIFE);
            }
            draw(level, w);
            scan(level, w);
            if (--w.ticksLeft <= 0) {
                done.add(w);
                if (w.visual != null && w.visual.isAlive()) {
                    w.visual.discard();
                }
            }
        }
        ACTIVE.removeAll(done);
        if (ACTIVE.isEmpty()) {
            settle(level);
        }
    }

    /** 视觉交给 {@code BloodWaveEntity} + 渲染器，这里不再撒粒子。 */
    private static void draw(ServerLevel level, Wave w) {
        if (w.visual != null && !w.visual.isAlive()) {
            w.visual = null;
        }
    }

    /** **无视地形**的判定：只看剑气那一层薄薄的纵深窗口。 */
    private static void scan(ServerLevel level, Wave w) {
        Vec3 right = new Vec3(-w.dir.z, 0.0D, w.dir.x).normalize();
        AABB area = new AABB(w.origin, w.origin)
                .inflate(WAVE_HALF_WIDTH + 3.0D, 6.0D, WAVE_HALF_WIDTH + 3.0D);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, area)) {
            if (e == w.caster || !e.isAlive() || w.hit.contains(e.getUUID())) {
                continue;
            }
            Vec3 rel = e.position().add(0.0D, e.getBbHeight() * 0.5D, 0.0D).subtract(w.origin);
            if (Math.abs(rel.dot(w.dir)) > DEPTH) {
                continue;
            }
            if (Math.abs(rel.dot(right)) > WAVE_HALF_WIDTH + 0.6D) {
                continue;
            }
            w.hit.add(e.getUUID());
            w.bonus += 0.1F;
            if (w.visual != null && w.visual.isAlive()) {
                w.visual.setScale(1.0F + w.bonus * 1.5F);
            }
            hit(level, w, e);
        }
    }

    private static void hit(ServerLevel level, Wave w, LivingEntity e) {
        ServerPlayer caster = w.caster;
        e.invulnerableTime = 0;
        // **第三斩破盾**：先打掉格挡状态，再结算伤害
        if (w.shieldBreak && e.isBlocking()) {
            // 1.20.1 没有 disableShield(boolean)：直接打断"正在使用物品"，
            // 再给玩家盾牌挂 5 秒冷却，效果等同"破盾"
            e.stopUsingItem();
            if (e instanceof ServerPlayer sp) {
                sp.getCooldowns().addCooldown(net.minecraft.world.item.Items.SHIELD, 100);
            }
            level.playSound(null, e.blockPosition(),
                    net.minecraft.sounds.SoundEvents.SHIELD_BREAK,
                    net.minecraft.sounds.SoundSource.PLAYERS, 1.5F, 0.7F);
            level.sendParticles(ParticleTypes.CRIT,
                    e.getX(), e.getY() + e.getBbHeight() * 0.6D, e.getZ(),
                    30, 0.4D, 0.5D, 0.4D, 0.3D);
        }
        float dmg = (BASE_DAMAGE + (float) (e.getHealth() * HEALTH_RATIO)) * (1.0F + w.bonus);
        e.hurt(level.damageSources().playerAttack(caster), dmg);
        ROUNDS.computeIfAbsent(caster.getUUID(), k -> new Round()).dealt += dmg;

        applyCurse(level, e);

        if (e instanceof Mob mob) {
            mob.setTarget(null);
            mob.getNavigation().stop();
            Vec3 away = e.position().subtract(caster.position()).normalize().scale(0.8D);
            mob.setDeltaMovement(away.x, 0.15D, away.z);
            mob.hurtMarked = true;
            mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 1));
        }

        if (e.isAlive() && !(e instanceof net.minecraft.world.entity.player.Player)
                && e.getHealth() < e.getMaxHealth() * EXECUTE_RATIO) {
            execute(level, caster, e);
        }

        level.playSound(null, e.blockPosition(), SoundEvents.SKELETON_HURT,
                SoundSource.PLAYERS, 1.0F, 1.3F);
        level.sendParticles(ParticleTypes.CRIT,
                e.getX(), e.getY() + e.getBbHeight() * 0.6D, e.getZ(),
                12, 0.3D, 0.4D, 0.3D, 0.2D);
        // 命中爆点：环形冲击波 Mesh + 粒子飞散
        var burst = new com.lengyang.fulu.entity.HitBurstEntity(
                com.lengyang.fulu.registry.ModEntities.HIT_BURST.get(), level);
        burst.setup(e.position().add(0.0D, e.getBbHeight() * 0.5D, 0.0D), 1.0F);
        level.addFreshEntity(burst);
        level.sendParticles(ParticleTypes.END_ROD,
                e.getX(), e.getY() + e.getBbHeight() * 0.5D, e.getZ(),
                18, 0.4D, 0.4D, 0.4D, 0.12D);
    }

    /** 处决：黑雾 + 震退 + 灵魂碎片（炼妖阵内额外掉妖丹）。 */
    private static void execute(ServerLevel level, ServerPlayer caster, LivingEntity e) {
        e.invulnerableTime = 0;
        e.hurt(level.damageSources().magic(), 1000.0F);

        level.sendParticles(ParticleTypes.SQUID_INK,
                e.getX(), e.getY() + e.getBbHeight() * 0.5D, e.getZ(),
                60, 0.6D, 0.7D, 0.6D, 0.08D);
        level.sendParticles(ParticleTypes.SOUL,
                e.getX(), e.getY() + e.getBbHeight() * 0.5D, e.getZ(),
                20, 0.4D, 0.5D, 0.4D, 0.02D);
        level.playSound(null, e.blockPosition(), SoundEvents.WITHER_HURT,
                SoundSource.PLAYERS, 1.3F, 0.5F);

        for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class,
                e.getBoundingBox().inflate(4.0D))) {
            if (other == e || other == caster) {
                continue;
            }
            Vec3 push = other.position().subtract(e.position()).normalize().scale(1.2D);
            other.push(push.x, 0.4D, push.z);
            other.hurtMarked = true;
        }

        // 灵魂碎片
        level.addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(level,
                e.getX(), e.getY() + 0.5D, e.getZ(),
                new ItemStack(com.lengyang.fulu.registry.ModItems.DEMON_SHARD.get())));

        // 炼妖阵：斩杀必掉 2 颗妖丹
        if (FormationCastBonus.near(caster, TalismanKind.REFINING)) {
            for (int i = 0; i < 2; i++) {
                com.lengyang.fulu.combat.RefiningHandler.grantEssence(level, e);
            }
        }

        ROUNDS.computeIfAbsent(caster.getUUID(), k -> new Round()).kills++;
        FuLu.LOGGER.info("[blood] executed {}", e.getType());
    }

    /** 结算：吸血 + 斩杀奖励，然后清空这一轮。 */
    private static void settle(ServerLevel level) {
        if (ROUNDS.isEmpty()) {
            return;
        }
        for (Map.Entry<UUID, Round> entry : ROUNDS.entrySet()) {
            ServerPlayer p = level.getServer().getPlayerList().getPlayer(entry.getKey());
            Round r = entry.getValue();
            if (p != null && p.isAlive() && r.dealt > 0.0F) {
                awardLifesteal(p, r.dealt, r.kills);
            }
        }
        ROUNDS.clear();
    }

    /** 吸血：回血，溢出部分转成吸收（金色额外生命）。 */
    public static void awardLifesteal(ServerPlayer player, float dealt, int kills) {
        float before = player.getHealth();
        float after = Math.min(player.getMaxHealth(), before + dealt);
        player.setHealth(after);
        float overflow = before + dealt - player.getMaxHealth();
        if (overflow > 0.0F) {
            player.setAbsorptionAmount(player.getAbsorptionAmount() + overflow);
        }
        if (kills > 0) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 100, 1));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100, 1));
        }
        player.displayClientMessage(
                net.minecraft.network.chat.Component.translatable("msg.fulu.blood.drain",
                        Math.round(dealt), kills), true);
    }

    /** 打上冥界诅咒（护甲清零 + 禁疗）。 */
    public static void applyCurse(ServerLevel level, LivingEntity e) {
        e.getPersistentData().putLong("FuluBloodCurse", level.getGameTime() + CURSE_TICKS);
        AttributeInstance armor = e.getAttribute(Attributes.ARMOR);
        if (armor != null && armor.getModifier(ARMOR_BREAK_ID) == null) {
            armor.addTransientModifier(new AttributeModifier(ARMOR_BREAK_ID, "fulu_blood_curse",
                    -1.0D, AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
        BloodCurseHandler.track(e);
    }

    public static boolean isCursed(Entity e) {
        return e.getPersistentData().getLong("FuluBloodCurse") > e.level().getGameTime();
    }

    /** 摘掉护甲修饰符（诅咒到期时调）。 */
    public static void clearArmorBreak(LivingEntity e) {
        AttributeInstance armor = e.getAttribute(Attributes.ARMOR);
        if (armor != null && armor.getModifier(ARMOR_BREAK_ID) != null) {
            armor.removeModifier(ARMOR_BREAK_ID);
        }
    }
}
