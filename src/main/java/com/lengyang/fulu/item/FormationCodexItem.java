package com.lengyang.fulu.item;

import java.util.List;

import javax.annotation.Nullable;

import com.lengyang.fulu.block.FormationEyeBlockEntity;
import com.lengyang.fulu.codex.FormationCodex;
import com.lengyang.fulu.menu.CodexMenu;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

/**
 * 阵法图鉴：右键打开**界面**（第十三步起；在那之前是把列表打在聊天栏里）。
 *
 * <p>它是本模组第一个<b>有自己行为的物品</b> —— 之前的符纸、乱神符都只是
 * {@code new Item(...)}。物品要"会做事"，就继承 {@link Item} 并覆盖
 * {@link #use}（对应方块的 {@code use}，只是主角从"方块"换成了"手里的东西"）。
 *
 * <p>两个入口：
 * <ul>
 *   <li>{@link #use}：对着空气右键 → 开**图鉴界面**（左侧列表 + 右侧详情）；</li>
 *   <li>{@link #useOn}：对着**阵眼**右键 → 开**阵眼界面**（那个阵眼现在的状态）。</li>
 * </ul>
 * 后者就是"顺便把右键打开阵法的 GUI 也做一下"：图鉴本来就是"查看阵法"的工具，
 * 拿它对阵眼用最自然。空手右键阵眼仍然是原来的"取符 / 报信息"，
 * 两条路不冲突。
 *
 * <p>⚠️ 开界面必须**只在服务端**做（{@code NetworkHooks.openScreen} 会自己发包
 * 通知客户端）；客户端那份调用只会重复开一次。
 */
public class FormationCodexItem extends Item {

    public FormationCodexItem(Properties properties) {
        super(properties);
    }

    /**
     * 鼠标停在物品上显示的两行**说明** —— 名字在语言文件的
     * {@code item.fulu.formation_codex}，这里补的是"它是什么、怎么用"。
     *
     * <p>第一行写它是什么，第二行写怎么用。两行都是语言 key，
     * 所以在英文客户端上会显示英文（见 {@code en_us.json}）。
     */
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip,
                                TooltipFlag flag) {
        tooltip.add(Component.translatable("item.fulu.formation_codex.desc")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.fulu.formation_codex.howto")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    /**
     * 对着空气右键：开图鉴界面。
     *
     * <p>（这里原本有一条"装了帕秋莉手册就交给它画"的分支，按你的要求撤掉了 ——
     * 图鉴现在只有我们自己的界面这一条路。）
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            // 解锁位**随菜单一起发给客户端**。界面不能自己去读计分板：
            // 原版只把"显示中的目标"的分数同步给客户端，我们这个隐藏目标收不到。
            int bits = FormationCodex.unlocked(serverPlayer);
            NetworkHooks.openScreen(serverPlayer,
                    new SimpleMenuProvider(
                            (id, inventory, p) -> new CodexMenu(id, inventory, bits),
                            Component.translatable("codex.fulu.title")),
                    buf -> buf.writeByte(CodexMenu.MODE_CODEX));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    /** 对着方块右键：如果那是个阵眼，就开阵眼界面。 */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.getBlockEntity(context.getClickedPos()) instanceof FormationEyeBlockEntity eye
                && context.getPlayer() instanceof ServerPlayer serverPlayer) {
            int bits = FormationCodex.unlocked(serverPlayer);
            NetworkHooks.openScreen(serverPlayer,
                    new SimpleMenuProvider(
                            (id, inventory, p) -> new CodexMenu(id, inventory, eye, bits),
                            Component.translatable("block.fulu.formation_eye")),
                    buf -> {
                        buf.writeByte(CodexMenu.MODE_EYE);
                        buf.writeBlockPos(context.getClickedPos());
                    });
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        return super.useOn(context);
    }
}
