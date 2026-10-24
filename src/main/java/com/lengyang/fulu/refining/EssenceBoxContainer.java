package com.lengyang.fulu.refining;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 妖丹匣内部的 6 个槽位（内容存在**物品自己的 NBT** 里）。
 *
 * <p>要点：
 * <ul>
 *   <li>只接受妖丹 —— {@link #canPlaceItem} 挡掉其它物品（放别的东西会被界面弹回去）；</li>
 *   <li>任何改动都写回物品 NBT（{@link #setChanged}），所以关掉界面、退出重进都还在；</li>
 *   <li>实现 {@link Container} 而不是继承 {@code SimpleContainer}：后者不知道"要存到哪个物品上"。</li>
 * </ul>
 */
public class EssenceBoxContainer implements Container {

    public static final int SIZE = 6;
    private static final String KEY_ITEMS = "FuluBoxItems";

    private final ItemStack box;
    private final NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);

    public EssenceBoxContainer(ItemStack box) {
        this.box = box;
        this.load();
    }

    private void load() {
        CompoundTag tag = this.box.getTag();
        if (tag == null || !tag.contains(KEY_ITEMS)) {
            return;
        }
        ListTag list = tag.getList(KEY_ITEMS, 10);
        for (int i = 0; i < list.size() && i < SIZE; i++) {
            this.items.set(i, ItemStack.of(list.getCompound(i)));
        }
    }

    @Override
    public void setChanged() {
        ListTag list = new ListTag();
        for (ItemStack stack : this.items) {
            list.add(stack.save(new CompoundTag()));
        }
        this.box.getOrCreateTag().put(KEY_ITEMS, list);
    }

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : this.items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot >= 0 && slot < SIZE ? this.items.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack got = ContainerHelper.removeItem(this.items, slot, amount);
        if (!got.isEmpty()) {
            this.setChanged();
        }
        return got;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack got = ContainerHelper.takeItem(this.items, slot);
        this.setChanged();
        return got;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot >= 0 && slot < SIZE) {
            this.items.set(slot, stack);
            this.setChanged();
        }
    }

    /** 只允许放妖丹（用户要求：放别的东西会弹出来）。 */
    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return EssenceKind.of(stack) != null;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        this.items.clear();
        this.setChanged();
    }
}
