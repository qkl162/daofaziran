package com.lengyang.fulu.menu;

import com.lengyang.fulu.block.FormationEyeBlockEntity;
import com.lengyang.fulu.registry.ModMenus;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

/**
 * 图鉴界面的"菜单"（服务端那一半）。
 *
 * <p>Minecraft 的界面是**两端合作**的：服务端开一个 {@code AbstractContainerMenu}，
 * 客户端照着同一个 id 开一个 {@code Screen}。所以这里一格物品栏都不放，
 * 只负责运送"这次要显示什么"。
 *
 * <p>两种模式共用一个菜单类型：
 * <pre>
 *   MODE_CODEX = 0  阵法图鉴（左侧列表 + 右侧详情）
 *   MODE_EYE   = 1  阵眼状态（打开的那个阵眼现在怎么样）
 * </pre>
 *
 * <p><b>为什么阵眼状态要用 {@link ContainerData} 而不是开界面时读一次</b>：
 * 怪物数量每刻都在变，只在打开那一瞬间读，玩家盯着界面看数字却不动会很奇怪。
 * ContainerData 会被逐刻同步，所以界面上的数字是活的。
 */
public class CodexMenu extends AbstractContainerMenu {

    public static final int MODE_CODEX = 0;
    public static final int MODE_EYE = 1;

    /**
     * 阵眼模式：里面装的是哪种符（{@link com.lengyang.fulu.block.TalismanKind} 的序号）。
     *
     * <p>以前这格叫 DATA_ACTIVE、只报"有没有符"。现在有三种状态，
     * 所以直接送种类序号，界面照着它挑文案。
     */
    public static final int DATA_TALISMAN = 0;
    public static final int DATA_RADIUS = 1;      // 范围半径
    public static final int DATA_MONSTERS = 2;    // 当前怪物数量
    /**
     * 解锁位（两种模式都带）。
     *
     * <p><b>为什么必须走这里、不能让界面自己去读计分板</b>：界面原来读的是**客户端**那份
     * 计分板，可原版只会把"显示在侧边栏/列表里的目标"的分数同步给客户端 ——
     * 我们这个 fuluCodex 是隐藏目标，客户端**一个字节都收不到**，
     * 于是界面永远显示"？？？"。走菜单的 ContainerData 就没有这个问题：
     * 它是菜单自己的数据，每刻由服务端同步一次。
     */
    public static final int DATA_UNLOCKS = 3;
    public static final int DATA_COUNT = 4;

    private final int mode;
    private final ContainerData data;

    /** 服务端：打开图鉴界面时用这个。 */
    public CodexMenu(int id, Inventory playerInventory, int unlockedBits) {
        super(ModMenus.CODEX.get(), id);
        this.mode = MODE_CODEX;
        this.data = fixedData(unlockedBits);
        addDataSlots(this.data);
    }

    /** 服务端：打开阵眼界面时用这个。 */
    public CodexMenu(int id, Inventory playerInventory, FormationEyeBlockEntity eye,
                     int unlockedBits) {
        super(ModMenus.CODEX.get(), id);
        this.mode = MODE_EYE;
        this.data = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case DATA_TALISMAN -> eye.talismanKind().ordinal();
                    case DATA_RADIUS -> FormationEyeBlockEntity.getScanRadius();
                    case DATA_MONSTERS -> eye.getMonsterCount();
                    case DATA_UNLOCKS -> unlockedBits;
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                // 客户端收到的同步数据；客户端这份不需要往回写
            }

            @Override
            public int getCount() {
                return DATA_COUNT;
            }
        };
        addDataSlots(this.data);
    }

    /** 图鉴模式：四个数字都是定值，只有解锁位有内容。 */
    private static ContainerData fixedData(int unlockedBits) {
        return new ContainerData() {
            @Override
            public int get(int index) {
                return index == DATA_UNLOCKS ? unlockedBits : 0;
            }

            @Override
            public void set(int index, int value) {
            }

            @Override
            public int getCount() {
                return DATA_COUNT;
            }
        };
    }

    /** 客户端：从网络包里的那几个字节还原出同一个菜单。 */
    public CodexMenu(int id, Inventory playerInventory, FriendlyByteBuf buf) {
        super(ModMenus.CODEX.get(), id);
        this.mode = buf.readByte();
        // 阵眼模式下包后面还跟着位置，这里读掉就行 —— 真正要显示的数字走 ContainerData
        if (this.mode == MODE_EYE) {
            buf.readBlockPos();
        }
        this.data = new SimpleContainerData(DATA_COUNT);
        addDataSlots(this.data);
    }

    /**
     * 界面里没有物品格。
     *
     * <p>原来这里顺手加了玩家背包那 36 格，坐标全是 (0,0) —— 结果是屏幕左上角叠了
     * 一堆看不见的格子，鼠标划过去还会弹出里面物品的提示（实测截图上显示"煤炭"）。
     * 图鉴是**只读面板**，本来就不该带背包格，直接不加最干净。
     * （不加不会掉东西：这些格子只是玩家自己背包的视图，不是独立的容器。）
     */
    public int getMode() {
        return mode;
    }

    /** 界面要显示的数字（阵眼模式是实时的，图鉴模式全 0）。 */
    public int getData(int index) {
        return data.get(index);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;      // 没有可搬的东西
    }

    @Override
    public boolean stillValid(Player player) {
        return true;                 // 图鉴拿在手里，站着就能看
    }
}
