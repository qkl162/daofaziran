package com.lengyang.fulu.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * 【九幽冥网 · 空间囚笼】：法则级禁锢。
 *
 * <p>设计原则：**不放置任何方块、不放任何可被攻击的碰撞体**。
 * 怪物只要踏进区域，就被代码直接剥夺行为权：
 *
 * <ol>
 *   <li><b>绝对法则</b>：{@code setNoAI(true)} + {@code setNoGravity(true)} +
 *       每刻把位置钉在 {@code 原始Y + 2.0}，{@code deltaMovement} 清零；</li>
 *   <li><b>降维破甲</b>：进入瞬间护甲 -100%（归零）；</li>
 *   <li><b>空间绞杀</b>：每秒 3 点真伤（先清 {@code invulnerableTime}），伴随黑紫裂纹；</li>
 *   <li><b>湮灭</b>：玩家在笼内释放**任何技能** → 整笼坍缩，30 点绝对伤害，
 *       被击杀者**掉落翻倍**（{@link #DROP_TAG}）。</li>
 * </ol>
 *
 * <p>它本身**不可被攻击**（{@link #hurt} 恒 false），所以怪物既打不到也拆不掉。
 */
public class WebPrisonEntity extends Entity {

    public static final float RADIUS = 5.0F;
    public static final int LIFE = 240;
    /** 悬空高度。 */
    public static final double LIFT = 2.0D;
    private static final float DOT_DAMAGE = 3.0F;
    private static final float ANNIHILATE_DAMAGE = 30.0F;

    /** 被本技能击杀 -> 掉落翻倍的标记。 */
    public static final String DROP_TAG = "FuluDoubleDrop";

    private static final UUID ARMOR_ID = UUID.fromString("9c1b4f52-7e38-4a90-b2c1-5f4d8e3a6b01");

    private static final List<WebPrisonEntity> LIVE = new ArrayList<>();

    private boolean annihilated;

    public WebPrisonEntity(EntityType<? extends WebPrisonEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    public void onAddedToWorld() {
        super.onAddedToWorld();
        if (!this.level().isClientSide) {
            LIVE.add(this);
        }
    }

    @Override
    public void onRemovedFromWorld() {
        super.onRemovedFromWorld();
        LIVE.remove(this);
    }

    /** 囚笼内有人施法 -> 全部湮灭。 */
    public static void annihilateAt(ServerLevel level, net.minecraft.world.phys.Vec3 pos) {
        for (WebPrisonEntity web : new ArrayList<>(LIVE)) {
            if (web.level() == level && web.distanceToSqr(pos) <= (double) RADIUS * RADIUS) {
                web.annihilate(level);
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        ServerLevel level = (ServerLevel) this.level();

        if (this.tickCount >= LIFE) {
            releaseAll(level);
            this.discard();
            return;
        }

        boolean second = this.tickCount % 20 == 0;
        AABB area = new AABB(this.position(), this.position()).inflate(RADIUS, 5.0D, RADIUS);

        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, area)) {
            if (!e.isAlive() || e instanceof Player) {
                continue;                            // 玩家不受影响
            }
            capture(e);

            if (second) {
                e.invulnerableTime = 0;              // 无视无敌帧
                e.hurt(level.damageSources().magic(), DOT_DAMAGE);
                if (!e.isAlive()) {
                    e.getPersistentData().putBoolean(DROP_TAG, true);
                }
            }
        }

        if (level.getGameTime() % 3 == 0) {
            for (int i = 0; i < 4; i++) {
                double a = level.random.nextDouble() * Math.PI * 2.0D;
                double r = Math.sqrt(level.random.nextDouble()) * RADIUS;
                level.sendParticles(ParticleTypes.SQUID_INK,
                        this.getX() + Math.cos(a) * r,
                        this.getY() + level.random.nextDouble() * 4.0D,
                        this.getZ() + Math.sin(a) * r,
                        1, 0.0D, 0.02D, 0.0D, 0.0D);
            }
        }
    }

    /** 捕获：剥夺行为权 + 固定悬空 + 破甲。每刻执行，没有挣脱机会。 */
    private void capture(LivingEntity e) {
        if (!e.getPersistentData().getBoolean("FuluWebLifted")) {
            e.getPersistentData().putBoolean("FuluWebLifted", true);
            e.getPersistentData().putDouble("FuluWebBaseY", e.getY());
            if (e instanceof Mob mob) {
                mob.setNoAi(true);
            }
            e.setNoGravity(true);
        }

        double base = e.getPersistentData().getDouble("FuluWebBaseY");
        e.setPos(e.getX(), base + LIFT, e.getZ());
        e.setDeltaMovement(0.0D, 0.0D, 0.0D);
        e.fallDistance = 0.0F;
        e.hurtMarked = true;

        AttributeInstance armor = e.getAttribute(Attributes.ARMOR);
        if (armor != null) {
            if (armor.getModifier(ARMOR_ID) != null) {
                armor.removeModifier(ARMOR_ID);
            }
            armor.addTransientModifier(new AttributeModifier(ARMOR_ID, "fulu_web_break",
                    -1.0D, AttributeModifier.Operation.MULTIPLY_TOTAL));
        }

        e.addEffect(new MobEffectInstance(
                com.lengyang.fulu.registry.ModMobEffects.WEBBED.get(), 40, 0,
                false, false, true));

        if (e.level() instanceof ServerLevel level && level.getGameTime() % 4 == 0) {
            level.sendParticles(ParticleTypes.PORTAL,
                    e.getX(), e.getY() + e.getBbHeight() * 0.6D, e.getZ(),
                    3, 0.3D, 0.4D, 0.3D, 0.02D);
        }
    }

    /** 湮灭：整笼向内坍缩，30 点绝对伤害。 */
    public void annihilate(ServerLevel level) {
        if (this.annihilated) {
            return;
        }
        this.annihilated = true;

        AABB area = new AABB(this.position(), this.position()).inflate(RADIUS + 1.0D, 6.0D,
                RADIUS + 1.0D);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, area)) {
            if (!e.isAlive() || e instanceof Player) {
                continue;
            }
            release(e);
            e.invulnerableTime = 0;
            e.hurt(level.damageSources().magic(), ANNIHILATE_DAMAGE);
            if (!e.isAlive()) {
                e.getPersistentData().putBoolean(DROP_TAG, true);
            }
            level.sendParticles(ParticleTypes.SQUID_INK,
                    e.getX(), e.getY() + e.getBbHeight() * 0.5D, e.getZ(),
                    30, 0.5D, 0.6D, 0.5D, 0.08D);
            level.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                    e.getX(), e.getY() + e.getBbHeight() * 0.5D, e.getZ(),
                    12, 0.4D, 0.5D, 0.4D, 0.1D);
        }

        for (int ring = 0; ring < 6; ring++) {
            double r = RADIUS * (1.0D - ring / 6.0D);
            for (int i = 0; i < 24; i++) {
                double a = Math.PI * 2.0D * i / 24.0D;
                level.sendParticles(ParticleTypes.SQUID_INK,
                        this.getX() + Math.cos(a) * r,
                        this.getY() + 1.0D + ring * 0.4D,
                        this.getZ() + Math.sin(a) * r,
                        1, 0.0D, 0.05D, 0.0D, 0.0D);
            }
        }
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                this.getX(), this.getY() + 1.0D, this.getZ(),
                3, 1.0D, 0.5D, 1.0D, 0.0D);
        level.playSound(null, this.blockPosition(), SoundEvents.WITHER_DEATH,
                SoundSource.PLAYERS, 2.0F, 0.6F);
        level.playSound(null, this.blockPosition(), SoundEvents.GENERIC_EXPLODE,
                SoundSource.PLAYERS, 2.0F, 0.5F);

        com.lengyang.fulu.FuLu.LOGGER.info("[web] annihilated at {}", this.position());
        this.discard();
    }

    private void releaseAll(ServerLevel level) {
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(this.position(), this.position()).inflate(RADIUS + 2.0D, 6.0D,
                        RADIUS + 2.0D))) {
            release(e);
        }
    }

    /** 解除禁锢。 */
    private void release(LivingEntity e) {
        if (e.getPersistentData().getBoolean("FuluWebLifted")) {
            e.getPersistentData().putBoolean("FuluWebLifted", false);
            double base = e.getPersistentData().getDouble("FuluWebBaseY");
            e.setPos(e.getX(), base, e.getZ());
            e.setNoGravity(false);
            if (e instanceof Mob mob) {
                mob.setNoAi(false);
            }
            e.fallDistance = 0.0F;
            e.hurtMarked = true;
        }
        e.removeEffect(com.lengyang.fulu.registry.ModMobEffects.WEBBED.get());
        AttributeInstance armor = e.getAttribute(Attributes.ARMOR);
        if (armor != null && armor.getModifier(ARMOR_ID) != null) {
            armor.removeModifier(ARMOR_ID);
        }
    }

    // ---------------------------------------------------------------- 不可被攻击

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }
}
