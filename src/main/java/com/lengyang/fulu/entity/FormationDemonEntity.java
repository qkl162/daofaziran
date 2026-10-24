package com.lengyang.fulu.entity;

import com.lengyang.fulu.FuLu;
import com.lengyang.fulu.registry.ModEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * 阵魔：阵法系 BOSS，按血量分三阶段，每阶段招式不同。
 *
 * <p><b>为什么不用 {@code MeleeAttackGoal}</b>：它会和"去砸阵眼"抢同一个 Navigation，
 * 每刻互相覆盖，结果哪件事都做不好（表现就是"有些阵眼它死活不去砸"）。
 * 这里改成在 {@link #tick()} 里按优先级手动决定这一刻干什么。
 *
 * <p><b>AI 优先级</b>：
 * <ol>
 *   <li>附近有刚碎的阵法 → 去吸收（回 10% 血）；</li>
 *   <li>血量 &lt; 10% → 钻地逃跑；</li>
 *   <li>附近有激活阵眼 → 去砸（二阶段起改成冲刺撞击）；</li>
 *   <li>玩家在近战范围 → 打玩家；</li>
 *   <li>否则朝玩家移动，边走边放射线。</li>
 * </ol>
 *
 * <p><b>招式表</b>：
 * <pre>
 * 一阶段  1 近战 8 点(前摇 0.5s) / 2 射线 6 点 / 3 砸阵眼 10%
 * 二阶段  4 二连近战 12 点(前摇 0.3s) / 5 三连射线 5 点(45°) / 6 冲刺砸阵眼 3×20%
 *        7 召唤 2~3 阵仆 / 8 旋转阵纹 3 点 5 秒
 * 三阶段  9 三连近战 15 点+缓慢(前摇 0.2s) / 10 五连射线 4 点(90°) / 11 撞击阵眼 50%+停顿
 *        12 召唤 4~5 阵仆 / 13 阵爆 10 点(1 秒预警) / 14 扩大阵纹 5 点 8 秒
 * 通用   15 吸收碎裂阵法 / 16 阵痕眩晕 / 17 低血逃跑 / 18 穿屏障受伤 2 点每秒
 * </pre>
 */
public class FormationDemonEntity extends Monster implements software.bernie.geckolib.animatable.GeoEntity {

    /** 屏幕上方那条宽带血条（原生 BossBar，和凋灵/末影龙同一套）。 */
    private final net.minecraft.server.level.ServerBossEvent bossBar =
            new net.minecraft.server.level.ServerBossEvent(
                    net.minecraft.network.chat.Component.translatable("entity.fulu.formation_demon"),
                    net.minecraft.world.BossEvent.BossBarColor.RED,
                    net.minecraft.world.BossEvent.BossBarOverlay.NOTCHED_10);

    /** 主控制器：用来按移速缩放走路动画速度。 */
    private software.bernie.geckolib.core.animation.AnimationController<FormationDemonEntity> mainController;

    private final software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache geckoCache =
            software.bernie.geckolib.util.GeckoLibUtil.createInstanceCache(this);

    @Override
    public software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geckoCache;
    }

    @Override
    public void registerControllers(
            software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar controllers) {
        this.mainController = new software.bernie.geckolib.core.animation.AnimationController<>(
                this, "main", 4, this::pickAnimation);
        controllers.add(this.mainController);
    }

    /** 按服务端同步的动作编号选择动画。 */
    private software.bernie.geckolib.core.object.PlayState pickAnimation(
            software.bernie.geckolib.core.animation.AnimationState<FormationDemonEntity> state) {
        // **死亡优先**：必须放在最前面。
        // 之前放在 switch 后面，结果死在攻击/冲刺动作期间时，
        // switch 已经 return 了攻击动画，死亡动作永远轮不到 —— 这就是"死亡动画不生效"。
        if (this.deathTime > 0) {
            return state.setAndContinue(software.bernie.geckolib.core.animation.RawAnimation
                    .begin().thenPlay("death"));
        }

        int a = this.getAnim();
        String one = switch (a) {
            case ANIM_MELEE -> "attack";
            case ANIM_DASH  -> "dash_grab";
            // 射线：优先用专属动画 cast_ray（你加进 json 后自动生效），没有就退回 attack
            case ANIM_RAY   -> pickClientAnim("cast_ray", "attack");
            case ANIM_EXECUTE -> "slam_impale";
            case ANIM_ROAR -> pickClientAnim("phase_roar", "idle");
            // 召唤和阵爆暂时没有专属动画，先复用 attack，避免动作卡住
            case ANIM_SUMMON -> pickClientAnim("summon", "attack");
            case ANIM_BURST  -> pickClientAnim("burst", "attack");
            case ANIM_HURT  -> "hurt";
            default -> null;
        };
        if (one != null) {
            return state.setAndContinue(
                    software.bernie.geckolib.core.animation.RawAnimation.begin().thenPlay(one));
        }
        // GeckoLib 的 isMoving() 只认 walkAnimation 的瞬时速度，
        // 导航刚起步或短距离调整时会误判成"站着"，表现就是滑步/僵着。
        // 这里改成看真实位移 + walkAnimation 累计，判定更可靠。
        boolean moving = this.getDeltaMovement().horizontalDistanceSqr() > 1.0E-4D
                || this.walkAnimation.speed() > 0.05F
                || this.walkAnimation.position() > 0.02F;
        return state.setAndContinue(
                software.bernie.geckolib.core.animation.RawAnimation.begin()
                        .thenLoop(moving ? "walk" : "idle"));
    }

    /** 客户端探测同名动画；服务端直接返回 fallback。 */
    private static String pickClientAnim(String preferred, String fallback) {
        try {
            if (!net.minecraftforge.fml.loading.FMLEnvironment.dist.isClient()) {
                return fallback;
            }
            return com.lengyang.fulu.client.model.DemonAnimations.has(preferred) ? preferred : fallback;
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    private static final float PHASE2 = 0.70F;
    private static final float PHASE3 = 0.30F;

    private static final double RAY_RANGE = 15.0D;
    private static final int RAY_CD_P1 = 200;
    private static final int RAY_CD_P2 = 200;
    private static final int RAY_CD_P3 = 160;

    private static final int MELEE_WINDUP_P1 = 10;
    private static final int MELEE_WINDUP_P2 = 6;
    private static final int MELEE_WINDUP_P3 = 4;
    private static final int MELEE_CD = 20;

    private static final double EYE_REACH = 2.6D;

    private int phase = 1;

    private int demonMark;
    private int stunTicks;

    private int eyeTimer = 200;
    private BlockPos targetEye;
    private int dashTicks = -1;
    private BlockPos dashTarget;
    private int pauseTicks;

    private int meleeWindup = -1;
    private int meleeChain;
    private int meleeChainDelay;
    private int meleeCd;
    /** 近战计数：用来交替左右手、每三次来一次双手重砸。 */
    private int meleeCount;
    /** 无敌剩余刻数（转阶段仪式给的）。 */
    private int invulTicks;
    /** 各招式的冷却（按用户规格：横扫 2 秒 / 射线 3~4 秒 / 冲刺 20 秒 / 阵爆 15 秒）。 */
    private int sweepCd;
    private int rayCd;
    private int dashCd;
    /** 玩家在 8 格内连续待了多久（阵爆触发条件）。 */
    private int burstClose;
    /** 这次冲刺是不是冲玩家（否则是冲阵眼）。 */
    private boolean dashAtPlayer;
    /** 本次冲刺已经冲了多远（超过 15 格强制停下）。 */
    private double dashTravelled;
    /** 处决倒计时（-1 表示没在处决）。 */
    private int executeTicks = -1;
    /** 被处决的玩家。 */
    private Player executeTarget;

    private int rayTimer = 40;                            // 2 秒后就来第一发，进战斗就能看到
    private int pendingRays;
    private int rayDelay;
    private double rayBaseYaw;
    /** 射线残留：发射后继续画 0.5 秒，不然只有一帧谁也看不见。 */
    private int beamTicks;
    private Vec3 beamFrom;
    private Vec3 beamDir;

    private int summonTimer = 140;                        // 7 秒就来第一次召唤
    private int burstTimer = 200;
    private int burstWindup = -1;
    private int runeTicks;
    private int runeCd = 300;


    private BlockPos absorbPos;
    private int absorbTicks;
    /** 临死召唤是否已经用过（只召唤一次）。 */
    private boolean lastStandSummoned;

    /** 无敌冷却（刻）：放完一次无敌后要等这么久才能再进无敌。 */
    private int invulCd;
    /** 是否正在"死亡反击"中：这时它是 1 血但免疫，必杀技结算后才决定复活还是真死。 */
    private boolean pendingDeath;
    /** 锁血后到放必杀技之间的延迟（刻）。 */
    private int pendingUltDelay;
    /** 无敌期间的高频攻击计时（5 刻 = 每秒 4 次）。 */
    private int invulAtkCd;

    // ---- 必杀技（血量 20% 触发，100 格锁定，必中）
    /** 是否已经放过必杀技。 */
    private boolean ultimateUsed;
    /** 必杀技剩余刻数（0 = 未进行）。 */
    private int ultimateTicks;
    /** 被锁定的玩家。 */
    private Player ultimateTarget;
    /**
     * 必杀技伤害：**无限**（10 的 7 次方）。
     *
     * <p>不用 {@code Float.MAX_VALUE} 是为了避免后续乘算（护甲/附魔/吸收）出现
     * 溢出或 NaN；1000 万在游戏里已经是"任何血量都会归零"的量级。
     */
    private static final float ULTIMATE_DAMAGE = 1.0E7F;

    private static final int ULT_TOTAL = 70;      // 3.5 秒：蓄力 2 秒 + 冲刺 1.5 秒
    private static final int ULT_WINDUP = 40;     // 前 40 刻蓄力

    // ---- 动作阶段计时（射线蓄力 / 冲刺预备 / 召唤两段）
    private int rayCharge;
    private int dashPrep;
    private int summonPrep;
    private int summonChant;
    private int summonN;

    // ================================================================
    // 动画状态：**必须同步给客户端**，否则模型不知道自己该摆什么姿势
    // ================================================================
    public static final int ANIM_IDLE = 0;
    public static final int ANIM_MELEE = 1;
    public static final int ANIM_RAY = 2;
    public static final int ANIM_DASH = 3;
    public static final int ANIM_SUMMON = 4;
    public static final int ANIM_BURST = 5;
    public static final int ANIM_HURT = 6;
    /** 处决：冲刺抓到之后插旗砸地（对应动画 slam_impale）。 */
    public static final int ANIM_EXECUTE = 7;
    /** 转阶段仰天怒吼。 */
    public static final int ANIM_ROAR = 8;

    private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> ANIM =
            net.minecraft.network.syncher.SynchedEntityData.defineId(
                    FormationDemonEntity.class,
                    net.minecraft.network.syncher.EntityDataSerializers.INT);
    private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> ANIM_ARM =
            net.minecraft.network.syncher.SynchedEntityData.defineId(
                    FormationDemonEntity.class,
                    net.minecraft.network.syncher.EntityDataSerializers.INT);
    private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> ANIM_LEN =
            net.minecraft.network.syncher.SynchedEntityData.defineId(
                    FormationDemonEntity.class,
                    net.minecraft.network.syncher.EntityDataSerializers.INT);
    private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> ANIM_START =
            net.minecraft.network.syncher.SynchedEntityData.defineId(
                    FormationDemonEntity.class,
                    net.minecraft.network.syncher.EntityDataSerializers.INT);

    public FormationDemonEntity(EntityType<? extends FormationDemonEntity> type, Level level) {
        super(type, level);
        this.xpReward = 500;
        this.setPersistenceRequired();
        this.setGlowingTag(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 300.0D)
                .add(Attributes.ARMOR, 10.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.38D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.ATTACK_DAMAGE, 8.0D)
                .add(Attributes.FOLLOW_RANGE, 40.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ANIM, ANIM_IDLE);
        this.entityData.define(ANIM_START, 0);
        this.entityData.define(ANIM_LEN, 0);
        this.entityData.define(ANIM_ARM, 0);
    }

    /** 切到某个动作（服务端调用，客户端靠同步数据读到）。 */
    public void setAnim(int anim) {
        setAnim(anim, 0);
    }

    /** 切动作，并告诉客户端"前摇有多长"（近战前摇随阶段变化）。 */
    public void setAnim(int anim, int len) {
        if (this.entityData.get(ANIM) != anim) {
            FuLu.LOGGER.info("[demon] anim -> {} (len {})", anim, len);
        }
        this.entityData.set(ANIM, anim);
        this.entityData.set(ANIM_START, this.tickCount);
        this.entityData.set(ANIM_LEN, len);
    }

    public int getAnimLen() {
        return this.entityData.get(ANIM_LEN);
    }

    /** 这一击用哪只手：0 右 / 1 左 / 2 双手（客户端模型靠它决定摆哪条手臂）。 */
    public int getAnimArm() {
        return this.entityData.get(ANIM_ARM);
    }

    public void setAnimArm(int arm) {
        this.entityData.set(ANIM_ARM, arm);
    }

    /** 当前阶段（1/2/3），给客户端血条上色用。 */
    public int getPhase() {
        return this.phase;
    }

    /** 当前动作编号（客户端读）。 */
    public int getAnim() {
        return this.entityData.get(ANIM);
    }

    /** 当前动作从哪一刻开始（客户端读，用来算动作进度）。 */
    public int getAnimStart() {
        return this.entityData.get(ANIM_START);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("FuluDemonPhase", this.phase);
        tag.putInt("FuluDemonMark", this.demonMark);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.phase = tag.getInt("FuluDemonPhase");
        this.demonMark = tag.getInt("FuluDemonMark");
    }

    // ================================================================ 主循环
    @Override
    public void tick() {
        super.tick();

        // 双端都要跑：步伐匹配（走路动画速度跟着真实移速）
        this.updateWalkSpeed();

        if (this.level().isClientSide) {
            return;
        }
        ServerLevel level = (ServerLevel) this.level();

        if (this.invulCd > 0) {
            this.invulCd--;
        }

        // ---- 无敌倒计时（必须在服务端每刻递减，否则就变成永久无敌）
        if (this.invulTicks > 0) {
            this.invulTicks--;

            // **无敌期间一秒至少 4 次攻击**：每 5 刻打一下，命中即回 10% 上限血
            if (--this.invulAtkCd <= 0) {
                this.invulAtkCd = 5;
                LivingEntity vt = this.getTarget();
                if (vt != null && vt.isAlive()) {
                    this.setAnim(ANIM_MELEE);
                    if (vt instanceof Player vp) {
                        this.damagePlayer(level, vp, 5.0F);
                    } else {
                        vt.hurt(this.damageSources().mobAttack(this), 5.0F);
                    }
                    this.healFromAttack();
                    level.sendParticles(ParticleTypes.CRIT,
                            vt.getX(), vt.getY() + vt.getBbHeight() * 0.6D, vt.getZ(),
                            6, 0.3D, 0.3D, 0.3D, 0.2D);
                }
            }
            if (this.invulTicks == 0) {
                this.announce(level, "msg.fulu.demon.invul_end");
                level.sendParticles(ParticleTypes.WITCH,
                        this.getX(), this.getY() + getBbHeight() * 0.6D, this.getZ(),
                        20, 1.0D, 1.2D, 1.0D, 0.1D);
            }
        }

        // ---- 动画到期回 idle
        // 这一步是**必须**的：GeckoLib 的 setAndContinue 播完一次性动画后会**停在最后一帧**，
        // 如果这里不把动作编号改回 IDLE，阵魔会永远定格在挥旗/砸地的收尾姿势上。
        // 时长取自 animation.json 的 animation_length（×20 刻）：
        //   attack 1.45s=29 / dash_grab 1.61s=32 / slam_impale 2.6s=52 / hurt 0.9s=18
        if (this.getAnim() != ANIM_IDLE) {
            int total = switch (this.getAnim()) {
                case ANIM_MELEE -> 29;
                case ANIM_RAY -> 29;
                case ANIM_DASH -> 32;
                case ANIM_SUMMON -> 52;
                case ANIM_BURST -> 52;
                case ANIM_HURT -> 18;
                case ANIM_EXECUTE -> 52;
                case ANIM_ROAR -> 40;
                default -> 0;
            };
            if (total > 0 && this.tickCount - this.getAnimStart() >= total) {
                this.setAnim(ANIM_IDLE);
            }
        }

        float ratio = this.getHealth() / this.getMaxHealth();
        int newPhase = ratio <= PHASE3 ? 3 : (ratio <= PHASE2 ? 2 : 1);
        if (newPhase != this.phase) {
            this.phase = newPhase;
            this.onPhaseChange(level, newPhase);
        }

        // 目标若是创造模式玩家，立刻放弃（不仇恨创造模式）
        if (this.getTarget() instanceof Player tp && tp.isCreative()) {
            this.setTarget(null);
        }

        // ---- Boss 血条：血量、颜色、可见范围
        this.bossBar.setProgress(Math.max(0.0F, Math.min(1.0F, this.getHealth() / this.getMaxHealth())));
        this.bossBar.setColor(this.phase == 3
                ? net.minecraft.world.BossEvent.BossBarColor.PURPLE
                : (this.phase == 2
                        ? net.minecraft.world.BossEvent.BossBarColor.RED
                        : net.minecraft.world.BossEvent.BossBarColor.WHITE));
        // 原版 BossBar 不再使用：改由客户端 DemonBossBarOverlay 画自定义贴图。
        // 这里只保留 visible=false，避免两条血条同时出现。
        this.bossBar.setVisible(false);

        // ---- 转身平滑 + 头部锁定
        // 之前是瞬间朝向玩家，看起来像"木偶被掰过去"。这里每刻只转一点，
        // 同时让头始终盯住目标（身体在转、头先到位），动作就活了。
        LivingEntity lookTarget = this.getTarget();
        if (lookTarget != null && lookTarget.isAlive()) {
            this.getLookControl().setLookAt(lookTarget, 30.0F, 30.0F);
            double dx = lookTarget.getX() - this.getX();
            double dz = lookTarget.getZ() - this.getZ();
            float want = (float) (Math.toDegrees(Math.atan2(dz, dx))) - 90.0F;
            float next = Mth.approachDegrees(this.getYRot(), want, 9.0F);
            this.setYRot(next);
            this.setYHeadRot(next);
            this.yBodyRot = next;
        }

        // ---- 锁血后的延迟：先让玩家看到它掉到 1 血，再放必杀技
        if (this.pendingDeath && this.ultimateTicks <= 0 && this.pendingUltDelay > 0) {
            if (--this.pendingUltDelay <= 0) {
                if (!this.startUltimate(level)) {
                    // 附近没人：直接真死，不复活
                    this.pendingDeath = false;
                    this.kill();
                    return;
                }
            }
            return;
        }

        // ---- 必杀技：现在由「致命伤」触发（见 hurt 里的拦截），不再看 20% 血
        if (this.ultimateTicks > 0) {
            this.tickUltimate(level);
            return;
        }

        // ---- 16 眩晕
        if (this.stunTicks > 0) {
            this.stunTicks--;
            this.getNavigation().stop();
            this.setDeltaMovement(0.0D, this.getDeltaMovement().y, 0.0D);
            if (this.tickCount % 8 == 0) {
                level.sendParticles(ParticleTypes.END_ROD,
                        this.getX(), this.getY() + getBbHeight() + 0.8D, this.getZ(),
                        5, 0.45D, 0.15D, 0.45D, 0.02D);
            }
            if (this.stunTicks == 0) {
                this.demonMark = 0;
            }
            return;
        }

        if (this.tickCount % 100 == 0 && this.demonMark > 0) {
            this.demonMark--;
        }

        // ---- 处决倒计时：到点就结算（对齐 slam_impale 的插旗帧 1.5 秒）
        if (this.executeTicks >= 0) {
            this.executeTicks--;
            this.getNavigation().stop();
            this.setDeltaMovement(0.0D, this.getDeltaMovement().y, 0.0D);
            if (this.executeTicks == 0) {
                this.applyExecution(level);
            }
            if (this.executeTicks <= 0) {
                this.executeTicks = -1;
                this.executeTarget = null;
                this.pauseTicks = 12;                      // 处决后硬直 0.6 秒（原来 2 秒，太像发呆）
            }
            return;
        }

        if (this.pauseTicks > 0) {
            this.pauseTicks--;
            // 硬直期间**仍然移动**：慢慢压上去，不再原地不动
            LivingEntity pt = this.getTarget();
            if (pt != null && pt.isAlive()) {
                this.getLookControl().setLookAt(pt, 30.0F, 30.0F);
                if (this.distanceTo(pt) > 2.6D) {
                    this.getNavigation().moveTo(pt, 0.9D);
                }
            }
            if (this.tickCount % 4 == 0) {
                level.sendParticles(ParticleTypes.SMOKE,
                        this.getX(), this.getY() + getBbHeight() * 0.5D, this.getZ(),
                        4, 0.4D, 0.4D, 0.4D, 0.0D);
            }
            return;
        }

        // ---- 17 逃跑机制已删除：阵魔残血不会钻地消失，会一直打到死

        // ---- 15 吸收
        if (this.tickAbsorb(level)) {
            return;
        }

        // ---- 6/11 冲刺预备（下蹲、脚底黑粒子）
        if (this.dashPrep > 0) {
            this.dashPrep--;
            this.getNavigation().stop();
            for (int i = 0; i < 6; i++) {
                double a = Math.PI * 2.0D * i / 6.0D;
                level.sendParticles(ParticleTypes.SQUID_INK,
                        this.getX() + Math.cos(a) * 1.1D, this.getY() + 0.1D,
                        this.getZ() + Math.sin(a) * 1.1D, 1, 0.0D, 0.02D, 0.0D, 0.0D);
            }
            if (this.dashPrep == 0) {
                this.dashTicks = 20;
            }
            return;
        }

        // ---- 6/11 冲刺
        if (this.dashTicks > 0) {
            this.tickDash(level);
            return;
        }

        // ---- 主动索敌：20 格内的玩家**不管看不看得见**都算目标
        this.acquireNearbyPlayer(level);

        // ---- 18 屏障伤害
        this.tickBarrierDamage(level);

        // ---- 3/6/11 阵眼：只更新"目标 + 到了就砸"，**不再独占整个 tick**
        this.updateEyeTarget(level);

        // ---- 下面这些**每刻都要跑**，否则技能会互相饿死
        this.tickRunes(level);
        if (this.phase == 3) {
            this.tickBurst(level);
        }
        this.tickSummon(level);
        this.tickMelee(level);
        this.tickRay(level);
        this.tickDashTrigger(level);
        this.moveTowardTarget(level);

        if (this.tickCount % 4 == 0) {
            level.sendParticles(ParticleTypes.SMOKE,
                    this.getX(), this.getY() + 1.2D, this.getZ(), 3, 0.5D, 0.6D, 0.5D, 0.01D);
            level.sendParticles(ParticleTypes.PORTAL,
                    this.getX(), this.getY() + 0.1D, this.getZ(), 4, 0.9D, 0.05D, 0.9D, 0.0D);
        }
        if (this.tickCount % 20 == 0) {
            for (int i = 0; i < 10; i++) {
                level.sendParticles(ParticleTypes.END_ROD,
                        this.getX(), this.getY() + getBbHeight() + 0.5D + i * 0.9D, this.getZ(),
                        1, 0.05D, 0.0D, 0.05D, 0.0D);
            }
        }
    }

    /**
     * 主动索敌：**20 格内的玩家，不管视线、不管朝向**，都直接锁定。
     *
     * <p>为什么需要这个：原来的索敌靠 {@code NearestAttackableTargetGoal}，
     * 它带 {@code mustSee = true} —— 看不见你就不把你当目标，于是"远处的玩家"
     * 永远不会触发冲刺。BOSS 不该因为一堵墙就变瞎。
     *
     * <p>注意：创造模式的玩家在 {@code Player.hurt} 里就被原版拦掉了，
     * 所以会看到**动画照播、伤害无效**；测试请用生存模式。
     */
    private void acquireNearbyPlayer(ServerLevel level) {
        LivingEntity cur = this.getTarget();
        if (cur != null && cur.isAlive() && cur.distanceToSqr(this) <= 40.0D * 40.0D) {
            return;
        }
        Player best = null;
        double bestD = 20.0D * 20.0D;
        for (Player p : level.getEntitiesOfClass(Player.class,
                this.getBoundingBox().inflate(20.0D))) {
            if (p.isSpectator() || !p.isAlive() || p.isCreative()) {
                continue;      // **不仇恨创造模式玩家**
            }
            double d = p.distanceToSqr(this);
            if (d < bestD) {
                bestD = d;
                best = p;
            }
        }
        if (best != null) {
            this.setTarget(best);
        }
    }

    // ================================================================ 必杀技
    /** 100 格内最近的玩家（**不看视线、不管遮挡**）。 */
    private Player nearestPlayer(ServerLevel level, double range) {
        Player best = null;
        double bestD = range * range;
        for (Player p : level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(range))) {
            if (!p.isAlive() || p.isSpectator() || p.isCreative()) {
                continue;      // **不仇恨创造模式玩家**（必杀技锁定同样跳过）
            }
            double d = p.distanceToSqr(this);
            if (d < bestD) {
                bestD = d;
                best = p;
            }
        }
        return best;
    }

    /**
     * 开启必杀技：**100 格内锁定玩家，必中**。
     *
     * <p>动作使用 {@code dash_grab}（冲刺抓取）接 {@code slam_impale}（插旗处决）。
     */
    private boolean startUltimate(ServerLevel level) {
        Player target = this.nearestPlayer(level, 100.0D);
        if (target == null) {
            return false;                            // 附近没人：不触发，让它正常死
        }
        this.ultimateUsed = true;
        this.ultimateTarget = target;
        this.ultimateTicks = ULT_TOTAL;
        this.getNavigation().stop();
        this.setTarget(target);
        this.setAnim(ANIM_DASH);                     // dash_grab
        this.announce(level, "msg.fulu.demon.ultimate");
        level.playSound(null, this.blockPosition(), SoundEvents.ELDER_GUARDIAN_CURSE,
                SoundSource.HOSTILE, 2.0F, 0.6F);
        for (int i = 0; i < 60; i++) {
            double a = Math.PI * 2.0D * i / 60.0D;
            level.sendParticles(ParticleTypes.SQUID_INK,
                    this.getX() + Math.cos(a) * 3.0D, this.getY() + 0.2D,
                    this.getZ() + Math.sin(a) * 3.0D, 1, 0.0D, 0.05D, 0.0D, 0.0D);
        }
        return true;
    }

    /** 必杀技每一刻：蓄力 -> 强行突进 -> 插旗处决。 */
    private void tickUltimate(ServerLevel level) {
        this.ultimateTicks--;
        Player target = this.ultimateTarget;
        if (target == null || !target.isAlive()) {
            target = this.nearestPlayer(level, 100.0D);
            this.ultimateTarget = target;
            if (target == null) {
                this.ultimateTicks = 0;
                return;
            }
        }

        Vec3 to = target.position().subtract(this.position());
        double dist = to.length();

        if (this.ultimateTicks > ULT_TOTAL - ULT_WINDUP) {
            // ---- 蓄力：站定、身体压低、黑红光环收缩
            this.getNavigation().stop();
            double f = (double) (ULT_TOTAL - this.ultimateTicks) / ULT_WINDUP;
            double r = 4.0D - 3.0D * f;
            for (int i = 0; i < 24; i++) {
                double a = Math.PI * 2.0D * i / 24.0D + this.tickCount * 0.3D;
                level.sendParticles(ParticleTypes.SQUID_INK,
                        this.getX() + Math.cos(a) * r, this.getY() + 0.3D + f,
                        this.getZ() + Math.sin(a) * r, 1, 0.0D, 0.02D, 0.0D, 0.0D);
            }
            return;
        }

        // ---- 突进：无视阻挡（必中），每刻最多 1.8 格
        if (dist > 2.2D) {
            Vec3 step = to.normalize().scale(Math.min(1.8D, dist));
            this.teleportTo(this.getX() + step.x, this.getY(), this.getZ() + step.z);
            level.sendParticles(ParticleTypes.LARGE_SMOKE,
                    this.getX(), this.getY() + 1.0D, this.getZ(), 5, 0.4D, 0.5D, 0.4D, 0.01D);
            level.sendParticles(ParticleTypes.CRIMSON_SPORE,
                    this.getX(), this.getY() + 1.6D, this.getZ(), 4, 0.3D, 0.3D, 0.3D, 0.0D);
        }

        if (dist <= 2.6D || this.ultimateTicks <= 1) {
            // ---- 抓到了（或者时间到，强行贴脸）：插旗处决
            if (dist > 2.6D) {
                this.teleportTo(target.getX(), target.getY(), target.getZ());
            }
            this.ultimateTicks = 0;
            this.ultimateTarget = null;
            this.beginExecution(level, target);      // slam_impale
        }
    }

    /**
     * 步伐匹配：走路动画的播放速度跟着实际移速缩放。
     *
     * <p>否则慢走时腿摆得太快、快冲时又像在滑冰 —— 这是"僵硬"最直观的来源之一。
     * 用反射调用是为了不依赖 GeckoLib 的具体参数类型（float / double 在不同版本不同）。
     */
    private void updateWalkSpeed() {
        if (this.mainController == null) {
            return;
        }
        double speed = this.getDeltaMovement().horizontalDistance();
        double scale = Mth.clamp(speed * 9.0D, 0.7D, 2.4D);
        try {
            if (SPEED_SETTER == null) {
                SPEED_SETTER = this.mainController.getClass()
                        .getMethod("setAnimationSpeed", double.class);
            }
            SPEED_SETTER.invoke(this.mainController, scale);
        } catch (Throwable ignored) {
            // 拿不到就保持默认速度，不影响其它逻辑
        }
    }

    private static java.lang.reflect.Method SPEED_SETTER;

    /**
     * 对玩家造成伤害，**创造模式也吃伤害**。
     *
     * <p>原版 {@code Player.hurt} 会因为 abilities.invulnerable 直接返回 false，
     * 所以对创造玩家改用带 {@code BYPASSES_INVULNERABILITY} 标签的伤害源。
     * 这样在创造模式下也能测伤害和必杀技。
     */
    private void damagePlayer(ServerLevel level, Player p, float dmg) {
        if (p == null || !p.isAlive()) {
            return;
        }
        if (p.isCreative()) {
            p.hurt(level.damageSources().fellOutOfWorld(), dmg);
        } else {
            p.hurt(this.damageSources().indirectMagic(this, this), dmg);
        }
    }

    /**
     * 必杀技专用伤害源：`fulu:demon_ultimate`。
     *
     * <p>这个伤害类型在数据包里挂了两个标签：
     * <ul>
     *   <li>{@code bypasses_armor} —— 无视护甲/韧性，无尽套装那种高护甲也挡不住；</li>
     *   <li>{@code bypasses_effects} —— 无视抗性提升、伤害吸收。</li>
     * </ul>
     * 但**故意不加** {@code bypasses_invulnerability} ——
     * 原版的 {@code checkTotemDeathProtection} 遇到那个标签会直接跳过，
     * 那样不死图腾就失效了；用户要求图腾和替身阵**仍然能挡**。
     */
    private net.minecraft.world.damagesource.DamageSource ultimateDamage() {
        net.minecraft.server.level.ServerLevel level = (net.minecraft.server.level.ServerLevel) this.level();
        return new net.minecraft.world.damagesource.DamageSource(
                level.registryAccess()
                        .registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE)
                        .getHolderOrThrow(net.minecraft.resources.ResourceKey.create(
                                net.minecraft.core.registries.Registries.DAMAGE_TYPE,
                                new net.minecraft.resources.ResourceLocation("fulu", "demon_ultimate"))),
                this);
    }

    /**
     * **绕过事件层的强制处决**。
     *
     * <p>实测：无尽套装那类模组是在 {@code LivingAttackEvent} 里直接取消伤害的，
     * 所以无论伤害源带什么标签（连 {@code genericKill} 都不行）都打不动它。
     * 唯一可靠的办法是绕开 {@code hurt()} 整条链路：
     * <ol>
     *   <li>直接把血量写成 0（{@code setHealth} 不经过任何事件）；</li>
     *   <li>反射调用 {@code LivingEntity#die(DamageSource)} 走正常的死亡结算
     *       （掉落、死亡信息、重生界面）。</li>
     * </ol>
     *
     * <p>只对**没有被保命物品救下**的玩家使用，所以不会抢走不死图腾/替身阵的作用。
     */
    private void forceKill(ServerLevel level, Player p) {
        try {
            p.setHealth(0.0F);
            java.lang.reflect.Method die = net.minecraft.world.entity.LivingEntity.class
                    .getDeclaredMethod("die", net.minecraft.world.damagesource.DamageSource.class);
            die.setAccessible(true);
            die.invoke(p, this.ultimateDamage());
            FuLu.LOGGER.info("[demon] forceKill(die) -> alive={} hp={}", p.isAlive(), p.getHealth());
        } catch (Throwable t1) {
            FuLu.LOGGER.warn("[demon] forceKill die() failed: {}", t1.toString());
            try {
                p.kill();
            } catch (Throwable t2) {
                FuLu.LOGGER.warn("[demon] forceKill kill() failed: {}", t2.toString());
            }
            FuLu.LOGGER.info("[demon] forceKill(kill) -> alive={} hp={}", p.isAlive(), p.getHealth());
        }
    }

    /**
     * 主动消耗一次**不死图腾**。
     *
     * <p>无尽套装那类模组会把伤害整个取消，图腾根本收不到"致命伤害"，
     * 所以在必杀技里我们手动替它完成：取走 1 个图腾，给足图腾该给的效果。
     */
    /** 哪些物品算"保命物品"：原版不死图腾 + 无尽贪婪的无尽图腾。 */
    private static java.util.List<net.minecraft.world.item.Item> protectionItems() {
        java.util.List<net.minecraft.world.item.Item> list = new java.util.ArrayList<>();
        list.add(net.minecraft.world.item.Items.TOTEM_OF_UNDYING);
        try {
            net.minecraft.world.item.Item inf = net.minecraftforge.registries.ForgeRegistries.ITEMS
                    .getValue(new net.minecraft.resources.ResourceLocation("avaritia", "infinity_totem"));
            if (inf != null) {
                list.add(inf);                 // 无尽图腾（没装无尽贪婪时为 null，忽略）
            }
        } catch (Throwable ignored) {
            // 注册表还没就绪之类的极端情况：当作只有原版图腾
        }
        return list;
    }

    private static boolean isProtectionItem(net.minecraft.world.item.ItemStack st) {
        for (net.minecraft.world.item.Item it : protectionItems()) {
            if (st.is(it)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 消耗一个保命物品。
     *
     * <p>原版不死图腾是"一次性摧毁"，而无尽图腾有耐久（耗尽时会提示"无尽图腾已耗尽"）。
     * 所以对可损坏物品扣 1 点耐久，耐久见底再移除；对普通物品直接减 1。
     */
    private static void consumeOne(net.minecraft.world.item.ItemStack st) {
        if (st.isDamageableItem()) {
            st.setDamageValue(st.getDamageValue() + 1);
            if (st.getDamageValue() >= st.getMaxDamage()) {
                st.shrink(1);
            }
        } else {
            st.shrink(1);
        }
    }

    private boolean consumeTotem(Player p) {
        boolean took = false;
        for (int i = 0; i < p.getInventory().items.size(); i++) {
            net.minecraft.world.item.ItemStack st = p.getInventory().items.get(i);
            if (isProtectionItem(st)) {
                consumeOne(st);
                FuLu.LOGGER.info("[demon] ultimate: consumed {} (inventory)", st.getItem());
                took = true;
                break;
            }
        }
        if (!took) {
            net.minecraft.world.item.ItemStack off = p.getInventory().offhand.get(0);
            if (isProtectionItem(off)) {
                consumeOne(off);
                FuLu.LOGGER.info("[demon] ultimate: consumed {} (offhand)", off.getItem());
                took = true;
            }
        }
        // 护甲槽（有些整合包会把图腾放头盔位）
        if (!took) {
            for (net.minecraft.world.item.ItemStack st : p.getInventory().armor) {
                if (isProtectionItem(st)) {
                    consumeOne(st);
                    FuLu.LOGGER.info("[demon] ultimate: consumed {} (armor slot)", st.getItem());
                    took = true;
                    break;
                }
            }
        }
        // 饰品栏（Curios）：背包里找不到时再扫一遍饰品槽。
        // 用反射是为了不把 Curios 变成硬依赖 —— 没装的时候这里直接返回 false。
        if (!took) {
            took = this.consumeTotemFromCurios(p);
        }
        if (!took) {
            return false;
        }
        p.setHealth(1.0F);
        p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 1));
        p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));
        p.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800, 0));
        ((ServerLevel) p.level()).broadcastEntityEvent(p, (byte) 35);   // 原版图腾特效
        FuLu.LOGGER.info("[demon] ultimate: consumed a Totem of Undying");
        return true;
    }

    /**
     * 从 **Curios 饰品栏** 里扣除一个不死图腾（反射调用，不硬依赖 Curios）。
     *
     * <p>饰品图腾不在这条路径上时，玩家会被误判成"没有保命物品"然后被强杀 ——
     * 这就是"饰品图腾也死了"的原因。
     */
    private boolean consumeTotemFromCurios(Player p) {
        try {
            Class<?> api = Class.forName("top.theillusivec4.curios.api.CuriosApi");
            java.lang.reflect.Method getInv = api.getMethod("getCuriosInventory",
                    net.minecraft.world.entity.LivingEntity.class);
            Object lazy = getInv.invoke(null, p);
            Object resolved = lazy.getClass().getMethod("resolve").invoke(lazy);
            Object handler = ((java.util.Optional<?>) resolved).orElse(null);
            if (handler == null) {
                return false;
            }
            java.util.Map<?, ?> curios = (java.util.Map<?, ?>)
                    handler.getClass().getMethod("getCurios").invoke(handler);
            for (Object stackHandler : curios.values()) {
                Object itemHandler = stackHandler.getClass().getMethod("getStacks").invoke(stackHandler);
                java.lang.reflect.Method getSlots = itemHandler.getClass().getMethod("getSlots");
                java.lang.reflect.Method getStack = itemHandler.getClass().getMethod("getStackInSlot", int.class);
                java.lang.reflect.Method extract = itemHandler.getClass()
                        .getMethod("extractItem", int.class, int.class, boolean.class);
                int slots = (Integer) getSlots.invoke(itemHandler);
                for (int i = 0; i < slots; i++) {
                    Object stack = getStack.invoke(itemHandler, i);
                    if (stack instanceof net.minecraft.world.item.ItemStack st
                            && isProtectionItem(st)) {
                        // 耐久类物品（无尽图腾）优先扣耐久，别直接抽走整件
                        if (st.isDamageableItem()) {
                            st.setDamageValue(st.getDamageValue() + 1);
                            if (st.getDamageValue() >= st.getMaxDamage()) {
                                extract.invoke(itemHandler, i, 1, false);
                            }
                        } else {
                            extract.invoke(itemHandler, i, 1, false);
                        }
                        FuLu.LOGGER.info("[demon] ultimate: consumed {} (curios)", st.getItem());
                        return true;
                    }
                }
            }
        } catch (Throwable ignored) {
            // 没装 Curios 或版本 API 不同：当作没有饰品图腾
        }
        return false;
    }

    /** 必杀技对玩家结算：走专用伤害源（无视护甲），创造模式也照打。 */
    private void damagePlayerUltimate(ServerLevel level, Player p, float dmg) {
        if (p == null || !p.isAlive()) {
            return;
        }
        if (p.isCreative()) {
            p.hurt(level.damageSources().fellOutOfWorld(), dmg);
        } else {
            p.hurt(this.ultimateDamage(), dmg);
        }
    }

    /** 无敌期间每次攻击回复 **7%** 上限血量（用户要求：3% -> 7%）。 */
    private void healFromAttack() {
        if (this.invulTicks > 0) {
            this.heal(this.getMaxHealth() * 0.07F);
        }
    }

    /**
     * 必杀技结算：决定阵魔**复活**还是**真死**。
     *
     * @param playerDied 玩家是否被这一套必杀技打死
     */
    private void resolveDeathUltimate(ServerLevel level, boolean playerDied) {
        boolean pending = this.pendingDeath;
        this.pendingDeath = false;
        this.ultimateUsed = true;
        if (!pending) {
            return;                       // 不是"死亡反击"流程，无需处理
        }
        if (playerDied) {
            // 玩家被必杀技杀死 -> 阵魔复活，回 30% 血
            this.setHealth(Math.max(1.0F, this.getMaxHealth() * 0.30F));
            this.invulTicks = 0;
            this.invulCd = 200;
            this.setAnim(ANIM_ROAR);
            this.announce(level, "msg.fulu.demon.revive");
            level.playSound(null, this.blockPosition(), SoundEvents.WITHER_SPAWN,
                    SoundSource.HOSTILE, 1.8F, 0.7F);
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    this.getX(), this.getY() + 1.0D, this.getZ(), 60, 1.2D, 1.4D, 1.2D, 0.08D);
        } else {
            // 玩家没死 -> 阵魔真正死亡
            this.announce(level, "msg.fulu.demon.ultimate_failed");
            this.kill();
        }
    }

    private void announce(ServerLevel level, String key) {
        for (Player p : level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(48.0D))) {
            p.displayClientMessage(Component.translatable(key), true);
        }
    }

    private void moveTowardTarget(ServerLevel level) {
        LivingEntity target = this.getTarget();
        boolean playerClose = target != null && target.isAlive() && this.distanceTo(target) <= 4.0D;

        // 玩家贴脸 → 先打人；否则有阵眼目标就去砸阵眼
        if (this.targetEye != null && !playerClose) {
            this.getNavigation().moveTo(this.targetEye.getX() + 0.5D, this.targetEye.getY(),
                    this.targetEye.getZ() + 0.5D, 1.2D);
            return;
        }
        if (target == null || !target.isAlive()) {
            return;
        }
        this.getLookControl().setLookAt(target, 30.0F, 30.0F);
        if (this.distanceTo(target) > 3.0D) {
            this.getNavigation().moveTo(target, 1.0D);
        } else {
            // **贴身时绕圈**而不是原地站桩。
            // 原来这里调 stop()，碰上横扫冷却 2 秒 / 射线冷却 3.5 秒，
            // 它就会站在原地一动不动 —— 这就是"发呆"。
            double ang = this.tickCount * 0.08D + this.getId() * 0.7D;
            double wx = target.getX() + Math.cos(ang) * 2.3D;
            double wz = target.getZ() + Math.sin(ang) * 2.3D;
            this.getNavigation().moveTo(wx, target.getY(), wz, 1.7D);
        }
    }

    /**
     * 冲刺抓取的触发：玩家在 **10~20 格** 且冷却好了（20 秒）就冲。
     *
     * <p>这是最有压迫感的技能：0.5 秒前摇（旗子后拉、尖端冒紫光、尖锐嗡鸣），
     * 然后以 1.5 格/刻 直线冲出去，最多 15 格。冲空就停 3 秒（虚弱窗口）。
     */
    private void tickDashTrigger(ServerLevel level) {
        if (this.dashCd > 0) {
            this.dashCd--;
        }
        LivingEntity target = this.getTarget();
        if (target == null || this.dashCd > 0) {
            return;
        }
        if (this.dashPrep > 0 || this.dashTicks > 0) {
            return;
        }
        double d = this.distanceTo(target);
        if (d >= 8.0D && d <= 20.0D) {
            this.dashCd = 400;                             // 20 秒
            this.dashAtPlayer = true;
            this.startDash(level, target.blockPosition());
        }
    }

    // ================================================================ 挥旗横扫（近战普攻）
    /**
     * 挥旗横扫：玩家进到 3.5 格就停下扭腰，0.3 秒后向前方 **120° 扇形、半径 4 格** 扇出一击。
     *
     * <p>伤害 8~12，**击退极强**（5 格）。背后是死角 —— 看到扭腰就往它背后绕。
     */
    private void tickMelee(ServerLevel level) {
        if (this.sweepCd > 0) {
            this.sweepCd--;
        }
        // 贴身时把冷却压到 0.5 秒以内，避免"冷却期站着不动"
        LivingEntity near = this.getTarget();
        if (near != null && near.isAlive() && this.distanceTo(near) <= 3.6D && this.sweepCd > 10) {
            this.sweepCd = 10;
        }
        LivingEntity target = this.getTarget();

        // 前摇中：扭腰、旗子拉到身后、冒红粒子
        if (this.meleeWindup > 0) {
            this.meleeWindup--;
            // 前摇期间继续压上去：出招不是"站住"，而是"边走边举旗"
            LivingEntity chasing = this.getTarget();
            if (chasing != null && this.distanceTo(chasing) > 2.4D) {
                this.getNavigation().moveTo(chasing, 1.0D);
            }
            level.sendParticles(ParticleTypes.CRIMSON_SPORE,
                    this.getX(), this.getY() + getBbHeight() * 0.9D, this.getZ(),
                    3, 0.45D, 0.3D, 0.45D, 0.0D);
            if (this.meleeWindup == 0) {
                this.resolveSweep(level);
            }
            return;
        }

        if (target == null || this.sweepCd > 0) {
            return;
        }
        if (this.distanceTo(target) <= 3.5D) {
            // attack 动画抬手到 0.34s（7 刻）结束，0.66s（13 刻）是打击帧
            this.meleeWindup = 13;                         // 对齐打击帧
            this.meleeCount++;
            this.setAnimArm(this.meleeCount % 2);
            this.setAnim(ANIM_MELEE, 13);
            level.playSound(null, this.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                    SoundSource.HOSTILE, 1.3F, 0.6F);
        }
    }

    /** 扇形判定：120° / 半径 4 格，全部击飞 5 格。 */
    private void resolveSweep(ServerLevel level) {
        Vec3 fwd = this.getLookAngle();
        Vec3 flat = new Vec3(fwd.x, 0.0D, fwd.z);
        if (flat.length() < 0.01D) {
            flat = new Vec3(0.0D, 0.0D, 1.0D);
        }
        flat = flat.normalize();
        float base = this.phase == 3 ? 12.0F : (this.phase == 2 ? 10.0F : 8.0F);

        // 扇形特效：一条弧线扫过去
        for (int i = 0; i <= 18; i++) {
            double ang = Math.toRadians(-60.0D + 120.0D * i / 18.0D);
            Vec3 dir = new Vec3(
                    flat.x * Math.cos(ang) - flat.z * Math.sin(ang), 0.0D,
                    flat.x * Math.sin(ang) + flat.z * Math.cos(ang));
            for (int r = 1; r <= 4; r++) {
                level.sendParticles(ParticleTypes.SWEEP_ATTACK,
                        this.getX() + dir.x * r, this.getY() + 1.2D, this.getZ() + dir.z * r,
                        1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
        }
        level.playSound(null, this.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.HOSTILE, 1.6F, 0.5F);
        this.healFromAttack();          // 无敌期间：攻击回血

        for (Player p : level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(4.5D))) {
            Vec3 to = p.position().subtract(this.position());
            double dist = Math.hypot(to.x, to.z);
            if (dist > 4.0D) {
                continue;
            }
            Vec3 flatTo = new Vec3(to.x, 0.0D, to.z).normalize();
            if (flatTo.dot(flat) < Math.cos(Math.toRadians(60.0D))) {
                continue;                                  // 不在扇形里（背后是死角）
            }
            float dmg = base * (0.8F + this.random.nextFloat() * 0.4F);
            this.damagePlayer(level, p, dmg);
            // 夸张的击退：5 格
            p.push(flatTo.x * 2.6D, 0.55D, flatTo.z * 2.6D);
            p.hurtMarked = true;
            level.sendParticles(ParticleTypes.CRIT,
                    p.getX(), p.getY() + 1.0D, p.getZ(), 12, 0.4D, 0.4D, 0.4D, 0.2D);
        }
        this.sweepCd = 40;                                 // 2 秒
    }

    // ================================================================ 阵纹射线（远程）
    /**
     * 阵纹射线：玩家在 **4~15 格** 时抬手蓄力 0.4 秒，然后射出一道 **1 格宽、15 格长** 的直线。
     *
     * <p>命中 6 点魔法伤害 + 2 秒缓慢。二阶段三连扇形、三阶段五连散射。
     * 特效刻意做得很重：亮白光柱 + 紫红外晕，并且**残留 0.75 秒**，白天也一眼看得见。
     */
    private void tickRay(ServerLevel level) {
        if (this.rayCd > 0) {
            this.rayCd--;
        }

        // 残留光柱
        if (this.beamTicks > 0) {
            this.beamTicks--;
            this.drawBeam(level, this.beamFrom, this.beamDir,
                    0.4F + 0.6F * (this.beamTicks / 15.0F));
        }

        // 蓄力中
        if (this.rayCharge > 0) {
            this.rayCharge--;
            // 同样不在蓄力时站定，保持移动的连贯感
            float grow = (13 - this.rayCharge) / 13.0F;
            Vec3 look = this.getLookAngle();
            Vec3 hand = this.position().add(0.0D, this.getBbHeight() * 0.78D, 0.0D)
                    .add(look.x * 1.2D, look.y * 0.6D + 0.2D, look.z * 1.2D);
            int cnt = 3 + (int) (grow * 14.0F);
            level.sendParticles(ParticleTypes.END_ROD, hand.x, hand.y, hand.z,
                    cnt, 0.06D + 0.28D * grow, 0.06D + 0.28D * grow, 0.06D + 0.28D * grow, 0.02D);
            level.sendParticles(ParticleTypes.DRAGON_BREATH, hand.x, hand.y, hand.z,
                    cnt, 0.1D + 0.4D * grow, 0.1D + 0.4D * grow, 0.1D + 0.4D * grow, 0.0D);
            if (this.rayCharge == 0) {
                this.fireRayVolley(level);
            }
            return;
        }

        LivingEntity target = this.getTarget();
        if (target == null || this.rayCd > 0) {
            return;
        }
        double d = this.distanceTo(target);
        if (d >= 4.0D && d <= 15.0D) {
            this.rayCharge = 13;                           // 对齐 attack 动画的打击帧
            this.setAnim(ANIM_RAY);
            Vec3 from = this.position().add(0.0D, this.getBbHeight() * 0.7D, 0.0D);
            Vec3 dir = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D).subtract(from);
            this.rayBaseYaw = Math.atan2(dir.z, dir.x);
            level.playSound(null, this.blockPosition(), SoundEvents.BEACON_ACTIVATE,
                    SoundSource.HOSTILE, 1.2F, 1.6F);
        }
    }

    /** 按阶段决定几发：一阶 1 发，二阶 3 发扇形，三阶 5 发散射。 */
    private void fireRayVolley(ServerLevel level) {
        int count = this.phase == 3 ? 5 : (this.phase == 2 ? 3 : 1);
        double spread = this.phase == 3 ? 50.0D : (this.phase == 2 ? 24.0D : 0.0D);
        Vec3 from = this.position().add(0.0D, this.getBbHeight() * 0.7D, 0.0D);
        for (int i = 0; i < count; i++) {
            double offset = count <= 1 ? 0.0D : -spread + 2.0D * spread * i / (double) (count - 1);
            double yaw = this.rayBaseYaw + Math.toRadians(offset);
            // **关键修正**：原来是纯水平射线，炮口在 2.2 格高，从玩家头顶飞过去。
            // 这里加上俯仰角，让每一发都指向玩家胸口。
            Vec3 aim = from;
            LivingEntity rt = this.getTarget();
            if (rt != null && rt.isAlive()) {
                aim = rt.position().add(0.0D, rt.getBbHeight() * 0.55D, 0.0D);
            } else {
                aim = from.add(Math.cos(yaw) * RAY_RANGE, 0.0D, Math.sin(yaw) * RAY_RANGE);
            }
            double horiz = Math.hypot(aim.x - from.x, aim.z - from.z);
            double pitch = Math.atan2(aim.y - from.y, Math.max(0.01D, horiz));
            Vec3 dir = new Vec3(Math.cos(yaw) * Math.cos(pitch), Math.sin(pitch),
                    Math.sin(yaw) * Math.cos(pitch)).normalize();
            this.beamFrom = from;
            this.beamDir = dir;
            this.beamTicks = 15;                           // 残留 0.75 秒
            this.drawBeam(level, from, dir, 1.0F);
        }
        level.playSound(null, this.blockPosition(), SoundEvents.BLAZE_SHOOT,
                SoundSource.HOSTILE, 1.4F, 0.7F);

        // ---- **必定命中**：不再依赖几何判定。
        // 只要目标在射程内，这一轮齐射就结算一次伤害（多发射线只算一次，避免叠伤）。
        LivingEntity rt = this.getTarget();
        if (rt != null && rt.isAlive() && this.distanceTo(rt) <= RAY_RANGE) {
            float hitDmg = this.phase == 3 ? 4.0F : (this.phase == 2 ? 5.0F : 6.0F);
            if (rt instanceof Player rp) {
                this.damagePlayer(level, rp, hitDmg);
            } else {
                rt.hurt(this.damageSources().indirectMagic(this, this), hitDmg);
            }
            this.healFromAttack();
            rt.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
            level.sendParticles(ParticleTypes.CRIT,
                    rt.getX(), rt.getY() + rt.getBbHeight() * 0.5D, rt.getZ(),
                    16, 0.35D, 0.35D, 0.35D, 0.25D);
        }
        this.rayCd = 70;                                   // 3.5 秒
    }

    /** 直线判定：玩家到射线的垂直距离 < 0.9 格就算命中（约 1 格宽）。 */
    private void damageBeam(ServerLevel level, Vec3 from, Vec3 dir) {
        for (Player p : level.getEntitiesOfClass(Player.class,
                this.getBoundingBox().inflate(RAY_RANGE + 2.0D))) {
            Vec3 to = p.position().add(0.0D, p.getBbHeight() * 0.5D, 0.0D).subtract(from);
            double along = to.dot(dir);
            if (along < 0.0D || along > RAY_RANGE) {
                continue;
            }
            Vec3 closest = from.add(dir.scale(along));
            double perp = closest.distanceTo(p.position().add(0.0D, p.getBbHeight() * 0.5D, 0.0D));
            if (perp > 1.2D) {
                continue;
            }
            this.damagePlayer(level, p, 6.0F);
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
            p.push(dir.x * 0.3D, 0.1D, dir.z * 0.3D);
            level.sendParticles(ParticleTypes.CRIT,
                    closest.x, closest.y, closest.z, 14, 0.3D, 0.3D, 0.3D, 0.25D);
        }
    }

    /** 画一条又粗又亮的光柱：白核心 + 紫红外晕。 */
    private void drawBeam(ServerLevel level, Vec3 from, Vec3 dir, float alpha) {
        if (from == null || dir == null) {
            return;
        }
        int steps = 30;
        int core = alpha > 0.6F ? 3 : 2;
        for (int i = 0; i <= steps; i++) {
            double dist = (double) i / steps * RAY_RANGE;
            double x = from.x + dir.x * dist;
            double y = from.y + dir.y * dist;
            double z = from.z + dir.z * dist;
            level.sendParticles(ParticleTypes.END_ROD, x, y, z, core, 0.04D, 0.04D, 0.04D, 0.0D);
            level.sendParticles(ParticleTypes.DRAGON_BREATH, x, y, z, 2, 0.22D, 0.22D, 0.22D, 0.0D);
            if (i % 5 == 0) {
                level.sendParticles(ParticleTypes.FIREWORK, x, y, z, 1, 0.1D, 0.1D, 0.1D, 0.02D);
            }
        }
    }

    // ================================================================ 3/6/11 阵眼
    /**
     * 阵眼逻辑：**只决定"要不要去砸、到了就砸"，不抢占整个 tick**。
     *
     * <p>上一版这里返回 true 就 {@code return}，结果走向阵眼时射线、阵爆、召唤、近战
     * 全被跳过 —— 表现就是"只会偶尔单手砍一下，其它技能从来不出"。
     * 现在移动交给 {@link #moveTowardTarget}，技能各自正常计时。
     */
    private void updateEyeTarget(ServerLevel level) {
        if (this.targetEye != null) {
            if (!isActiveEye(level, this.targetEye)) {
                this.targetEye = null;
                return;
            }
            double d = Math.sqrt(this.targetEye.distToCenterSqr(this.getX(), this.getY(), this.getZ()));
            // **10 格内直接破阵眼**，不用跑到跟前（用户要求）
            if (d <= 10.0D) {
                this.getNavigation().stop();
                this.smashEye(level, this.targetEye,
                        this.phase == 3 ? 0.5F : (this.phase == 2 ? 0.2F : 0.1F));
                this.targetEye = null;
                this.eyeTimer = this.nextEyeDelay();
            } else {
                // 超出 10 格才走过去
                this.getNavigation().moveTo(this.targetEye.getX() + 0.5D, this.targetEye.getY(),
                        this.targetEye.getZ() + 0.5D, 1.2D);
            }
            return;
        }
        if (--this.eyeTimer > 0) {
            return;
        }
        this.eyeTimer = this.nextEyeDelay();
        this.targetEye = this.findNearestEye(level);
        FuLu.LOGGER.info("[demon] pick eye: chosen={}", this.targetEye);
    }

    private int nextEyeDelay() {
        return this.phase == 3 ? 200 : (this.phase == 2 ? 300 : 400);
    }

    private void smashEye(ServerLevel level, BlockPos pos, float chance) {
        level.playSound(null, pos, SoundEvents.ZOMBIE_ATTACK_IRON_DOOR,
                SoundSource.HOSTILE, 1.2F, 0.6F);
        level.sendParticles(ParticleTypes.CRIT,
                pos.getX() + 0.5D, pos.getY() + 0.7D, pos.getZ() + 0.5D,
                12, 0.35D, 0.35D, 0.35D, 0.05D);
        FuLu.LOGGER.info("[demon] smash {} chance={} hp={}", pos, chance,
                String.format("%.0f", this.getHealth()));
        if (this.random.nextFloat() < chance) {
            level.destroyBlock(pos, false);
            level.playSound(null, pos, SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 1.6F, 0.6F);
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    pos.getX() + 0.5D, pos.getY() + 0.6D, pos.getZ() + 0.5D,
                    24, 0.5D, 0.5D, 0.5D, 0.03D);
            this.absorbPos = pos;
            this.absorbTicks = 0;
            FuLu.LOGGER.info("[demon] eye broken -> absorb target {}", pos);
        }
    }

    // ================================================================ 15 吸收
    private boolean tickAbsorb(ServerLevel level) {
        if (this.absorbPos == null) {
            return false;
        }
        if (++this.absorbTicks > 100) {
            this.absorbPos = null;
            return false;
        }
        double d = Math.sqrt(this.absorbPos.distToCenterSqr(this.getX(), this.getY(), this.getZ()));
        if (d > 10.0D) {
            this.absorbPos = null;
            return false;
        }
        if (d > 2.5D) {
            this.getNavigation().moveTo(this.absorbPos.getX() + 0.5D, this.absorbPos.getY(),
                    this.absorbPos.getZ() + 0.5D, 1.3D);
            return true;
        }
        float before = this.getHealth();
        this.setHealth(Math.min(this.getMaxHealth(), before + this.getMaxHealth() * 0.10F));
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
                this.getX(), this.getY() + 1.2D, this.getZ(), 30, 0.8D, 0.8D, 0.8D, 0.3D);
        level.sendParticles(ParticleTypes.SQUID_INK,
                this.getX(), this.getY() + 1.0D, this.getZ(), 24, 0.7D, 0.7D, 0.7D, 0.02D);
        level.playSound(null, this.blockPosition(), SoundEvents.PLAYER_LEVELUP,
                SoundSource.HOSTILE, 1.0F, 0.6F);
        FuLu.LOGGER.info("[demon] absorbed -> hp {} -> {}", String.format("%.0f", before),
                String.format("%.0f", this.getHealth()));
        this.absorbPos = null;
        return false;
    }

    // ================================================================ 6/11 冲刺
    private void startDash(ServerLevel level, BlockPos eye) {
        this.dashTarget = eye;
        this.dashTravelled = 0.0D;
        if (this.dashAtPlayer) {
            level.playSound(null, this.blockPosition(), SoundEvents.ELDER_GUARDIAN_CURSE,
                    SoundSource.HOSTILE, 1.0F, 1.8F);
        }
        this.dashPrep = 8;                                 // 对齐 dash_grab 的蓄力段（0.38s）
        this.dashTicks = -1;
        this.setAnim(ANIM_DASH);
        this.setTarget(null);
        level.playSound(null, this.blockPosition(), SoundEvents.WITHER_SHOOT,
                SoundSource.HOSTILE, 1.2F, 1.4F);
    }

    private void tickDash(ServerLevel level) {
        this.dashTicks--;
        if (this.dashTarget == null || !isActiveEye(level, this.dashTarget)) {
            this.dashTicks = -1;
            this.dashTarget = null;
            return;
        }
        Vec3 from = this.position();
        Vec3 to = Vec3.atCenterOf(this.dashTarget);
        Vec3 dir = new Vec3(to.x - from.x, 0.0D, to.z - from.z);
        double d = dir.length();
        double speed = this.phase == 3 ? 1.4D : 1.1D;
        if (d > speed) {
            this.move(MoverType.SELF, dir.normalize().scale(speed));
            // 拖尾：黑烟 + 红色余烬，够亮才看得见
            level.sendParticles(ParticleTypes.LARGE_SMOKE,
                    this.getX(), this.getY() + 1.0D, this.getZ(), 4, 0.4D, 0.5D, 0.4D, 0.01D);
            level.sendParticles(this.phase == 3 ? ParticleTypes.CRIMSON_SPORE : ParticleTypes.END_ROD,
                    this.getX(), this.getY() + 1.4D, this.getZ(), 6, 0.35D, 0.35D, 0.35D, 0.02D);
            this.dashTravelled += speed;
            // 冲刺途中撞到玩家 = 处决连招（挑飞 → 砸地 → 插旗）
            for (Player p : level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(2.5D))) {
                this.beginExecution(level, p);
                return;
            }
            // 冲满 15 格还没撞到东西 → 强制停下，进入 3 秒虚弱
            if (this.dashTravelled >= 15.0D) {
                this.dashTicks = -1;
                this.dashTarget = null;
                this.pauseTicks = 60;                      // 3 秒虚弱
                level.playSound(null, this.blockPosition(), SoundEvents.ANVIL_LAND,
                        SoundSource.HOSTILE, 1.2F, 0.5F);
                level.sendParticles(ParticleTypes.LARGE_SMOKE,
                        this.getX(), this.getY() + 1.0D, this.getZ(), 14, 0.8D, 0.5D, 0.8D, 0.02D);
            }
            return;
        }
        this.dashTicks = -1;
        BlockPos hit = this.dashTarget;
        this.dashTarget = null;
        level.playSound(null, this.blockPosition(), SoundEvents.ANVIL_LAND,
                SoundSource.HOSTILE, 1.4F, 0.6F);
        level.sendParticles(ParticleTypes.EXPLOSION,
                this.getX(), this.getY() + 1.0D, this.getZ(), 3, 0.4D, 0.3D, 0.4D, 0.0D);
        if (this.phase == 3) {
            this.smashEye(level, hit, 0.5F);
            this.pauseTicks = 20;
        } else {
            for (int i = 0; i < 3; i++) {
                this.smashEye(level, hit, 0.2F);
            }
        }
        this.eyeTimer = this.nextEyeDelay();
    }

    /**
     * 冲刺抓到的瞬间：**播放 slam_impale**（挑飞 → 砸地 → 插旗），1.5 秒后在插旗帧结算。
     *
     * <p>按用户规格："必定命中而且必杀，只能用保命物品活下来"。
     * 所以三段都走 {@code hurt()}（用魔法伤害绕过护甲），
     * 累计远超玩家血量 —— **普通装备必死**，只有替身符 / 次数盾这类保命物品能挡。
     * 之所以不直接 {@code setHealth(0)}：那样会绕过保命物品的拦截，玩家就没法活了。
     */
    private void beginExecution(ServerLevel level, Player p) {
        this.dashTicks = -1;
        this.dashTarget = null;
        this.dashAtPlayer = false;
        this.executeTarget = p;
        this.executeTicks = 30;                            // 对齐 slam_impale 的 1.5 秒插旗帧
        this.setAnim(ANIM_EXECUTE);
        this.getNavigation().stop();

        level.playSound(null, p.blockPosition(), SoundEvents.ELDER_GUARDIAN_CURSE,
                SoundSource.HOSTILE, 1.4F, 0.6F);
        // 抓取瞬间：把玩家挑到空中
        p.push(0.0D, 0.9D, 0.0D);
        p.hurtMarked = true;
        level.sendParticles(ParticleTypes.SWEEP_ATTACK,
                p.getX(), p.getY() + 1.1D, p.getZ(), 12, 0.4D, 0.4D, 0.4D, 0.0D);
    }

    /**
     * 调试用：立刻对某个玩家发动处决（供 {@code /fulu exec} 调用）。
     *
     * <p>走的是和冲刺抓到玩家**完全相同**的那条路径，所以看到的效果是真的。
     */
    public void forceExecution(net.minecraft.world.entity.player.Player target) {
        if (this.level() instanceof ServerLevel sl) {
            this.beginExecution(sl, target);
        }
    }

    /**
     * 调试用：把阵魔放到玩家面前 14 格，然后**完整跑一遍冲刺 → 抓取 → 处决**。
     *
     * <p>和真实战斗完全同一条路径（蓄力 8 刻 → 1.4 格/刻冲刺 → 撞到才抓），
     * 所以你能看到冲刺画面和 `dash_grab` / `slam_impale` 两段动画。
     */
    public void forceFullDash(net.minecraft.world.entity.player.Player target) {
        if (!(this.level() instanceof ServerLevel sl)) {
            return;
        }
        Vec3 look = target.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0.0D, look.z);
        if (flat.length() < 0.01D) {
            flat = new Vec3(0.0D, 0.0D, 1.0D);
        }
        flat = flat.normalize();
        double x = target.getX() + flat.x * 14.0D;
        double z = target.getZ() + flat.z * 14.0D;
        this.teleportTo(x, target.getY(), z);
        this.setTarget(target);
        this.dashCd = 0;
        this.pauseTicks = 0;
        this.executeTicks = -1;
        this.dashAtPlayer = true;
        this.startDash(sl, target.blockPosition());
        FuLu.LOGGER.info("[demon] debug dash from {},{},{} toward player", (int) x, (int) target.getY(), (int) z);
    }

    /** 插旗帧结算：三段必杀伤害。 */
    private void applyExecution(ServerLevel level) {
        Player p = this.executeTarget;
        if (p == null || !p.isAlive()) {
            return;
        }
        // 挑飞
        p.push(0.0D, 1.0D, 0.0D);
        p.hurtMarked = true;
        // ---- 第一段：必杀技结算（无视护甲/效果）
        this.damagePlayerUltimate(level, p, ULTIMATE_DAMAGE);
        p.invulnerableTime = 0;

        if (p.isAlive()) {
            // ---- 方案 C：判断"是不是被保命物品救下来的"
            //
            //   不死图腾命中后：血量被设成 **1**，并给吸收与抗性；
            //   替身阵命中后  ：满血 + **吸收（次数盾 20）** + 免疫。
            //
            // 所以「血量 ≤ 2 或 有吸收」= 保命物品生效过，放过玩家；
            // 否则说明是**别的减伤**（比如无尽套装/别的模组）扛住的，那就强行处决。
            // ① 伤害真的打进去了：图腾/替身已经自己救过一次（血量 1 或满血+吸收）
            boolean savedByItem = p.getHealth() <= 2.0F || p.getAbsorptionAmount() > 0.5F;
            FuLu.LOGGER.info("[demon] ult vs player: alive hp={} absorb={} savedByItem={}",
                    p.getHealth(), p.getAbsorptionAmount(), savedByItem);

            // ② 伤害被别的模组整段拦掉：图腾没机会触发，这里**主动**替它生效
            if (!savedByItem && this.consumeTotem(p)) {
                savedByItem = true;
                FuLu.LOGGER.info("[demon] ult: totem consumed -> protected");
            }

            // ③ 替身阵：走原本的替死逻辑（满血 + 免疫 + 碎阵眼）
            if (!savedByItem && com.lengyang.fulu.standin.StandInData.hasStandIn(p)) {
                com.lengyang.fulu.combat.PlayerDamageHandler.triggerStandIn(p);
                savedByItem = true;
                FuLu.LOGGER.info("[demon] ult: stand-in triggered -> protected");
            }

            if (!savedByItem) {
                // 先试一次正常伤害（会触发图腾/替身的判定），再不行就绕过事件强杀
                p.hurt(level.damageSources().genericKill(), Float.MAX_VALUE);
                if (p.isAlive()) {
                    this.forceKill(level, p);
                }
                FuLu.LOGGER.info("[demon] ult force-kill result: alive={}", p.isAlive());
            }
            this.resolveDeathUltimate(level, !p.isAlive());
            return;
        }

        // 第一段就打死了：补足表演，然后按"玩家死"结算
        p.setDeltaMovement(0.0D, -1.4D, 0.0D);
        this.healFromAttack();          // 无敌期间：攻击回血
        this.resolveDeathUltimate(level, true);

        level.playSound(null, p.blockPosition(), SoundEvents.ANVIL_LAND,
                SoundSource.HOSTILE, 2.2F, 0.4F);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                p.getX(), p.getY() + 1.0D, p.getZ(), 3, 0.6D, 0.4D, 0.6D, 0.0D);
        level.sendParticles(ParticleTypes.SWEEP_ATTACK,
                p.getX(), p.getY() + 0.8D, p.getZ(), 18, 0.7D, 0.4D, 0.7D, 0.0D);
        level.sendParticles(ParticleTypes.CRIT,
                p.getX(), p.getY() + 1.2D, p.getZ(), 30, 0.5D, 0.6D, 0.5D, 0.4D);
    }

    // ================================================================ 8/14 旋转阵纹
    private void tickRunes(ServerLevel level) {
        if (this.runeTicks > 0) {
            this.runeTicks--;
            double radius = this.phase == 3 ? 3.6D : 2.6D;
            float dmg = this.phase == 3 ? 5.0F : 3.0F;
            float knock = this.phase == 3 ? 2.0F : 1.0F;
            double rot = (this.tickCount + 1) * 0.18D;
            for (int i = 0; i < 8; i++) {
                double a = rot + Math.PI * 2.0D * i / 8.0D;
                level.sendParticles(ParticleTypes.ENCHANT,
                        this.getX() + Math.cos(a) * radius, this.getY() + 0.6D,
                        this.getZ() + Math.sin(a) * radius, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
            for (Player p : level.getEntitiesOfClass(Player.class,
                    this.getBoundingBox().inflate(radius + 1.0D))) {
                if (p.distanceTo(this) <= radius + 0.4D && p.tickCount % 10 == 0) {
                    this.damagePlayer(level, p, dmg);
                    Vec3 away = p.position().subtract(this.position()).normalize();
                    p.push(away.x * knock * 0.45D, 0.2D, away.z * knock * 0.45D);
                }
            }
            return;
        }
        if (this.phase < 2 || --this.runeCd > 0) {
            return;
        }
        this.runeCd = this.phase == 3 ? 400 : 300;
        this.runeTicks = this.phase == 3 ? 160 : 100;
        level.playSound(null, this.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE,
                SoundSource.HOSTILE, 1.2F, 0.6F);
    }

    // ================================================================ 阵爆（二阶段解锁）
    /**
     * 阵爆：玩家或召唤物在 **8 格内持续 5 秒** 且冷却好了（15 秒）就原地举手。
     *
     * <p>前摇 1.5 秒，地面画出 **半径 8 格的黑圈预警**，跑出圈就安全。
     * 爆炸 10 点真实伤害（魔法伤害，几乎无视护甲）+ 击飞 + 破坏周围轻型方块。
     */
    private void tickBurst(ServerLevel level) {
        if (this.burstWindup > 0) {
            this.burstWindup--;
            this.getNavigation().stop();
            float p = (30 - this.burstWindup) / 30.0F;
            // 膨胀
            level.sendParticles(ParticleTypes.LARGE_SMOKE,
                    this.getX(), this.getY() + 1.0D, this.getZ(),
                    2 + (int) (p * 8.0F), 0.9D, 0.9D, 0.9D, 0.02D);
            // 地面预警圈（半径 8）
            for (int i = 0; i < 36; i++) {
                double a = Math.PI * 2.0D * i / 36.0D + p * 2.0D;
                level.sendParticles(ParticleTypes.SQUID_INK,
                        this.getX() + Math.cos(a) * 8.0D, this.getY() + 0.15D,
                        this.getZ() + Math.sin(a) * 8.0D, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
            if (this.burstWindup == 0) {
                this.explodeBurst(level);
            }
            return;
        }

        // 统计"玩家在 8 格内"的持续时间
        boolean anyNear = false;
        for (Player p : level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(9.0D))) {
            if (p.distanceTo(this) <= 8.0D) {
                anyNear = true;
                break;
            }
        }
        if (anyNear) {
            this.burstClose++;
        } else {
            this.burstClose = Math.max(0, this.burstClose - 2);
        }
        if (this.burstTimer > 0) {
            this.burstTimer--;
        }
        if (this.burstClose >= 100 && this.burstTimer <= 0) {
            this.burstClose = 0;
            this.burstTimer = 300;                         // 15 秒冷却
            this.burstWindup = 30;                         // 1.5 秒前摇
            this.setAnim(ANIM_BURST);
            level.playSound(null, this.blockPosition(), SoundEvents.WARDEN_HEARTBEAT,
                    SoundSource.HOSTILE, 1.5F, 0.6F);
        }
    }

    private void explodeBurst(ServerLevel level) {
        level.playSound(null, this.blockPosition(), SoundEvents.GENERIC_EXPLODE,
                SoundSource.HOSTILE, 2.2F, 0.5F);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                this.getX(), this.getY() + 1.0D, this.getZ(), 5, 1.4D, 0.6D, 1.4D, 0.0D);
        for (int i = 0; i < 48; i++) {
            double a = Math.PI * 2.0D * i / 48.0D;
            level.sendParticles(ParticleTypes.LARGE_SMOKE,
                    this.getX() + Math.cos(a) * 8.0D, this.getY() + 0.6D,
                    this.getZ() + Math.sin(a) * 8.0D, 1, 0.1D, 0.1D, 0.1D, 0.05D);
        }
        // 破坏周围轻型方块
        BlockPos origin = this.blockPosition();
        for (int dx = -3; dx <= 3; dx++) {
            for (int dy = -1; dy <= 2; dy++) {
                for (int dz = -3; dz <= 3; dz++) {
                    BlockPos bp = origin.offset(dx, dy, dz);
                    BlockState st = level.getBlockState(bp);
                    if (st.getDestroySpeed(level, bp) >= 0.0F && st.getDestroySpeed(level, bp) < 0.6F) {
                        level.destroyBlock(bp, false);
                    }
                }
            }
        }
        for (Player p : level.getEntitiesOfClass(Player.class,
                this.getBoundingBox().inflate(8.5D))) {
            if (p.distanceTo(this) <= 8.0D) {
                this.damagePlayer(level, p, 10.0F);   // 真实伤害
                Vec3 away = p.position().subtract(this.position()).normalize();
                p.push(away.x * 2.0D, 0.7D, away.z * 2.0D);
                p.hurtMarked = true;
            }
        }
    }

    // ================================================================ 7/12 召唤
    private void tickSummon(ServerLevel level) {
        // ---- 念咒（1 秒）：身体抖动、黑粒子、漩涡扩大
        if (this.summonChant > 0) {
            this.summonChant--;
            this.getNavigation().stop();
            level.sendParticles(ParticleTypes.SQUID_INK,
                    this.getX(), this.getY() + getBbHeight() * 0.95D, this.getZ(),
                    3, 0.25D, 0.15D, 0.25D, 0.01D);
            double grow = (20 - this.summonChant) / 20.0D;
            for (int i = 0; i < 10; i++) {
                double a = Math.PI * 2.0D * i / 10.0D + this.tickCount * 0.1D;
                level.sendParticles(ParticleTypes.LARGE_SMOKE,
                        this.getX() + Math.cos(a) * (1.0D + grow * 1.8D), this.getY() + 0.1D,
                        this.getZ() + Math.sin(a) * (1.0D + grow * 1.8D), 1, 0.0D, 0.02D, 0.0D, 0.0D);
            }
            if (this.summonChant == 0) {
                this.doSummon(level, this.summonN);
            }
            return;
        }
        // ---- 抬双手（0.5 秒）
        if (this.summonPrep > 0) {
            this.summonPrep--;
            this.getNavigation().stop();
            for (int i = 0; i < 6; i++) {
                double a = Math.PI * 2.0D * i / 6.0D;
                level.sendParticles(ParticleTypes.SQUID_INK,
                        this.getX() + Math.cos(a) * 0.9D, this.getY() + 0.1D,
                        this.getZ() + Math.sin(a) * 0.9D, 1, 0.0D, 0.02D, 0.0D, 0.0D);
            }
            if (this.summonPrep == 0) {
                this.summonChant = 20;
            }
            return;
        }
        if (this.phase < 2 || --this.summonTimer > 0) {
            return;
        }
        this.summonTimer = this.phase == 3 ? 300 : 400;
        this.summonN = this.phase == 3 ? 4 + this.random.nextInt(2) : 2 + this.random.nextInt(2);
        this.summonPrep = 10;
        this.setAnim(ANIM_SUMMON);
        level.playSound(null, this.blockPosition(), SoundEvents.EVOKER_CAST_SPELL,
                SoundSource.HOSTILE, 1.2F, 0.6F);
    }

    /** 真正把阵仆放出来（在念咒结束时调用）。 */
    private void doSummon(ServerLevel level, int n) {
        for (int i = 0; i < n; i++) {
            double a = Math.PI * 2.0D * i / n;
            double x = this.getX() + Math.cos(a) * 2.5D;
            double z = this.getZ() + Math.sin(a) * 2.5D;
            FormationMinionEntity minion = ModEntities.FORMATION_MINION.get().create(level);
            if (minion == null) {
                continue;
            }
            minion.moveTo(x, this.getY(), z, this.getYRot(), 0.0F);
            minion.setOwner(this);
            level.addFreshEntity(minion);
            level.sendParticles(ParticleTypes.SQUID_INK,
                    x, this.getY() + 0.3D, z, 12, 0.45D, 0.3D, 0.45D, 0.02D);
            for (int k = 0; k < 8; k++) {
                double aa = Math.PI * 2.0D * k / 8.0D;
                level.sendParticles(ParticleTypes.LARGE_SMOKE,
                        x + Math.cos(aa) * 0.9D, this.getY() + 0.1D, z + Math.sin(aa) * 0.9D,
                        1, 0.0D, 0.05D, 0.0D, 0.0D);
            }
        }
    }

    /**
     * 临死召唤（用户要求）：**平时不召唤阵仆，只有血量极低时才呼唤帮手**。
     *
     * <p>只触发一次，避免变成"无限召唤机"。
     */
    private void tickLastStandSummon(ServerLevel level) {
        if (this.lastStandSummoned) {
            return;
        }
        if (this.getHealth() / this.getMaxHealth() > 0.10F) {
            return;
        }
        this.lastStandSummoned = true;
        this.doSummon(level, 2 + this.random.nextInt(2));
        level.playSound(null, this.blockPosition(), SoundEvents.EVOKER_CAST_SPELL,
                SoundSource.HOSTILE, 1.4F, 0.5F);
        this.announce(level, "msg.fulu.demon.lastStand");
    }

    // ================================================================ 18 屏障伤害
    private void tickBarrierDamage(ServerLevel level) {
        if (this.tickCount % 20 != 0) {
            return;
        }
        BlockPos eye = this.findNearestEye(level);
        if (eye == null) {
            return;
        }
        double d = Math.sqrt(eye.distToCenterSqr(this.getX(), this.getY(), this.getZ()));
        if (d > 8.0D && d < 10.5D) {
            this.hurt(this.damageSources().magic(), 2.0F);
            level.sendParticles(ParticleTypes.FIREWORK,
                    this.getX(), this.getY() + 1.4D, this.getZ(), 8, 0.5D, 0.5D, 0.5D, 0.05D);
            level.playSound(null, this.blockPosition(), SoundEvents.BEACON_POWER_SELECT,
                    SoundSource.HOSTILE, 0.6F, 1.6F);
        }
    }

    // ================================================================ 17 逃跑（已移除）

    // ================================================================ 16 阵痕
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.pendingDeath) {
            return false;                                  // 死亡反击期间免疫（它只有 1 血）
        }
        if (this.invulTicks > 0) {
            return false;                                  // 转阶段仪式期间无敌
        }
        if (this.stunTicks > 0) {
            amount *= 2.0F;
        }

        // 血量 60% 以下：**70% 减伤**（只吃三成伤害）
        if (this.getHealth() <= this.getMaxHealth() * 0.60F) {
            amount *= 0.30F;
        }

        // ---- **致命伤拦截**：这一下会打死它 -> 不死，改成放必杀技
        //
        // 只要这一下**能把阵魔打死**就拦截：血量强制锁到 1，
        // 然后**等一下**（0.5 秒）再放必杀技 —— 让它先"掉到 1 血"给玩家看到。
        if (!this.ultimateUsed && !this.level().isClientSide && this.getHealth() > 0.0F) {
            float afterArmor = this.getDamageAfterArmorAbsorb(source, amount);
            float afterMagic = this.getDamageAfterMagicAbsorb(source, afterArmor);
            float finalDamage = afterMagic;
            if (this.getHealth() + this.getAbsorptionAmount() - finalDamage <= 0.0F) {
                FuLu.LOGGER.info("[demon] death-ult trigger: hp={}/{} dmg={} phase={}",
                        this.getHealth(), this.getMaxHealth(), finalDamage, this.phase);
                // 先把血锁到 1，再延迟 0.5 秒放必杀技（顺序：先 1 血 -> 再必杀）
                this.pendingDeath = true;
                this.pendingUltDelay = 10;
                this.setHealth(1.0F);
                this.setAbsorptionAmount(0.0F);
                this.setAnim(ANIM_HURT);
                this.announce((ServerLevel) this.level(), "msg.fulu.demon.ult_charge");
                return true;                           // 这一下被"死亡反击"吞掉
            }
        }
        boolean result = super.hurt(source, amount);
        if (result && !this.level().isClientSide && this.getAnim() == ANIM_IDLE) {
            this.setAnim(ANIM_HURT);                       // 出招期间不打断，只有待机才播受伤
        }
        if (result && !this.level().isClientSide && this.level() instanceof ServerLevel sl) {
            boolean inFormation = this.findNearestEye(sl) != null;
            if (inFormation && this.stunTicks <= 0) {
                this.demonMark++;
                sl.sendParticles(ParticleTypes.ENCHANT,
                        this.getX(), this.getY() + getBbHeight() * 0.7D, this.getZ(),
                        6, 0.5D, 0.6D, 0.5D, 0.4D);
                if (this.demonMark >= 10) {
                    this.stunTicks = 60;
                    sl.playSound(null, this.blockPosition(), SoundEvents.ANVIL_BREAK,
                            SoundSource.HOSTILE, 1.4F, 0.5F);
                    sl.sendParticles(ParticleTypes.END_ROD,
                            this.getX(), this.getY() + getBbHeight() + 0.8D, this.getZ(),
                            24, 0.6D, 0.4D, 0.6D, 0.05D);
                    this.getNavigation().stop();
                }
            }
            FuLu.LOGGER.info("[demon] hurt {} inFormation={} mark={} stun={} hp={}",
                    String.format("%.1f", amount), inFormation, this.demonMark,
                    this.stunTicks, String.format("%.0f", this.getHealth()));
        }
        return result;
    }

    // ================================================================ 阵眼工具
    private static boolean isActiveEye(ServerLevel level, BlockPos pos) {
        BlockState st = level.getBlockState(pos);
        if (!(st.getBlock() instanceof com.lengyang.fulu.block.FormationEyeBlock)) {
            return false;
        }
        return st.getValue(com.lengyang.fulu.block.FormationEyeBlock.TALISMAN)
                != com.lengyang.fulu.block.TalismanKind.EMPTY;
    }

    private BlockPos findNearestEye(ServerLevel level) {
        BlockPos best = null;
        double bestD = 32.0D * 32.0D;
        for (BlockPos p : com.lengyang.fulu.block.FormationEyeBlockEntity.liveEyes(level)) {
            if (!isActiveEye(level, p)) {
                continue;
            }
            double d = p.distToCenterSqr(this.getX(), this.getY(), this.getZ());
            if (d < bestD) {
                bestD = d;
                best = p;
            }
        }
        return best;
    }

    // ================================================================ 阶段切换
    private void onPhaseChange(ServerLevel level, int newPhase) {
        // ================= 转阶段仪式 =================
        // 打断当前动作
        this.meleeWindup = -1;
        this.meleeChain = 0;
        this.pendingRays = 0;
        this.rayCharge = 0;
        this.dashPrep = 0;
        this.dashTicks = -1;
        this.getNavigation().stop();

        // 怒吼 + 冲击波：把周围玩家震开（二阶段 10 格 / 三阶段 15 格）
        double pushRadius = newPhase == 3 ? 15.0D : 10.0D;
        // 无敌 5 秒，之后冷却 10 秒才能再次进入无敌（用户要求）
        if (this.invulCd <= 0) {
            this.invulTicks = 100;
            this.invulCd = 300;      // 5s 无敌 + 10s 冷却 = 15s 一轮
            this.invulAtkCd = 0;     // 进入无敌立刻可以开始高频攻击
        }

        level.playSound(null, this.blockPosition(), SoundEvents.WARDEN_ROAR,
                SoundSource.HOSTILE, 2.0F, newPhase == 3 ? 0.5F : 0.7F);
        for (int ring = 1; ring <= 6; ring++) {
            double r = pushRadius * ring / 6.0D;
            for (int i = 0; i < 24; i++) {
                double a = Math.PI * 2.0D * i / 24.0D;
                level.sendParticles(newPhase == 3 ? ParticleTypes.SQUID_INK : ParticleTypes.LARGE_SMOKE,
                        this.getX() + Math.cos(a) * r, this.getY() + 0.4D + ring * 0.2D,
                        this.getZ() + Math.sin(a) * r, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
        }
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                this.getX(), this.getY() + 1.0D, this.getZ(), 4, 1.2D, 0.6D, 1.2D, 0.0D);
        for (Player p : level.getEntitiesOfClass(Player.class,
                this.getBoundingBox().inflate(pushRadius + 1.0D))) {
            double d = p.distanceTo(this);
            if (d > pushRadius) {
                continue;
            }
            Vec3 away = p.position().subtract(this.position()).normalize();
            p.push(away.x * 2.2D, 0.8D, away.z * 2.2D);
            p.hurtMarked = true;
        }

        // 三阶段：可能直接吸掉一个阵法来回血
        if (newPhase == 3) {
            BlockPos victimEye = this.findNearestEye(level);
            if (victimEye != null
                    && victimEye.distToCenterSqr(this.getX(), this.getY(), this.getZ()) < 24.0D * 24.0D) {
                level.destroyBlock(victimEye, false);
                this.setHealth(Math.min(this.getMaxHealth(),
                        this.getHealth() + this.getMaxHealth() * 0.10F));
                level.playSound(null, victimEye, SoundEvents.GLASS_BREAK,
                        SoundSource.HOSTILE, 1.8F, 0.5F);
                level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
                        this.getX(), this.getY() + 1.2D, this.getZ(), 40, 1.0D, 1.0D, 1.0D, 0.3D);
            }
        }

        level.playSound(null, this.blockPosition(), SoundEvents.WITHER_SPAWN,
                SoundSource.HOSTILE, 1.2F, newPhase == 3 ? 0.6F : 0.9F);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                this.getX(), this.getY() + 1.2D, this.getZ(), 40, 1.0D, 1.2D, 1.0D, 0.05D);
        level.sendParticles(ParticleTypes.SQUID_INK,
                this.getX(), this.getY() + 1.2D, this.getZ(), 24, 0.8D, 1.0D, 0.8D, 0.02D);
        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(
                newPhase == 3 ? 0.58D : (newPhase == 2 ? 0.48D : 0.38D));
        this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(
                newPhase == 3 ? 15.0D : (newPhase == 2 ? 12.0D : 8.0D));

        // 无敌提示：转阶段仪式期间打不动，必须告诉玩家
        this.setAnim(ANIM_ROAR);
        this.announce(level, "msg.fulu.demon.invul");
        level.sendParticles(ParticleTypes.END_ROD,
                this.getX(), this.getY() + getBbHeight() * 0.6D, this.getZ(),
                40, 1.2D, 1.4D, 1.2D, 0.15D);
        this.announce(level, "msg.fulu.demon.phase" + newPhase);
    }

    // ================================================================ 移除与掉落
    @Override
    public void remove(RemovalReason reason) {
        this.bossBar.removeAllPlayers();
        FuLu.LOGGER.info("[demon] removed: {} at {},{},{} hp={}", reason,
                (int) this.getX(), (int) this.getY(), (int) this.getZ(),
                String.format("%.0f", this.getHealth()));
        super.remove(reason);
    }

    @Override
    protected void tickDeath() {
        // 原版 20 刻（1 秒）就移除，death 动画 2 秒放不完；延长到 40 刻
        ++this.deathTime;
        if (this.deathTime >= 40 && !this.level().isClientSide
                && !this.isRemoved()) {
            this.remove(RemovalReason.KILLED);
        }
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        // 死亡时残留一小波阵仆（几率性，主体还是靠周期性召唤）
        if (this.level() instanceof ServerLevel deathLevel && this.random.nextFloat() < 0.5F) {
            this.doSummon(deathLevel, 2 + this.random.nextInt(2));
        }
        this.spawnAtLocation(com.lengyang.fulu.registry.ModItems.DEMON_CORE.get());
        this.spawnAtLocation(com.lengyang.fulu.registry.ModItems.DEMON_SHARD.get());
        if (this.random.nextFloat() < 0.35F) {
            this.spawnAtLocation(com.lengyang.fulu.registry.ModItems.DEMON_CORE.get());
        }
        if (this.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    this.getX(), this.getY() + 1.0D, this.getZ(), 60, 1.2D, 1.0D, 1.2D, 0.1D);
            sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
                    this.getX(), this.getY() + 1.0D, this.getZ(), 40, 1.0D, 0.8D, 1.0D, 0.2D);
            sl.playSound(null, this.blockPosition(), SoundEvents.GLASS_BREAK,
                    SoundSource.HOSTILE, 2.0F, 0.5F);
        }
    }
}

