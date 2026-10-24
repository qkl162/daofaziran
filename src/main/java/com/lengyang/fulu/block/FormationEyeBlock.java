package com.lengyang.fulu.block;

import com.lengyang.fulu.codex.FormationCodex;
import com.lengyang.fulu.registry.ModBlockEntities;
import com.lengyang.fulu.registry.ModItems;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 阵眼方块本体。
 *
 * <p>它比第二步时多干三件事：
 * <ol>
 *   <li>{@link #newBlockEntity}：告诉游戏"这一格要创建哪个方块实体"；</li>
 *   <li>{@link #getTicker}：把 {@link FormationEyeBlockEntity#tick} 交给游戏，
 *       游戏就会每刻调它一次；</li>
 *   <li>{@link #use}：右键把阵法开关翻过来。</li>
 * </ol>
 *
 * <p>⚠️ {@code newBlockEntity} / {@code getTicker} 不是 {@code Block} 自带的，
 * 它们来自 {@code EntityBlock} 接口。继承 {@link BaseEntityBlock} 就自动实现了
 * 这个接口，还顺带给了 {@code createTickerHelper} 这个类型安全的助手方法。
 */
public class FormationEyeBlock extends BaseEntityBlock {

    /**
     * "有没有符"的<b>方块状态</b>属性。
     *
     * <p>为什么第二套外观必须走方块状态，而不是直接用方块实体里的那个开关：
     * <b>方块状态文件（blockstates/*.json）只能按方块状态分模型</b> ——
     * 它看不见方块实体里的数据。客户端要换模型，就得让方块状态本身变化。
     *
     * <p>好处是方块状态由区块自己存档 ✓，所以"装着哪种符"的外观本身就能存下来。
     *
     * <p>第九步这里是 {@code BooleanProperty has_talisman}（有符 / 没符）。
     * 现在符有两种，于是换成三值的枚举属性：{@code talisman=empty|chaos|sword}，
     * 方块状态文件里也就是三套模型。
     */
    public static final EnumProperty<TalismanKind> TALISMAN =
            EnumProperty.create("talisman", TalismanKind.class);

    public FormationEyeBlock(Properties properties) {
        super(properties);
        // 默认"空的" —— 和方块实体里那个字段的默认值保持一致
        registerDefaultState(this.stateDefinition.any().setValue(TALISMAN, TalismanKind.EMPTY));
    }

    /** 把属性登记进方块状态定义；漏了这一步，{@code setblock ... [talisman=chaos]} 会直接报错。 */
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TALISMAN);
    }

    /**
     * 玩家把阵眼放下去时调一次 —— 图鉴就是在这里"解锁"的。
     *
     * <p>为什么覆盖这个方法而不是 {@code onPlace}：{@code setPlacedBy} 会告诉我们
     * <b>是谁放的</b>（{@code placer}），而图鉴的解锁是**记在玩家名下**的。
     * {@code onPlace} 只看得到方块和世界，看不到人。
     *
     * <p>只在服务端记：计分板是服务端的东西，客户端那份调用了也没有意义。
     */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer,
                            ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (placer instanceof ServerPlayer player) {
            // 第一次放下阵眼 = 乱神阵变"已见闻"（写玩家 NBT + 计分板镜像）
            FormationCodex.unlock(player, FormationCodex.UNLOCK_PLACED);
        }
    }

    /** 放下方块时游戏会调它一次，问"这一格要配哪个方块实体"。 */
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FormationEyeBlockEntity(pos, state);
    }

    /**
     * 告诉游戏每刻调用哪个方法。返回 null = 这一格不需要每刻执行。
     *
     * <p>这里只在**服务端**返回 ticker：单人游戏里客户端和服务端会各加载一份
     * 方块实体，两边都 tick 的话同一句话会打印两遍，改世界状态的代码也会跑两遍。
     * 客户端真的需要每刻做动画，那种活应该交给方块实体渲染器（BER）。
     */
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                 BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.FORMATION_EYE.get(),
                FormationEyeBlockEntity::tick);
    }

    /**
     * ⚠️ 这一条是"继承 BaseEntityBlock 的隐藏陷阱"，必须覆盖。
     *
     * <p>{@code BaseEntityBlock} 默认返回 {@link RenderShape#INVISIBLE} ——
     * 它假设你有一个方块实体渲染器（BER）自己把方块画出来。我们是用普通方块模型
     * 显示的、没有 BER，所以不覆盖这个方法的话，阵眼放进世界里会**完全看不见**
     * （方块还在、还能挖、还在 tick，就是没有画面，也不报任何错）。
     */
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /**
     * 右键"看范围边界"用的物品。<b>先用木棍试</b>。
     *
     * <p>以后换成专属工具（"阵尺"、"罗盘"之类）时只改这一行 ——
     * 判断它的地方只有一处（{@link #use} 里那个 {@code held.is(...)}）。
     */
    private static final Item OUTLINE_TOOL = Items.STICK;

    /**
     * 右键交互，管三件事：
     * <ol>
     *   <li><b>手里拿着{@link #OUTLINE_TOOL}（现在是木棍）</b>：把阵法范围的边界
     *       用粒子画给他看（<b>只有他看得见</b>）；</li>
     *   <li>手里拿乱神符 + 阵眼没符 → 装上（<b>插上就立刻生效</b>）；</li>
     *   <li>空手 + 阵眼有符 → 取下；</li>
     *   <li>其他情况什么都不做。</li>
     * </ol>
     *
     * <p><b>符就是开关</b>（第十步第七部分）：原来那个"潜行右键开 / 关阵法"的手动开关
     * 已经删掉了 —— 因为它和"有符就自动开始"是重复的，而且留着它会出现
     * "点一下提示阵法已开启、其实什么也没发生"这种假反馈。
     *
     * <p>⚠️ 但<b>潜行时仍然要让位</b>（返回 {@code PASS}）：不判断的话，玩家蹲着
     * 拿方块想往阵眼上贴，就会变成"把符取下来了"。
     *
     * <p>另外三个"多人游戏里会翻车"的细节也都在这里：
     * <ul>
     *   <li><b>只在服务端改状态</b>。单人游戏里客户端和服务端各有一份方块实体，
     *       两边都改一次就等于没改；客户端只返回成功（摆手臂动画）。</li>
     *   <li><b>创造模式不扣物品</b>（{@code getAbilities().instabuild}），
     *       否则创造模式装一次符手里的符就少一个。</li>
     *   <li><b>背包满了就把东西掉在脚下</b>（{@code player.drop}），
     *       不然"取下符"会让物品凭空消失。</li>
     * </ul>
     */
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof FormationEyeBlockEntity eye)) {
            return InteractionResult.PASS;
        }

        // 潜行 = 让位：蹲着右键应该还能放方块、用东西，而不是被我们截走。
        if (player.isSecondaryUseActive()) {
            return InteractionResult.PASS;
        }

        ItemStack held = player.getItemInHand(hand);

        // ---------- ① 拿着"看边界"的道具（现在是木棍）→ 画一圈范围边界 ----------
        // 放在装符/取符之前：两者手里拿的东西不一样（木棍 vs 乱神符/空手），
        // 不会互相抢；放前面只是让"看边界"这个只读操作优先被处理。
        if (held.is(OUTLINE_TOOL)) {
            if (!level.isClientSide()) {
                boolean accepted = eye.requestOutline(player);
                if (!accepted) {
                    // 还在间隔里：给一句提示，不然玩家会以为没生效
                    player.displayClientMessage(
                            Component.translatable("msg.fulu.formation_eye.outline_wait"), true);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        TalismanKind inside = state.getValue(TALISMAN);
        TalismanKind carried = TalismanKind.of(held);

        // ---------- ① 手里拿着符 → 装进去（已经装了就不让装）----------
        if (carried != TalismanKind.EMPTY) {
            if (!level.isClientSide()) {
                if (inside != TalismanKind.EMPTY) {
                    // 已经有符：先取下来。给一句提示，不然玩家以为右键没反应
                    player.displayClientMessage(
                            Component.translatable("msg.fulu.formation_eye.already_has_talisman",
                                    inside.item() == null ? Component.empty()
                                            : new ItemStack(inside.item()).getHoverName()), true);
                    return InteractionResult.sidedSuccess(false);
                }
                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                }
                // 这一句同时改"方块实体里的字段"和"方块状态"，后者才是外观立刻变的开关
                eye.setTalisman(carried);
                // 装符 = "激活了这个阵法"，三个阵法各自在图鉴里解锁自己的条目
                if (carried != TalismanKind.EMPTY && player instanceof ServerPlayer serverPlayer) {
                    FormationCodex.unlock(serverPlayer, FormationCodex.UNLOCK_ACTIVATED);
                }
                player.displayClientMessage(Component.translatable(
                        switch (carried) {
                            case CHAOS -> "msg.fulu.formation_eye.talisman_on";
                            case SWORD -> "msg.fulu.formation_eye.sword_on";
                            case ANTIGRAVITY -> "msg.fulu.formation_eye.antigravity_on";
                            case STAND_IN -> "msg.fulu.formation_eye.stand_in_on";
                            case SHIFT_BLAME -> "msg.fulu.formation_eye.shift_blame_on";
                            case SUMMON -> "msg.fulu.formation_eye.summon_on";
                            case SLIPPERY -> "msg.fulu.formation_eye.slippery_on";
                            case BLIND -> "msg.fulu.formation_eye.blind_on";
                            case LOOP -> "msg.fulu.formation_eye.loop_on";
                            case INK -> "msg.fulu.formation_eye.ink_on";
                            default -> "msg.fulu.formation_eye.distance_on";
                        }), false);
                level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE,
                        SoundSource.BLOCKS, 0.7F, 1.4F);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        // ---------- ② 空手：里面有符就取下来，空的就报一下阵法信息 ----------
        if (held.isEmpty()) {
            if (!level.isClientSide()) {
                if (inside != TalismanKind.EMPTY) {
                    // 先记下是哪一种，再清空 —— 还回去的必须和装进去的是同一个东西
                    ItemStack back = new ItemStack(inside.item());
                    eye.setTalisman(TalismanKind.EMPTY);
                    if (!player.getAbilities().instabuild) {
                        if (!player.getInventory().add(back)) {
                            player.drop(back, false);       // 背包装不下，掉在脚下
                        }
                    }
                    // 用 switch 而不是三元的 if：这里是"按种类分别给文案"，
                    // 三元的写法加第三种符时不会报错，表现就是"取反重力符却说飞剑符"。
                    // switch 是穷尽的，以后再加符编译器会直接拦住。
                    player.displayClientMessage(Component.translatable(
                            switch (inside) {
                                case CHAOS -> "msg.fulu.formation_eye.talisman_off";
                                case SWORD -> "msg.fulu.formation_eye.sword_off";
                                case ANTIGRAVITY -> "msg.fulu.formation_eye.antigravity_off";
                                case STAND_IN -> "msg.fulu.formation_eye.stand_in_off";
                                case SHIFT_BLAME -> "msg.fulu.formation_eye.shift_blame_off";
                                case SUMMON -> "msg.fulu.formation_eye.summon_off";
                                case SLIPPERY -> "msg.fulu.formation_eye.slippery_off";
                                case BLIND -> "msg.fulu.formation_eye.blind_off";
                                case LOOP -> "msg.fulu.formation_eye.loop_off";
                                case INK -> "msg.fulu.formation_eye.ink_off";
                                default -> "msg.fulu.formation_eye.distance_off";
                            }), false);
                    level.playSound(null, pos, SoundEvents.BOOK_PAGE_TURN,
                            SoundSource.BLOCKS, 0.8F, 1.0F);
                } else {
                    showFormationInfo(player, eye);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        // ---------- ③ 其他情况：什么都不做 ----------
        // 返回 PASS 而不是 SUCCESS：PASS 的意思是"这一下我没接手"，
        // 游戏会继续往下走（比如你手里拿着方块，就还能把方块放下去）。
        return InteractionResult.PASS;
    }

    /**
     * 空手右键、而且里面没符时：把阵法情况报给玩家。
     *
     * <p>三条信息：
     * <ol>
     *   <li><b>阵法状态</b>：开启 / 关闭。现在"插了符"就等于开启（符就是开关）；</li>
     *   <li><b>范围半径</b>：直接问 {@link FormationEyeBlockEntity#getScanRadius()} —— 
     *       范围只有那一个来源，以后改半径这里自动跟着变；</li>
     *   <li><b>当前怪物数量</b>：读 {@link FormationEyeBlockEntity#getMonsterCount()}，
     *       也就是**每刻扫描时顺手存下来的那个数字** —— 这里不会再扫一遍世界。</li>
     * </ol>
     *
     * <p>为什么用聊天栏（{@code false}）而不是动作栏：一次三条，动作栏只放得下一条，
     * 而且动作栏几秒就淡了，玩家来不及看完。
     */
    private static void showFormationInfo(Player player, FormationEyeBlockEntity eye) {
        // 状态现在是三档：空的 / 装着乱神符 / 装着飞剑符
        TalismanKind kind = eye.talismanKind();
        player.displayClientMessage(Component.translatable("msg.fulu.formation_eye.info.state",
                Component.translatable(kind.stateKey())), false);
        player.displayClientMessage(Component.translatable("msg.fulu.formation_eye.info.radius",
                FormationEyeBlockEntity.getScanRadius()), false);
        player.displayClientMessage(Component.translatable("msg.fulu.formation_eye.info.monsters",
                eye.getMonsterCount()), false);
    }
}
