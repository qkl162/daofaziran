package com.lengyang.fulu.client.render;

import com.lengyang.fulu.client.model.FormationDemonGeoModel;
import com.lengyang.fulu.entity.FormationDemonEntity;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * 阵魔渲染（GeckoLib）+ 它自己的「破碎屏障」和「破碎星空」。
 *
 * <p>这两样都是**纯视觉**，不阻挡玩家（用户规格第十一条）。
 * 画法沿用项目里其它阵法渲染的写法：直接开 GL 状态 + 内置的 {@code position_color} 着色器，
 * 不自定义 RenderType —— 这样不会和其它模组的渲染管线打架。
 *
 * <p>绘制顺序：GeoLib 模型 → 屏障圆环 → 头顶星空 → 偶尔闪过的龙影。
 * 死亡时按 {@code deathTime} 淡出，做到"屏障和星空一起碎裂消失"。
 */
public class FormationDemonGeoRenderer extends GeoEntityRenderer<FormationDemonEntity> {

    /** 星空里的星点（固定随机，避免每帧乱跳）：{角度, 半径, 亮度} */
    private static final float[][] STARS = {
            {0.35F, 0.55F, 1.00F}, {0.90F, 0.95F, 0.75F}, {1.55F, 0.70F, 0.90F},
            {2.10F, 1.15F, 0.65F}, {2.75F, 0.60F, 0.85F}, {3.30F, 1.00F, 0.95F},
            {3.95F, 0.80F, 0.70F}, {4.50F, 1.25F, 0.80F}, {5.10F, 0.65F, 1.00F},
            {5.70F, 1.05F, 0.60F}, {6.00F, 0.45F, 0.90F}, {1.20F, 1.30F, 0.70F},
    };

    public FormationDemonGeoRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new FormationDemonGeoModel());
        this.shadowRadius = 1.1F;
    }

    @Override
    public void render(FormationDemonEntity entity, float entityYaw, float partialTick,
                       PoseStack pose, MultiBufferSource buffers, int packedLight) {
        // ---- 阶段配色：二阶段转紫，三阶段转血红（顶点色乘算）
        int ph = entity.getPhase();
        if (ph >= 2) {
            RenderSystem.setShaderColor(
                    ph == 3 ? 1.00F : 0.72F,
                    ph == 3 ? 0.38F : 0.50F,
                    ph == 3 ? 0.34F : 1.00F,
                    1.0F);
        }
        super.render(entity, entityYaw, partialTick, pose, buffers, packedLight);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        // 死亡淡出
        float fade = 1.0F;
        if (entity.deathTime > 0) {
            fade = Math.max(0.0F, 1.0F - entity.deathTime / 40.0F);
        }
        if (fade <= 0.01F) {
            return;
        }

        pose.pushPose();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = tess.getBuilder();
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        float time = (entity.tickCount + partialTick) * 0.05F;

        // ---- 1. 破碎屏障：三圈暗紫圆环（下宽上窄），带缺口
        ring(buf, pose, 1.62F, 0.35F, 0.16F, 0.06F, 0.26F, 0.62F * fade, time, 0.0F);
        ring(buf, pose, 1.45F, 1.70F, 0.16F, 0.06F, 0.26F, 0.50F * fade, time, 1.3F);
        ring(buf, pose, 1.05F, 2.95F, 0.16F, 0.06F, 0.26F, 0.38F * fade, time, 2.6F);

        // ---- 2. 破碎星空：头顶一块暗盘 + 暗红星点
        float skyY = 4.05F;
        float skyR = 2.1F;
        // 底盘
        disc(buf, pose, skyR, skyY, 0.05F, 0.02F, 0.09F, 0.72F * fade);
        // 星点
        for (float[] s : STARS) {
            float ang = s[0] + time * 0.35F;
            float r = s[1] * skyR * 0.9F;
            float x = (float) Math.cos(ang) * r;
            float z = (float) Math.sin(ang) * r;
            float b = s[2];
            float size = 0.055F + 0.03F * b;
            // 暗红色：故意压低绿色和蓝色分量
            quadFlat(buf, pose, x - size, skyY + 0.03F, z - size,
                    x + size, skyY + 0.03F, z + size,
                    0.62F * b, 0.13F * b, 0.16F * b, 0.95F * fade);
        }

        // ---- 3. 偶尔闪过的黑色龙影（每 8 秒一次，持续 0.6 秒）
        int cycle = entity.tickCount % 120;   // 6 秒一次，比原来更频繁
        if (cycle < 12) {
            float a = (float) Math.sin(Math.PI * cycle / 12.0F) * 0.92F * fade;
            dragonShadow(buf, pose, skyY + 0.06F, skyR, time, a);
        }

        BufferUploader.drawWithShader(buf.end());
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        pose.popPose();
    }

    // ================================================================ 绘制工具
    /** 一圈由若干小四边形拼成的圆环（带旋转和缺口）。 */
    private void ring(BufferBuilder buf, PoseStack pose, float radius, float y,
                      float r, float g, float b, float a, float time, float phase) {
        int seg = 40;
        float th = 0.045F * radius;
        for (int i = 0; i < seg; i++) {
            // 每 10 段留一个缺口，做出"破碎"的感觉
            if ((i + (int) (phase * 5)) % 11 == 0) {
                continue;
            }
            double a0 = Math.PI * 2.0D * i / seg + time;
            double a1 = Math.PI * 2.0D * (i + 1) / seg + time;
            float x0 = (float) Math.cos(a0) * radius;
            float z0 = (float) Math.sin(a0) * radius;
            float x1 = (float) Math.cos(a1) * radius;
            float z1 = (float) Math.sin(a1) * radius;
            float x0i = (float) Math.cos(a0) * (radius - th);
            float z0i = (float) Math.sin(a0) * (radius - th);
            float x1i = (float) Math.cos(a1) * (radius - th);
            float z1i = (float) Math.sin(a1) * (radius - th);
            vertex(buf, pose, x0, y, z0, r, g, b, a);
            vertex(buf, pose, x1, y, z1, r, g, b, a);
            vertex(buf, pose, x1i, y, z1i, r, g, b, a);
            vertex(buf, pose, x0i, y, z0i, r, g, b, a);
        }
    }

    /** 一个水平圆盘。 */
    private void disc(BufferBuilder buf, PoseStack pose, float radius, float y,
                      float r, float g, float b, float a) {
        int seg = 32;
        for (int i = 0; i < seg; i++) {
            double a0 = Math.PI * 2.0D * i / seg;
            double a1 = Math.PI * 2.0D * (i + 1) / seg;
            vertex(buf, pose, 0.0F, y, 0.0F, r, g, b, a);
            vertex(buf, pose, (float) Math.cos(a0) * radius, y, (float) Math.sin(a0) * radius, r, g, b, a);
            vertex(buf, pose, (float) Math.cos(a1) * radius, y, (float) Math.sin(a1) * radius, r, g, b, a);
            vertex(buf, pose, 0.0F, y, 0.0F, r, g, b, a);
        }
    }

    private void quadFlat(BufferBuilder buf, PoseStack pose,
                          float x0, float y, float z0, float x1, float y1, float z1,
                          float r, float g, float b, float a) {
        vertex(buf, pose, x0, y, z0, r, g, b, a);
        vertex(buf, pose, x1, y, z0, r, g, b, a);
        vertex(buf, pose, x1, y1, z1, r, g, b, a);
        vertex(buf, pose, x0, y1, z1, r, g, b, a);
    }

    /** 一条弯曲的黑色龙影：用 8 段小方块拼出一条蛇形。 */
    private void dragonShadow(BufferBuilder buf, PoseStack pose, float y, float skyR, float time, float a) {
        int seg = 8;
        for (int i = 0; i < seg; i++) {
            float t = (float) i / (seg - 1) - 0.5F;
            float x = t * skyR * 1.5F;
            float z = (float) Math.sin(t * 5.0F + time * 3.0F) * skyR * 0.35F;
            float size = 0.10F + 0.06F * (1.0F - Math.abs(t) * 2.0F);
            quadFlat(buf, pose, x - size, y, z - size, x + size, y + 0.02F, z + size,
                    0.02F, 0.01F, 0.04F, a);
        }
    }

    private void vertex(BufferBuilder buf, PoseStack pose, float x, float y, float z,
                        float r, float g, float b, float a) {
        buf.vertex(pose.last().pose(), x, y, z).color(r, g, b, a).endVertex();
    }
}
