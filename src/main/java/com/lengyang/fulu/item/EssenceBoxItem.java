package com.lengyang.fulu.item;

import com.lengyang.fulu.menu.EssenceBoxMenu;
import com.lengyang.fulu.refining.EssenceKind;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

import java.util.List;

/**
 * 妖丹匣：右键打开 6 格妖丹收纳界面。
 *
 * <p>匣子里的东西存在**物品自己的 NBT** 里（见 {@code EssenceBoxContainer}），
 * 所以这个物品可以随便转手、放箱子、掉地上，内容都不会丢。
 */
public class EssenceBoxItem extends Item {

    public EssenceBoxItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResultHolder.pass(stack);       // 只认主手，避免两只手开两次
        }
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            NetworkHooks.openScreen(serverPlayer, new SimpleMenuProvider(
                    (id, inv, p) -> new EssenceBoxMenu(id, inv, p.getMainHandItem()),
                    Component.translatable("menu.fulu.essence_box")), buf -> {
                    });
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.fulu.essence_box").withStyle(ChatFormatting.GRAY));
        // 显示出里面装了几颗
        int count = 0;
        var tag = stack.getTag();
        if (tag != null && tag.contains("FuluBoxItems")) {
            count = tag.getList("FuluBoxItems", 10).size();
        }
        tooltip.add(Component.translatable("tooltip.fulu.essence_box.count", count)
                .withStyle(ChatFormatting.DARK_PURPLE));
    }
}
