package com.lengyang.fulu.client;

import com.lengyang.fulu.refining.EssenceBoxContainer;
import com.lengyang.fulu.refining.EssenceKind;
import com.lengyang.fulu.registry.ModItems;
import com.lengyang.fulu.network.ModNetwork;
import com.lengyang.fulu.network.SelectSlotPacket;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

/**
 * 妖丹轮盘（R）= **选择生物能力**。
 *
 * <p>一颗妖丹可能有**多个技能**（目前只有恶魂：陨石 / 飞行）。
 * 所以轮盘上不再按"格子"排，而是按**能力条目**排：
 * 每个非空格子按它的技能数量展开成 1~N 个条目，沿圆周均分。
 *
 * <p>选中某个条目 = 同时确定「用哪一个格子」和「用这个格子的第几个技能」，
 * 按 Z 就施放这个能力。这样**选择只有 R 一个键**，不需要第二个面板。
 */
public class EssenceWheelScreen extends Screen {

    private static final int RING_RADIUS = 84;
    private static final int DISC_RADIUS = 140;
    private static final int FRAME = 28;
    private static final int FRAME_BIG = 34;

    private static final int DISC_FILL = 0xC8140F18;
    private static final int DISC_EDGE = 0xFF6E5A8A;
    private static final int DISC_EDGE_IN = 0x88B9A6D0;
    private static final int SPOKE = 0x66E8E0F0;
    private static final int CROSS = 0x99FFF3C4;

    private static final ResourceLocation FRAME_TEX =
            new ResourceLocation("fulu", "textures/gui/spell_frame.png");

    /** 一个可选能力条目。 */
    private static final class Entry {
        final int slot;
        final int sub;
        final EssenceKind kind;
        final String skill;

        Entry(int slot, int sub, EssenceKind kind, String skill) {
            this.slot = slot;
            this.sub = sub;
            this.kind = kind;
            this.skill = skill;
        }
    }

    private final List<Entry> entries = new ArrayList<>();
    private final ItemStack[] slots = new ItemStack[6];
    private int hovered = -1;

    public EssenceWheelScreen() {
        super(Component.translatable("screen.fulu.wheel.title"));
        for (int i = 0; i < 6; i++) {
            this.slots[i] = ItemStack.EMPTY;
        }
        var player = net.minecraft.client.Minecraft.getInstance().player;
        if (player != null) {
            for (ItemStack stack : player.getInventory().items) {
                if (stack.is(ModItems.ESSENCE_BOX.get())) {
                    EssenceBoxContainer box = new EssenceBoxContainer(stack);
                    for (int i = 0; i < 6; i++) {
                        this.slots[i] = box.getItem(i);
                    }
                    break;
                }
            }
        }
        // 展开成能力条目
        for (int i = 0; i < 6; i++) {
            EssenceKind kind = EssenceKind.of(this.slots[i]);
            if (kind == null) {
                continue;
            }
            // 轮盘只选**妖丹**（每个格子一条）；具体用哪个技能由 V 键切换
            this.entries.add(new Entry(i, 0, kind, kind.skills()[0]));
        }
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gui);
        int cx = this.width / 2;
        int cy = this.height / 2;

        this.hovered = pick(mouseX, mouseY);
        Entry cur = this.hovered >= 0 ? this.entries.get(this.hovered) : null;

        disc(gui, cx, cy, DISC_RADIUS, DISC_FILL);
        ring(gui, cx, cy, DISC_RADIUS, 2, DISC_EDGE);
        ring(gui, cx, cy, DISC_RADIUS - 6, 1, DISC_EDGE_IN);

        gui.fill(cx - 10, cy - 1, cx + 10, cy + 1, CROSS);
        gui.fill(cx - 1, cy - 10, cx + 1, cy + 10, CROSS);
        ring(gui, cx, cy, 14, 1, CROSS);

        int n = Math.max(1, this.entries.size());
        for (int i = 0; i < this.entries.size(); i++) {
            Entry e = this.entries.get(i);
            double a = Math.PI * 2.0D * i / n - Math.PI / 2.0D;
            int sx = (int) Math.round(cx + Math.cos(a) * RING_RADIUS);
            int sy = (int) Math.round(cy + Math.sin(a) * RING_RADIUS);

            line(gui, cx, cy, sx, sy, SPOKE);

            boolean hot = (i == this.hovered);
            int size = hot ? FRAME_BIG : FRAME;
            int fx = sx - size / 2;
            int fy = sy - size / 2;

            gui.fill(fx + 2, fy + 2, fx + size - 2, fy + size - 2, 0xE0100C14);
            gui.blit(FRAME_TEX, fx, fy, hot ? 32 : 0, 0, 32, 32, 64, 32);

            ItemStack stack = this.slots[e.slot];
            if (!stack.isEmpty()) {
                float k = hot ? 1.15F : 1.0F;
                gui.pose().pushPose();
                gui.pose().translate(sx - 8.0F * k, sy - 8.0F * k, 0.0F);
                gui.pose().scale(k, k, 1.0F);
                gui.renderItem(stack, 0, 0);
                gui.pose().popPose();
            }
        }

        if (cur != null) {
            // 标题：技能名（多技能时带妖丹名）
            String title = Component.translatable(cur.kind.nameKey()).getString();

            gui.pose().pushPose();
            gui.pose().translate(cx, cy - DISC_RADIUS - 44, 0.0F);
            gui.pose().scale(1.6F, 1.6F, 1.0F);
            gui.drawCenteredString(this.font, title, 0, 0, 0xFFFFF3C4);
            gui.pose().popPose();

            gui.drawCenteredString(this.font,
                    Component.translatable("info.fulu.essence.desc." + cur.kind.id()),
                    cx, cy - DISC_RADIUS - 20, 0xFFCFC6E0);
            gui.drawCenteredString(this.font,
                    Component.translatable("screen.fulu.wheel.hint"),
                    cx, cy + DISC_RADIUS + 12, 0xFF9A90B0);
        } else {
            gui.drawCenteredString(this.font,
                    this.entries.isEmpty()
                            ? Component.translatable("screen.fulu.wheel.empty")
                            : this.title,
                    cx, cy - DISC_RADIUS - 30, 0xFFB9A6D0);
        }

        gui.fill(cx - 90, cy - DISC_RADIUS - 30, cx + 90, cy - DISC_RADIUS - 29, 0x88B9A6D0);
        super.render(gui, mouseX, mouseY, partialTick);
    }

    private void disc(GuiGraphics gui, int cx, int cy, int r, int color) {
        for (int dy = -r; dy <= r; dy++) {
            int half = (int) Math.sqrt((double) r * r - (double) dy * dy);
            gui.fill(cx - half, cy + dy, cx + half, cy + dy + 1, color);
        }
    }

    private void ring(GuiGraphics gui, int cx, int cy, int r, int thick, int color) {
        int steps = Math.max(64, r * 3);
        for (int i = 0; i < steps; i++) {
            double a = Math.PI * 2.0D * i / steps;
            int x = (int) Math.round(cx + Math.cos(a) * r);
            int y = (int) Math.round(cy + Math.sin(a) * r);
            gui.fill(x, y, x + thick, y + thick, color);
        }
    }

    private void line(GuiGraphics gui, int x0, int y0, int x1, int y1, int color) {
        int dx = x1 - x0;
        int dy = y1 - y0;
        int steps = Math.max(Math.abs(dx), Math.abs(dy));
        for (int i = 0; i <= steps; i++) {
            int x = x0 + dx * i / Math.max(1, steps);
            int y = y0 + dy * i / Math.max(1, steps);
            gui.fill(x, y, x + 1, y + 1, color);
        }
    }

    /** 鼠标角度 → 条目下标。 */
    private int pick(double mouseX, double mouseY) {
        if (this.entries.isEmpty()) {
            return -1;
        }
        double cx = this.width / 2.0D;
        double cy = this.height / 2.0D;
        double dx = mouseX - cx;
        double dy = mouseY - cy;
        double dist = Math.sqrt(dx * dx + dy * dy);
        if (dist < 24.0D || dist > DISC_RADIUS + 60.0D) {
            return -1;
        }
        int n = this.entries.size();
        double deg = Math.toDegrees(Math.atan2(dy, dx)) + 90.0D;
        if (deg < 0.0D) {
            deg += 360.0D;
        }
        double sector = 360.0D / n;
        return (int) Math.floor((deg + sector / 2.0D) / sector) % n;
    }

    private void confirm() {
        if (this.hovered >= 0) {
            Entry e = this.entries.get(this.hovered);
            ClientEssence.setActiveSlotLocal(e.slot);
            ClientEssence.setSubSkill(0);        // 换妖丹后技能回到第一个
            ModNetwork.CHANNEL.sendToServer(new SelectSlotPacket(e.slot));
        }
        this.onClose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            this.hovered = pick(mouseX, mouseY);
            confirm();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_R) {
            confirm();
            return true;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
