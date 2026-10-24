package com.lengyang.fulu.entity;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import com.lengyang.fulu.FuLu;
import com.lengyang.fulu.registry.ModEntities;
import com.lengyang.fulu.registry.ModParticles;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import com.lengyang.fulu.combat.FormationTarget;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 飞剑：一把真的会飞、会追、会砍的剑。
 *
 * <p>之前飞剑只是"服务端每刻算位置 + 撒粒子"（见方块实体的旧代码），
 * 那是一条**看不见的轨迹**：粒子在飞，但没有东西在飞。现在它是一个**实体** ——
 * 有自己的坐标、朝向、跟踪目标、命中结算，玩家还能看到一把剑在天上飞。
 *
 * <p>三个属性（也就是用户要的"飞行速度 / 伤害值 / 目标怪物"）：
 * <ul>
 *   <li>{@link #SPEED}：每刻走多少格；</li>
 *   <li>{@link #damage}：命中扣多少血（默认 {@link #DAMAGE}）；</li>
 *   <li>{@link #targetUuid} / {@link #cachedTarget}：追谁。</li>
 * </ul>
 *
 * <p>行为：每刻朝目标飞；飞行途中撒剑光粒子；撞到就结算伤害、撒爆发粒子、消失；
 * 目标死了或不见了，自己也消失（不会有一把剑在原地打转）。
 */
public class FlyingSwordEntity extends Entity {

    /** 每刻飞多少格（用户要求 0.5 格/刻，约 10 格/秒）。 */
    private static final double SPEED = 0.5D;

    /** 默认伤害：5 点。 */
    private static final float DAMAGE = 5.0F;

    /** 撞到这个距离之内就算命中（实体中心到目标中心）。 */
    private static final double HIT_DISTANCE = 0.9D;

    /**
     * 没有目标时，自动找这个范围内的怪。
     *
     * <p>只对"出生时就没有目标"的剑生效（比如手动 {@code /summon} 出来的）——
     * 阵法发射的剑**锁死自己的目标**，目标死了它就消失，不会改追别人。
     */
    private static final double SEEK_RANGE = 12.0D;

    /** 存在超过这么久还没命中就自己消失（用户要求 5 秒 = 100 刻）。 */
    private static final int MAX_LIFE = 100;

    /** 每刻撒几颗拖尾（用户要求 1~2 颗，取 2 颗更连贯）。 */

    /** 命中时炸几颗。 */
    private static final int BURST_PARTICLES = 10;

    /** 符文：一圈几颗。 */
    private static final int RUNE_COUNT = 8;

    /** 符文绕剑轴转几圈（从剑尾到剑锋）。 */
    private static final double RUNE_SPIRALS = 1.5D;

    /** 符文离剑轴多远（格）。 */
    private static final double RUNE_RADIUS = 0.42D;

    /** 符文转一圈用多少刻（40 刻 = 2 秒一圈，快得看得出在转，又不晕）。 */
    private static final int RUNE_PERIOD_TICKS = 40;

    /**
     * 剑锋到剑尾的长度（格）。
     *
     * <p>必须和渲染器里的 {@code MODEL_SCALE} 保持一致：模型本身 48 单位 = 3 格，
     * 渲染时缩了一半，所以世界里看到的剑是 1.5 格。符文要贴着剑身绕，就得用同一个数。
     */
    private static final double BLADE_LENGTH = 1.5D;

    /** 命中音效：剑砍声。（发射的"咻"在方块实体那边，一次齐射只播一次。） */
    private static final SoundEvent HIT_SOUND = SoundEvents.PLAYER_ATTACK_STRONG;

    // ------------------------------------------------------------------
    // 动画状态（要和客户端一致，所以放在**实体**里而不是模型类里 ——
    // 模型类是纯客户端的，服务端引用它会崩）
    // ------------------------------------------------------------------

    /** 待机（停着不动时）。 */
    public static final byte ANIM_IDLE = 0;
    /** 飞行（1 秒一圈）。 */
    public static final byte ANIM_FLY = 1;
    /** 发射（0.5 秒，只播一次）。 */
    public static final byte ANIM_LAUNCH = 2;
    /** 命中（0.3 秒，只播一次，播完消失）。 */
    public static final byte ANIM_HIT = 3;

    /** launch 时长（刻）。 */
    public static final int ANIM_LAUNCH_TICKS = 10;
    /** idle 周期（刻）。 */
    public static final int ANIM_IDLE_TICKS = 40;
    /** fly 周期（刻）。 */
    public static final int ANIM_FLY_TICKS = 20;
    /** hit 时长（刻）。 */
    public static final int ANIM_HIT_TICKS = 6;

    private static final EntityDataAccessor<Byte> DATA_ANIM =
            SynchedEntityData.defineId(FlyingSwordEntity.class, EntityDataSerializers.BYTE);
    /** hit 动画已经播了几刻（只在命中的那 6 刻里同步，代价可以忽略）。 */
    private static final EntityDataAccessor<Integer> DATA_HIT_AGE =
            SynchedEntityData.defineId(FlyingSwordEntity.class, EntityDataSerializers.INT);

    /** 已经命中、正在播 hit：此时不再移动、不再造成伤害，播完就消失。 */
    private boolean dying = false;

    @Nullable
    private UUID targetUuid;
    @Nullable
    private Entity cachedTarget;
    /**
     * 出生时就有目标 = 锁定；这种剑不会改追别人（目标没了它就消失）。
     *
     * <p>不是 final：另一个构造函数会先委托给无参那个（那里写 false），
     * 再按"有没有目标"改掉它。
     */
    private boolean locked;
    private float damage = DAMAGE;
    private int life = 0;

    public FlyingSwordEntity(EntityType<? extends FlyingSwordEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;              // 剑不被方块挡住（撞墙由下面的检查自己判）
        this.setNoGravity(true);
        this.locked = false;
    }

    /** 由阵法发射：从某个位置出发，去追某只怪（**锁定**这只）。 */
    public FlyingSwordEntity(Level level, Vec3 from, Vec3 look, @Nullable Mob target,
                             float damage) {
        this(ModEntities.FLYING_SWORD.get(), level);
        this.setPos(from.x, from.y, from.z);
        this.damage = damage;
        // 锁定的剑记下目标；反过来，出生时没目标（/summon）的剑会自己去找
        this.locked = target != null;
        if (target != null) {
            this.targetUuid = target.getUUID();
            this.cachedTarget = target;
        }
        // 先把朝向摆好，第一帧就不会"横着飞"
        faceTowards(from, from.add(look));
    }

    @Override
    protected void defineSynchedData() {
        // 动画状态：出生时是 launch（前 10 刻自转 720→1080 度、缩放 0.5→1.2→1.0），
        // 10 刻后自动切 fly。命中时切成 hit 并同步播放进度。
        this.entityData.define(DATA_ANIM, ANIM_LAUNCH);
        this.entityData.define(DATA_HIT_AGE, 0);
    }

    /** 当前动画（客户端渲染时读）。 */
    public int animState() {
        return this.entityData.get(DATA_ANIM);
    }

    /**
     * 当前动画已经播了几刻。
     *
     * <p>launch / idle / fly 都用实体自己的 {@code tickCount}：出生时两边都从 0 开始数，
     * 所以相位天然一致 ✓。只有 hit 需要额外同步（它发生在生命周期的任意时刻）。
     */
    public int animAge() {
        return animState() == ANIM_HIT ? this.entityData.get(DATA_HIT_AGE) : this.tickCount;
    }

    private void setAnim(byte state) {
        if (this.entityData.get(DATA_ANIM) != state) {
            this.entityData.set(DATA_ANIM, state);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Target")) {
            this.targetUuid = tag.getUUID("Target");
        }
        this.damage = tag.contains("Damage") ? tag.getFloat("Damage") : DAMAGE;
        this.life = tag.getInt("Life");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.targetUuid != null) {
            tag.putUUID("Target", this.targetUuid);
        }
        tag.putFloat("Damage", this.damage);
        tag.putInt("Life", this.life);
    }

    @Override
    public Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }

    @Nullable
    public Entity target() {
        return this.cachedTarget;
    }

    @Override
    public void tick() {
        super.tick();
        this.life++;

        if (this.level().isClientSide()) {
            return;                         // 飞行的权威在服务端，客户端只负责画
        }
        if (!(this.level() instanceof ServerLevel server)) {
            return;
        }

        // ---- 动画状态机：出生 launch -> fly；命中 hit -> 消失 ----
        if (this.dying) {
            // 正在播 hit：不动、不再造成伤害，播完就消失
            int age = this.entityData.get(DATA_HIT_AGE) + 1;
            this.entityData.set(DATA_HIT_AGE, age);
            if (age >= ANIM_HIT_TICKS) {
                this.discard();
            }
            return;
        }
        if (this.animState() == ANIM_LAUNCH && this.tickCount >= ANIM_LAUNCH_TICKS) {
            setAnim(ANIM_FLY);              // launch 播完自动切 fly
        }

        // ---- 找目标：锁定的目标优先，只有出生时就没目标才去找 ----
        Entity target = this.cachedTarget;
        if (target == null && this.targetUuid != null) {
            target = server.getEntity(this.targetUuid);   // 区块重载之后也能找回来
            this.cachedTarget = target;
        }
        // 锁定的目标死了或不见了 —— 剑也消失（用户要求），**不改追别人**
        if (this.locked && (target == null || !target.isAlive())) {
            if (DEBUG_LOG) {
                FuLu.LOGGER.info("[sword] locked target is gone, discarding");
            }
            this.discard();
            return;
        }
        if (!this.locked && (target == null || !target.isAlive())) {
            // 出生时就没目标（比如 /summon 出来的）：自己找最近的怪
            target = findNearestMonster(server);
            this.cachedTarget = target;
            this.targetUuid = target == null ? null : target.getUUID();
            if (target == null) {
                this.discard();
                return;
            }
        }
        if (this.life > MAX_LIFE) {
            // 5 秒还没打到（目标跑太远、或者被挡住）就自己消失
            if (DEBUG_LOG) {
                FuLu.LOGGER.info("[sword] lived {} ticks without hitting, discarding", this.life);
            }
            this.discard();
            return;
        }

        // 停在原地（没有目标可追）时播 idle；正常飞行时 fly
        setAnim(this.cachedTarget == null ? ANIM_IDLE : ANIM_FLY);

        // ---- 朝目标飞 ----
        Vec3 here = this.position();
        Vec3 to = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
        Vec3 delta = to.subtract(here);
        double distance = delta.length();
        if (distance <= HIT_DISTANCE) {
            hit(server, target);
            return;
        }

        Vec3 step = delta.normalize().scale(Math.min(SPEED, distance));
        Vec3 next = here.add(step);

        // 撞到方块就消失（用户要求）。这里自己判，不依赖原版的碰撞：
        // 剑的碰撞箱很小、飞得又直，直接看下一格是不是实心方块最清楚，
        // 而且不会因为"擦着墙飞"被弹开。
        if (isBlocked(server, next)) {
            if (DEBUG_LOG) {
                FuLu.LOGGER.info("[sword] hit a block at {}, discarding", next);
            }
            this.discard();
            return;
        }

        this.setPos(next.x, next.y, next.z);
        faceTowards(here, next);
        // 符文环绕：每刻沿剑轴从剑尾到剑锋摆一圈符印，整体随时间旋转
        emitRunes(server);
    }

    /**
     * 符文环绕：从**剑尾**到**剑锋**的一圈螺旋，而且一直转。
     *
     * <p>做法是纯几何的：剑的指向就是它的朝向（{@link #faceTowards} 刚算过），
     * 于是可以拿它当轴，沿轴取若干个位置，每个位置再按一个随位置增大的角度
     * 往侧面偏一点 —— 连起来就是螺旋。
     *
     * <p>"一直转"来自两处：整个螺旋的角度每刻加上 {@code phase}（随时间前进），
     * 每个符文的参数里也含 {@code phase} —— 所以符印既绕圈转、又沿着剑身往剑锋流。
     */
    private void emitRunes(ServerLevel server) {
        if (RUNE_COUNT <= 0) {
            return;
        }
        // 剑轴（从柄指向剑锋）= 实体朝向的前向量
        Vec3 axis = calculateViewVector(this.getXRot(), this.getYRot());
        // 两个垂直于剑轴的基向量，用来把符文甩到侧面
        Vec3 side = axis.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (side.lengthSqr() < 1.0E-4D) {
            side = axis.cross(new Vec3(1.0D, 0.0D, 0.0D));
        }
        side = side.normalize();
        Vec3 up = side.cross(axis).normalize();

        // 每刻往前走一格；走满一圈就是"转了一圈"
        double phase = (double) (this.level().getGameTime() % RUNE_PERIOD_TICKS) / RUNE_PERIOD_TICKS;
        Vec3 centre = this.position();

        for (int i = 0; i < RUNE_COUNT; i++) {
            // t: 0 = 剑尾，1 = 剑锋。加上 phase 让它往剑锋方向流动
            double t = ((double) i / RUNE_COUNT + phase) % 1.0D;
            // 沿剑轴的位移：剑的中段在实体位置，所以从 -BLADE/2 到 +BLADE/2
            double along = (t - 0.5D) * BLADE_LENGTH;
            // 绕轴的角度：t 越大角度越大 = 螺旋；再叠加 phase 让它整体转
            double angle = t * RUNE_SPIRALS * Math.PI * 2.0D + phase * Math.PI * 2.0D;
            double r = RUNE_RADIUS;
            Vec3 p = centre
                    .add(axis.scale(along))
                    .add(side.scale(Math.cos(angle) * r))
                    .add(up.scale(Math.sin(angle) * r));
            server.sendParticles(ModParticles.RUNE_MOTE.get(), p.x, p.y, p.z, 1,
                    0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    /** 下一格是不是实心方块（留一点余量，免得贴着地面飞就自爆）。 */
    private boolean isBlocked(ServerLevel server, Vec3 next) {
        BlockPos at = BlockPos.containing(next);
        if (!isSolidAt(server, at)) {
            return false;
        }
        // 只挡"明显埋在方块里"的情况：剑的位置和它上方一点都在实心方块内部
        return isSolidAt(server, BlockPos.containing(next.x, next.y + 0.3D, next.z));
    }

    private static boolean isSolidAt(ServerLevel server, BlockPos pos) {
        return server.getBlockState(pos).isSolidRender(server, pos);
    }

    /**
     * 命中：扣血 + 爆发粒子 + 剑砍声，然后自己消失。
     *
     * <p><b>每把剑各打各的 5 点</b>，不是"一轮加起来 5 点"。这里有个原版的坑：
     * 生物受伤后有 **10 刻无敌帧**（{@code invulnerableTime}），同一轮齐射的十几把剑
     * 几乎同刻到达，所以除了第一把，其余的伤害会被吃掉 ——
     * 玩家看到的就是"十把剑只掉了 5 点血"。
     *
     * <p>所以打之前先把目标的无敌帧清零：这一把的 5 点一定算数。
     * （只清这个目标、只在这一刻；飞剑命中后本来就消失，不存在刷伤害。）
     */
    private void hit(ServerLevel server, Entity target) {
        if (target instanceof LivingEntity living) {
            living.invulnerableTime = 0;
        }
        // 阵魔：飞剑伤害减半（用户规格）
            float swordDmg = (target instanceof com.lengyang.fulu.entity.FormationDemonEntity) ? this.damage * 0.5F : this.damage;
            target.hurt(server.damageSources().magic(), swordDmg);
        emitBurst(server, target);
        server.playSound(null, BlockPos.containing(target.position()), HIT_SOUND,
                SoundSource.PLAYERS, 0.9F, 1.0F);
        if (DEBUG_LOG) {
            FuLu.LOGGER.info("[sword] hit entity {} for {} (health now {})",
                    target.getId(), this.damage,
                    target instanceof net.minecraft.world.entity.LivingEntity living
                            ? living.getHealth() : -1.0F);
        }
        // 切到 hit 动画：剑身抖一下再消失。伤害是**立刻**结算的，
        // 这 6 刻只是让动画播完（期间不再移动、不再造成伤害）。
        this.dying = true;
        this.entityData.set(DATA_ANIM, ANIM_HIT);
        this.entityData.set(DATA_HIT_AGE, 0);
    }

    /** 没目标时找最近的怪（这样手动 summon 出来的飞剑也会自己去打）。 */
    @Nullable
    private Mob findNearestMonster(ServerLevel server) {
        AABB box = this.getBoundingBox().inflate(SEEK_RANGE);
        List<Mob> monsters = server.getEntitiesOfClass(Mob.class, box);
        Mob nearest = null;
        double best = Double.MAX_VALUE;
        for (Mob monster : monsters) {
            if (!FormationTarget.isTarget(monster)) {
                continue;
            }
            double d = monster.distanceToSqr(this);
            if (d < best) {
                best = d;
                nearest = monster;
            }
        }
        return nearest;
    }

    /** 让剑头指向飞行方向（模型里刀刃是 +Y，渲染器再把它躺下）。 */
    private void faceTowards(Vec3 from, Vec3 to) {
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double dz = to.z - from.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx))) - 90.0F;
        float pitch = (float) (-Math.toDegrees(Math.atan2(dy, horizontal)));
        this.setYRot(yaw);
        this.setXRot(pitch);
        this.yRotO = yaw;
        this.xRotO = pitch;
    }


    /**
     * 命中爆发：以目标为中心炸开一圈**金色**小星星（用的是"命中爆发"粒子）。
     */
    private void emitBurst(ServerLevel server, Entity target) {
        double cx = target.getX();
        double cy = target.getY() + target.getBbHeight() * 0.5D;
        double cz = target.getZ();
        for (int i = 0; i < BURST_PARTICLES; i++) {
            double angle = (Math.PI * 2.0D) * i / BURST_PARTICLES;
            server.sendParticles(ModParticles.SWORD_BURST.get(),
                    cx + Math.cos(angle) * 0.45D,
                    cy + (server.random.nextDouble() - 0.5D) * 0.5D,
                    cz + Math.sin(angle) * 0.45D,
                    1, 0.15D, 0.15D, 0.15D, 0.18D);
        }
        if (DEBUG_LOG) {
            FuLu.LOGGER.info("[sword] hit burst: {} particles on entity {}",
                    BURST_PARTICLES, target.getId());
        }
    }

    /** 和方块实体里那个开关同名：自动化测试靠它拿到飞剑的行为日志。 */
    private static final boolean DEBUG_LOG = false;
}
