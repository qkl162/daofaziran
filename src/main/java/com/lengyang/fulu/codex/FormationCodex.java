package com.lengyang.fulu.codex;

import java.util.ArrayList;
import java.util.List;

import com.lengyang.fulu.FuLu;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Score;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

/**
 * 阵法图鉴的<b>数据和文本</b>都在这里 —— 物品、命令、解锁都调这一份。
 *
 * <p><b>以后每做一个新阵法，往 {@link #ALL} 里加一行就够了</b>：
 * 列表、详情、解锁判断、点击事件全都是照着这份表生成的，
 * 不需要再去改物品或者命令。
 *
 * <p>解锁状态存在**计分板**里（每个玩家一条分数，按位记）：
 * <pre>
 *   第 0 位（1）= 见过：放下过阵眼
 *   第 1 位（2）= 用过：装符激活过阵法
 * </pre>
 * 为什么用计分板而不是玩家 NBT：计分板本来就是"每个玩家一份、随存档保存"的东西，
 * 而且**可以用命令读写** —— 所以这套解锁逻辑在没有玩家的自动化测试里也能验。
 */
public final class FormationCodex {

    /**
     * 图鉴里的一条记录。
     *
     * <p>字段全是**语言文件的 key**，不是写死的中文：图鉴要能翻译，
     * 而且加阵法时只需要多写几条 lang，不用碰代码逻辑。
     *
     * <p>为什么"范围"和"效果"各有**两条**：已见闻只给个大概（半径多少、干什么用的），
     * 已精通才给全（下方多少、上方无限、负面效果会传递）。
     * 名字、材料、激活/关闭方式则是两条共用的。
     *
     * <p>{@code iconItemId} 是列表左边那个图标用的物品 id（比如乱神阵用乱神符）。
     * 存字符串而不是 {@code Item} 对象：这个类在**两端都会加载**，
     * 而注册表在客户端刚启动的那一刻还没准备好，直接拿 Item 可能拿到 null。
     */
    public record Entry(String id, String nameKey,
                        String rangeShortKey, String rangeKey,
                        String effectShortKey, String effectKey,
                        String materialKey, String activateKey, String deactivateKey,
                        String iconItemId) {
    }

    /** 一个阵法在玩家那边的三种状态 —— 左侧列表的图标、右侧的详情都按它决定。 */
    public enum Stage {
        /** 未解锁：名字是 ？？？，图标是灰问号。 */
        LOCKED,
        /** 已见闻（放下过阵眼）：显示名字，图标半亮，详情只有大致效果。 */
        SEEN,
        /** 已精通（装符激活过）：图标全亮，详情含材料与合成方式。 */
        MASTERED
    }

    /** 目前收录的阵法。<b>加新阵法 = 在这里加一行。</b> */
    public static final List<Entry> ALL = List.of(
            new Entry("infighting",
                    "codex.fulu.infighting.name",
                    "codex.fulu.infighting.range_short",
                    "codex.fulu.infighting.range",
                    "codex.fulu.infighting.effect_short",
                    "codex.fulu.infighting.effect",
                    "codex.fulu.infighting.material",
                    "codex.fulu.infighting.activate",
                    "codex.fulu.infighting.deactivate",
                    "fulu:chaos_talisman"),
            new Entry("sword",
                    "codex.fulu.sword.name",
                    "codex.fulu.sword.range_short",
                    "codex.fulu.sword.range",
                    "codex.fulu.sword.effect_short",
                    "codex.fulu.sword.effect",
                    "codex.fulu.sword.material",
                    "codex.fulu.sword.activate",
                    "codex.fulu.sword.deactivate",
                    "fulu:flying_sword_talisman"),
            new Entry("antigravity",
                    "codex.fulu.antigravity.name",
                    "codex.fulu.antigravity.range_short",
                    "codex.fulu.antigravity.range",
                    "codex.fulu.antigravity.effect_short",
                    "codex.fulu.antigravity.effect",
                    "codex.fulu.antigravity.material",
                    "codex.fulu.antigravity.activate",
                    "codex.fulu.antigravity.deactivate",
                    "fulu:anti_gravity_talisman"),
            new Entry("stand_in",
                    "codex.fulu.stand_in.name",
                    "codex.fulu.stand_in.range_short",
                    "codex.fulu.stand_in.range",
                    "codex.fulu.stand_in.effect_short",
                    "codex.fulu.stand_in.effect",
                    "codex.fulu.stand_in.material",
                    "codex.fulu.stand_in.activate",
                    "codex.fulu.stand_in.deactivate",
                    "fulu:stand_in_talisman"),
            new Entry("shift_blame",
                    "codex.fulu.shift_blame.name",
                    "codex.fulu.shift_blame.range_short",
                    "codex.fulu.shift_blame.range",
                    "codex.fulu.shift_blame.effect_short",
                    "codex.fulu.shift_blame.effect",
                    "codex.fulu.shift_blame.material",
                    "codex.fulu.shift_blame.activate",
                    "codex.fulu.shift_blame.deactivate",
                    "fulu:shift_blame_talisman"),
            new Entry("summon",
                    "codex.fulu.summon.name",
                    "codex.fulu.summon.range_short",
                    "codex.fulu.summon.range",
                    "codex.fulu.summon.effect_short",
                    "codex.fulu.summon.effect",
                    "codex.fulu.summon.material",
                    "codex.fulu.summon.activate",
                    "codex.fulu.summon.deactivate",
                    "fulu:summon_talisman"),
            new Entry("slippery",
                    "codex.fulu.slippery.name",
                    "codex.fulu.slippery.range_short",
                    "codex.fulu.slippery.range",
                    "codex.fulu.slippery.effect_short",
                    "codex.fulu.slippery.effect",
                    "codex.fulu.slippery.material",
                    "codex.fulu.slippery.activate",
                    "codex.fulu.slippery.deactivate",
                    "fulu:slippery_talisman"),
            new Entry("blind",
                    "codex.fulu.blind.name",
                    "codex.fulu.blind.range_short",
                    "codex.fulu.blind.range",
                    "codex.fulu.blind.effect_short",
                    "codex.fulu.blind.effect",
                    "codex.fulu.blind.material",
                    "codex.fulu.blind.activate",
                    "codex.fulu.blind.deactivate",
                    "fulu:blind_talisman"),
            new Entry("loop",
                    "codex.fulu.loop.name",
                    "codex.fulu.loop.range_short",
                    "codex.fulu.loop.range",
                    "codex.fulu.loop.effect_short",
                    "codex.fulu.loop.effect",
                    "codex.fulu.loop.material",
                    "codex.fulu.loop.activate",
                    "codex.fulu.loop.deactivate",
                    "fulu:loop_talisman"),
            new Entry("distance",
                    "codex.fulu.distance.name",
                    "codex.fulu.distance.range_short",
                    "codex.fulu.distance.range",
                    "codex.fulu.distance.effect_short",
                    "codex.fulu.distance.effect",
                    "codex.fulu.distance.material",
                    "codex.fulu.distance.activate",
                    "codex.fulu.distance.deactivate",
                    "fulu:distance_talisman"),
            new Entry("demon",
                    "codex.fulu.demon.name",
                    "codex.fulu.demon.range_short",
                    "codex.fulu.demon.range",
                    "codex.fulu.demon.effect_short",
                    "codex.fulu.demon.effect",
                    "codex.fulu.demon.material",
                    "codex.fulu.demon.activate",
                    "codex.fulu.demon.deactivate",
                    "fulu:demon_core"),
            new Entry("ink",
                    "codex.fulu.ink.name",
                    "codex.fulu.ink.range_short",
                    "codex.fulu.ink.range",
                    "codex.fulu.ink.effect_short",
                    "codex.fulu.ink.effect",
                    "codex.fulu.ink.material",
                    "codex.fulu.ink.activate",
                    "codex.fulu.ink.deactivate",
                    "fulu:ink_talisman"),
            new Entry("time_slow",
                    "codex.fulu.time_slow.name",
                    "codex.fulu.time_slow.range_short",
                    "codex.fulu.time_slow.range",
                    "codex.fulu.time_slow.effect_short",
                    "codex.fulu.time_slow.effect",
                    "codex.fulu.time_slow.material",
                    "codex.fulu.time_slow.activate",
                    "codex.fulu.time_slow.deactivate",
                    "fulu:time_slow_talisman"),
            new Entry("refining",
                    "codex.fulu.refining.name",
                    "codex.fulu.refining.range_short",
                    "codex.fulu.refining.range",
                    "codex.fulu.refining.effect_short",
                    "codex.fulu.refining.effect",
                    "codex.fulu.refining.material",
                    "codex.fulu.refining.activate",
                    "codex.fulu.refining.deactivate",
                    "fulu:refining_talisman"));

    /** 解锁位：放下过阵眼（条目从"？？？"变成真名）。 */
    public static final int UNLOCK_PLACED = 1;

    /** 解锁位：装符激活过（详细信息才可见）。 */
    public static final int UNLOCK_ACTIVATED = 2;

    /** 存解锁状态用的计分板目标名。 */
    public static final String OBJECTIVE = "fuluCodex";

    /**
     * 玩家自己那份数据在 NBT 里的键名。
     *
     * <p>存在 {@code player.getPersistentData()} 里 —— Forge 给每个玩家附的一个
     * {@code CompoundTag}，它会被写进存档的 {@code playerdata/<uuid>.dat}。
     * 这就是"数据存在玩家身上"：换世界、退出重进，这份数据跟着那个玩家走。
     */
    public static final String NBT_KEY = "FuLuCodex";

    /**
     * 调试开关：把图鉴渲染出来的每一行打到控制台。<b>默认关</b>。
     *
     * <p>为什么需要它：图鉴是发给玩家看的聊天文本，而无头服务端没有玩家 ——
     * 打开它就能看见"到底渲染成了什么字"，自动化测试靠这个核对
     * （没解锁时是不是 ？？？、解锁后有没有那三行详情）。
     */
    private static final boolean DEBUG_CODEX_LOG = false;

    private FormationCodex() {
    }

    // ======================= 解锁状态（计分板） =======================

    private static Objective objective(ServerLevel level) {
        Scoreboard scoreboard = level.getScoreboard();
        Objective existing = scoreboard.getObjective(OBJECTIVE);
        if (existing != null) {
            return existing;
        }
        return scoreboard.addObjective(OBJECTIVE, ObjectiveCriteria.DUMMY,
                Component.literal("fuluCodex"), ObjectiveCriteria.RenderType.INTEGER);
    }

    /** 这个玩家解锁到哪一步了（按位看，见类注释）。 */
    public static int unlocked(ServerLevel level, String playerName) {
        Objective objective = level.getScoreboard().getObjective(OBJECTIVE);
        if (objective == null) {
            return 0;                       // 目标还没建 = 什么都没解锁
        }
        // 注意：1.20.1 的计分板 API 用的是**玩家名字符串**，
        // 不是后来版本里的 ScoreHolder（那个类在 1.20.3 之后才有）。
        Score score = level.getScoreboard().getOrCreatePlayerScore(playerName, objective);
        return score.getScore();
    }

    /** 给玩家加上一位解锁标志（已经有了就什么也不做）。 */
    @SuppressWarnings("deprecation")
    public static void unlock(ServerLevel level, String playerName, int flag) {
        Objective objective = objective(level);
        Score score = level.getScoreboard().getOrCreatePlayerScore(playerName, objective);
        int now = score.getScore();
        if ((now & flag) == flag) {
            return;                         // 已经解锁过：不重复记，也不刷屏
        }
        score.setScore(now | flag);
        if (DEBUG_CODEX_LOG) {
            FuLu.LOGGER.info("[codex] {} unlocked {} (now {})", playerName, flag, now | flag);
        }
    }

    /**
     * 玩家解锁了某个阵法 —— <b>写两处</b>：
     * <ol>
     *   <li><b>玩家自己的 NBT</b>（{@link #NBT_KEY}）：这是正式存档，
     *       进 {@code playerdata/<uuid>.dat}，退出重进不丢；</li>
     *   <li><b>计分板</b>：同一份数据的镜像，用来按**名字**查（玩家不在线时也能查），
     *       顺便让无头测试有办法读写这个状态。</li>
     * </ol>
     * 正式数据是玩家 NBT；计分板只是镜像，两者每次解锁一起更新。
     */
    public static void unlock(ServerPlayer player, int flag) {
        CompoundTag data = player.getPersistentData();
        int now = data.getInt(NBT_KEY);
        // 兼容：之前只记在计分板上的存档，这里补一次迁移，进度不会丢
        if (now == 0) {
            int mirrored = unlocked(player.serverLevel(), player.getScoreboardName());
            if (mirrored != 0) {
                now = mirrored;
            }
        }
        int after = now | flag;
        data.putInt(NBT_KEY, after);
        // 镜像（按名字可查 + 无头测试用）
        unlock(player.serverLevel(), player.getScoreboardName(), flag);
        if (DEBUG_CODEX_LOG) {
            FuLu.LOGGER.info("[codex] player {} bits {} -> {} (nbt key {})",
                    player.getScoreboardName(), now, after, NBT_KEY);
        }
    }

    /** 这个玩家自己 NBT 里的解锁位。没写过就是 0。 */
    public static int unlocked(ServerPlayer player) {
        return player.getPersistentData().getInt(NBT_KEY);
    }

    /**
     * 按名字查解锁位：<b>在线就用他身上的 NBT</b>（正式数据），
     * 不在线就退回计分板镜像（比如命令要查一个已经下线的玩家）。
     */
    public static int bitsForName(ServerLevel level, String playerName) {
        ServerPlayer online = level.getServer().getPlayerList().getPlayerByName(playerName);
        if (online != null) {
            return unlocked(online);
        }
        return unlocked(level, playerName);
    }

    // ======================= 渲染成聊天文本 =======================

    /**
     * 图鉴的列表：标题 + 每个阵法一行。
     *
     * <p>没解锁的显示 {@code ？？？}，解锁了显示真名，并且**点一下能看详情**
     * （用的是原版聊天的点击事件 {@code /fulu codex show <id>}）——
     * 这样不需要自己写一个 GUI 界面，原版输入框里的命令也能直接调。
     */
    public static List<Component> buildList(ServerLevel level, String playerName) {
        int bits = bitsForName(level, playerName);
        List<Component> lines = new ArrayList<>();
        int named = 0;
        int locked = 0;
        lines.add(Component.translatable("codex.fulu.title").withStyle(ChatFormatting.GOLD));
        for (Entry entry : ALL) {
            boolean seen = (bits & UNLOCK_PLACED) != 0;
            if (seen) {
                named++;
                Component name = Component.translatable(entry.nameKey()).withStyle(ChatFormatting.YELLOW);
                Component line = Component.literal("▸ ").withStyle(ChatFormatting.GRAY)
                        .append(name)
                        .append(Component.literal("  [点击查看]").withStyle(ChatFormatting.DARK_GRAY))
                        .withStyle(Style.EMPTY
                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                        "/fulu codex show " + entry.id()))
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                        Component.translatable("codex.fulu.click_hint"))));
                lines.add(line);
            } else {
                locked++;
                lines.add(Component.literal("▸ ").withStyle(ChatFormatting.GRAY)
                        .append(Component.translatable("codex.fulu.locked")
                                .withStyle(ChatFormatting.DARK_GRAY)));
            }
        }
        lines.add(Component.translatable("codex.fulu.hint").withStyle(ChatFormatting.DARK_GRAY));
        debugLines("list", playerName, bits, lines);
        // 摘要行只写 ASCII：自动化测试的脚本是 ASCII-only 的，
        // 中文那一行给人看，这一行给机器看。stages 用来核对三档状态的判定。
        if (DEBUG_CODEX_LOG) {
            StringBuilder stages = new StringBuilder();
            for (Entry entry : ALL) {
                if (stages.length() > 0) {
                    stages.append(",");
                }
                stages.append(stage(bits).name());
            }
            FuLu.LOGGER.info("[codex] list player={} bits={} lines={} named={} locked={} stages={}",
                    playerName, bits, lines.size(), named, locked, stages);
        }
        return lines;
    }

    /**
     * 搜索：返回匹配的阵法。界面底部的搜索框和 {@code /fulu codex find} 都走它。
     *
     * <p>抽成方法是为了让"筛哪些"这条规则**只有一份**，而且可以脱离界面测试。
     */
    public static List<Entry> find(String query) {
        List<Entry> hits = new ArrayList<>();
        for (Entry entry : ALL) {
            if (matches(entry, query)) {
                hits.add(entry);
            }
        }
        if (DEBUG_CODEX_LOG) {
            FuLu.LOGGER.info("[codex] find query={} hits={}", query == null ? "" : query, hits.size());
        }
        return hits;
    }

    /**
     * 某个阵法的详情（**聊天栏那一路**）。
     *
     * <p>内容就是 {@link #detailLines} 那几行 —— 界面和聊天栏共用同一份，
     * 免得"界面上写着六个字段、聊天里只打出三个"这种两边不一致的毛病。
     * 这里只是额外加一行标题（界面上的标题画在顶部，不在正文里）。
     */
    public static List<Component> buildDetail(ServerLevel level, String playerName, String id) {
        Entry entry = byId(id);
        List<Component> lines = new ArrayList<>();
        if (entry == null) {
            lines.add(Component.translatable("codex.fulu.unknown", id).withStyle(ChatFormatting.RED));
            debugLines("detail", playerName, -1, lines);
            if (DEBUG_CODEX_LOG) {
                FuLu.LOGGER.info("[codex] detail player={} id={} bits=-1 lines={}",
                        playerName, id, lines.size());
            }
            return lines;
        }

        int bits = bitsForName(level, playerName);
        Stage stage = stage(bits);
        boolean mastered = stage == Stage.MASTERED;

        lines.add(Component.translatable("codex.fulu.title").withStyle(ChatFormatting.GOLD));
        for (Component line : detailLines(entry, stage)) {
            lines.add(line.copy().withStyle(ChatFormatting.WHITE));
        }
        debugLines("detail", playerName, bits, lines);
        if (DEBUG_CODEX_LOG) {
            // 摘要（ASCII）：标题 + detailLines 的行数。
            // 未解锁 2 行（标题 + 尚未解锁）、已见闻 4 行、已精通 7 行。
            FuLu.LOGGER.info("[codex] detail player={} id={} bits={} unlocked={} lines={}",
                    playerName, entry.id(), bits, mastered, lines.size());
        }
        return lines;
    }

    /** 按 id 找一条记录；找不到返回 null（命令里会提示"没有这个阵法"）。 */
    public static Entry byId(String id) {
        for (Entry entry : ALL) {
            if (entry.id().equals(id)) {
                return entry;
            }
        }
        return null;
    }

    // ======================= 纯逻辑：状态 / 搜索 / 详情 =======================
    //
    // 下面这几个方法**不碰渲染、不碰网络、不碰客户端**，只吃数据出数据。
    // 好处：界面里"该显示什么"的规则可以单独测（-CodexTest 就是测它们），
    // 而真正要人眼看的只剩"画出来的样子"。

    /** 解锁位 -> 三档状态。这是整个界面的判定核心。 */
    public static Stage stage(int bits) {
        if ((bits & UNLOCK_ACTIVATED) != 0) {
            return Stage.MASTERED;      // 装符激活过
        }
        if ((bits & UNLOCK_PLACED) != 0) {
            return Stage.SEEN;          // 只放下过阵眼
        }
        return Stage.LOCKED;
    }

    /**
     * 搜索框的匹配规则：阵法 id、中文名、英文名任意一个包含关键字就算命中。
     *
     * <p>空关键字命中所有。关键字不区分大小写。
     *
     * <p>⚠️ 没解锁的阵法**按 id 也能搜到**（不然搜索框在"？？？"阶段完全没用），
     * 但列表上仍然显示 ？？？、右边仍然显示"尚未解锁" —— 搜索只负责筛，
     * 不负责泄露。
     */
    public static boolean matches(Entry entry, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }
        String q = query.trim().toLowerCase();
        if (entry.id().toLowerCase().contains(q)) {
            return true;
        }
        return Component.translatable(entry.nameKey()).getString().toLowerCase().contains(q);
    }

    /** 左上角那一行："乱神阵" 或 "？？？"。 */
    public static Component titleOf(Entry entry, Stage stage) {
        return stage == Stage.LOCKED
                ? Component.translatable("codex.fulu.locked")
                : Component.translatable(entry.nameKey());
    }

    /**
     * 右侧详情要显示的行，按状态给不同的量。
     *
     * <pre>
     *   未解锁   1 行 ：尚未解锁
     *   已见闻   3 行 ：名称 / 范围（大概）/ 效果（大概）
     *   已精通   6 行 ：名称 / 范围（全）/ 效果（全）/ 材料 / 激活方式 / 关闭方式
     * </pre>
     *
     * <p>每行都是"标签：值"的形式：标签是通用文案（{@code codex.fulu.detail.xxx}），
     * 值是这个阵法自己的文案。这样翻译时标签只需译一次，加阵法也只需填值。
     */
    public static List<Component> detailLines(Entry entry, Stage stage) {
        List<Component> lines = new ArrayList<>();
        switch (stage) {
            case LOCKED -> lines.add(Component.translatable("codex.fulu.screen.not_unlocked"));
            case SEEN -> {
                lines.add(labelled("name", entry.nameKey()));
                lines.add(labelled("range", entry.rangeShortKey()));
                lines.add(labelled("effect", entry.effectShortKey()));
            }
            case MASTERED -> {
                lines.add(labelled("name", entry.nameKey()));
                lines.add(labelled("range", entry.rangeKey()));
                lines.add(labelled("effect", entry.effectKey()));
                lines.add(labelled("material", entry.materialKey()));
                lines.add(labelled("activate", entry.activateKey()));
                lines.add(labelled("deactivate", entry.deactivateKey()));
            }
        }
        return lines;
    }

    /** 拼一行"标签：值"。 */
    private static Component labelled(String label, String valueKey) {
        return Component.translatable("codex.fulu.detail." + label,
                Component.translatable(valueKey));
    }

    /** 调试：把渲染出来的每一行按纯文本打到控制台（人看的）。 */
    private static void debugLines(String tag, String playerName, int bits, List<Component> lines) {
        if (!DEBUG_CODEX_LOG) {
            return;
        }
        for (int i = 0; i < lines.size(); i++) {
            FuLu.LOGGER.info("[codex] {} line {}: {}", tag, i, lines.get(i).getString());
        }
    }
}
