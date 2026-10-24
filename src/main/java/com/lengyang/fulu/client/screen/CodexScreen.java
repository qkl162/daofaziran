package com.lengyang.fulu.client.screen;

import java.util.ArrayList;
import java.util.List;

import com.lengyang.fulu.block.TalismanKind;
import com.lengyang.fulu.codex.FormationCodex;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * 阵法图鉴的界面（客户端那一半）。
 *
 * <p>布局：
 * <pre>
 *   ┌───────────────────────────────┐
 *   │ 阵法图鉴                  [X] │   标题 + 关闭按钮
 *   ├───────────────┬───────────────┤
 *   │ ▸ [?] ？？？   │  尚未解锁      │   左侧列表 / 右侧详情
 *   │ ▸ [符] 乱神阵  │  名字 范围 ... │
 *   ├───────────────┴───────────────┤
 *   │ [搜索：________]              │   底部搜索框
 *   └───────────────────────────────┘
 * </pre>
 *
 * <p><b>为什么全部手工绘制，不用现成的列表控件</b>：原版的 {@code ObjectSelectionList}
 * 是给"条目 = 一行文字 + 可选中"设计的，而这里每行要画图标、要按状态变三副样子，
 * 自己画反而更短也更好懂（十几行 fill/drawString 而已）。
 *
 * <p>左侧图标三种状态：
 * <ul>
 *   <li>未解锁：画一个灰色问号；</li>
 *   <li>已见闻：画配方图标，上面盖一层半透明黑（"半亮"）；</li>
 *   <li>已精通：画配方图标，不盖（"全亮"）。</li>
 * </ul>
 *
 * <p>这个类是**纯客户端**的：它只在 {@code Dist.CLIENT} 下被加载
 * （注册在 {@link com.lengyang.fulu.client.ModClientSetup} 里），
 * 服务端永远不会碰到它 —— 否则专用服务器会因为找不到客户端类而崩。
 */
public class CodexScreen extends AbstractContainerScreen<com.lengyang.fulu.menu.CodexMenu> {

    private static final int PANEL_W = 320;
    private static final int PANEL_H = 200;
    private static final int ROW_H = 20;
    private static final int LIST_W = 130;
    private static final int ICON = 16;

    /** 当前选中的阵法（左列表里的下标）。 */
    private int selected = 0;

    /** 搜索框里的字。 */
    private String query = "";

    private EditBox search;
    private Button closeButton;

    /** 当前筛选出来的条目（搜索会改变它）。 */
    private List<FormationCodex.Entry> shown = new ArrayList<>();

    public CodexScreen(com.lengyang.fulu.menu.CodexMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = PANEL_W;
        this.imageHeight = PANEL_H;
    }

    @Override
    protected void init() {
        super.init();
        int left = this.leftPos;
        int top = this.topPos;

        // 底部搜索框：阵法多了以后靠它找
        this.search = new EditBox(this.font, left + 8, top + PANEL_H - 24, PANEL_W - 80, 16,
                Component.translatable("codex.fulu.screen.search"));
        this.search.setHint(Component.translatable("codex.fulu.screen.search"));
        this.search.setValue(this.query);
        this.search.setResponder(value -> {
            this.query = value;
            refreshShown();
        });
        addRenderableWidget(this.search);

        // 关闭按钮（ESC 也能关，但按钮更直观）
        this.closeButton = Button.builder(Component.translatable("codex.fulu.screen.close"),
                        b -> this.onClose())
                .bounds(left + PANEL_W - 64, top + PANEL_H - 24, 56, 16)
                .build();
        addRenderableWidget(this.closeButton);

        refreshShown();
    }

    private void refreshShown() {
        this.shown = new ArrayList<>();
        for (FormationCodex.Entry entry : FormationCodex.ALL) {
            if (FormationCodex.matches(entry, this.query)) {
                this.shown.add(entry);
            }
        }
        if (this.selected >= this.shown.size()) {
            this.selected = Math.max(0, this.shown.size() - 1);
        }
    }

    /**
     * 解锁位：<b>问菜单要</b>，不去读计分板。
     *
     * <p>原来这里读的是客户端那份计分板 —— 那是**错的**，而且症状很误导：
     * 服务端明明记下了进度（动作栏的提示就是证据），界面却一直显示"？？？"，
     * 因为原版只把"显示中的目标"的分数同步给客户端，隐藏目标根本不会传到这边。
     * 现在这四个数字由菜单的 ContainerData 每刻同步一次，稳。
     */
    private int bits() {
        return this.menu.getData(com.lengyang.fulu.menu.CodexMenu.DATA_UNLOCKS);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // 先让父类画它的（背景、格子、标签）—— 我们自己的面板和文字最后画，
        // 这样不可能被父类盖掉。父类那一层里没有任何属于我们的东西。
        super.render(graphics, mouseX, mouseY, partialTick);

        int left = this.leftPos;
        int top = this.topPos;

        // 背板
        graphics.fill(left, top, left + PANEL_W, top + PANEL_H, 0xF0101010);
        graphics.fill(left, top, left + PANEL_W, top + 2, 0xFF8B2FC9);           // 顶边一道紫
        graphics.drawString(this.font, this.title, left + 8, top + 8, 0xFFD9A03A, false);

        if (this.menu.getMode() == com.lengyang.fulu.menu.CodexMenu.MODE_EYE) {
            renderEyePanel(graphics, left, top);
        } else {
            renderList(graphics, left, top, mouseX, mouseY);
            renderDetail(graphics, left, top);
        }

        this.renderTooltip(graphics, mouseX, mouseY);
    }

    /** 左列表：每行一个阵法，带图标和状态。 */
    private void renderList(GuiGraphics graphics, int left, int top, int mouseX, int mouseY) {
        int listTop = top + 24;
        int listBottom = top + PANEL_H - 32;
        graphics.fill(left + 4, listTop - 2, left + LIST_W, listBottom + 2, 0x40000000);

        int bits = bits();
        for (int i = 0; i < this.shown.size(); i++) {
            FormationCodex.Entry entry = this.shown.get(i);
            FormationCodex.Stage stage = FormationCodex.stage(bits);
            int rowY = listTop + i * ROW_H;
            if (rowY + ROW_H > listBottom) {
                break;
            }

            boolean hovered = mouseX >= left + 6 && mouseX < left + LIST_W - 2
                    && mouseY >= rowY && mouseY < rowY + ROW_H;
            if (i == this.selected) {
                graphics.fill(left + 6, rowY, left + LIST_W - 2, rowY + ROW_H, 0x60FFFFFF);
            } else if (hovered) {
                graphics.fill(left + 6, rowY, left + LIST_W - 2, rowY + ROW_H, 0x30FFFFFF);
            }

            drawStatusIcon(graphics, left + 8, rowY + 2, entry, stage);
            Component name = FormationCodex.titleOf(entry, stage);
            int color = stage == FormationCodex.Stage.LOCKED ? 0xFF808080 : 0xFFEDEDED;
            graphics.drawString(this.font, name, left + 28, rowY + 6, color, false);
        }
    }

    /**
     * 左侧那个小图标：灰色问号 / 半亮 / 全亮。
     *
     * <p>"半亮"是盖一层半透明黑实现的 —— 原版渲染物品没法直接调透明度，
     * 但盖一层就够了，而且看不出来是"盖"的。
     */
    private void drawStatusIcon(GuiGraphics graphics, int x, int y,
                                FormationCodex.Entry entry, FormationCodex.Stage stage) {
        if (stage == FormationCodex.Stage.LOCKED) {
            graphics.drawString(this.font, "?", x + 5, y + 4, 0xFF606060, false);
            return;
        }
        ItemStack icon = iconOf(entry);
        graphics.renderItem(icon, x, y);
        if (stage == FormationCodex.Stage.SEEN) {
            graphics.fill(x, y, x + ICON, y + ICON, 0x99000000);   // 半亮
        }
    }

    /** 图标物品：按 id 从注册表里取；取不到就用书兜底（不会崩）。 */
    private static ItemStack iconOf(FormationCodex.Entry entry) {
        ResourceLocation id = ResourceLocation.tryParse(entry.iconItemId());
        var item = id == null ? null : ForgeRegistries.ITEMS.getValue(id);
        return new ItemStack(item == null ? Items.BOOK : item);
    }

    /** 右侧详情：按状态给不同量。 */
    private void renderDetail(GuiGraphics graphics, int left, int top) {
        int x = left + LIST_W + 8;
        int y = top + 28;
        int width = PANEL_W - LIST_W - 16;

        if (this.shown.isEmpty()) {
            graphics.drawString(this.font, Component.translatable("codex.fulu.screen.no_match"),
                    x, y, 0xFF808080, false);
            return;
        }
        FormationCodex.Entry entry = this.shown.get(this.selected);
        FormationCodex.Stage stage = FormationCodex.stage(bits());

        for (Component line : FormationCodex.detailLines(entry, stage)) {
            // 手工折行：原版没有"自动换行画一段文字"的公开方法，
            // 详情里有长句子，不折行会直接画出面板外面。
            for (var wrapped : this.font.split(line, width)) {
                graphics.drawString(this.font, wrapped, x, y, 0xFFCCCCCC, false);
                y += 11;
                if (y > top + PANEL_H - 36) {
                    return;
                }
            }
            y += 3;
        }
    }

    /** 阵眼模式：显示这个阵眼现在的状态（数字走 ContainerData，是活的）。 */
    private void renderEyePanel(GuiGraphics graphics, int left, int top) {
        int x = left + 12;
        int y = top + 32;
        // 0/1/2 -> TalismanKind 的序号，对应"空的 / 乱神符 / 飞剑符"三套文案
        int ordinal = this.menu.getData(com.lengyang.fulu.menu.CodexMenu.DATA_TALISMAN);
        TalismanKind[] kinds = TalismanKind.values();
        String stateKey = (ordinal >= 0 && ordinal < kinds.length)
                ? kinds[ordinal].stateKey()
                : TalismanKind.EMPTY.stateKey();

        graphics.drawString(this.font, Component.translatable("msg.fulu.formation_eye.info.state",
                Component.translatable(stateKey)), x, y, 0xFFEDEDED, false);
        y += 14;
        graphics.drawString(this.font, Component.translatable("msg.fulu.formation_eye.info.radius",
                this.menu.getData(com.lengyang.fulu.menu.CodexMenu.DATA_RADIUS)), x, y, 0xFFEDEDED, false);
        y += 14;
        graphics.drawString(this.font, Component.translatable("msg.fulu.formation_eye.info.monsters",
                this.menu.getData(com.lengyang.fulu.menu.CodexMenu.DATA_MONSTERS)), x, y, 0xFFEDEDED, false);
    }

    /** 点击左列表选阵法（没有用控件，所以自己算命中）。 */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.menu.getMode() == com.lengyang.fulu.menu.CodexMenu.MODE_CODEX) {
            int listTop = this.topPos + 24;
            int listBottom = this.topPos + PANEL_H - 32;
            if (mouseX >= this.leftPos + 6 && mouseX < this.leftPos + LIST_W - 2
                    && mouseY >= listTop && mouseY < listBottom) {
                int index = (int) ((mouseY - listTop) / ROW_H);
                if (index >= 0 && index < this.shown.size()) {
                    this.selected = index;
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        // 背板在 render() 里自己画了，这里不需要贴图
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // 标题在 render() 里画过了，避免默认的那一份重复
    }
}
