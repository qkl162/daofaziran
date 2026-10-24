package com.lengyang.fulu.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 冥气反噬：放完【凋零断魂斩】后，屏幕**边缘短暂蔓延黑色**（约 1 秒）。
 *
 * <p>实现上没有做真正的"蔓延"（那要 shader），而是画四边渐变黑框 +
 * 透明度随时间衰减：视觉上就是"黑色从边缘往里压，然后散掉"。
 *
 * <p>触发方式：{@link RefiningKeys} 在按下施法键且当前妖丹是凋零骷髅时，
 * 调用 {@link #trigger()}（读条 0.5 秒后才是挥砍，所以延迟 10 刻再开始显示）。
 */
@Mod.EventBusSubscriber(modid = "fulu", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class SoulSlashOverlay {

    /** 触发前的等待（刻）：读条 0.5 秒。 */
    private static int delay;
    /** 正在显示的剩余刻。 */
    private static int showing;

    private SoulSlashOverlay() {
    }

    /** 由客户端按键在施法时调用。 */
    public static void trigger() {
        delay = 20;
        showing = 0;
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) {
            return;
        }
        if (delay > 0) {
            delay--;
            if (delay == 0) {
                showing = 20;                    // 1 秒
            }
            return;
        }
        if (showing <= 0) {
            return;
        }
        showing--;

        GuiGraphics gui = event.getGuiGraphics();
        int w = gui.guiWidth();
        int h = gui.guiHeight();
        // 透明度：开始最浓，随后线性衰减
        float k = showing / 20.0F;
        int layers = 12;
        int band = Math.max(2, (int) (h * 0.16F));
        for (int i = 0; i < layers; i++) {
            int a = (int) (0x66 * k * (1.0F - i / (float) layers));
            if (a <= 0) {
                continue;
            }
            int color = (a << 24) | 0x0A0008;
            int off = band * i / layers;
            int y0 = off;
            int y1 = off + band / layers + 1;
            // 上
            gui.fill(0, y0, w, y1, color);
            // 下
            gui.fill(0, h - y1, w, h - y0, color);
            // 左
            gui.fill(y0, 0, y1, h, color);
            // 右
            gui.fill(w - y1, 0, w - y0, h, color);
        }
    }
}
