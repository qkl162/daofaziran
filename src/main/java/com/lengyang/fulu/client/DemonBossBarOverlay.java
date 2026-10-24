package com.lengyang.fulu.client;

import com.lengyang.fulu.entity.FormationDemonEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 阵魔专属 Boss 血条（屏幕正上方）。
 *
 * <p>为什么不用原版 {@code ServerBossEvent}：原版那条只能纯色 + 刻度，做不出
 * 「暗红底 + 金饰 + 菱形扣」这种花纹。所以这里自己画一张贴图。
 *
 * <p>绘制分两层：
 * <ol>
 *   <li><b>填充层</b>：整条按血量比例用 {@code enableScissor} 裁掉右侧；</li>
 *   <li><b>外框层</b>：叠在上面，中间窗口是透明的，所以填充能透出来。</li>
 * </ol>
 *
 * <p>两张贴图同为 512x64，所以用同一个矩形、同一套 UV 直接叠，不需要额外对齐。
 */
public final class DemonBossBarOverlay {

    private static final ResourceLocation FRAME =
            new ResourceLocation("fulu", "textures/gui/boss_bar_frame.png");
    private static final ResourceLocation FILL =
            new ResourceLocation("fulu", "textures/gui/boss_bar_fill.png");

    private static final int TEX_W = 384;
    private static final int TEX_H = 32;

    /** 屏幕上的尺寸与位置：更细更长（120x24 -> 200x18）。 */
    private static final int BAR_W = 200;
    private static final int BAR_H = 18;
    private static final int TOP = 12;

    /** 贴图里左右各 4 像素是边框，填充只画在中间。 */
    private static final int PAD = 3;

    private DemonBossBarOverlay() {
    }

    public static void register() {
        MinecraftForge.EVENT_BUS.register(DemonBossBarOverlay.class);
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.options.hideGui) {
            return;
        }

        // 找 100 格内最近的一只阵魔
        FormationDemonEntity demon = null;
        double best = 100.0D * 100.0D;
        for (FormationDemonEntity e : mc.level.getEntitiesOfClass(
                FormationDemonEntity.class, mc.player.getBoundingBox().inflate(100.0D))) {
            double d = e.distanceToSqr(mc.player);
            if (d < best) {
                best = d;
                demon = e;
            }
        }
        if (demon == null) {
            return;
        }

        GuiGraphics gui = event.getGuiGraphics();
        int screenW = gui.guiWidth();
        int x = (screenW - BAR_W) / 2;
        int y = TOP;

        float progress = demon.getHealth() / Math.max(1.0F, demon.getMaxHealth());
        progress = Math.max(0.0F, Math.min(1.0F, progress));

        // ---- 填充层（按血量裁切）
        int winLeft = x + Math.round(BAR_W * (float) PAD / TEX_W);
        int winWidth = Math.round(BAR_W * (float) (TEX_W - PAD * 2) / TEX_W);
        int filled = Math.round(winWidth * progress);
        if (filled > 0) {
            // 按阶段给**填充层**上色（金框保持原色，所以只染填充）
            //   一阶段：暖金（试探）
            //   二阶段：血红（愤怒）
            //   三阶段：紫金（狂暴）
            float[] tint = switch (demon.getPhase()) {
                case 3 -> new float[]{0.92F, 0.62F, 1.00F};
                case 2 -> new float[]{1.00F, 0.52F, 0.42F};
                default -> new float[]{1.00F, 0.90F, 0.68F};
            };
            gui.setColor(tint[0], tint[1], tint[2], 1.0F);
            gui.enableScissor(winLeft, y, winLeft + filled, y + BAR_H);
            gui.blit(FILL, x, y, BAR_W, BAR_H, 0.0F, 0.0F, TEX_W, TEX_H, TEX_W, TEX_H);
            gui.disableScissor();
            gui.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        }

        // ---- 外框层
        gui.blit(FRAME, x, y, BAR_W, BAR_H, 0.0F, 0.0F, TEX_W, TEX_H, TEX_W, TEX_H);

        // ---- 名字（金色，压在框上方）
        Component name = demon.getDisplayName();
        gui.drawCenteredString(mc.font, name, screenW / 2, y - 9, 0xFFF0D682);
    }
}
