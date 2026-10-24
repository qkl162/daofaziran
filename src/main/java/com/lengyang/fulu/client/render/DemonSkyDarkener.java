package com.lengyang.fulu.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import com.lengyang.fulu.entity.FormationDemonEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 三阶段天空压暗。
 *
 * <p>做法分两层，互相配合：
 * <ol>
 *   <li><b>天幕压暗</b>：在 {@code AFTER_SKY} 阶段画一个巨大的暗紫色内球面。
 *       这个阶段在天空之后、地形之前，所以它盖住天空，但地形画上去时会正常覆盖它 ——
 *       结果是"天黑了，地还看得见"。</li>
 *   <li><b>雾色压暗</b>：改 {@code ComputeFogColor}，让远处雾也变暗，
 *       远景不会出现"天上黑、地平线却发白"的割裂。</li>
 * </ol>
 *
 * <p>强度按距离平滑：越近越暗，32 格内达到最大，96 格外完全没有。
 */
public final class DemonSkyDarkener {

    /** 生效半径：超过这个距离不受影响。 */
    private static final double RANGE = 96.0D;
    /** 满强度半径：这个距离内达到最大压暗。 */
    private static final double FULL = 32.0D;
    /** 最大压暗强度（0~1）。 */
    private static final float MAX_DARK = 0.82F;

    /** 平滑后的当前强度，逐帧靠近目标值，避免开关式跳变。 */
    private static float current;

    private DemonSkyDarkener() {
    }

    public static void register() {
        MinecraftForge.EVENT_BUS.register(DemonSkyDarkener.class);
    }

    /** 当前压暗强度（0 = 正常，1 = 最暗）。 */
    private static float targetStrength() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return 0.0F;
        }
        double best = RANGE;
        boolean found = false;
        for (FormationDemonEntity e : mc.level.getEntitiesOfClass(
                FormationDemonEntity.class, mc.player.getBoundingBox().inflate(RANGE))) {
            if (e.getPhase() < 3) {
                continue;
            }
            double d = e.distanceTo(mc.player);
            if (d < best) {
                best = d;
                found = true;
            }
        }
        if (!found) {
            return 0.0F;
        }
        if (best <= FULL) {
            return 1.0F;
        }
        return (float) ((RANGE - best) / (RANGE - FULL));
    }

    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        float s = current * MAX_DARK;
        if (s <= 0.01F) {
            return;
        }
        // 往暗紫偏，而不是单纯变黑
        float k = 1.0F - s;
        event.setRed(event.getRed() * k + 0.05F * s);
        event.setGreen(event.getGreen() * k + 0.03F * s);
        event.setBlue(event.getBlue() * k + 0.09F * s);
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        // 每帧更新一次平滑强度
        float target = targetStrength();
        current += (target - current) * 0.06F;
        if (current < 0.01F) {
            current = 0.0F;
        }

        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) {
            return;
        }
        if (current <= 0.01F) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }

        Vec3 cam = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();      // 天空没有深度，直接盖
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        float a = current * MAX_DARK;
        float r = 0.055F;
        float g = 0.030F;
        float b = 0.105F;

        // 半径 220 的内球面（只要在雾外的天空区域即可）
        final double rad = 220.0D;
        final int lon = 48;
        final int lat = 24;
        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = tess.getBuilder();
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (int i = 0; i < lon; i++) {
            double l0 = Math.PI * 2.0D * i / lon;
            double l1 = Math.PI * 2.0D * (i + 1) / lon;
            for (int j = -6; j < lat; j++) {
                double a0 = Math.PI * (j / (double) lat);
                double a1 = Math.PI * ((j + 1) / (double) lat);
                double y0 = Math.cos(a0) * rad;
                double y1 = Math.cos(a1) * rad;
                double r0 = Math.sin(a0) * rad;
                double r1 = Math.sin(a1) * rad;
                v(buf, pose, cam.x + Math.cos(l0) * r0, cam.y + y0, cam.z + Math.sin(l0) * r0, r, g, b, a);
                v(buf, pose, cam.x + Math.cos(l1) * r0, cam.y + y0, cam.z + Math.sin(l1) * r0, r, g, b, a);
                v(buf, pose, cam.x + Math.cos(l1) * r1, cam.y + y1, cam.z + Math.sin(l1) * r1, r, g, b, a);
                v(buf, pose, cam.x + Math.cos(l0) * r1, cam.y + y1, cam.z + Math.sin(l0) * r1, r, g, b, a);
            }
        }
        BufferUploader.drawWithShader(buf.end());

        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        pose.popPose();
    }

    private static void v(BufferBuilder buf, PoseStack pose,
                          double x, double y, double z, float r, float g, float b, float a) {
        buf.vertex(pose.last().pose(), (float) x, (float) y, (float) z).color(r, g, b, a).endVertex();
    }
}
