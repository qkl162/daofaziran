package com.lengyang.fulu.menu;

import com.lengyang.fulu.refining.EssenceBoxContainer;
import com.lengyang.fulu.refining.EssenceKind;
import com.lengyang.fulu.registry.ModMenus;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 妖丹匣的容器菜单：左边玩家 27 格背包，中间 6 个妖丹槽，右边是信息面板（界面里画）。
 *
 * <p>槽位坐标就是**界面里的绝对坐标**（相对界面左上角），所以界面那边只负责画背景。
 */
public class EssenceBoxMenu extends AbstractContainerMenu {

    /** 界面尺寸（客户端画背景用同一套数字）。 */
    public static final int WIDTH = 330;
    public static final int HEIGHT = 190;

    /** 6 个妖丹槽的锚点（2 列 x 3 行）。 */
    public static final int BOX_X = 190;
    public static final int BOX_Y = 36;
    public static final int BOX_STEP = 26;

    /** 玩家背包锚点。 */
    public static final int INV_X = 10;
    public static final int INV_Y = 30;
    public static final int HOTBAR_Y = 94;

    private final EssenceBoxContainer box;

    /** 服务端用：直接拿手里的妖丹匣。 */
    public EssenceBoxMenu(int id, Inventory inventory, ItemStack boxStack) {
        this(id, inventory, new EssenceBoxContainer(boxStack));
    }

    /** 客户端工厂（{@code IForgeMenuType} 会传这个）。 */
    public EssenceBoxMenu(int id, Inventory inventory, FriendlyByteBuf buf) {
        this(id, inventory, new EssenceBoxContainer(inventory.player.getMainHandItem()));
    }

    private EssenceBoxMenu(int id, Inventory inventory, EssenceBoxContainer box) {
        super(ModMenus.ESSENCE_BOX.get(), id);
        this.box = box;

        // ---- 6 个妖丹槽
        for (int i = 0; i < EssenceBoxContainer.SIZE; i++) {
            int col = i % 2;
            int row = i / 2;
            this.addSlot(new Slot(box, i,
                    BOX_X + col * BOX_STEP, BOX_Y + row * BOX_STEP) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return EssenceKind.of(stack) != null;
                }

                @Override
                public int getMaxStackSize() {
                    return 1;                       // 一颗妖丹占一格
                }
            });
        }

        // ---- 玩家背包 27 格
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9,
                        INV_X + col * 18, INV_Y + row * 18));
            }
        }
        // ---- 快捷栏
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, INV_X + col * 18, HOTBAR_Y));
        }
    }

    public EssenceBoxContainer box() {
        return this.box;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return result;
        }
        ItemStack stack = slot.getItem();
        result = stack.copy();

        int boxSlots = EssenceBoxContainer.SIZE;
        if (index < boxSlots) {
            // 从妖丹槽 -> 背包
            if (!this.moveItemStackTo(stack, boxSlots, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            // 背包 -> 妖丹槽（只搬妖丹）
            if (EssenceKind.of(stack) == null) {
                return ItemStack.EMPTY;
            }
            if (!this.moveItemStackTo(stack, 0, boxSlots, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return result;
    }

    @Override
    public void slotsChanged(net.minecraft.world.Container container) {
        super.slotsChanged(container);
        // 每次匣内变化，把装进去的妖丹记进"已炼化/已收集"列表（永久数据）。
        // 既然不再靠"吃"来解锁，这里就是它唯一的写入点。
        for (int i = 0; i < EssenceBoxContainer.SIZE; i++) {
            com.lengyang.fulu.refining.EssenceKind kind =
                    EssenceKind.of(this.box.getItem(i));
            if (kind != null) {
                for (Player p : this.getPlayers()) {
                    com.lengyang.fulu.refining.RefiningPlayerData.learn(p, kind);
                }
            }
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    /** 当前打开界面的玩家（AbstractContainerMenu 没暴露，用槽位反查）。 */
    private java.util.List<Player> getPlayers() {
        java.util.List<Player> out = new java.util.ArrayList<>();
        for (Slot slot : this.slots) {
            if (slot.container instanceof net.minecraft.world.entity.player.Inventory inv
                    && inv.player != null) {
                out.add(inv.player);
                break;
            }
        }
        return out;
    }
}
