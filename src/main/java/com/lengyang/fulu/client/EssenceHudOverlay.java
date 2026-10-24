package com.lengyang.fulu.client;

import com.lengyang.fulu.refining.EssenceKind;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 右下角 HUD：**当前选中的妖丹** + 技能名 + **扇形冷却**。
 *
 * <p>冷却的画法参考原版物品栏的冷却：从 12 点方向开始**顺时针**盖住，
 * 随着时间推移扇形缩小；结束的瞬间图标闪一下白光并"叮"一声。
 *
 * <p>位置放在快捷栏右上方，视线不用离开准星太多；没有妖丹匣 → 整块不画。
 */
@Mod.EventBusSubscriber(modid = "fulu", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class EssenceHudOverlay {

    private static final int SIZE = 26;
    private static final int MARGIN = 8;
    private static final int BOTTOM_OFFSET = 52;

    /** 冷却结束时的闪白剩余刻。 */
    private static int flashTicks;
    /** 上一帧的冷却比例（用来检测"刚好冷却完"）。 */
    private static float lastFraction;

    private EssenceHudOverlay() {
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) {
            return;
        }
        if (ClientEssence.findBox().isEmpty()) {
            lastFraction = 0.0F;
            return;
        }
        int slot = ClientEssence.activeSlot();
        EssenceKind kind = ClientEssence.kindAt(slot);
        if (kind == null) {
            return;
        }

        GuiGraphics gui = event.getGuiGraphics();
        int sw = gui.guiWidth();
        int sh = gui.guiHeight();
        int x = sw - SIZE - MARGIN;
        int y = sh - SIZE - BOTTOM_OFFSET;

        // ---- 底板 + 金边
        gui.fill(x - 3, y - 3, x + SIZE + 3, y + SIZE + 3, 0xC0100A14);
        gui.fill(x - 3, y - 3, x + SIZE + 3, y - 1, 0xFFE8C86A);
        gui.fill(x - 3, y + SIZE + 1, x + SIZE + 3, y + SIZE + 3, 0xFFE8C86A);
        gui.fill(x - 3, y - 3, x - 1, y + SIZE + 3, 0xFFE8C86A);
        gui.fill(x + SIZE + 1, y - 3, x + SIZE + 3, y + SIZE + 3, 0xFFE8C86A);

        // ---- 妖丹图标
        gui.renderItem(new ItemStack(kind.item()), x + 5, y + 5);

        // ---- 扇形冷却
        float frac = ClientEssence.cooldownFraction(slot, ClientEssence.subSkill());
        if (frac > 0.0F) {
            drawCooldownArc(gui, x, y, SIZE, frac);
        }
        // 刚好冷却完：闪白 + 叮
        if (lastFraction > 0.02F && frac <= 0.0F) {
            flashTicks = 8;
            if (mc.player != null) {
                mc.player.playSound(SoundEvents.NOTE_BLOCK_CHIME.value(), 0.8F, 2.0F);
            }
        }
        lastFraction = frac;

        if (flashTicks > 0) {
            flashTicks--;
            int a = (int) (0xB0 * (flashTicks / 8.0F));
            gui.fill(x, y, x + SIZE, y + SIZE, (a << 24) | 0xFFFFFF);
        }

        // ---- 技能名：多技能妖丹显示**当前选中的那个技能**
        String[] skills = kind.skills();
        int sub = Math.max(0, Math.min(skills.length - 1, ClientEssence.subSkill()));
        Component label = skills.length > 1
                ? Component.translatable("skill.fulu." + skills[sub])
                : Component.translatable("hud.fulu.skill." + kind.id());
        gui.drawCenteredString(mc.font, label, x + SIZE / 2, y + SIZE + 5, 0xFFDDDDDD);
    }

    /**
     * 画扇形遮罩：从 12 点开始顺时针盖住 {@code fraction} 的比例。
     *
     * <p>做法是逐像素判断角度与半径 —— 26×26 的小方块，逐像素完全够用，
     * 也不用引入 RenderType（这个模组的渲染坑已经够多了）。
     */
    private static void drawCooldownArc(GuiGraphics gui, int x, int y, int size, float fraction) {
        double cx = size / 2.0D;
        double cy = size / 2.0D;
        double r = size / 2.0D;
        for (int py = 0; py < size; py++) {
            for (int px = 0; px < size; px++) {
                double dx = px + 0.5D - cx;
                double dy = py + 0.5D - cy;
                if (dx * dx + dy * dy > r * r) {
                    continue;
                }
                // 角度：12 点方向为 0，顺时针增加，归一化到 0~1
                double ang = Math.atan2(dx, -dy);
                double turn = (ang + Math.PI * 2.0D) % (Math.PI * 2.0D) / (Math.PI * 2.0D);
                if (turn < fraction) {
                    gui.fill(x + px, y + py, x + px + 1, y + py + 1, 0xC8000000);
                }
            }
        }
    }
}
