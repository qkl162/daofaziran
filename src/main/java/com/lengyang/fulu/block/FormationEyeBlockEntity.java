package com.lengyang.fulu.block;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import com.lengyang.fulu.FuLu;
import com.lengyang.fulu.combat.AntiGravityTickHandler;
import com.lengyang.fulu.combat.FormationTarget;
import com.lengyang.fulu.effect.AntiGravityEffect;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import com.lengyang.fulu.entity.FlyingSwordEntity;
import com.lengyang.fulu.registry.ModBlockEntities;
import com.lengyang.fulu.registry.ModMobEffects;
import com.lengyang.fulu.registry.ModParticles;
import com.lengyang.fulu.combat.FormationFieldEffects;
import com.lengyang.fulu.combat.FormationMarks;
import com.lengyang.fulu.combat.ShiftBlameHandler;
import com.lengyang.fulu.standin.StandInData;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 阵眼的运行逻辑（方块实体）。
 *
 * <p>先分清两样东西，这是最容易糊的地方：
 * <pre>
 *   方块（Block）       "世界里的一格长什么样" —— 全游戏只有一份定义
 *   方块实体（BlockEntity）"这一格自己带的数据和行为" —— 每放一个就 new 一份
 * </pre>
 * 阵眼要记"我这里有没有插符"、"上一次报的数字是多少"，这种状态只能放在
 * 方块实体里 —— Block 是共享的，写在里面会让全世界所有阵眼共用一份数据。
 *
 * <p>三条和直觉不一样的地方：
 * <ol>
 *   <li><b>构造函数必须只有 {@code (BlockPos, BlockState)} 两个参数。</b>
 *       {@code BlockEntityType.Builder.of()} 要的是一个"函数式"的
 *       {@code BlockEntitySupplier}，签名不对编译就过不去。</li>
 *   <li><b>{@link #tick} 是 static 的，而且它不是覆盖父类方法</b> ——
 *       {@code BlockEntity} 本身根本没有 tick 方法。是我们在
 *       {@link FormationEyeBlock#getTicker} 里把这个方法交给游戏的。</li>
 *   <li><b>方块实体不会自己保存。</b> {@link #lastMonsterCount}、{@link #displayTimer}
 *       这些只活在内存里，区块卸载后重新加载会回到初值 —— 对"上一次报的数字"
 *       和"显示倒计时"来说无所谓。真要存下来得重写
 *       {@code saveAdditional} / {@code load}（"有没有符"就是这么存的）。</li>
 * </ol>
 *
 * <p>其中"有没有符"（{@link #hasTalisman}）是**会存档**的：
 * 见 {@link #saveAdditional} 与 {@link #load}。它同时也是整座阵法的开关 ——
 * 插上就自动开始挑事，取下就只剩"看得到怪物、报个数"。
 */
public class FormationEyeBlockEntity extends BlockEntity {

    /**
     * 有没有插符：只有两种值 —— <b>有符</b> / <b>没符</b>，默认没符。
     *
     * <p><b>符就是这座阵法的开关</b>（第十步第七部分起）：
     * 插上就自动生效，取下就只剩下"看得到怪物、报个数"。
     * 所以不需要第二个开关了 —— 原来那个"阵法开 / 关"的手动布尔值已经删掉。
     *
     * <p>而且它<b>会跟着存档一起保存</b>（见 {@link #saveAdditional} / {@link #load}）。
     * 所以走远、区块卸载、退出游戏再回来，它还是原来的样子 ——
     * "这只阵眼里插着哪张符"属于世界状态，不该因为离开就消失。
     *
     * <p>第九步这里是个 boolean（插了 / 没插）。现在有两种符，所以换成
     * {@link TalismanKind}：空 / 乱神符 / 飞剑符。
     */
    private TalismanKind talisman = TalismanKind.EMPTY;

    /**
     * 存 NBT 时用的键名。     *
     * <p>抽成常量是为了让"存"和"读"用同一个字符串：这两处一旦写岔
     * （哪怕只差大小写），表现是**存进去了、读出来永远是默认值**，
     * 而且不报任何错 —— 属于最难查的一类 bug。
     */
    private static final String NBT_TALISMAN = "Talisman";

    /**
     * 旧存档里的键名（boolean 时代）。
     *
     * <p>读的时候会看一眼它：如果新键没有、旧键是 true，就当成"装着乱神符"。
     * 这样第九步存档里的阵眼升级到这一版不会突然变空、把符弄丢。
     */
    private static final String NBT_OLD_HAS_TALISMAN = "HasTalisman";

    /**
     * 上一次播报出去的怪物数量；{@code -1} 表示"还没播报过"。
     *
     * <p>它决定这次要不要说话：**只有和它不一样时才发一次**。
     * 数量没变就不发 —— 动作栏不是日志，同一句话每 5 秒刷一遍会让人烦。
     *
     * <p>初值 {@code -1} 是为了保证"第一次一定会播报一次"（哪怕是 0 只）。
     */
    private int lastMonsterCount = -1;

    /**
     * "显示"的节奏：多少刻播报 + 放一次粒子。100 刻 = 5 秒。
     *
     * <p>⚠️ 它<b>只管这两件事</b>，不冷却扫描、也不冷却打架。
     * 这就是"去掉冷却"之后该有的样子：该快的快（发现怪物、让它们内讧），
     * 该慢的慢（动作栏和粒子都是给人看的，每秒刷 20 次只会烦人、还费网络）。
     */
    private static final int DISPLAY_INTERVAL_TICKS = 100;

    /**
     * 显示倒计时：每刻减 1，减到 0 才播报一次、放一次粒子，然后重新拉满。
     *
     * <p>初值 0 = 开阵法后的第一刻就先显示一次，不用干等 5 秒。
     */
    private int displayTimer = 0;

    /**
     * 扫描半径：前后左右各 10 格（第十一步从 5 改成 10）。
     *
     * <p>只改这一个数字，整座阵法的范围就一起变了 —— 因为
     * {@link #formationBox} 是范围**唯一定义的地方**，
     * 扫描、数数、挂内讧、清理、冒粒子全都读它。
     */
    private static final int SCAN_RADIUS = 10;

    /**
     * 向下探多少格：阵眼下方 10 格以内算，超过 10 格不算。
     * <b>向上没有这个限制</b> —— 一直算到世界最高处。
     */
    private static final int SCAN_DEPTH_BELOW = 10;

    /**
     * 播报半径：把"附近怪物：X 只"发给阵眼周围 16 格内的玩家。
     *
     * <p>⚠️ 它<b>不是</b>扫描半径，别看混了：这个数字决定"谁收得到消息"，
     * 而不是"数哪些怪"。第十一步把扫描改成 ±10 之后它<b>保持 16 不动</b>，
     * 因为 10×10 的方阵角上离阵眼 √(10²+10²) ≈ 14.1 格 ——
     * 改成 10 的话，站在阵脚边上的玩家反而收不到自家阵法的播报了。
     */
    private static final double ANNOUNCE_RADIUS = 16.0D;

    /**
     * 调试开关：顺手把扫描结果打到控制台。<b>默认关</b>。
     *
     * <p>正常玩法里结果只发给附近玩家的动作栏，日志里不需要（第 7 步已经把
     * 原来那句"找到 X 只怪物"的控制台打印删掉了）。开发和自动化测试时把它
     * 临时改成 true，就能从服务端日志里看到每次播报的数字。
     *
     * <p>打开后还会多两类行：{@code [mark]}（内讧粒子的每刻统计）
     * 和 {@code [outline]}（范围边界画了几格）。
     */
    private static final boolean DEBUG_SCAN_LOG = false;

    /**
     * 所有"还活着"的阵眼位置（按维度分）。
     *
     * <p>为什么要这个：阵魔的召唤条件要统计"附近有几个激活阵法"，
     * 而逐个方块扫描太贵。阵眼自己在加载/卸载时登记一次就够了。
     */
    private static final java.util.Map<Level, java.util.Set<BlockPos>> LIVE_EYES =
            new java.util.concurrent.ConcurrentHashMap<>();

    /** 附近所有已登记阵眼（给召唤逻辑用）。 */
    public static java.util.Set<BlockPos> liveEyes(Level level) {
        return LIVE_EYES.getOrDefault(level, java.util.Collections.emptySet());
    }

    /**
     * 调试开关：不用玩家右键也让边界一直画。
     *
     * <p>为什么需要它：范围边界是"玩家右键才画给他看"的，
     * 而无头服务端**没有玩家**，那条路根本走不到。
     * 打开它之后 tick 会自己反复请求边界，于是 {@code [outline]} 那行日志
     * 就能被打出来，自动化测试可以核对"画了几格、画在哪一圈"。
     */
    private static final boolean DEBUG_OUTLINE_ALWAYS = false;

    /** 范围边界用的粒子。先只画"地面一圈"，所以是平平的一层。 */
    private static final ParticleOptions OUTLINE_PARTICLE = ParticleTypes.END_ROD;

    /** 边界画多久：3 秒。粒子自己活不到一秒，所以这期间要反复补。 */
    private static final int OUTLINE_DURATION_TICKS = 60;

    /** 每隔多少刻补一轮（0.5 秒）。 */
    private static final int OUTLINE_INTERVAL_TICKS = 10;

    /** 连点保护：这么多次刻之内不接受第二次请求（1 秒）。 */
    private static final int OUTLINE_COOLDOWN_TICKS = 20;

    // ======================= 飞剑阵 =======================

    /** 多久射一发（1 秒 = 20 刻）。 */
    private static final int SWORD_INTERVAL_TICKS = 20;

    /** 一剑打多少伤害。设成 5 点（两点半心）。 */
    /** 一发齐射几把剑（用户要求每秒 10 把）。 */
    private static final int SWORDS_PER_VOLLEY = 10;

    /** 一次齐射最多打几只怪（超过就只打最近的这么多只）。 */
    private static final int MAX_TARGETS = 10;

    /** 出生点的散开幅度（格）：10 把剑不能叠在一个坐标上。 */
    private static final double SWORD_SPREAD = 0.7D;

    /** 瞄准时加一点抖动，剑才不会排成一条直线。 */
    private static final double SWORD_AIM_JITTER = 0.06D;

    private static final float SWORD_DAMAGE = 5.0F;

    /**
     * 正在飞的那一剑。{@code -1} 表示没有。
     *
     * <p>为什么要记住"打谁、飞了多久"：飞剑是**移动**的 —— 发射是一瞬间，
     * 命中在十刻之后。这十刻里得有人记着这件事，方块实体就是记它的地方。
     */

    /** 一次放几颗灵气。 */
    private static final int PARTICLE_COUNT = 6;

    /**
     * 站在阵法里时，"内讧"的持续时间：<b>无限</b>（{@code -1}）。
     *
     * <p>只要还在阵法里就一直有，不会掉 —— 这是第十步第十部分定的规则。
     */
    private static final int MARK_DURATION_INSIDE = -1;

    /**
     * 走出阵法之后，"内讧"还剩多久：20 秒。
     *
     * <p>不是立刻删掉：刚出圈还给 20 秒缓冲，玩家看得到图标在倒计时，
     * 也解释了"为什么它已经出圈了还在打"。
     */
    private static final int MARK_DURATION_AFTER_LEAVING = 400;

    /**
     * 每只怪每刻最多冒几个粒子（实际是 {@code 1 + random(这个值)}，
     * 也就是 1~2 个）。想更像"冒烟"就调大，但注意包数量是线性涨的。
     */
    private static final int MARK_PARTICLE_COUNT = 2;

    /** 粒子从"怪物头顶再往上"多少格冒出来。 */
    private static final double MARK_PARTICLE_HEAD_OFFSET = 0.25D;

    /**
     * 清标记时向外多查几格（在阵法范围之外再放宽这么多）。
     *
     * <p>放宽是为了让"刚走出去"的怪能被我们找到并改成 20 秒倒计时；
     * 更远的怪就不管了 —— 它们身上那 20 秒会自己走完。
     */
    private static final double MARK_CLEAR_MARGIN = 16.0D;

    public FormationEyeBlockEntity(BlockPos pos, BlockState state) {
        // 第一个参数是"我是哪种方块实体"，必须和 ModBlockEntities 里登记的一致，
        // 否则游戏加载存档时会说"这个方块实体类型不存在"
        super(ModBlockEntities.FORMATION_EYE.get(), pos, state);
    }

    // ===================== 存档：saveAdditional / load =====================

    /**
     * 保存：游戏把这个方块实体写进区块时调用。
     *
     * <p>两条规矩：
     * <ol>
     *   <li><b>先调 {@code super.saveAdditional(tag)}</b>。父类要写 id / x / y / z
     *       这些基础字段，漏了它，下次读档时游戏认不出这是什么方块实体。</li>
     *   <li>只写"需要跨存档保留"的东西。上一次播报的数量这种丢了无所谓的
     *       状态就别写 —— 写多了存档变大，还得考虑读到旧值怎么办。</li>
     * </ol>
     */
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        // 存的是种类名（empty / chaos / sword），不是真假 —— 以后再加符也不用改存档格式
        tag.putString(NBT_TALISMAN, talisman.getSerializedName());
    }

    /**
     * 读取：区块从存档加载时调用，用来把上次存的东西读回来。
     *
     * <p>同样先调 {@code super.load(tag)}。
     *
     * <p>⚠️ 这里的顺序是"先读新键，读不到再看旧键"：
     * 第九步的存档存的是 {@code HasTalisman:1b}，那时候只有乱神符一种，
     * 所以旧键为真就等于"装着乱神符"。少这一句的话，老存档升级上来
     * 阵眼会突然变成空的 —— 玩家会以为符被吞了。
     */
    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains(NBT_TALISMAN)) {
            talisman = TalismanKind.byName(tag.getString(NBT_TALISMAN));
        } else if (tag.getBoolean(NBT_OLD_HAS_TALISMAN)) {
            talisman = TalismanKind.CHAOS;      // 旧存档：有符 = 乱神符
        } else {
            talisman = TalismanKind.EMPTY;
        }
    }

    /** 这只阵眼里装的是哪种符。 */
    public TalismanKind talismanKind() {
        return talisman;
    }

    /** 里面有没有符（等于"不是空的"）。 */
    public boolean hasTalisman() {
        return talisman != TalismanKind.EMPTY;
    }

    /**
     * 最近一次扫描到的怪物数量。
     *
     * <p>每刻扫描完就更新（见 {@link #tick}）—— 右键"显示阵法信息"时**直接读它**，
     * 不会再扫一遍世界。这就是"复用已经扫到的数字"。
     *
     * <p>和 {@link #lastMonsterCount} 的区别：那个是"上一次**播报出去**的数字"
     * （只在真的说话时才更新），这个永远是最新一次扫描的结果。
     */
    private int currentMonsterCount = 0;

    /** 现在范围内有几只怪（右键显示信息用）。 */
    public int getMonsterCount() {
        return currentMonsterCount;
    }

    /**
     * 阵法半径（格）。显示信息时要报给玩家。
     *
     * <p>做成静态方法而不是让外面直接读 {@link #SCAN_RADIUS}：那个常量是私有的，
     * 保留私有可以保证"范围只有一个来源"，而外面要报数字时走这个方法。
     */
    public static int getScanRadius() {
        return SCAN_RADIUS;
    }

    /** 范围边界还要画多久（0 = 没在画）。 */
    private int outlineTicks = 0;

    /** 连点保护的剩余刻数。 */
    private int outlineCooldown = 0;

    /** 边界只画给这个人（右键的那位）。没人在看时是 null。 */
    @Nullable
    private UUID outlineViewer = null;

    /**
     * 玩家右键请求"画一下范围边界"。
     *
     * <p>只是**登记一个请求**，真正的画在 {@link #tick} 里 —— 因为粒子要持续几秒，
     * 而右键只发生一次。方块实体是唯一能记住"我还欠谁一次显示"的地方。
     *
     * @return 这次请求有没有被接受（false = 还在冷却里，连点了）
     */
    public boolean requestOutline(Player player) {
        if (outlineCooldown > 0) {
            return false;
        }
        outlineCooldown = OUTLINE_COOLDOWN_TICKS;
        outlineTicks = OUTLINE_DURATION_TICKS;
        outlineViewer = player.getUUID();
        return true;
    }

    /**
     * 装符 / 取下符。
     *
     * <p>{@link com.lengyang.fulu.block.FormationEyeBlock#use} 里"装符 / 取符"就是调它。
     *
     * <p>它同时改<b>两处</b>，缺一不可：
     * <ol>
     *   <li>{@link #talisman} 字段 —— 存进 NBT，可以用 {@code /data get} 查；</li>
     *   <li>方块状态 {@link FormationEyeBlock#TALISMAN} —— <b>客户端靠它换模型</b>。</li>
     * </ol>
     * 只改字段的话，逻辑是对的、外观不会变；只改方块状态的话，
     * 外观变了但 NBT 里查不到。两处都改才是"看起来也变了、查起来也对"。
     */
    public void setTalisman(TalismanKind kind) {
        // 先把这只阵眼发出去的替身标记清掉，再换符：符没了就不该还能替死
        clearStandInMarkers(kind);
        this.talisman = kind;
        setChanged();
        syncBlockState();
    }

    /**
     * 把字段的值同步到方块状态上（外观）。
     *
     * <p>{@code level.setBlock(..., 3)} 的 3 = 通知客户端 + 更新邻居。
     * 只改方块状态对象而不调 setBlock 是不生效的 —— 方块状态是不可变对象，
     * 得让世界换上一个新的。
     *
     * <p>这个方法是幂等的：值已经一致就什么都不做，避免无谓的方块更新。
     */
    private void syncBlockState() {
        if (level == null || level.isClientSide()) {
            return;
        }
        BlockState state = getBlockState();
        if (!state.hasProperty(FormationEyeBlock.TALISMAN)) {
            return;
        }
        if (state.getValue(FormationEyeBlock.TALISMAN) != talisman) {
            level.setBlock(worldPosition, state.setValue(FormationEyeBlock.TALISMAN, talisman), 3);
        }
    }

    /**
     * 每刻执行一次。参数是 Forge 约定的四件套，方块实体自己是最后一个。
     *
     * <p>为什么是 static 而不是实例方法：游戏手里只有"方块实体类型"，
     * 它需要先拿到一个方法才能对每一个实例调用（见
     * {@link net.minecraft.world.level.block.entity.BlockEntityTicker}），
     * 所以写成静态方法 + 自己当参数传进去。
     *
     * <p><b>没有"整段跳过"的闸门了</b>：原来那个"阵法开 / 关"的手动开关已经删掉 ——
     * 现在**符就是开关**。放下去就开始扫描，插上符就自动开始挑事，
     * 玩家不需要再去点第二下。
     *
     * <p>每一刻都做：
     * <ol>
     *   <li><b>扫描</b>范围内的怪物；</li>
     *   <li><b>有符才做</b>：让范围内的怪物互相攻击（少于 2 只时跳过）。</li>
     * </ol>
     * 而<b>播报和灵气粒子走另一个 5 秒的节拍</b>（{@link #DISPLAY_INTERVAL_TICKS}）——
     * 该快的快，该慢的慢：
     * <ul>
     *   <li>扫描/打架：每刻（发现变化要立刻反应；换目标要跟得上战斗）；</li>
     *   <li>播报/粒子：每 5 秒（动作栏和粒子都是给人看的，每秒刷 20 次只会烦人，
     *       粒子还额外是每秒 20 个网络包）。</li>
     * </ul>
     * 播报仍然只在"数字和上次播出去的不一样"时才发 —— 5 秒只是它的<b>最快</b>节奏。
     */
    public static void tick(Level level, BlockPos pos, BlockState state, FormationEyeBlockEntity eye) {
        // ==================== 每刻都做 ====================

        // 扫描。注意这里要的是**列表**而不只是数量 ——
        // 挑怪物打架得拿到具体对象。
        List<Mob> monsters = findMonsters(level, pos);
        int count = monsters.size();
        // 存一份"现在有几只"：右键显示阵法信息时直接复用这个数字，不再扫一遍
        eye.currentMonsterCount = count;

        // 装的是什么符，就做哪一件事。空的时候**只扫描和报数量**
        // （第十步那会儿是"有没有符都要挂内讧标记"，现在符有两种，
        //  标记属于乱神阵，所以只在装乱神符时挂）。
        // ??????????????? / ???????? / ??????
        // ?? switch ?????"??????"??????????
        FormationFieldEffects.applyAura((ServerLevel) level, formationBox(level, pos), pos, eye.talismanKind());

        switch (eye.talismanKind()) {
            case CHAOS -> {
                // **每一只**都分一个目标，全场大乱斗。
                // 范围内只有一只时，makeThemFight 自己会跳过（没有"别人"可以打）。
                makeThemFight(level, monsters);
                // 内讧"标记"：这是乱神阵的状态显示（也是别的逻辑可以查询的状态），
                // 本身不掉血。挂上之后顺手冒粒子 —— 玩家一眼看出阵法在生效。
                markInfighting(monsters);
                emitInfightingParticles(level, pos, monsters, eye);
                // 出圈的旧标记要清掉：只查带标记的怪，范围放宽一点，
                // 免得它们刚走出阵法就"忘了"自己身上还挂着内讧。
                clearInfightingOutside(level, pos);
            }
            case SWORD -> {
                // 飞剑阵：每 1 秒挑一只最近的怪，从阵眼顶部**放出一把飞剑实体**。
                // 飞行、追踪、命中、消失都交给那个实体自己（见 FlyingSwordEntity）——
                // 方块实体只负责"什么时候、朝谁、从哪儿放"。
                // 用游戏时间取模，不用自己数刻 —— 区块卸载再加载也不会错拍。
                if (level.getGameTime() % SWORD_INTERVAL_TICKS == 0) {
                    launchVolley(level, pos, monsters);
                }
                // 换过符的阵眼里可能还留着乱神标记，顺手清掉
                clearAllInfighting(level, pos);
            }
            case ANTIGRAVITY -> {
                // 反重力阵：挂标记（阵法内无限时长）+ 把离场的怪改成 3 秒倒计时。
                // 升空和"不能走"在 AntiGravityTickHandler，倒立在 AntiGravityRenderHandler。
                applyAntiGravity(level, pos, monsters);
                releaseAntiGravity(level, pos);
            }
            case DISTANCE -> {
                // 咫尺天涯阵：给范围内怪物挂标记 + 记下阵眼位置（判断"在阵法内"用）
                applyDistance(level, pos);
            }
            case TIME_SLOW -> {
                // 时缓阵：怪物挂时缓标记并减速；玩家挂"时缓免疫"；
                // 投射物被拖慢。真正的减速/攻击限速在 TimeSlowHandler。
                applyTimeSlow(level, pos);
            }
            case REFINING -> {
                // 炼妖阵：给范围内怪物挂炼妖标记（每刻刷新，离开就过期）。
                // 死亡凝丹与妖丹飘移在 RefiningHandler 里。
                applyRefining(level, pos);
            }
            case LOOP -> {
                // 闭环阵：给范围内怪物挂闭环标记，并记下这只阵眼的位置（传送要用）
                applyLoop(level, pos);
            }
            case BLIND -> {
                // 无明阵：给范围内的**怪物**挂无明标记（每刻刷新），BlindHandler 靠它做失明/听觉
                applyBlind(level, pos);
            }
            case SLIPPERY -> {
                // 滑地阵：给范围内的**怪物**挂滑地标记（每刻刷新），SlipperyHandler 靠它做惯性
                applySlippery(level, pos);
            }
            case SUMMON -> {
                // 招魂阵：给范围内的**怪物**挂招魂标记（每刻刷新），死的就会被 SummonSoulHandler 变成魂
                applySummon(level, pos);
            }
            case SHIFT_BLAME -> {
                // 嫁祸阵：给范围内的**所有生物**（怪物 + 玩家）挂嫁祸标记，每刻刷新。
                // 标记是普通状态效果（3 秒、每刻刷新），所以拆了阵眼它也会自己过期 ——
                // 不会重演"无限时长的标记没人回收"那个 bug。
                applyShiftBlame(level, pos);
            }
            case STAND_IN -> {
                // 替身阵：给范围内的**玩家**挂"替身"标记（每刻刷新），离开就清。
                // 标记是玩家持久数据，不是状态效果 —— 替死要读它，而效果会被牛奶之类的清掉。
                applyStandIn(level, pos);
                releaseStandIn(level, pos);
            }
            case EMPTY -> {
                // 空的：只扫描 + 每 5 秒报一次数量（下面那一段），什么都不做。
                // 但要把之前留下的内讧标记清掉，不然符取走之后图标会一直挂着。
                clearAllInfighting(level, pos);
            }
        }

        // 范围边界：只有玩家右键要过才画，而且只画给他一个人。
        // 粒子自己活不到一秒，所以这 3 秒里每 0.5 秒补一轮 ——
        // "持续几秒后自然消失"是这么来的（不用手动清除）。
        if (eye.outlineCooldown > 0) {
            eye.outlineCooldown--;
        }
        if (DEBUG_OUTLINE_ALWAYS && eye.outlineTicks <= 0) {
            // 调试：无头服务端没有玩家可右键，这里替玩家反复按。
            // 注意是"降到 0 才重来"，不是每刻重置 —— 每刻重置的话计数会永远停在
            // 同一个值上（60→59），`% INTERVAL` 永远不成立，一格都画不出来。
            eye.outlineTicks = OUTLINE_DURATION_TICKS;
            eye.outlineViewer = null;
        }
        if (eye.outlineTicks > 0) {
            // 先判断再递减：请求后的**第一刻**就会画出来（60 % 10 == 0），
            // 玩家不用先愣半秒才看到边界。
            if (eye.outlineTicks % OUTLINE_INTERVAL_TICKS == 0) {
                emitOutline(level, pos, eye);
            }
            eye.outlineTicks--;
        }

        // ==================== 每 5 秒才做一次：播报 + 粒子 ====================
        if (eye.displayTimer > 0) {
            eye.displayTimer--;
            return;
        }
        eye.displayTimer = DISPLAY_INTERVAL_TICKS;

        // 播报是有条件的：只有和上一次**播出去的**不一样才发一次。
        // 数量没变就不发 —— 动作栏不是日志，同一句话反复刷会让人烦。
        //
        // lastMonsterCount 只在真的播报时才更新，所以它的含义是
        // "玩家上一次被告知的数字"：中间那些来回变化不用逐条汇报，
        // 玩家在 5 秒内一定会收到最新的那个数。
        if (count != eye.lastMonsterCount) {
            announceMonsterCount(level, pos, count);
            eye.lastMonsterCount = count;
            if (DEBUG_SCAN_LOG) {
                // 调试用（默认关）：和上面那次播报一一对应，方便自动化核对
                FuLu.LOGGER.info("[scan] monsters={}", count);
            }
        }

        // 灵气粒子：必须由服务端发，ServerLevel#sendParticles 会把这个包
        // 广播给范围内的所有玩家；客户端自己 new 粒子只有自己看得见。
        // 每次 6 颗，散布在方块顶面 ±0.3 格，剩下的交给 SpiritMoteParticle
        // 自己的负重力（往上飘）和淡入淡出。
        //
        // 它也跟着上面这个 5 秒的节拍走：每秒 6 颗而不是 120 颗。
        if (level instanceof ServerLevel server) {
            server.sendParticles(ModParticles.SPIRIT_MOTE.get(),
                    pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D,
                    PARTICLE_COUNT,
                    0.3D, 0.15D, 0.3D,
                    0.02D);
        }
    }

    /**
     * 让范围内<b>每一只</b>怪物都分到一个目标 —— 全场大乱斗。
     *
     * <p>怎么让怪物攻击别人：{@code setTarget(目标)}。它会覆盖这只怪原本的目标
     * （僵尸平时只想打玩家和村民），接下来的寻路和挥击都是原版 AI 自己做的
     * （{@code MeleeAttackGoal} 看到 {@code getTarget()} 不为空就会冲上去打）。
     *
     * <p>为什么要<b>每只都设</b>而不是只挑一只：只挑一只的话，那一只会冲过去打，
     * 其他怪还在原地发呆（原版的 {@code HurtByTargetGoal} 只会让挨打的那只还手，
     * 不会让旁边看戏的也加入）。每只都分一个目标，才是真正的乱斗。
     *
     * <p>⚠️ 这只对**开着 AI** 的怪物有效。{@code NoAI:1b} 的怪连目标都会被清掉
     * （{@code Mob.serverAiStep} 里做的），只会站着不动 —— 调试时很容易被这一点坑到。
     *
     * <p>"目标不能是自己"用了一个小技巧，不需要循环重抽：
     * <pre>
     *   对第 i 只怪：在 0..size-2 里随机挑一个下标 ti（少一个位置）
     *   如果 ti >= i，就把它往后挪一格  ->  ti 永远不会等于 i
     * </pre>
     * 这样每个"别人"被选中的概率是一样的，而且一次就中。
     *
     * <p>范围内只有它一只时（{@code size < 2}）直接返回：
     * 没有"别人"可选，也就不给它设目标。
     */
    private static void makeThemFight(Level level, List<Mob> monsters) {
        int size = monsters.size();
        if (size < 2) {
            return;                     // 只有它一只，跳过
        }

        for (int i = 0; i < size; i++) {
            int targetIndex = level.random.nextInt(size - 1);
            if (targetIndex >= i) {
                targetIndex++;          // 跳过它自己
            }

            Mob attacker = monsters.get(i);
            Mob target = monsters.get(targetIndex);
            attacker.setTarget(target);

            if (DEBUG_SCAN_LOG) {
                // 打上实体 id：坐标不能当身份用 —— 混战里两只怪经常挤在同一格里，
                // 坐标一样但实体不同，光看坐标会误判成"打自己"。
                // "第几个/共几只" 这段是为了让自动化测试能直接看出
                // "这一轮有没有把每一只都处理到" —— 日志里一眼就是 1/3、2/3、3/3。
                FuLu.LOGGER.info("[fight] {}/{} #{} {} -> #{} {}", i + 1, size,
                        attacker.getId(), attacker.blockPosition().toShortString(),
                        target.getId(), target.blockPosition().toShortString());
            }
        }
    }

    /**
     * 数一数阵眼范围内有多少只**敌对怪物**。
     *
     * <p>范围（第 7 步改成上不封顶，第十一步把水平从 5 改成 10）：
     * <pre>
     *   水平：前后左右各 10 格       -> x、z 各 21 格
     *   垂直：往下 10 格，往上无限     -> 一直算到世界最高处
     * </pre>
     *
     * <p>垂直方向的判定用的是**怪物脚底的高度**（{@code monster.getY()}），
     * 而不是"碰撞箱有没有挨着范围"。这两个不一样，而且差别正好卡在你测试的那一格：
     * 僵尸高 1.95 格，站在阵眼下方 11 格时，它的脚在 -11、头顶却在 -9.05 ——
     * <b>用碰撞箱就会把它的脑袋算进来</b>，和"下方超过 10 格不算"矛盾。
     * 所以这里显式卡脚底高度：脚底不低于（阵眼 y - 10）才算。
     *
     * <p><b>筛选用的是 {@link Enemy} 接口，不是 {@code Monster} 类</b> —— 这个区别踩过一次坑：
     * 僵尸、骷髅、苦力怕这些继承 {@code Monster}，但**史莱姆、岩浆怪、恶魂、幻翼、疣猪兽**
     * 只实现 {@code Enemy} 接口，用 {@code Monster} 筛会把它们全漏掉，表现就是
     * "阵眼识别不了某些怪"。{@code Enemy} 才是原版对"敌对生物"的正式定义。
     *
     * <p>唯一的例外是**已驯服**的动物（狼、猫之类）：它们也实现了 {@code Enemy}，
     * 但那是玩家的宠物，不该被阵法打 —— 所以 {@link TamableAnimal#isTame()} 为真时跳过。
     *
     * <p>玩家、村民、牛羊猪、盔甲架都不是 Enemy，所以不会被数进去。
     *
     * <p>返回的是**列表**而不只是数量：挑怪物打架需要拿到具体对象
     * （数量拿不到"另一只是谁"）。
     */
    private static List<Mob> findMonsters(Level level, BlockPos pos) {
        // 垂直先取整个世界的高度范围，"太深"交给下面的 floorY 卡
        AABB area = formationBox(level, pos);

        int floorY = pos.getY() - SCAN_DEPTH_BELOW;
        List<Mob> found = new ArrayList<>();
        for (Mob monster : level.getEntitiesOfClass(Mob.class, area)) {
            if (FormationTarget.isTarget(monster) && monster.getY() >= floorY) {
                found.add(monster);
            }
        }
        return found;
    }

    /**
     * 阵法范围那个盒子。<b>只在这里定义一次</b>，别的地方都调它。
     *
     * <p>为什么要抽出来：标记（{@link #markInfighting}）和清除
     * （{@link #clearInfightingOutside}）必须对"在不在范围内"用**同一个判断**。
     * 两处各写一遍的话，改半径时改了一处漏一处，表现就是
     * "刚走出去的怪身上图标闪来闪去"这种很难查的毛病。
     *
     * <p>坐标系提醒：{@code AABB} 的最大值是**开区间**，所以右边写
     * {@code +SCAN_RADIUS + 1.0} 才正好包含到"各 10 格"。
     */
    /** ???????????????????????????? */
    public boolean formationContains(net.minecraft.world.phys.Vec3 p) {
        return formationBox(level, worldPosition).contains(p);
    }

    private static AABB formationBox(Level level, BlockPos pos) {
        return new AABB(
                pos.getX() - SCAN_RADIUS, level.getMinBuildHeight(), pos.getZ() - SCAN_RADIUS,
                pos.getX() + SCAN_RADIUS + 1.0D, level.getMaxBuildHeight(), pos.getZ() + SCAN_RADIUS + 1.0D);
    }

    /** 这只怪是不是站在阵法范围内（判定和 {@link #findMonsters} 完全一致）。 */
    /**
     * 站位判定：收 {@link Entity}（玩家不是 {@code Mob}，但"在不在阵法里的那个盒子"是同一套几何）。
     *
     * <p>不直接改成收 Entity 再在内部强转，是因为怪物那边已经有一堆调用点；
     * 多一个重载最省事，而且两边的判定标准**只有一份**（下面这个重载转调它）。
     */
    private static boolean isInsideFormation(Level level, BlockPos pos, Entity entity) {
        AABB box = formationBox(level, pos);
        return entity.getY() >= pos.getY() - SCAN_DEPTH_BELOW && box.intersects(entity.getBoundingBox());
    }

    private static boolean isInsideFormation(Level level, BlockPos pos, Mob monster) {
        return monster.getY() >= pos.getY() - SCAN_DEPTH_BELOW
                && formationBox(level, pos).intersects(monster.getBoundingBox());
    }

    /**
     * 给范围内的每一只怪物挂上"内讧"标记，<b>持续时间无限</b>。
     *
     * <p>⚠️ <b>不是每刻都调 {@code addEffect}</b>。那样虽然效果一样，
     * 但每刻都会产生一次"同步到客户端"的动作（20 只怪 × 20 刻 = 每秒 400 个包），
     * 图标还会一直闪。所以只在"现在没有"或"现在不是无限"的时候挂一次。
     *
     * <p>持续时间无限是刻意的：只要还站在阵法里，这个状态就一直有效。
     * 而"离开之后"的处理在 {@link #clearInfightingOutside} 里 —— 它会把它换成 20 秒。
     */
    private static void markInfighting(List<Mob> monsters) {
        for (Mob monster : monsters) {
            MobEffectInstance current = monster.getEffect(ModMobEffects.INFIGHTING.get());
            if (current == null || current.getDuration() != MARK_DURATION_INSIDE) {
                monster.addEffect(new MobEffectInstance(
                        ModMobEffects.INFIGHTING.get(),
                        MARK_DURATION_INSIDE,   // 无限
                        0,        // amplifier：0 级就够了，这只是个标记
                        false,    // ambient：不是信标那种"环境效果"
                        false,    // visible：不冒粒子（怪物身上一直冒紫烟太吵）
                        true));   // showIcon：状态栏要显示这个图标
            }
        }
    }

    /**
     * 让**刚被标记的**怪物身上冒粒子（内讧看得出来）。
     *
     * <p>几个刻意的选择：
     * <ul>
     *   <li><b>只发给阵眼周围 16 格内的玩家</b>（{@link #ANNOUNCE_RADIUS}）。
     *       注意不能用 {@code ServerLevel#sendParticles(...)} 那个"广播"版本 ——
     *       它自己的距离上限是 <b>32</b> 格，比我们要的宽一倍。
     *       所以这里先自己挑出"看得见的玩家"，再一个个发。</li>
     *   <li><b>每只每刻 1~2 个</b>（{@link #MARK_PARTICLE_COUNT}）。20 刻/秒 × 每只 2 个
     *       已经够看出"冒烟"，再多就是往客户端灌包。</li>
     *   <li><b>位置在头顶再往上一点</b>（{@code getBbHeight() + 0.25}），
     *       看起来像从怪物身上冒出来的，而不是卡在脚底。</li>
     *   <li><b>没有玩家在线时一个包都不发</b>，连遍历都不用做。</li>
     * </ul>
     *
     * <p>粒子本身用原版的"愤怒的村民"（{@link ParticleTypes#ANGRY_VILLAGER}）：
     * 它就是原版村民生气时头顶冒的那个灰白色小云，语义正好是"有火气"，
     * 而且不需要自己写贴图。想换成红/紫烟也很简单 ——
     * 换成 {@code new DustParticleOptions(new Vector3f(0.55F, 0.18F, 0.78F), 1.0F)}
     * 就是紫色尘粒（颜色用 RGB 0~1 给）。
     */
    private static void emitInfightingParticles(Level level, BlockPos pos, List<Mob> monsters,
                                                FormationEyeBlockEntity eye) {
        if (monsters.isEmpty() || !(level instanceof ServerLevel server)) {
            return;
        }

        // 先算一次"谁看得见"：距离按**阵眼**算（不是按怪物）。
        // 这样玩家站在阵眼旁边就能看到整片阵法的动静，而远处的玩家一个包都收不到。
        double centerX = pos.getX() + 0.5D;
        double centerY = pos.getY() + 0.5D;
        double centerZ = pos.getZ() + 0.5D;
        List<ServerPlayer> watchers = new ArrayList<>();
        for (ServerPlayer player : server.players()) {
            if (player.distanceToSqr(centerX, centerY, centerZ) <= ANNOUNCE_RADIUS * ANNOUNCE_RADIUS) {
                watchers.add(player);
            }
        }
        if (watchers.isEmpty()) {
            if (DEBUG_SCAN_LOG) {
                // 没人在线也要能验"该冒几只"：这里报的是**意图**，
                // 真正发出去的包是 0 个（没有收件人）。
                // 顺带报 count：那是每刻存下来、右键显示信息时复用的数字。
                FuLu.LOGGER.info("[mark] marked={} particles={} watchers=0 count={}",
                        monsters.size(), particleCountOf(monsters.size(), level),
                        eye.currentMonsterCount);
            }
            return;
        }

        int count = particleCountOf(monsters.size(), level);
        for (Mob monster : monsters) {
            double x = monster.getX();
            double y = monster.getY() + monster.getBbHeight() + MARK_PARTICLE_HEAD_OFFSET;
            double z = monster.getZ();
            // 每只怪自己抽 1~2 个，让它们不是整齐划一地一起冒
            int perMonster = 1 + level.random.nextInt(MARK_PARTICLE_COUNT);
            for (ServerPlayer player : watchers) {
                server.sendParticles(player, ParticleTypes.ANGRY_VILLAGER, false,
                        x, y, z, perMonster,
                        0.15D, 0.1D, 0.15D,   // 散开一点点，不然会叠成一个点
                        0.0D);
            }
        }
        if (DEBUG_SCAN_LOG) {
            FuLu.LOGGER.info("[mark] marked={} particles={} watchers={} count={}",
                    monsters.size(), count, watchers.size(), eye.currentMonsterCount);
        }
    }

    /**
     * 这一轮总共要冒多少颗（只是给调试日志看的估算值）。
     *
     * <p>真发的时候是每只怪各自随机的 1~2 颗，这里取中位数 1.5 × 只数，
     * 所以日志里出现 3 只怪时报的是 4，属于正常。
     */
    private static int particleCountOf(int monsterCount, Level level) {
        return monsterCount + (monsterCount + 1) / 2;
    }

    /**
     * 在阵法范围的**边界**上画一圈粒子（现在只画"地面一圈"）。
     *
     * <p>画在哪：贴合扫描框最外圈的那一格（正好 ±{@link #SCAN_RADIUS}），
     * 高度取阵眼自己那一层 +0.1（贴着地面、不会埋进方块里）。
     * 一圈正好 {@code (2r+1)*2 + (2r-1)*2} 个点 —— 每条边每隔 1 格一个，
     * 四个角只算一次（所以是 80 而不是 84）。
     *
     * <p><b>只发给右键的那位玩家</b>：用的是 {@code sendParticles(ServerPlayer, ...)}
     * 这个"发给单个玩家"的重载，而不是那个会在 32 格内广播的版本。
     * 找不到人（下线了、或者调试模式）就只算坐标不发送。
     *
     * <p>上下两个面和四条竖边都还没画 —— 那需要按 y 分层，先按要求的"简单的地面一圈"来。
     */
    private static void emitOutline(Level level, BlockPos pos, FormationEyeBlockEntity eye) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        ServerPlayer viewer = eye.outlineViewer == null
                ? null
                : server.getServer().getPlayerList().getPlayer(eye.outlineViewer);

        int r = SCAN_RADIUS;
        double y = pos.getY() + 0.1D;
        double x = pos.getX() + 0.5D;
        double z = pos.getZ() + 0.5D;
        int sent = 0;

        // 前后两条边（z = ±r，x 走满）
        for (int i = -r; i <= r; i++) {
            sent += outlineDot(server, viewer, x + i, y, z - r);
            sent += outlineDot(server, viewer, x + i, y, z + r);
        }
        // 左右两条边（x = ±r，z 只走中间，角上已经画过了）
        for (int i = -r + 1; i <= r - 1; i++) {
            sent += outlineDot(server, viewer, x - r, y, z + i);
            sent += outlineDot(server, viewer, x + r, y, z + i);
        }

        if (DEBUG_SCAN_LOG) {
            // 只报"相对阵眼的偏移"，日志里一眼能看出这一圈画在哪：±10
            FuLu.LOGGER.info("[outline] points={} r={} y={} sent={}",
                    (2 * r + 1) * 2 + (2 * r - 1) * 2, r, y, sent);
        }
    }

    /** 画一个点：有人看着就发一个粒子给他，返回发出去的个数（0 或 1）。 */
    private static int outlineDot(ServerLevel server, @Nullable ServerPlayer viewer,
                                  double x, double y, double z) {
        if (viewer == null) {
            return 0;
        }
        server.sendParticles(viewer, OUTLINE_PARTICLE, false, x, y, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        return 1;
    }

    /**
     * 阵法内标记**无限时长**（{@code -1} = 原版意义上的"无限"，客户端会显示成 ∞）。
     *
     * <p>用户要求："反重力是负面效果，在阵法内时间是无限的"。
     * 离开阵法时它会被改成 {@link #FLOAT_DURATION_TICKS}，也就是 3 秒倒计时。
     */
    private static final int FLOAT_DURATION_INFINITE = -1;

    /** 离开阵法之后还能撑多久（3 秒 = 60 刻），然后掉下来。 */
    private static final int FLOAT_DURATION_TICKS = 60;

    /** 浮空高度上限（阵眼上方几格）。 */
    private static final int FLOAT_HEIGHT = 5;

    /** 整个阵法每几刻响一次低沉的"启动"声（不是每个怪一声）。 */
    private static final int ANTI_GRAVITY_SOUND_INTERVAL = 100;

    /**
     * 反重力阵：给范围内的敌人挂上**反重力标记**（负面效果，阵法内无限时长）。
     *
     * <p>这个标记本身什么都不做（不掉血、不改属性）—— 它是给另外两件事看的暗号：
     * <ul>
     *   <li>{@code AntiGravityTickHandler}（服务端每刻）看到就抬它升空、按住移动、**保留攻击**；</li>
     *   <li>{@code AntiGravityRenderHandler}（客户端）看到就把模型上下翻 180 度。</li>
     * </ul>
     * 拆开是因为适用范围不同：标记要同步、升空和按住移动在服务端、倒立只能在客户端。
     *
     * <p>天花板（阵眼 y + {@link #FLOAT_HEIGHT}）写进生物的持久数据，
     * 抬升那边读它 —— 那样"每刻在跑"的代码不需要知道阵眼在哪。
     *
     * <p>对飞行怪物和 BOSS 不挂标记，判定统一走 {@link FormationTarget#canInvert}。
     */
    private static void applyAntiGravity(Level level, BlockPos pos, List<Mob> monsters) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        double ceiling = pos.getY() + FLOAT_HEIGHT;
        int applied = 0;
        for (Mob monster : monsters) {
            if (!FormationTarget.canInvert(monster)) {
                continue;
            }
            boolean wasFloating = monster.getEffect(ModMobEffects.ANTIGRAVITY.get()) != null;
            monster.getPersistentData().putDouble(AntiGravityTickHandler.CEILING_KEY, ceiling);
            monster.addEffect(new MobEffectInstance(ModMobEffects.ANTIGRAVITY.get(),
                    FLOAT_DURATION_INFINITE, 0, false, false, false));
            applied++;
            if (!wasFloating) {
                // "被翻转"的那一声：只在**刚被罩住**的那一刻响，不是每刻响
                server.playSound(null, monster.blockPosition(), SoundEvents.BOOK_PAGE_TURN,
                        SoundSource.PLAYERS, 0.7F, 0.7F);
            }
        }
        // 整个阵法一声：有怪被罩住的时候，每 5 秒一次低沉的启动声
        if (applied > 0 && level.getGameTime() % ANTI_GRAVITY_SOUND_INTERVAL == 0) {
            server.playSound(null, pos, SoundEvents.SHULKER_BOX_OPEN, SoundSource.BLOCKS,
                    0.5F, 0.6F);
        }
        if (DEBUG_SCAN_LOG && applied > 0 && level.getGameTime() % 20 == 0) {
            FuLu.LOGGER.info("[antigravity] {} monster(s) marked, ceiling {}",
                    applied, String.format("%.2f", ceiling));
        }
    }


    /**
     * 离开阵法的怪：把"无限时长"改成 3 秒倒计时，并清掉天花板。
     *
     * <p>用户要求"在阵法内时间是无限的"，同时早先那条"离开阵法后 3 秒内消失、然后掉下来"
     * 也还要成立。这两条要同时满足，就不能只靠"每刻刷新"：
     * 无限时长的效果不会自己过期，所以**离开时必须主动把它改成 60 刻**，
     * 让它像普通效果一样倒计时消失 —— 消失那一刻抬升和按住同时停止，它就掉下来了。
     *
     * <p>掉下来照常摔伤：抬升那边从不清 {@code fallDistance}。
     */
    private static void releaseAntiGravity(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        AABB wide = formationBox(level, pos).inflate(MARK_CLEAR_MARGIN);
        for (Mob monster : server.getEntitiesOfClass(Mob.class, wide)) {
            if (isInsideFormation(level, pos, monster)) {
                continue;                                  // 还在阵法里，保持无限
            }
            MobEffectInstance instance = monster.getEffect(ModMobEffects.ANTIGRAVITY.get());
            if (instance == null) {
                continue;
            }
            monster.getPersistentData().remove(AntiGravityTickHandler.CEILING_KEY);
            if (instance.getDuration() < 0) {
                // 无限 -> 3 秒倒计时（重新挂一次，客户端那边也会从 ∞ 变成数字）
                monster.removeEffect(ModMobEffects.ANTIGRAVITY.get());
                monster.addEffect(new MobEffectInstance(ModMobEffects.ANTIGRAVITY.get(),
                        FLOAT_DURATION_TICKS, 0, false, false, false));
            }
        }
    }

    /**
     * 替身阵：给范围内的玩家挂上"替身"标记。
     *
     * <p>每刻调一次，所以标记是**持续刷新**的（用户要求"每刻刷新标记，离开阵法后消失"）。
     * 刷新时把阵眼位置一并写进去 —— 替死时靠它知道该碎哪一个阵眼。
     *
     * <p>旁观者不挂：他们不受伤害，挂上也永远用不上。
     */
    /**
     * 嫁祸阵：给范围内的所有生物挂"嫁祸"标记。
     *
     * <p>范围判据和别的阵法一致（{@link #formationBox}），但对象是 {@link LivingEntity} ——
     * 怪物和玩家**一视同仁**，这是用户明确要的风险：站在阵里打怪，怪打你时伤害会被转走，
     * 但旁边没怪的时候你就自己全吃。
     *
     * <p>排除盔甲架和旁观者：前者不是"生物"，后者不受伤害。
     */
    /**
     * 招魂阵：给范围内的怪物挂"招魂"标记。
     *
     * <p>标记本身不做任何事；它只是"这只怪死在阵法里"的凭据 ——
     * {@code SummonSoulHandler} 监听死亡事件时靠它判断该不该生成魂。
     * 时长 3 秒、每刻刷新，离开阵法就掉。
     */
    /**
     * 滑地阵：给范围内的怪物挂"滑地"标记。
     *
     * <p>玩家不挂（用户要求"只有怪物受影响"）；飞行怪也挂，但 SlipperyHandler 会跳过它们。
     * 时长 3 秒、每刻刷新 —— 离开阵法就掉，惯性也会被清掉。
     */
    /**
     * 无明阵：给范围内的怪物挂"无明"标记。
     *
     * <p>玩家不挂（"玩家视野正常"）。
     */
    /**
     * 闭环阵：给范围内的**怪物**挂闭环标记，并把阵眼坐标一起写进去。
     *
     * <p>为什么要写坐标：传送要用"阵眼在哪"来算对面那个点；标记只是"在阵法里"的凭据，
     * 不带位置的话处理器就不知道往哪传。
     */
    /**
     * 咫尺天涯阵：给范围内的怪物挂标记。
     *
     * <p>标记用持久数据（对状态效果免疫的模组 BOSS 也生效）。
     */
    /**
     * 时缓阵：每个 tick 刷新一次。
     *
     * <ul>
     *   <li><b>怪物</b>：挂 {@code TIME_SLOW} 标记 + 移动速度 -50%（瞬时修饰符，
     *       离开阵法标记过期后由 handler 摘掉）；</li>
     *   <li><b>玩家</b>：挂 {@code TIME_SLOW_IMMUNE} 标记 —— 减速逻辑只认 Mob，
     *       玩家天然不受影响，这个标记是给"万一以后有别的时缓源"留的护栏；</li>
     *   <li><b>投射物</b>：每刻把速度乘 0.35，飞得极慢，像在空中飘。</li>
     * </ul>
     */
    private static void applyTimeSlow(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        AABB box = formationBox(level, pos);

        // 怪物：减速 + 沙粒
        for (Mob mob : findMonsters(level, pos)) {
            com.lengyang.fulu.combat.TimeSlowHandler.applyTo(server, mob);
        }

        // 玩家：免疫标记 + 脚下钟表刻度
        for (net.minecraft.world.entity.player.Player p
                : server.getEntitiesOfClass(net.minecraft.world.entity.player.Player.class, box)) {
            FormationMarks.mark(level, p, FormationMarks.TIME_SLOW_IMMUNE);
            if (server.getGameTime() % 8 == 0) {
                double rr = 0.55D;
                for (int i = 0; i < 8; i++) {
                    double a = Math.PI * 2.0D * i / 8.0D;
                    server.sendParticles(net.minecraft.core.particles.ParticleTypes.WAX_ON,
                            p.getX() + Math.cos(a) * rr, p.getY() + 0.06D,
                            p.getZ() + Math.sin(a) * rr, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                }
            }
        }

        // 投射物：拖慢 + 光尾
        for (net.minecraft.world.entity.projectile.Projectile pr
                : server.getEntitiesOfClass(net.minecraft.world.entity.projectile.Projectile.class,
                        box.inflate(4.0D))) {
            net.minecraft.world.phys.Vec3 v = pr.getDeltaMovement();
            if (v.lengthSqr() > 0.0025D) {
                pr.setDeltaMovement(v.scale(0.35D));
                pr.hurtMarked = true;
            }
            server.sendParticles(net.minecraft.core.particles.ParticleTypes.WAX_ON,
                    pr.getX(), pr.getY() + 0.1D, pr.getZ(), 1, 0.04D, 0.04D, 0.04D, 0.0D);
        }

        // 换符/取符后清掉圈外残留的时缓标记。
        //
        // **必须留滞回**：灾变 boss 体积很大，中心点在边界附近来回微动时，
        // 如果按"一出去就清"，标记会一刻有一刻没有，
        // 属性修饰符跟着一刻加一刻删 —— 速度在快慢之间跳变，表现就是抽搐。
        // 这里改成"走出去 4 格以上才清"。
        AABB outer = box.inflate(4.0D);
        for (Mob mob : server.getEntitiesOfClass(Mob.class,
                box.inflate(MARK_CLEAR_MARGIN))) {
            if (!outer.contains(mob.position())) {
                FormationMarks.clear(mob, FormationMarks.TIME_SLOW);
            }
        }
        for (net.minecraft.world.entity.player.Player p
                : server.getEntitiesOfClass(net.minecraft.world.entity.player.Player.class,
                        box.inflate(MARK_CLEAR_MARGIN))) {
            AABB playerOuter = box.inflate(4.0D);
            if (!playerOuter.contains(p.position())) {
                FormationMarks.clear(p, FormationMarks.TIME_SLOW_IMMUNE);
            }
        }
    }

    /** 炼妖阵：给范围内怪物挂标记（每刻刷新，离开阵法就过期）。 */
    private static void applyRefining(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        int marked = 0;
        for (Mob mob : findMonsters(level, pos)) {
            FormationMarks.mark(level, mob, FormationMarks.REFINING);
            marked++;
        }
        if (DEBUG_SCAN_LOG && marked > 0 && level.getGameTime() % 100 == 0) {
            FuLu.LOGGER.info("[refining] {} monster(s) marked", marked);
        }
    }

    private static void applyDistance(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        AABB box = formationBox(level, pos);
        int marked = 0;
        for (Mob mob : server.getEntitiesOfClass(Mob.class, box)) {
            if (!mob.isAlive()) {
                continue;
            }
            FormationMarks.mark(level, mob, FormationMarks.DISTANCE);
            marked++;
        }
        if (DEBUG_SCAN_LOG && marked > 0 && level.getGameTime() % 20 == 0) {
            FuLu.LOGGER.info("[distance] {} monster(s) marked", marked);
        }
    }

    private static void applyLoop(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        AABB box = formationBox(level, pos);
        int marked = 0;
        for (Mob mob : server.getEntitiesOfClass(Mob.class, box)) {
            if (!mob.isAlive()) {
                continue;
            }
            FormationMarks.mark(level, mob, FormationMarks.LOOP);
            mob.getPersistentData().putLong(
                    com.lengyang.fulu.combat.LoopHandler.K_EYE, pos.asLong());
            marked++;
        }
        if (DEBUG_SCAN_LOG && marked > 0 && level.getGameTime() % 20 == 0) {
            FuLu.LOGGER.info("[loop] {} monster(s) marked", marked);
        }
    }

    private static void applyBlind(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        AABB box = formationBox(level, pos);
        int marked = 0;
        // 诊断：每 20 刻确认一次"阵眼侧到底有没有跑"
        if (level.getGameTime() % 20 == 0) {
            FuLu.LOGGER.info("[blind-eye] tick at {} box={}", pos, box);
        }
        // **除玩家以外的所有生物**都挂标记（用户要求）
        for (LivingEntity living : server.getEntitiesOfClass(LivingEntity.class, box)) {
            // 排除玩家（玩家不吃失明）；SoulEntity 是 Entity 不是 LivingEntity，进不来
            if (!living.isAlive()
                    || living instanceof net.minecraft.world.entity.player.Player) {
                continue;
            }
            // 持久数据标记：对状态效果免疫的模组 BOSS 也能挂上
            com.lengyang.fulu.combat.FormationMarks.mark(level, living,
                    com.lengyang.fulu.combat.FormationMarks.BLIND);
            living.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    ModMobEffects.BLIND.get(), 60, 0, true, false, false));
            marked++;
        }
        // 玩家也挂同一个标记 —— 用来判断"在无明阵内"（潜行加速 / +20% 伤害 / 30% 闪避）
        for (net.minecraft.world.entity.player.Player player
                : server.getEntitiesOfClass(net.minecraft.world.entity.player.Player.class, box)) {
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    ModMobEffects.BLIND.get(), 60, 0, true, false, false));
        }
        if (level.getGameTime() % 20 == 0) {
            FuLu.LOGGER.info("[blind-eye] marked {} creature(s)", marked);
        }
    }

    private static void applySlippery(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        AABB box = formationBox(level, pos);
        int marked = 0;
        for (Mob mob : server.getEntitiesOfClass(Mob.class, box)) {
            if (!mob.isAlive()) {
                continue;
            }
            com.lengyang.fulu.combat.FormationMarks.mark(level, mob,
                    com.lengyang.fulu.combat.FormationMarks.SLIPPERY);
            mob.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    ModMobEffects.SLIPPERY.get(), 60, 0, true, false, false));
            marked++;
        }
        if (DEBUG_SCAN_LOG && marked > 0 && level.getGameTime() % 20 == 0) {
            FuLu.LOGGER.info("[slippery] {} monster(s) marked", marked);
        }
    }

    private static void applySummon(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        AABB box = formationBox(level, pos);
        int marked = 0;
        for (Mob mob : server.getEntitiesOfClass(Mob.class, box)) {
            if (!mob.isAlive()) {
                continue;
            }
            com.lengyang.fulu.combat.FormationMarks.mark(level, mob,
                    com.lengyang.fulu.combat.FormationMarks.SUMMON);
            mob.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    ModMobEffects.SUMMON.get(), 60, 0, true, false, false));
            marked++;
        }
        if (DEBUG_SCAN_LOG && marked > 0 && level.getGameTime() % 20 == 0) {
            FuLu.LOGGER.info("[summon] {} monster(s) marked", marked);
        }
    }

    private static void applyShiftBlame(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        // 用户要求：**只给怪物挂标记**，玩家不挂 —— 玩家站在阵里没有风险，
        // 嫁祸阵变成纯辅助阵法（怪打玩家时玩家正常受伤，不转移）。
        AABB box = formationBox(level, pos);
        int marked = 0;
        for (Mob mob : server.getEntitiesOfClass(Mob.class, box)) {
            if (!mob.isAlive()) {
                continue;
            }
            com.lengyang.fulu.combat.FormationMarks.mark(level, mob,
                    com.lengyang.fulu.combat.FormationMarks.SHIFT_BLAME);
            ShiftBlameHandler.mark(mob);
            marked++;
        }
        if (DEBUG_SCAN_LOG && marked > 0 && level.getGameTime() % 20 == 0) {
            FuLu.LOGGER.info("[shift-blame] {} living thing(s) marked", marked);
        }
    }

    private static void applyStandIn(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        AABB box = formationBox(level, pos);
        int marked = 0;
        for (Player player : server.getEntitiesOfClass(Player.class, box)) {
            if (player.isSpectator() || !player.isAlive()) {
                continue;
            }
            StandInData.setStandIn(player, true, pos);
            marked++;
        }
        if (DEBUG_SCAN_LOG && marked > 0 && level.getGameTime() % 20 == 0) {
            FuLu.LOGGER.info("[standin] {} player(s) marked by eye at {}", marked, pos);
        }
    }

    /**
     * 离开阵法的玩家：清掉替身标记。
     *
     * <p>只清"这个阵眼给的"标记（比对 {@code eyePos}）—— 两个替身阵重叠时，
     * 从一个里走出去不该把另一个给的标记也清掉。
     */
    private static void releaseStandIn(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        AABB wide = formationBox(level, pos).inflate(MARK_CLEAR_MARGIN);
        for (Player player : server.getEntitiesOfClass(Player.class, wide)) {
            if (isInsideFormation(level, pos, player)) {
                continue;
            }
            if (StandInData.hasStandIn(player) && pos.equals(StandInData.eyePos(player))) {
                StandInData.setStandIn(player, false, null);
            }
        }
    }

    /**
     * 阵眼被移除（打掉 / 被替换 / 区块卸载）时，把它给过的标记全部清掉。
     *
     * <p><b>这修的是一个真 bug</b>：反重力标记的时长是 {@code -1}（无限，用户要求"阵法内无限"），
     * 而"离场就把无限改成 3 秒倒计时"只在 {@link #tick} 里跑 —— 阵眼一旦没了，tick 不再执行，
     * 那些无限时长的标记就**永远留在怪物身上**：阵法消失了，怪物还倒着、还不能走。
     * 内讧标记同理。
     *
     * <p>为什么在 {@code setRemoved()} 里做而不是 {@code onRemove}：前者在打掉、被替换、
     * 区块卸载这几种情况下都会调，是"这个方块实体不存在了"的统一出口。
     *
     * <p>两个阵法重叠时不用担心清错：另一个阵眼的 tick 下一帧就会把它该给的标记重新挂上。
     */
    @Override
    public void onLoad() {
        super.onLoad();
        // 登记到"已激活阵眼"表里，供刷怪拦截查（见 FormationFieldEffects）
        if (level != null) {
            FormationFieldEffects.registerEye(level, worldPosition);
            // 同时也登记给阵魔的召唤统计（要数"附近有几个激活阵法"）
            LIVE_EYES.computeIfAbsent(level,
                    k -> java.util.concurrent.ConcurrentHashMap.newKeySet()).add(worldPosition);
        }
    }

    @Override
    public void setRemoved() {
        if (level != null) {
            FormationFieldEffects.unregisterEye(level, worldPosition);
            java.util.Set<BlockPos> set = LIVE_EYES.get(level);
            if (set != null) {
                set.remove(worldPosition);
            }
        }
        clearMarksAround();
        super.setRemoved();
    }

    /** 清掉这只阵眼周围一片区域内、由阵法挂上去的标记（反重力 / 内讧 / 玩家替身）。 */
    private void clearMarksAround() {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        AABB wide = formationBox(level, worldPosition).inflate(32.0D);

        // 怪物：反重力（无限时长的那一种）和内讧
        for (Mob monster : server.getEntitiesOfClass(Mob.class, wide)) {
            if (monster.getEffect(ModMobEffects.ANTIGRAVITY.get()) != null) {
                monster.removeEffect(ModMobEffects.ANTIGRAVITY.get());
                monster.getPersistentData().remove(AntiGravityTickHandler.CEILING_KEY);
            }
            if (monster.getEffect(ModMobEffects.INFIGHTING.get()) != null) {
                monster.removeEffect(ModMobEffects.INFIGHTING.get());
            }
            if (monster.getEffect(ModMobEffects.SHIFT_BLAME.get()) != null) {
                monster.removeEffect(ModMobEffects.SHIFT_BLAME.get());
            }
            if (monster.getEffect(ModMobEffects.SUMMON.get()) != null) {
                monster.removeEffect(ModMobEffects.SUMMON.get());
            }
            if (monster.getEffect(ModMobEffects.SLIPPERY.get()) != null) {
                monster.removeEffect(ModMobEffects.SLIPPERY.get());
            }
            if (monster.getEffect(ModMobEffects.BLIND.get()) != null) {
                monster.removeEffect(ModMobEffects.BLIND.get());
            }
            com.lengyang.fulu.combat.FormationMarks.clear(
                    monster, com.lengyang.fulu.combat.FormationMarks.LOOP);
            com.lengyang.fulu.combat.FormationMarks.clear(
                    monster, com.lengyang.fulu.combat.FormationMarks.DISTANCE);
        }

        // 玩家：替身标记 + 嫁祸标记（玩家身上也有嫁祸标记，别漏）
        for (ServerPlayer player : server.getEntitiesOfClass(ServerPlayer.class, wide)) {
            if (StandInData.hasStandIn(player) && worldPosition.equals(StandInData.eyePos(player))) {
                StandInData.setStandIn(player, false, null);
            }
            if (player.getEffect(ModMobEffects.SHIFT_BLAME.get()) != null) {
                player.removeEffect(ModMobEffects.SHIFT_BLAME.get());
            }
        }

        // 招魂阵的魂：阵眼没了就一起消散（否则它们会一直漂着咬人）
        for (com.lengyang.fulu.entity.SoulEntity soul :
                server.getEntitiesOfClass(com.lengyang.fulu.entity.SoulEntity.class, wide)) {
            soul.discard();
        }

        // 27?????? ?? ?????????
        for (Mob mob : server.getEntitiesOfClass(Mob.class, wide)) {
            Vec3 away = mob.position().subtract(worldPosition.getX() + 0.5D,
                    worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D);
            if (away.lengthSqr() < 0.01D) {
                continue;
            }
            away = away.normalize().scale(0.8D);
            mob.push(away.x, 0.35D, away.z);
            mob.hurtMarked = true;
        }

        if (DEBUG_SCAN_LOG) {
            FuLu.LOGGER.info("[formation] eye at {} removed: cleared marks nearby", worldPosition);
        }
    }

    /**
     * 换符/取符时，把"这个阵眼给的"替身标记全部清掉。
     *
     * <p>不做这一步的话：玩家站在替身阵里，你空手把符取走，他身上的标记会一直挂着 ——
     * 符都没了还能替他死，那就荒唐了。碎阵（替死）走的也是 {@code setTalisman(EMPTY)}，
     * 所以这一处同时解决了"取符"和"碎阵"两种情况。
     */
    private void clearStandInMarkers(TalismanKind newKind) {
        if (newKind == TalismanKind.STAND_IN || !(level instanceof ServerLevel server)) {
            return;
        }
        AABB wide = formationBox(level, worldPosition).inflate(32.0D);
        for (Player player : server.getEntitiesOfClass(Player.class, wide)) {
            if (StandInData.hasStandIn(player) && worldPosition.equals(StandInData.eyePos(player))) {
                StandInData.setStandIn(player, false, null);
            }
        }
    }

    /**
     * 一次齐射：**10 把飞剑**，按怪物数量分配，全部从阵眼顶部射出。
     *
     * <p>分配规则（用户的例子就是这套算法的结果）：
     * <pre>
     *   1 只  -> 10 把全打它
     *   2 只  -> 5 + 5
     *   3 只  -> 3 + 3 + 4      （除不尽时，**排在后面**的怪物多拿一把）
     *   5 只  -> 2 + 2 + 2 + 2 + 2
     *  10 只  -> 每只 1 把
     *  >10 只 -> 只打最近的 10 只，每只 1 把
     * </pre>
     *
     * <p>也就是说：先按到阵眼的距离排序、最多取 {@link #MAX_TARGETS} 只，然后
     * 每只至少分到 {@code 10 / 数量} 把，余数分给**最后**那几只。等价于
     * "第 i 把剑打第 i 只怪、不够就循环取"，只是余数落在后面而不是前面
     * （用户写的是"前两只各 3 把，最后一只 4 把"）。
     *
     * <p>飞行、追踪、命中、消失都不在这里：那是 {@code FlyingSwordEntity} 自己的事。
     * 方块实体只决定"什么时候放、放几把、朝谁放、从哪儿放"。
     */
    private static void launchVolley(Level level, BlockPos pos, List<Mob> monsters) {
        if (monsters.isEmpty() || !(level instanceof ServerLevel server)) {
            return;                      // 数量为 0 就不发射
        }
        double cx = pos.getX() + 0.5D;
        double cy = pos.getY() + 1.0D;      // 阵眼**顶部**（方块顶面）
        double cz = pos.getZ() + 0.5D;

        // 1) 按距离排序，超过 10 只就只留最近的 10 只
        List<Mob> targets = new ArrayList<>(monsters);
        targets.sort(Comparator.comparingDouble(m -> m.distanceToSqr(cx, cy, cz)));
        if (targets.size() > MAX_TARGETS) {
            targets = targets.subList(0, MAX_TARGETS);
        }

        // 2) 每只分几把：先平均，余数给排在后面的
        int count = targets.size();
        int base = SWORDS_PER_VOLLEY / count;
        int extra = SWORDS_PER_VOLLEY % count;

        // 3) 发射。每把的出生点都抖一下，不然 10 把会叠在同一个坐标上。
        int fired = 0;
        for (int i = 0; i < count; i++) {
            Mob target = targets.get(i);
            int swordsForThisOne = base + (i >= count - extra ? 1 : 0);
            for (int k = 0; k < swordsForThisOne; k++) {
                spawnSword(server, pos, cx, cy, cz, target);
                fired++;
            }
        }
        // 一次齐射只播**一次**发射音效（用户要求：10 把剑同时发射不要叠成噪音）。
        // 用箭矢发射声当"咻"。
        server.playSound(null, pos, SoundEvents.ARROW_SHOOT, SoundSource.BLOCKS, 1.0F, 1.2F);

        if (DEBUG_SCAN_LOG) {
            FuLu.LOGGER.info("[sword] volley: {} sword(s) at {} target(s)", fired, count);
        }
    }

    /** 放出一把飞剑：出生点在阵眼顶部附近随机散开，朝向目标。 */
    private static void spawnSword(ServerLevel server, BlockPos pos,
                                   double cx, double cy, double cz, Mob target) {
        // 散开一点：水平 ±0.35 格、竖直 ±0.15 格 —— 看着像"从阵眼喷出来一把剑"
        double fromX = cx + (server.random.nextDouble() - 0.5D) * SWORD_SPREAD;
        double fromY = cy + (server.random.nextDouble() - 0.5D) * SWORD_SPREAD * 0.4D;
        double fromZ = cz + (server.random.nextDouble() - 0.5D) * SWORD_SPREAD;
        Vec3 from = new Vec3(fromX, fromY, fromZ);

        // 瞄准目标身体中心，再给一点点抖动，剑才不会排成一条直线
        Vec3 to = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
        Vec3 look = to.subtract(from);
        if (look.lengthSqr() < 1.0E-6D) {
            look = new Vec3(0.0D, 0.0D, 1.0D);
        }
        double jitter = SWORD_AIM_JITTER;
        look = look.normalize().add(
                (server.random.nextDouble() - 0.5D) * jitter,
                (server.random.nextDouble() - 0.5D) * jitter,
                (server.random.nextDouble() - 0.5D) * jitter);

        FlyingSwordEntity sword = new FlyingSwordEntity(server, from, look.normalize(), target,
                SWORD_DAMAGE);
        server.addFreshEntity(sword);
    }


    /** 阵法附近的观察者（和播报、标记粒子用同一个半径）。 */
    private static List<ServerPlayer> watchersNear(ServerLevel server, BlockPos pos) {
        double cx = pos.getX() + 0.5D;
        double cy = pos.getY() + 0.5D;
        double cz = pos.getZ() + 0.5D;
        List<ServerPlayer> watchers = new ArrayList<>();
        for (ServerPlayer player : server.players()) {
            if (player.distanceToSqr(cx, cy, cz) <= ANNOUNCE_RADIUS * ANNOUNCE_RADIUS) {
                watchers.add(player);
            }
        }
        return watchers;
    }

    /** 把范围内所有怪身上的乱神标记清掉（换了符、或者取走符时用）。 */
    private static void clearAllInfighting(Level level, BlockPos pos) {
        for (Mob monster : findMonsters(level, pos)) {
            if (monster.getEffect(ModMobEffects.INFIGHTING.get()) != null) {
                monster.removeEffect(ModMobEffects.INFIGHTING.get());
            }
        }
    }

    /** 按方块位置找方块实体（几个静态方法要用到"当前这只阵眼"）。 */
    @Nullable
    private static FormationEyeBlockEntity eyeAt(Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof FormationEyeBlockEntity eye ? eye : null;
    }

    /**
     * 处理"已经出了阵法、但身上还挂着内讧"的怪物：
     * <b>把无限时长改成 20 秒倒计时</b>（不是直接删掉）。
     *
     * <p>只查**身上有标记**的怪，而且范围放宽 {@link #MARK_CLEAR_MARGIN} 格：
     * 刚走出去的怪马上就会进入倒计时，而不是等玩家看不见了才处理。
     *
     * <p>放这么宽是因为怪物会跑：查询范围如果正好等于阵法范围，
     * 一只在边界上蹦跶的怪就会"出圈→改成 20 秒→回圈→又变无限"地来回切。
     */
    private static void clearInfightingOutside(Level level, BlockPos pos) {
        AABB wide = formationBox(level, pos).inflate(MARK_CLEAR_MARGIN);
        for (Mob monster : level.getEntitiesOfClass(Mob.class, wide)) {
            if (!FormationTarget.isTarget(monster)) {
                continue;
            }
            MobEffectInstance current = monster.getEffect(ModMobEffects.INFIGHTING.get());
            // 没标记的不管；还在阵法里的不管
            if (current == null || isInsideFormation(level, pos, monster)) {
                continue;
            }
            // 只处理"无限"的那一份：已经是 20 秒倒计时的就不要每刻重置它了，
            // 否则它在附近晃悠多久，这个 20 秒就永远走不完。
            if (current.getDuration() == MARK_DURATION_INSIDE) {
                monster.removeEffect(ModMobEffects.INFIGHTING.get());
                monster.addEffect(new MobEffectInstance(
                        ModMobEffects.INFIGHTING.get(),
                        MARK_DURATION_AFTER_LEAVING,
                        0, false, false, true));
            }
        }
    }

    /**
     * 把"附近怪物：X 只"发给阵眼周围 16 格内的玩家，显示在**动作栏**（物品栏上方那条）。
     *
     * <p>{@code displayClientMessage(component, true)} 里的 {@code true} 就是"用动作栏"；
     * 传 {@code false} 会打进聊天框，那会把聊天记录刷满。
     *
     * <p>文案走语言文件（{@code msg.fulu.formation_eye.monsters}），
     * 而不是把中文写死在代码里 —— 写死的话英文客户端也会看到中文，
     * 而且改文案要重新编译。
     */
    private static void announceMonsterCount(Level level, BlockPos pos, int monsters) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        Component message = Component.translatable("msg.fulu.formation_eye.monsters", monsters);
        double centerX = pos.getX() + 0.5D;
        double centerY = pos.getY() + 0.5D;
        double centerZ = pos.getZ() + 0.5D;
        for (ServerPlayer player : server.players()) {
            if (player.distanceToSqr(centerX, centerY, centerZ) <= ANNOUNCE_RADIUS * ANNOUNCE_RADIUS) {
                player.displayClientMessage(message, true);   // true = 动作栏
            }
        }
    }
}
