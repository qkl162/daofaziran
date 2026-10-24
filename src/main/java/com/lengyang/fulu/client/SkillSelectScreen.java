package com.lengyang.fulu.client;

import com.lengyang.fulu.refining.EssenceKind;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

/**
 * 选技能（**V 键**）：**和妖丹轮盘同款**的圆盘 + 方框槽位。
 *
 * <p>区别只在内容：妖丹轮盘画的是"每个格子的妖丹"，这里画的是"当前妖丹的每个技能"。
 * 视觉元素完全复用（同一个 `spell_frame.png`、同样的圆盘/射线/标题）。
 */
public class SkillSelectScreen extends Screen {

    private static final int RING_RADIUS = 76;
    private static final int DISC_RADIUS = 120;
    private static final int FRAME = 30;
    private static final int FRAME_BIG = 36;

    private static final int DISC_FILL = 0xC8140F18;
    private static final int DISC_EDGE = 0xFF6E5A8A;
    private static final int DISC_EDGE_IN = 0x88B9A6D0;
    private static final int SPOKE = 0x66E8E0F0;
    private static final int CROSS = 0x99FFF3C4;

    private static final ResourceLocation FRAME_TEX =
            new ResourceLocation("fulu", "textures/gui/spell_frame.png");

    private final int slot;
    private final EssenceKind kind;
    private int hovered = -1;
    /** 最后一帧的鼠标位置（keyReleased 时没有鼠标参数，只能自己记）。 */
    private double lastMouseX;
    private double lastMouseY;

    public SkillSelectScreen(int slot, EssenceKind kind) {
        super(Component.translatable("screen.fulu.skills.title"));
        this.slot = slot;
        this.kind = kind;
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gui);
        int cx = this.width / 2;
        int cy = this.height / 2;

        String[] skills = kind.skills();
        int n = Math.max(1, skills.length);
        this.lastMouseX = mouseX;
        this.lastMouseY = mouseY;
        this.hovered = pick(mouseX, mouseY);

        disc(gui, cx, cy, DISC_RADIUS, DISC_FILL);
        ring(gui, cx, cy, DISC_RADIUS, 2, DISC_EDGE);
        ring(gui, cx, cy, DISC_RADIUS - 6, 1, DISC_EDGE_IN);
        gui.fill(cx - 10, cy - 1, cx + 10, cy + 1, CROSS);
        gui.fill(cx - 1, cy - 10, cx + 1, cy + 10, CROSS);
        ring(gui, cx, cy, 14, 1, CROSS);

        ItemStack icon = new ItemStack(kind.item());
        for (int i = 0; i < skills.length; i++) {
            double a = Math.PI * 2.0D * i / n - Math.PI / 2.0D;
            int sx = (int) Math.round(cx + Math.cos(a) * RING_RADIUS);
            int sy = (int) Math.round(cy + Math.sin(a) * RING_RADIUS);

            line(gui, cx, cy, sx, sy, SPOKE);

            boolean hot = (i == this.hovered);
            boolean selected = ClientEssence.subSkill() == i;
            int size = hot ? FRAME_BIG : FRAME;
            int fx = sx - size / 2;
            int fy = sy - size / 2;

            gui.fill(fx + 2, fy + 2, fx + size - 2, fy + size - 2, 0xE0100C14);
            gui.blit(FRAME_TEX, fx, fy, (hot || selected) ? 32 : 0, 0, 32, 32, 64, 32);

            float k = hot ? 1.15F : 1.0F;
            gui.pose().pushPose();
            gui.pose().translate(sx - 8.0F * k, sy - 8.0F * k, 0.0F);
            gui.pose().scale(k, k, 1.0F);
            gui.renderItem(icon, 0, 0);
            gui.pose().popPose();

            // 每个技能自己的冷却
            int cd = ClientEssence.cooldownSeconds(slot, i);
            if (cd > 0) {
                gui.fill(fx + 3, fy + 3, fx + size - 3, fy + size - 3, 0x99000000);
                gui.drawCenteredString(this.font, String.valueOf(cd), sx, sy - 4, 0xFFFF8080);
            }
        }

        // 标题：当前悬停的技能名
        String title = Component.translatable(kind.nameKey()).getString();
        String sub = null;
        if (this.hovered >= 0) {
            sub = Component.translatable("skill.fulu." + skills[this.hovered]).getString();
        }
        gui.pose().pushPose();
        gui.pose().translate(cx, cy - DISC_RADIUS - 40, 0.0F);
        gui.pose().scale(1.5F, 1.5F, 1.0F);
        gui.drawCenteredString(this.font, sub != null ? sub : title, 0, 0, 0xFFFFF3C4);
        gui.pose().popPose();

        gui.drawCenteredString(this.font,
                Component.translatable("screen.fulu.skills.hint"),
                cx, cy + DISC_RADIUS + 12, 0xFF9A90B0);
        gui.fill(cx - 80, cy - DISC_RADIUS - 26, cx + 80, cy - DISC_RADIUS - 25, 0x88B9A6D0);

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

    /** 鼠标角度 → 技能下标。 */
    private int pick(double mouseX, double mouseY) {
        String[] skills = kind.skills();
        if (skills.length == 0) {
            return -1;
        }
        double cx = this.width / 2.0D;
        double cy = this.height / 2.0D;
        double dx = mouseX - cx;
        double dy = mouseY - cy;
        double dist = Math.sqrt(dx * dx + dy * dy);
        if (dist < 22.0D || dist > DISC_RADIUS + 60.0D) {
            return -1;
        }
        int n = skills.length;
        double deg = Math.toDegrees(Math.atan2(dy, dx)) + 90.0D;
        if (deg < 0.0D) {
            deg += 360.0D;
        }
        double sector = 360.0D / n;
        return (int) Math.floor((deg + sector / 2.0D) / sector) % n;
    }

    private void confirm() {
        // 没指向任何槽位时**保持原技能**，不要把选择清掉
        if (this.hovered >= 0) {
            ClientEssence.setSubSkill(this.hovered);
            var skills = this.kind.skills();
            if (net.minecraft.client.Minecraft.getInstance().player != null) {
                net.minecraft.client.Minecraft.getInstance().player.displayClientMessage(
                        net.minecraft.network.chat.Component.translatable(
                                "msg.fulu.skill.selected",
                                net.minecraft.network.chat.Component.translatable(
                                        "skill.fulu." + skills[this.hovered])),
                        true);
            }
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
        if (keyCode == GLFW.GLFW_KEY_V) {
            // 用**鼠标实际位置**判断（之前固定取正上方 -> 一松手就总是选中第 1 个技能）
            this.hovered = pick(this.lastMouseX, this.lastMouseY);
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
