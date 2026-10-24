package com.lengyang.fulu.refining;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.lengyang.fulu.FuLu;
import com.lengyang.fulu.registry.ModMobEffects;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * 施法（带**前摇与打断**）。
 *
 * <p>流程：
 * <pre>
 *   按 Z  →  beginCast：校验（匣在不在 / 这颗妖丹在不在 / 冷却）
 *        →  0.5 秒读条：头顶阵纹粒子、移速 -50%、手臂前推、开始音效
 *        →  读完 → finishCast：真正生效
 *        读条中被打 → interrupt：失败、短冷却、提示"施法被打断"
 * </pre>
 *
 * <p>为什么加前摇：按了秒出等于没有风险 —— 有了 0.5 秒，对手有反制窗口，
 * 自己也要考虑站位，这才有博弈。
 */
public final class RefiningCast {

    /** 读条时长（刻）：0.5 秒。 */
    public static final int CAST_TICKS = 10;
    /** 被打断后的短冷却（刻）：3 秒。 */
    private static final int INTERRUPT_COOLDOWN = 60;

    /**
     * 玩家 -> 每个**技能**的冷却结束时间。
     *
     * <p>下标 = {@code 格子 * 8 + 技能}，所以同一颗妖丹的不同技能**冷却互不影响**。
     */
    private static final Map<UUID, long[]> COOLDOWNS = new HashMap<>();

    /** 技能冷却数组的下标。 */
    public static int cdIndex(int slot, int sub) {
        return Math.max(0, Math.min(5, slot)) * 8 + Math.max(0, Math.min(7, sub));
    }
    /** 正在读条的玩家 -> 读条状态。 */
    private static final Map<UUID, Casting> CASTING = new HashMap<>();

    /** 读条减速用的修饰符 id。 */
    private static final UUID CAST_SLOW_ID = UUID.fromString("5f0c1b33-1f2e-4a55-9a60-2f7b8c9d1e22");

    /** 末影能力的作用距离（格）。 */
    private static final double SWAP_RANGE = 30.0D;

    /** 一次读条的状态。 */
    public record Casting(int slot, EssenceKind kind, int ticksLeft, int sub) {
    }

    /**
     * 谁靠「恶魂飞行」在飞（用于开关与收尾）。
     *
     * <p>用户要求：飞行不要再计时，改成**开关** —— 按一次开，再按一次关。
     */
    private static final java.util.Set<UUID> FLIGHT_ON = new java.util.HashSet<>();

    private RefiningCast() {
    }

    // ================================================================ 读条

    public static boolean isCasting(ServerPlayer player) {
        return CASTING.containsKey(player.getUUID());
    }

    /** 开始读条（按 Z 触发）。 */
    public static boolean beginCast(ServerPlayer player, int slot) {
        return beginCast(player, slot, 0);
    }

    public static boolean beginCast(ServerPlayer player, int slot, int sub) {
        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }
        if (CASTING.containsKey(player.getUUID())) {
            return false;
        }
        ItemStack box = findBox(player);
        if (box.isEmpty()) {
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable("msg.fulu.wheel.nobox"), true);
            return false;
        }
        EssenceBoxContainer container = new EssenceBoxContainer(box);
        if (slot < 0 || slot >= EssenceBoxContainer.SIZE) {
            return false;
        }
        EssenceKind kind = EssenceKind.of(container.getItem(slot));
        if (kind == null) {
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable("msg.fulu.cast.empty"), true);
            return false;
        }
        if (remainingTicks(player, slot, sub) > 0) {
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable("msg.fulu.cast.cooldown",
                            (remainingTicks(player, slot, sub) + 19) / 20), true);
            return false;
        }

        CASTING.put(player.getUUID(), new Casting(slot, kind, kind.windupTicks(), sub));

        // **法则压制**：只要玩家在囚笼里释放任何技能，整笼立刻湮灭
        com.lengyang.fulu.entity.WebPrisonEntity.annihilateAt(level, player.position());

        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null && speed.getModifier(CAST_SLOW_ID) == null) {
            speed.addTransientModifier(new AttributeModifier(
                    CAST_SLOW_ID, "fulu_casting", kind.castSlow(),
                    AttributeModifier.Operation.MULTIPLY_TOTAL));
        }

        // 手臂前推（原版挥手）+ 起手音效
        player.swing(InteractionHand.MAIN_HAND, true);
        if (kind == EssenceKind.WITHER_SKELETON) {
            // 剑修：起手先压低身体（用小幅度击退表现"蓄势"）
            player.push(0.0D, -0.02D, 0.0D);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.PLAYERS, 0.7F, 1.4F);

        // 脚下爆开一圈对应颜色的阵法光环
        ring(level, player, kind, 2.2D);

        if (kind == EssenceKind.WITHER_SKELETON) {
            // 【阿鼻地狱】聚气：暗红大阵 + 地狱钟声（凋灵出场音）
            level.playSound(null, player.blockPosition(), SoundEvents.WITHER_SPAWN,
                    SoundSource.PLAYERS, 1.6F, 0.45F);
            for (int i = 0; i < 90; i++) {
                double a = Math.PI * 2.0D * i / 90.0D;
                level.sendParticles(new DustParticleOptions(
                                new Vector3f(0.45F, 0.03F, 0.05F), 1.8F),
                        player.getX() + Math.cos(a) * 4.5D, player.getY() + 0.1D,
                        player.getZ() + Math.sin(a) * 4.5D, 1, 0.0D, 0.02D, 0.0D, 0.0D);
            }
        }

        FuLu.LOGGER.info("[cast] begin slot={} kind={} windup={}", slot, kind, kind.windupTicks());
        return true;
    }

    /** 每刻推进读条。 */
    public static void tickCasting(ServerPlayer player) {
        Casting c = CASTING.get(player.getUUID());
        if (c == null) {
            return;
        }
        ServerLevel level = (ServerLevel) player.level();

        double y = player.getY() + player.getBbHeight() + 0.35D;
        for (int i = 0; i < 3; i++) {
            double a = (level.getGameTime() * 0.6D) + i * (Math.PI * 2.0D / 3.0D);
            level.sendParticles(new DustParticleOptions(
                            new Vector3f(c.kind().red(), c.kind().green(), c.kind().blue()), 1.0F),
                    player.getX() + Math.cos(a) * 0.45D, y + Math.sin(a * 1.7D) * 0.12D,
                    player.getZ() + Math.sin(a) * 0.45D, 1, 0.0D, 0.02D, 0.0D, 0.0D);
        }

        if (c.kind() == EssenceKind.WITHER_SKELETON) {
            // 聚气期间：脚下暗红阵纹持续旋转 + 头顶落黑烟（压迫感）
            for (int i = 0; i < 4; i++) {
                double a = level.getGameTime() * 0.25D + i * (Math.PI / 2.0D);
                level.sendParticles(new DustParticleOptions(
                                new Vector3f(0.45F, 0.03F, 0.05F), 1.6F),
                        player.getX() + Math.cos(a) * 4.5D, player.getY() + 0.1D,
                        player.getZ() + Math.sin(a) * 4.5D, 1, 0.0D, 0.02D, 0.0D, 0.0D);
            }
            // 头顶的黑烟与白色闪电粒子已按用户要求删除
        }
        if (c.ticksLeft() > 1) {
            CASTING.put(player.getUUID(),
                    new Casting(c.slot(), c.kind(), c.ticksLeft() - 1, c.sub()));
            return;
        }
        CASTING.remove(player.getUUID());
        clearSlow(player);
        finishCast(player, level, c.slot(), c.kind(), c.sub());
    }

    /** 被打断。 */
    public static void interrupt(ServerPlayer player) {
        Casting c = CASTING.remove(player.getUUID());
        if (c == null) {
            return;
        }
        clearSlow(player);
        long[] arr = COOLDOWNS.computeIfAbsent(player.getUUID(), k -> new long[48]);
        int idx = cdIndex(c.slot(), c.sub());
        arr[idx] = player.level().getGameTime() + INTERRUPT_COOLDOWN;
        com.lengyang.fulu.network.ModNetwork.CHANNEL.send(
                net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                new com.lengyang.fulu.network.CooldownSyncPacket(c.slot(), c.sub(),
                        INTERRUPT_COOLDOWN));
        player.displayClientMessage(
                net.minecraft.network.chat.Component.translatable("msg.fulu.cast.interrupted"), true);
        if (player.level() instanceof ServerLevel level) {
            level.playSound(null, player.blockPosition(), SoundEvents.SHIELD_BLOCK,
                    SoundSource.PLAYERS, 1.0F, 0.7F);
            level.sendParticles(ParticleTypes.SMOKE,
                    player.getX(), player.getY() + 1.2D, player.getZ(), 12, 0.3D, 0.3D, 0.3D, 0.02D);
        }
        FuLu.LOGGER.info("[cast] interrupted slot={}", c.slot());
    }

    private static void clearSlow(ServerPlayer player) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(CAST_SLOW_ID);
        }
    }

    private static void finishCast(ServerPlayer player, ServerLevel level, int slot,
                                   EssenceKind kind, int sub) {
        if (!doCast(level, player, kind, sub)) {
            return;
        }
        long[] arr = COOLDOWNS.computeIfAbsent(player.getUUID(), k -> new long[48]);
        // **炼妖阵内冷却减半**；**按技能**各算各的
        int cdTicks = (int) Math.max(1,
                kind.cooldownTicks(sub) * FormationCastBonus.cooldownMultiplier(player));
        arr[cdIndex(slot, sub)] = level.getGameTime() + cdTicks;
        level.playSound(null, player.blockPosition(), soundOf(kind), SoundSource.PLAYERS, 1.0F, 1.0F);
        com.lengyang.fulu.network.ModNetwork.CHANNEL.send(
                net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                new com.lengyang.fulu.network.CooldownSyncPacket(slot, sub, cdTicks));
    }

    public static long remainingTicks(ServerPlayer player, int slot) {
        return remainingTicks(player, slot, 0);
    }

    /** 某个**技能**剩余冷却（刻）。 */
    public static long remainingTicks(ServerPlayer player, int slot, int sub) {
        long[] arr = COOLDOWNS.get(player.getUUID());
        if (arr == null) {
            return 0L;
        }
        return Math.max(0L, arr[cdIndex(slot, sub)] - player.level().getGameTime());
    }

    public static boolean hasEssence(net.minecraft.world.entity.player.Player player,
                                     EssenceKind kind) {
        if (player == null || kind == null) {
            return false;
        }
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof com.lengyang.fulu.item.EssenceBoxItem) {
                EssenceBoxContainer container = new EssenceBoxContainer(stack);
                for (int i = 0; i < EssenceBoxContainer.SIZE; i++) {
                    if (kind == EssenceKind.of(container.getItem(i))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static int slotOfEssence(ServerPlayer player, EssenceKind kind) {
        ItemStack box = findBox(player);
        if (box.isEmpty() || kind == null) {
            return -1;
        }
        EssenceBoxContainer container = new EssenceBoxContainer(box);
        for (int i = 0; i < EssenceBoxContainer.SIZE; i++) {
            if (kind == EssenceKind.of(container.getItem(i))) {
                return i;
            }
        }
        return -1;
    }

    public static ItemStack findBox(net.minecraft.world.entity.player.Player player) {
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof com.lengyang.fulu.item.EssenceBoxItem) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static net.minecraft.sounds.SoundEvent soundOf(EssenceKind kind) {
        return switch (kind) {
            case ENDERMAN -> SoundEvents.ENDERMAN_TELEPORT;
            case BLAZE -> SoundEvents.BLAZE_SHOOT;
            case CREEPER -> SoundEvents.CREEPER_PRIMED;
            case SKELETON -> SoundEvents.ARROW_SHOOT;
            case SPIDER -> SoundEvents.LLAMA_SPIT;
            case ZOMBIE -> SoundEvents.ZOMBIE_ATTACK_IRON_DOOR;
            case WITCH -> SoundEvents.WITCH_THROW;
            case SLIME -> SoundEvents.SLIME_BLOCK_PLACE;
            case WITHER_SKELETON -> SoundEvents.WITHER_SHOOT;
            case GHAST -> SoundEvents.GHAST_SCREAM;
        };
    }

    private static void ring(ServerLevel level, ServerPlayer player, EssenceKind kind, double r) {
        DustParticleOptions dust = new DustParticleOptions(
                new Vector3f(kind.red(), kind.green(), kind.blue()), 1.3F);
        for (int i = 0; i < 40; i++) {
            double a = Math.PI * 2.0D * i / 40.0D;
            level.sendParticles(dust,
                    player.getX() + Math.cos(a) * r, player.getY() + 0.12D,
                    player.getZ() + Math.sin(a) * r, 1, 0.0D, 0.02D, 0.0D, 0.0D);
        }
    }

    // ================================================================ 各妖丹效果

    public static boolean swapWithBlock(ServerLevel level, ServerPlayer player,
                                        net.minecraft.core.BlockPos target) {
        net.minecraft.core.BlockPos mine = net.minecraft.core.BlockPos.containing(
                player.getX(), player.getY(), player.getZ());
        net.minecraft.world.level.block.state.BlockState state = level.getBlockState(target);

        boolean canMove = !level.getBlockState(mine).isAir() || level.getBlockState(mine).canBeReplaced();
        boolean indestructible = state.getDestroySpeed(level, target) < 0.0F;

        if (target.equals(mine) || indestructible || !canMove) {
            level.sendParticles(ParticleTypes.PORTAL,
                    player.getX(), player.getY() + 1.0D, player.getZ(), 30, 0.4D, 0.6D, 0.4D, 0.4D);
            player.teleportTo(target.getX() + 0.5D, target.getY() + 1.0D, target.getZ() + 0.5D);
            player.fallDistance = 0.0F;
            return true;
        }

        net.minecraft.nbt.CompoundTag beTag = null;
        var be = level.getBlockEntity(target);
        if (be != null) {
            beTag = be.saveWithFullMetadata();
        }
        level.setBlockAndUpdate(mine, state);
        if (beTag != null) {
            var newBe = level.getBlockEntity(mine);
            if (newBe != null) {
                newBe.load(beTag);
                newBe.setChanged();
            }
        }
        level.removeBlock(target, false);

        Vec3 from = player.position();
        level.sendParticles(ParticleTypes.PORTAL,
                from.x, from.y + 1.0D, from.z, 30, 0.4D, 0.6D, 0.4D, 0.4D);
        player.teleportTo(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D);
        player.fallDistance = 0.0F;
        level.sendParticles(ParticleTypes.PORTAL,
                target.getX() + 0.5D, target.getY() + 1.0D, target.getZ() + 0.5D,
                30, 0.4D, 0.6D, 0.4D, 0.4D);
        return true;
    }

    private static boolean doCast(ServerLevel level, ServerPlayer player, EssenceKind kind,
                                  int sub) {
        switch (kind) {
            case ENDERMAN -> {
                Vec3 eye = player.getEyePosition();
                Vec3 look = player.getLookAngle();
                // **咫尺天涯阵内距离翻倍**
                double range = SWAP_RANGE * FormationCastBonus.teleportMultiplier(player);
                Vec3 end = eye.add(look.scale(range));
                net.minecraft.world.phys.AABB box =
                        player.getBoundingBox().expandTowards(look.scale(SWAP_RANGE)).inflate(1.0D);

                net.minecraft.world.phys.EntityHitResult ehr =
                        net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(
                                player, eye, end, box,
                                e -> e instanceof LivingEntity && e != player && e.isAlive(),
                                SWAP_RANGE * SWAP_RANGE);
                if (ehr != null && ehr.getEntity() instanceof LivingEntity other) {
                    // **虚空放逐**：目标血量 < 20% 且是怪物 -> 直接抹除，玩家回血
                    if (other instanceof net.minecraft.world.entity.monster.Enemy
                            && other.getMaxHealth() < 200.0F
                            && other.getHealth() < other.getMaxHealth() * 0.20F) {
                        level.sendParticles(ParticleTypes.PORTAL,
                                other.getX(), other.getY() + other.getBbHeight() * 0.5D,
                                other.getZ(), 60, 0.4D, 0.6D, 0.4D, 0.15D);
                        level.playSound(null, other.blockPosition(),
                                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.5F, 0.5F);
                        other.invulnerableTime = 0;
                        other.hurt(level.damageSources().magic(), 10000.0F);
                        player.heal(4.0F);
                        player.displayClientMessage(
                                net.minecraft.network.chat.Component
                                        .translatable("msg.fulu.banish"), true);
                        com.lengyang.fulu.FuLu.LOGGER.info("[cast] banished {}", other.getType());
                        return true;
                    }
                    Vec3 mine = player.position();
                    Vec3 theirs = other.position();
                    level.sendParticles(ParticleTypes.PORTAL,
                            mine.x, mine.y + 1.0D, mine.z, 30, 0.4D, 0.6D, 0.4D, 0.4D);
                    player.teleportTo(theirs.x, theirs.y, theirs.z);
                    other.teleportTo(mine.x, mine.y, mine.z);
                    // **在原地留下假身**吸引仇恨（用户要求）
                    FormationCastBonus.leaveDecoyAt(level, mine);
                    level.sendParticles(ParticleTypes.PORTAL,
                            theirs.x, theirs.y + 1.0D, theirs.z, 30, 0.4D, 0.6D, 0.4D, 0.4D);
                    player.fallDistance = 0.0F;
                    return true;
                }

                BlockHitResult hit = level.clip(new ClipContext(eye, end,
                        ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
                if (hit.getType() == HitResult.Type.BLOCK) {
                    Vec3 decoyPos = player.position();      // 换方块前记下原位
                    boolean ok = swapWithBlock(level, player, hit.getBlockPos());
                    if (ok) {
                        FormationCastBonus.leaveDecoyAt(level, decoyPos);
                    }
                    return ok;
                }
                player.displayClientMessage(
                        net.minecraft.network.chat.Component.translatable("msg.fulu.cast.notarget"),
                        true);
                return false;
            }
            case BLAZE -> {
                Vec3 look = player.getLookAngle();
                // **飞行极慢**（0.5 格/刻），但会自动追踪
                EssenceFireball ball = new EssenceFireball(level, player,
                        look.x * 0.5D, look.y * 0.5D, look.z * 0.5D);
                // **飞剑阵内**：爆炸时额外放出飞剑（标记存在火球身上）
                if (FormationCastBonus.inSwordFormation(player)) {
                    ball.getPersistentData().putBoolean("FuluSwordVolley", true);
                }
                ball.setPos(player.getX() + look.x, player.getEyeY() - 0.1D + look.y,
                        player.getZ() + look.z);
                level.addFreshEntity(ball);
                return true;
            }
            case CREEPER -> {
                // 自爆：不破坏地形。
                // **替身阵内不伤自己**（用户要求）：用不指定 owner 的爆炸 -> 玩家不受中心伤害。
                if (FormationCastBonus.inStandInFormation(player)) {
                    level.explode(null, player.getX(), player.getY() + 0.5D, player.getZ(),
                            3.0F, net.minecraft.world.level.Level.ExplosionInteraction.NONE);
                    player.displayClientMessage(
                            net.minecraft.network.chat.Component.translatable("msg.fulu.blast.safe"),
                            true);
                } else {
                    level.explode(player, player.getX(), player.getY() + 0.5D, player.getZ(),
                            3.0F, net.minecraft.world.level.Level.ExplosionInteraction.NONE);
                }
                return true;
            }
            case ZOMBIE -> {
                // **绝仙瘴气**：落在准星/前方地面（不再固定脚下，否则怪离得远就完全没效果）
                Vec3 target = MeteorStrike.pickTarget(player);
                MiasmaField.spawn(level, target);
                return true;
            }
            case SKELETON -> {
                var arrow = new net.minecraft.world.entity.projectile.Arrow(level, player);
                Vec3 look = player.getLookAngle();
                arrow.shoot(look.x, look.y, look.z, 2.4F, 0.5F);
                arrow.setPos(player.getX(), player.getEyeY() - 0.1D, player.getZ());
                level.addFreshEntity(arrow);
                return true;
            }
            case GHAST -> {
                if (sub == 1) {
                    // **恶魂飞行：开关**（再按一次关掉）
                    if (player.isCreative() || player.isSpectator()) {
                        player.displayClientMessage(
                                net.minecraft.network.chat.Component
                                        .translatable("msg.fulu.flight.creative"), true);
                        return false;
                    }
                    boolean on = FLIGHT_ON.contains(player.getUUID());
                    if (on) {
                        FLIGHT_ON.remove(player.getUUID());
                        player.getAbilities().mayfly = false;
                        player.getAbilities().flying = false;
                        player.onUpdateAbilities();
                        player.displayClientMessage(
                                net.minecraft.network.chat.Component
                                        .translatable("msg.fulu.flight.off"), true);
                    } else {
                        FLIGHT_ON.add(player.getUUID());
                        player.getAbilities().mayfly = true;
                        player.getAbilities().flying = true;
                        player.onUpdateAbilities();
                        player.displayClientMessage(
                                net.minecraft.network.chat.Component
                                        .translatable("msg.fulu.flight.on"), true);
                    }
                    level.playSound(null, player.blockPosition(), SoundEvents.PHANTOM_FLAP,
                            SoundSource.PLAYERS, 1.2F, 1.2F);
                    return true;
                }
                // 陨石：锁定落点 -> 1.5 秒预警 -> 砸下（见 MeteorStrike）
                MeteorStrike.cast(player);
                return true;
            }
            case WITHER_SKELETON -> {
                // 「光翼剑客」终极技能：2 秒五段式（蓄力→展翼→三连斩→飞行→收拢）
                player.swing(InteractionHand.MAIN_HAND, true);
                LightWingUltimate.start(player);
                return true;
            }
            case SLIME -> {
                // 视线前方找地面，在那里铺一片泥沼
                Vec3 eye = player.getEyePosition();
                Vec3 look = player.getLookAngle();
                Vec3 end = eye.add(look.scale(6.0D));
                var hit = level.clip(new ClipContext(eye, end,
                        ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
                Vec3 center = hit.getType() == HitResult.Type.BLOCK
                        ? hit.getLocation()
                        : player.position().add(look.x * 3.0D, 0.0D, look.z * 3.0D);
                SwampZoneHandler.spawn(level, center);
                return true;
            }
            case WITCH -> {
                // **诡计毒瓶**：抛出滞留型毒瓶，药水里带"每次 1.5 伤害、每秒 4 次"的诡计毒
                var potion = new ItemStack(net.minecraft.world.item.Items.LINGERING_POTION);
                net.minecraft.world.item.alchemy.PotionUtils.setCustomEffects(potion,
                        java.util.List.of(new MobEffectInstance(
                                ModMobEffects.WITCH_POISON.get(), 350, 0)));
                potion.getOrCreateTag().putInt("CustomPotionColor", 0x3FA34D);

                // 用专用实体：落地爆发 + 自建大毒云（见 WitchPotionEntity）
                var thrown = new WitchPotionEntity(level, player);
                thrown.setItem(potion);
                Vec3 look = player.getLookAngle();
                thrown.shoot(look.x, look.y + 0.12D, look.z, 1.6F, 0.6F);
                level.addFreshEntity(thrown);
                return true;
            }
            case SPIDER -> {
                if (sub == 0) {
                    // **盘丝天牢**：在准星/前方地面张开半径 5 的蛛网牢笼
                    Vec3 target = MeteorStrike.pickTarget(player);
                    var web = new com.lengyang.fulu.entity.WebPrisonEntity(
                            com.lengyang.fulu.registry.ModEntities.WEB_PRISON.get(), level);
                    web.setPos(target.x, target.y + 0.05D, target.z);
                    level.addFreshEntity(web);

                    player.swing(InteractionHand.MAIN_HAND, true);
                    level.playSound(null, player.blockPosition(),
                            SoundEvents.LLAMA_SPIT, SoundSource.PLAYERS, 1.5F, 0.8F);
                    level.playSound(null, player.blockPosition(),
                            SoundEvents.LEASH_KNOT_PLACE, SoundSource.PLAYERS, 1.2F, 0.7F);
                    com.lengyang.fulu.FuLu.LOGGER.info("[web] cast at {}", target);
                    return true;
                }
                var effect = ModMobEffects.SPIDER_WALK.get();
                MobEffectInstance cur = player.getEffect(effect);
                if (cur != null) {
                    player.removeEffect(effect);
                    player.displayClientMessage(
                            net.minecraft.network.chat.Component.translatable("msg.fulu.spider.off"),
                            true);
                } else {
                    player.addEffect(new MobEffectInstance(effect, -1, 0, false, false, true));
                    player.displayClientMessage(
                            net.minecraft.network.chat.Component.translatable("msg.fulu.spider.on"),
                            true);
                }
                level.sendParticles(new DustParticleOptions(
                                new Vector3f(0.55F, 0.35F, 0.70F), 1.1F),
                        player.getX(), player.getY() + 0.6D, player.getZ(),
                        26, 0.7D, 0.5D, 0.7D, 0.0D);
                return true;
            }
        }
        return false;
    }
}
