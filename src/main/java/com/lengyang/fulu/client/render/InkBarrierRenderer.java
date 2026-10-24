package com.lengyang.fulu.client.render;

import com.lengyang.fulu.block.FormationEyeBlock;
import com.lengyang.fulu.block.TalismanKind;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;

/**
 * 水墨阵的屏障：把一张水墨山水照片铺成**远处的天地**，让人站在画里。
 *
 * <p>为了"身临其境"，做了四件事：
 * <ol>
 *   <li><b>半径 40</b>：不再是 10 格的近身球壁（那看起来像贴了壁纸的球），
 *       而是像几十格外的远山；</li>
 *   <li><b>地平线对齐视线</b>：照片的山水交界处（约在图的 58% 高度）正好落在
 *       玩家眼睛的高度 —— 平视是远山云雾，抬头是乌云天，低头是墨色漩涡；</li>
 *   <li><b>不被地形切碎</b>：关掉深度测试，整幅画完整覆盖视野，
 *       不会出现山从方块缝里透出来的破绽；</li>
 *   <li><b>不透明</b>：里外互不可见，像真的进到了另一个世界。</li>
 * </ol>
 *
 * <p>球心固定在水墨阵眼（这是阵法的位置），只把高度对准玩家视线，
 * 所以走动时画**钉在原地**，不会黏在人身上。
 */
public final class InkBarrierRenderer {

    private static final ResourceLocation INK_TEX =
            new ResourceLocation("fulu", "textures/block/ink_scroll.png");

    /** 半径：够远才像远山，又不至于超出渲染距离。 */
    private static final double R = 24.0D;            // 包裹式：24 格，抬头低头都还在画里

    /** 照片里山水交界的高度比例（0 = 顶部，1 = 底部）。 */
    private static final double HORIZON_V = 0.62D;    // 画面里山脚/云带的位置
    private static final double TOP_DEG = 89.5D;
    private static final double BOTTOM_DEG = 0.0D;    // 只画地平线以上：地面/方块永远可见，用来当参照

    private static final int LON_STEPS = 96;
    private static final int LAT_STEPS = 36;

    private InkBarrierRenderer() {
    }

    public static void register() {
        MinecraftForge.EVENT_BUS.register(InkBarrierRenderer.class);
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }

        BlockPos nearest = null;
        double best = Double.MAX_VALUE;
        for (BlockPos pos : FormationBarrierRenderer.eyesView()) {
            if (kindAt(mc.level, pos) != TalismanKind.INK) {
                continue;
            }
            double d = pos.distToCenterSqr(mc.player.position());
            if (d < best) {
                best = d;
                nearest = pos;
            }
        }
        if (nearest == null) {
            return;
        }

        Vec3 cam = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);

        // 不透明 + 关深度测试：整幅画完整铺满视野，像一个真实的世界
        RenderSystem.disableBlend();
        RenderSystem.depthMask(false);
        RenderSystem.disableDepthTest();     // 包裹式：整幅画覆盖视野，地形不会从缝里透出来
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, INK_TEX);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        // 球心**完全固定在阵眼**，x/y/z 都不看玩家。
        // 之前高度用了 player.getEyeY()，结果人一走一跳，整幅画跟着上下浮动，
        // 看起来就像"画跟着玩家跑"。现在三个轴都钉死。
        double cx = nearest.getX() + 0.5D;
        double cy = nearest.getY() + 1.62D;   // 站立时的视线高度，固定值
        double cz = nearest.getZ() + 0.5D;

        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = tess.getBuilder();
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        dome(buf, pose, cx, cy, cz);
        BufferUploader.drawWithShader(buf.end());

        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        pose.popPose();
    }

    private static TalismanKind kindAt(Level level, BlockPos pos) {
        BlockState st = level.getBlockState(pos);
        if (st.getBlock() instanceof FormationEyeBlock) {
            return st.getValue(FormationEyeBlock.TALISMAN);
        }
        return TalismanKind.EMPTY;
    }

    /**
     * 球面。u 绕一圈首尾相接；v 分两段：
     * 地平线以上映射到 0..HORIZON_V（乌云天），以下映射到 HORIZON_V..1（水墨漩涡）。
     */
    private static void dome(BufferBuilder buf, PoseStack pose, double cx, double cy, double cz) {
        for (int i = 0; i < LON_STEPS; i++) {
            double lon0 = Math.PI * 2.0D * i / LON_STEPS;
            double lon1 = Math.PI * 2.0D * (i + 1) / LON_STEPS;
            double u0 = (double) i / LON_STEPS;
            double u1 = (double) (i + 1) / LON_STEPS;

            for (int j = 0; j < LAT_STEPS; j++) {
                double deg0 = BOTTOM_DEG + (TOP_DEG - BOTTOM_DEG) * j / LAT_STEPS;
                double deg1 = BOTTOM_DEG + (TOP_DEG - BOTTOM_DEG) * (j + 1) / LAT_STEPS;
                double a0 = Math.toRadians(deg0);
                double a1 = Math.toRadians(deg1);
                double v0 = vFor(deg0);
                double v1 = vFor(deg1);
                double y0 = cy + Math.sin(a0) * R;
                double y1 = cy + Math.sin(a1) * R;
                double r0 = Math.cos(a0) * R;
                double r1 = Math.cos(a1) * R;

                v(buf, pose, cx + Math.cos(lon0) * r0, y0, cz + Math.sin(lon0) * r0, u0, v0);
                v(buf, pose, cx + Math.cos(lon1) * r0, y0, cz + Math.sin(lon1) * r0, u1, v0);
                v(buf, pose, cx + Math.cos(lon1) * r1, y1, cz + Math.sin(lon1) * r1, u1, v1);
                v(buf, pose, cx + Math.cos(lon0) * r1, y1, cz + Math.sin(lon0) * r1, u0, v1);
            }
        }
    }

    /** 纬度（度）-> 贴图纵坐标：地平线正好落在照片的山水交界处。 */
    private static double vFor(double deg) {
        if (deg >= 0.0D) {
            return HORIZON_V * (1.0D - deg / TOP_DEG);
        }
        return HORIZON_V + (1.0D - HORIZON_V) * (-deg / -BOTTOM_DEG);
    }

    private static void v(BufferBuilder buf, PoseStack pose,
                          double x, double y, double z, double u, double v) {
        buf.vertex(pose.last().pose(), (float) x, (float) y, (float) z)
                .uv((float) u, (float) v)
                .endVertex();
    }
}
