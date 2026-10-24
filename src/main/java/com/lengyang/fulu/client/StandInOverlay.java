package com.lengyang.fulu.client;

import com.lengyang.fulu.FuLu;
import com.lengyang.fulu.standin.ClientStandInData;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import com.mojang.blaze3d.systems.RenderSystem;

/**
 * 替身阵在客户端画的东西。
 *
 * <p><b>次数盾血条</b>（十一）：在**原版血条上方另加一条** —— 原版红心照常画，
 * 这一条用的是**原版的心形图标**（同一张 {@code textures/gui/icons.png}），
 * 只是把红色**染成绿色**，因为用户要的就是"利用原版的红色心形血条，把红色改成绿色"。
 *
 * <p>为什么染色而不是另画一套心形：原版心形有三帧（空壳 / 满 / 半），
 * 自己画一套既多余、又不可能和原版的像素风格一模一样。用
 * {@code RenderSystem.setShaderColor} 把整张图染绿，连高光都跟着变，最省也最像。
 *
 * <p>格子：5 格，每格 4 点（用户要求），所以是 5 颗心；4 点 = 满心，2~3 点 = 半心，
 * 0~1 点 = 空壳。位置在血条正上方 2 像素，左边和血条对齐。
 *
 * <p>全屏碎裂图案：屏幕正中，256×256 贴图，先冲到 1.5 倍再平滑回落，总 1.8 秒。
 *
 * <p>注册是**显式**的（见 {@code ModClientSetup}）：注解扫描没命中就什么都不画，而且毫无提示。
 */
public final class StandInOverlay {

    private static final ResourceLocation SHATTER =
            new ResourceLocation(FuLu.MOD_ID, "textures/gui/stand_in_shatter.png");

    /**
     * 我们自己烘的「绿色心形」贴图：27×9 的三帧（u=0 空壳 / u=9 满 / u=18 半）。
     *
     * <p>为什么不用原版 {@code icons.png} 染绿：{@code setShaderColor} 是**乘法**，
     * 红心 (1,0,0) 乘 (0.3,1,0.35) 只会得到暗红，变不成绿。所以从原版抠出三帧、
     * 保留明暗、把色相换成绿色，烘成自己的贴图（见 {@code tools/gen_shield_hearts.py}）。
     */
    private static final ResourceLocation SHIELD_HEARTS =
            new ResourceLocation(FuLu.MOD_ID, "textures/gui/shield_hearts.png");

    /** 我们贴图里三帧的 u 坐标。 */
    private static final int HEART_EMPTY_U = 0;
    private static final int HEART_FULL_U = 9;
    private static final int HEART_HALF_U = 18;
    private static final int HEART_V = 0;
    private static final int HEART_SIZE = 9;
    /** 原版每颗心横向只前进 8 像素（9 宽、互相压一像素），照抄才和原版一个观感。 */
    private static final int HEART_STEP = 8;
    /** ?????????27x9??blit ???? UV?????"??????"? */
    private static final int HEART_SHEET_W = 27;
    private static final int HEART_SHEET_H = 9;

    /** 图案占屏幕高度的比例（固定像素在大屏上会显得很小）。 */
    private static final float PATTERN_SCREEN_RATIO = 0.55F;

    /** 动画总时长（秒），和 {@link ClientStandInData#shatterSeconds()} 的上限一致。 */
    private static final float ANIMATION_SECONDS = 1.8F;

    /** 绿色血条整体往上挪多少像素（和别的模组界面冲突时改这里）。 */
    private static final int SHIELD_BAR_OFFSET_Y = 0;

    /** 原版血量条的位置（1.20.1 的常量）。 */
    private static final int VANILLA_BAR_WIDTH = 81;
    private static final int VANILLA_BAR_TOP = 39;      // 距离屏幕底部 39 像素

    /** 10 颗心，每颗 2 点（20 点 = 10 颗）—— 和原版血条一样宽、一样对齐。 */
    private static final int CELLS = 10;
    private static final int POINTS_PER_CELL = 2;

    /** 染色：把红色心染成绿色（白高光也会跟着偏绿，正好）。 */
    private static final float TINT_R = 0.30F;
    private static final float TINT_G = 1.00F;
    private static final float TINT_B = 0.35F;

    /** 把红色心染成绿色是做不到的（染色是乘法），所以用自己烘的绿心贴图。 */
    private static int lastLoggedPoints = -1;

    private StandInOverlay() {
    }

    /** 由 {@code ModClientSetup} 在客户端启动时调用一次（显式注册，不靠注解扫描）。 */
    public static void register() {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(StandInOverlay.class);
    }

    /** 诊断用：每 N 次渲染打一行心跳（默认每秒左右一次）。 */
    private static int heartbeatCounter = 0;

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        GuiGraphics gui = event.getGuiGraphics();
        // 心跳：证明"这个事件确实在跑"，并带上它看到的点数 ——
        // 有了这两样，"覆盖层没跑" / "点数没到" / "画了但看不见" 才分得开。
        if (++heartbeatCounter >= 60) {
            heartbeatCounter = 0;
            com.lengyang.fulu.FuLu.LOGGER.debug(
                    "[standin-hud] alive: shield={} (frame {}x{})",
                    ClientStandInData.hitShield(), gui.guiWidth(), gui.guiHeight());
        }
        drawShieldHearts(gui, gui.guiWidth(), gui.guiHeight());
        drawShatterPattern(gui, gui.guiWidth(), gui.guiHeight());
    }

    /**
     * 在**原版血条上方**画 5 颗绿心（原版红心不动）。
     *
     * <p>4 点一颗心：20 点 = 5 颗满心；12 点 = 3 满 + 1 空壳（12 是 3 颗整 + 0 余，所以是 3 满 2 空）；
     * 14 点 = 3 满 + 1 半 + 1 空。
     */
    private static void drawShieldHearts(GuiGraphics gui, int width, int height) {
        int points = ClientStandInData.hitShield();
        if (points <= 0) {
            if (points != lastLoggedPoints) {
                com.lengyang.fulu.FuLu.LOGGER.debug(
                        "[standin-hud] nothing to draw: shield points = {}", points);
                lastLoggedPoints = points;
            }
            return;                                     // 没盾：什么都不画（原版血条本来就在）
        }

        int fullHearts = points / POINTS_PER_CELL;                     // 4 点 = 1 颗
        boolean halfHeart = (points % POINTS_PER_CELL) >= 1;                     // 剩 1 点 = 半颗

        // 诊断（默认开）：点数一变就记一行，连同算好的位置，方便确认"画了但位置不对"还是"没画"
        if (points != lastLoggedPoints) {
            int dbgY = height - VANILLA_BAR_TOP - HEART_SIZE - 2 - SHIELD_BAR_OFFSET_Y;
            com.lengyang.fulu.FuLu.LOGGER.debug(
                    "[standin-hud] drawing green hearts: points={} full={} half={} "
                            + "at x={} y={} (screen {}x{})",
                    points, fullHearts, halfHeart, (width - VANILLA_BAR_WIDTH) / 2, dbgY,
                    width, height);
            lastLoggedPoints = points;
        }

        int x0 = (width - VANILLA_BAR_WIDTH) / 2;                      // 和血条左对齐
        int y = height - VANILLA_BAR_TOP - HEART_SIZE - 2 - SHIELD_BAR_OFFSET_Y;   // 正上方 2 像素
        // 10 颗 × 8 像素 = 80，原版血条 81 —— 最后一颗会少 1 像素，所以整体再右移 0 像素即可对齐

        RenderSystem.enableBlend();
        for (int i = 0; i < CELLS; i++) {
            int x = x0 + i * HEART_STEP;
            // 先画空壳，再把满/半盖上去 —— 原版就是这么叠的
            // ??? 9 ????7 ???**????? 256x256**?
            // ????????? 27x9 ?? ?????? UV ?? 9/256 ????
            // ???????????????????????????????
            gui.blit(SHIELD_HEARTS, x, y, HEART_EMPTY_U, HEART_V,
                    HEART_SIZE, HEART_SIZE, HEART_SHEET_W, HEART_SHEET_H);
            if (i < fullHearts) {
                gui.blit(SHIELD_HEARTS, x, y, HEART_FULL_U, HEART_V,
                        HEART_SIZE, HEART_SIZE, HEART_SHEET_W, HEART_SHEET_H);
            } else if (i == fullHearts && halfHeart) {
                gui.blit(SHIELD_HEARTS, x, y, HEART_HALF_U, HEART_V,
                        HEART_SIZE, HEART_SIZE, HEART_SHEET_W, HEART_SHEET_H);
            }
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    /**
     * 全屏碎裂：淡入 → 快速放大到 1.5 倍 → 回落 1.0 → 淡出，全程连续曲线。
     */
    private static void drawShatterPattern(GuiGraphics gui, int width, int height) {
        float seconds = ClientStandInData.shatterSeconds();
        if (seconds < 0.0F) {
            return;
        }

        float baseSize = height * PATTERN_SCREEN_RATIO;

        float scale;
        float growTime = 0.35F;
        if (seconds < growTime) {
            float k = seconds / growTime;
            float ease = 1.0F - (1.0F - k) * (1.0F - k) * (1.0F - k);   // easeOutCubic
            scale = 0.7F + 0.8F * ease;                                  // 0.7 -> 1.5
        } else {
            float k = Math.min(1.0F, (seconds - growTime) / (ANIMATION_SECONDS - growTime));
            float ease = 1.0F - (1.0F - k) * (1.0F - k);
            scale = 1.5F - 0.5F * ease;                                  // 1.5 -> 1.0
        }

        float alpha = Math.min(1.0F, seconds / 0.15F);
        float fadeStart = ANIMATION_SECONDS - 0.3F;
        if (seconds > fadeStart) {
            alpha = Math.min(alpha, 1.0F - (seconds - fadeStart) / 0.3F);
        }
        alpha = Math.max(0.0F, Math.min(1.0F, alpha));

        int size = Math.round(baseSize * scale);
        int x = (width - size) / 2;
        int y = (height - size) / 2;

        gui.pose().pushPose();
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
        gui.blit(SHATTER, x, y, 0, 0, size, size, size, size);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
        gui.pose().popPose();
    }
}
