package com.lengyang.fulu.command;

import java.util.List;

import com.lengyang.fulu.FuLu;
import com.lengyang.fulu.codex.FormationCodex;
import com.lengyang.fulu.standin.StandInData;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * {@code /fulu codex ...} —— 图鉴的"后端"。
 *
 * <p>为什么要有它：原版聊天的**点击事件只能执行一条命令**，
 * 所以"点某个阵法看详情"这条路一定要有个命令可以点。有了它，
 * 就不必自己写一个 GUI 界面；而且命令本身也能在聊天框里直接敲、能被函数调用，
 * 于是这套逻辑在**没有玩家**的自动化测试里也验得了。
 *
 * <pre>
 *   /fulu codex                     列表（用命令执行者的解锁进度）
 *   /fulu codex list [名字]          同上，可以指定看谁
 *   /fulu codex show &lt;id&gt; [名字]     某个阵法的详情
 * </pre>
 *
 * <p>名字为什么是普通字符串而不是"在线玩家"参数：解锁进度存在计分板里，
 * 而计分板**认识离线玩家**（这正是它比玩家 NBT 好用的地方）。
 * 用字符串就能查任何人，测试里也不用真的连一个玩家进来。
 */
@Mod.EventBusSubscriber(modid = FuLu.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CodexCommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("fulu")
                        // 调试用：让最近的阵魔立刻对你发动处决（看必杀效果）
                        .then(Commands.literal("exec")
                                .executes(ctx -> forceExecution(ctx.getSource())))
                        // 调试用：在玩家面前 4 格直接生成一只阵魔（排查"看不见"最省事的方式)
                        .then(Commands.literal("demon")
                                .executes(ctx -> spawnDemon(ctx.getSource())))
                        // 调试用：直接设次数盾点数，方便看绿心（0~20，4 点一颗心）
                        .then(Commands.literal("shield")
                                .then(Commands.argument("points", IntegerArgumentType.integer(0, 20))
                                        .executes(ctx -> setShield(ctx.getSource(),
                                                IntegerArgumentType.getInteger(ctx, "points")))))
                        .then(Commands.literal("codex")
                                .executes(ctx -> list(ctx.getSource(), null))
                                .then(Commands.literal("list")
                                        .executes(ctx -> list(ctx.getSource(), null))
                                        .then(Commands.argument("name", StringArgumentType.word())
                                                .executes(ctx -> list(ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "name")))))
                                .then(Commands.literal("find")
                                        .then(Commands.argument("query", StringArgumentType.word())
                                                .executes(ctx -> find(ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "query")))))
                                .then(Commands.literal("show")
                                        .then(Commands.argument("id", StringArgumentType.word())
                                                .executes(ctx -> show(ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "id"), null))
                                                .then(Commands.argument("name", StringArgumentType.word())
                                                        .executes(ctx -> show(ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "id"),
                                                                StringArgumentType.getString(ctx, "name"))))))));
    }

    /**
     * {@code /fulu shield <0..20>} —— 直接设次数盾点数（**调试用**）。
     *
     * <p>设完会顺手同步给客户端，所以聊天栏回完话、HUD 上就会多出对应数量的绿心：
     * 20 = 5 颗满心，12 = 3 颗满，14 = 3 满 + 1 半。
     */
    /**
     * {@code /fulu demon} —— 在玩家**正前方 4 格**生成阵魔，并让它面朝玩家。
     *
     * <p>为什么不用 {@code /summon}：原生 summon 会把实体放在玩家脚下，
     * 和玩家碰撞箱重叠时容易被原版判定为无效位置从而立刻消失（日志里能看到 DISCARDED）。
     * 放在面前 4 格既避免碰撞，也保证玩家一转头就能看到。
     */
    private static int spawnDemon(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("这条命令要在游戏内由一个玩家执行"));
            return 0;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            source.sendFailure(Component.literal("只能在服务端世界使用"));
            return 0;
        }
        com.lengyang.fulu.entity.FormationDemonEntity demon =
                com.lengyang.fulu.registry.ModEntities.FORMATION_DEMON.get().create(level);
        if (demon == null) {
            source.sendFailure(Component.literal("阵魔创建失败"));
            return 0;
        }
        net.minecraft.world.phys.Vec3 look = player.getLookAngle();
        double x = player.getX() + look.x * 4.0D;
        double z = player.getZ() + look.z * 4.0D;
        double y = player.getY();
        demon.moveTo(x, y, z, player.getYRot() + 180.0F, 0.0F);
        demon.setYHeadRot(player.getYRot() + 180.0F);
        level.addFreshEntity(demon);

        source.sendSuccess(() -> Component.literal(
                "阵魔已生成在面前 4 格：" + (int) x + ", " + (int) y + ", " + (int) z), true);
        return 1;
    }

    /**
     * {@code /fulu exec} —— 让 64 格内最近的一只阵魔**立刻对你发动冲刺处决**。
     *
     * <p>由玩家自己执行（处决的受害者就是自己），这样不用选目标；
     * 想观察效果就先把血量补满、并准备好保命物品。
     */
    private static int forceExecution(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("这条命令要在游戏内由一个玩家执行"));
            return 0;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            source.sendFailure(Component.literal("只能在服务端世界使用"));
            return 0;
        }
        com.lengyang.fulu.entity.FormationDemonEntity demon =
                level.getEntitiesOfClass(com.lengyang.fulu.entity.FormationDemonEntity.class,
                        player.getBoundingBox().inflate(64.0D)).stream().findFirst().orElse(null);
        if (demon == null) {
            source.sendFailure(Component.literal("附近 64 格内没有阵魔 —— 先输入 /fulu demon"));
            return 0;
        }
        demon.forceFullDash(player);
        source.sendSuccess(() -> Component.literal(
                "阵魔已放到你面前 14 格，正在冲刺过来 —— 看它冲到你面前再挑飞插旗"), true);
        return 1;
    }

    private static int setShield(CommandSourceStack source, int points) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("这条命令要在游戏内由一个玩家执行"));
            return 0;
        }
        StandInData.setHitShield(player, points);      // 内部会 sync 给客户端
        source.sendSuccess(() -> Component.literal(
                "次数盾点数 = " + points + "（4 点一颗心：满 20 点是 5 颗满心）"), false);
        return points;
    }

    private static int list(CommandSourceStack source, String name) {
        ServerLevel level = source.getLevel();
        String who = name != null ? name : selfName(source);
        if (who == null) {
            source.sendFailure(Component.translatable("codex.fulu.need_player"));
            return 0;
        }
        List<Component> lines = FormationCodex.buildList(level, who);
        for (Component line : lines) {
            source.sendSuccess(() -> line, false);
        }
        return lines.size();
    }

    private static int find(CommandSourceStack source, String query) {
        List<FormationCodex.Entry> hits = FormationCodex.find(query);
        if (hits.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("codex.fulu.screen.no_match"), false);
            return 0;
        }
        for (FormationCodex.Entry entry : hits) {
            // 搜索只负责"筛"，不负责泄露：没解锁的仍然显示 ？？？
            Component line = Component.translatable(entry.nameKey());
            source.sendSuccess(() -> line, false);
        }
        return hits.size();
    }

    private static int show(CommandSourceStack source, String id, String name) {
        ServerLevel level = source.getLevel();
        String who = name != null ? name : selfName(source);
        if (who == null) {
            source.sendFailure(Component.translatable("codex.fulu.need_player"));
            return 0;
        }
        List<Component> lines = FormationCodex.buildDetail(level, who, id);
        for (Component line : lines) {
            source.sendSuccess(() -> line, false);
        }
        return lines.size();
    }

    /** 命令执行者自己叫什么；控制台/命令方块没有名字，返回 null。 */
    private static String selfName(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        return player == null ? null : player.getScoreboardName();
    }

    private CodexCommand() {
    }
}
