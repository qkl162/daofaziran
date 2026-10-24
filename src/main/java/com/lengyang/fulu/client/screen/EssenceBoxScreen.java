package com.lengyang.fulu.client.screen;

import com.lengyang.fulu.menu.EssenceBoxMenu;
import com.lengyang.fulu.refining.EssenceKind;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 妖丹匣界面。
 *
 * <p>布局（用户要求：左边像玩家背包、右边精致的信息区）：
 * <pre>
 *  ┌─────────────────────────────────────────────────┐
 *  │ ◆ 妖丹匣                                        │
 *  │  ┌── 背包 ──┐      ┌─ 妖丹 ─┐   ┌─ 妖丹信息 ─┐ │
 *  │  ▣▣▣▣▣▣▣▣▣        ▣ ▣        │ 图标 + 名称 │ │
 *  │  ▣▣▣▣▣▣▣▣▣        ▣ ▣        │ 技能：…     │ │
 *  │  ▣▣▣▣▣▣▣▣▣        ▣ ▣        │ 冷却：…     │ │
 *  │  ▣▣▣▣▣▣▣▣▣  ←快捷栏          └────────────┘ │
 *  └─────────────────────────────────────────────────┘
 * </pre>
 *
 * <p>所有图形都用 {@code fill} 画（不用贴图）：纯色面板不会因为图集/UV 问题变成紫黑格。
 */
public class EssenceBoxScreen extends AbstractContainerScreen<EssenceBoxMenu> {

    private static final int BG = 0xF0141018;
    private static final int BORDER = 0xFFE8C86A;
    private static final int BORDER_IN = 0xFF6A5230;
    private static final int SLOT_FILL = 0xFF2A1A28;
    private static final int SLOT_EDGE = 0xFF5A4058;
    private static final int PANEL_BG = 0xFF1E1020;
    private static final int PANEL_EDGE = 0xFF7A5A40;
    private static final int GOLD = 0xFFF0D682;
    private static final int TEXT = 0xFFD8D0C8;
    private static final int MUTED = 0xFF9A8A80;

    public EssenceBoxScreen(EssenceBoxMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = EssenceBoxMenu.WIDTH;
        this.imageHeight = EssenceBoxMenu.HEIGHT;
        this.inventoryLabelX = 10;
        this.inventoryLabelY = 20;
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        int w = this.imageWidth;
        int h = this.imageHeight;

        // ---- 主面板 + 双层边框
        gui.fill(x, y, x + w, y + h, BG);
        gui.fill(x, y, x + w, y + 2, BORDER);
        gui.fill(x, y + h - 2, x + w, y + h, BORDER);
        gui.fill(x, y, x + 2, y + h, BORDER);
        gui.fill(x + w - 2, y, x + w, y + h, BORDER);

        // ---- 所有槽位的底（统一画，看起来就是背包）
        for (Slot slot : this.menu.slots) {
            int sx = x + slot.x - 1;
            int sy = y + slot.y - 1;
            gui.fill(sx, sy, sx + 18, sy + 18, SLOT_EDGE);
            gui.fill(sx + 1, sy + 1, sx + 17, sy + 17, SLOT_FILL);
        }

        // ---- 妖丹槽：外面再套一圈暗金（提示这 6 格是专属）
        for (int i = 0; i < 6; i++) {
            int col = i % 2;
            int row = i / 2;
            int sx = x + EssenceBoxMenu.BOX_X + col * EssenceBoxMenu.BOX_STEP - 2;
            int sy = y + EssenceBoxMenu.BOX_Y + row * EssenceBoxMenu.BOX_STEP - 2;
            gui.fill(sx - 1, sy - 1, sx + 21, sy + 21, 0xFF6B4A20);
        }

        // ---- 右侧信息面板
        int px = x + 244;
        int py = y + 22;
        int pw = w - 244 - 10;
        int ph = h - 22 - 12;
        gui.fill(px, py, px + pw, py + ph, PANEL_BG);
        gui.fill(px, py, px + pw, py + 2, PANEL_EDGE);
        gui.fill(px, py + ph - 2, px + pw, py + ph, PANEL_EDGE);
        gui.fill(px, py, px + 2, py + ph, PANEL_EDGE);
        gui.fill(px + pw - 2, py, px + pw, py + ph, PANEL_EDGE);
    }

    @Override
    protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY) {
        gui.drawString(this.font, "◆ " + this.title.getString(), this.titleLabelX,
                this.titleLabelY, GOLD, false);

        gui.drawString(this.font, Component.translatable("gui.fulu.box.inventory"),
                10, 84, MUTED, false);
        gui.drawString(this.font, Component.translatable("gui.fulu.box.slots"),
                EssenceBoxMenu.BOX_X - 2, 24, MUTED, false);
        gui.drawString(this.font, Component.translatable("gui.fulu.box.info"),
                248, 24, MUTED, false);
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gui);
        super.render(gui, mouseX, mouseY, partialTick);
        this.renderTooltip(gui, mouseX, mouseY);

        // ---- 右侧：鼠标悬停的妖丹信息
        int px = this.leftPos + 250;
        int py = this.topPos + 40;
        EssenceKind hovered = hoveredEssence(mouseX, mouseY);
        if (hovered == null) {
            gui.drawWordWrap(this.font,
                    Component.translatable("gui.fulu.box.hint"), px, py,
                    this.imageWidth - 262, MUTED);
            return;
        }

        gui.renderItem(new ItemStack(hovered.item()), px, py);
        gui.drawString(this.font, Component.translatable(hovered.nameKey()),
                px + 20, py + 4, GOLD, false);

        int ty = py + 26;
        gui.drawString(this.font, Component.translatable("gui.fulu.box.skill"),
                px, ty, MUTED, false);
        ty += 11;
        gui.drawWordWrap(this.font,
                Component.translatable("info.fulu.essence.desc." + hovered.id()),
                px, ty, this.imageWidth - 262, TEXT);
        ty += 26;
        gui.drawString(this.font, Component.translatable("gui.fulu.box.cooldown", "0.1"),
                px, ty, 0xFF88BBFF, false);
    }

    /** 鼠标下面那个槽里是不是妖丹。 */
    private EssenceKind hoveredEssence(int mouseX, int mouseY) {
        for (Slot slot : this.menu.slots) {
            if (!slot.hasItem()) {
                continue;
            }
            int sx = this.leftPos + slot.x;
            int sy = this.topPos + slot.y;
            if (mouseX >= sx && mouseX < sx + 16 && mouseY >= sy && mouseY < sy + 16) {
                return EssenceKind.of(slot.getItem());
            }
        }
        return null;
    }
}
