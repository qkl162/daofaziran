package com.lengyang.fulu.client.render;

import com.lengyang.fulu.entity.BloodWaveEntity;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

/**
 * 诛仙血阵 / 「光翼连斩」的视觉实现。
 *
 * <p>按参考图（李信·光信三技能）实现四个层次：
 * <ol>
 *   <li><b>青白色巨大穹顶</b>：半透明半球壳 + 地面圆环边界；</li>
 *   <li><b>地面扇形光域</b>：向前方张开的半透明青色扇面；</li>
 *   <li><b>青色晶体刃</b>：4 道细长的叶形光刃，扇状向前铺开（两头尖、中间宽）；</li>
 *   <li><b>金色光翼</b>：玩家两侧各一组金色羽刃，向后上方张开；</li>
 *   <li><b>金色漩涡环</b>：地面上一串旋转的金色圆环。</li>
 * </ol>
 *
 * <p>混合：穹顶/扇面/晶体刃用**普通透明混合**（看起来像玻璃），
 * 金色光翼与刃芯用**加色混合**（发光）。两者在同一帧里切换状态。
 */
public class BloodWaveRenderer extends EntityRenderer<BloodWaveEntity> {

    private static final ResourceLocation TEX =
            new ResourceLocation("fulu", "textures/entity/blood_wave.png");

    private static final float[] BLADE_ANGLES = {-30.0F, -15.0F, 0.0F, 15.0F, 30.0F};
    private static final float BLADE_LENGTH = 11.0F;
    private static final float BLADE_WIDTH = 1.35F;
    private static final int SEGMENTS = 16;

    /** 穹顶半径。 */
    private static final float DOME_RADIUS = 12.0F;
    /** 地面扇形光域的半角（度）。 */
    private static final float FAN_HALF_DEG = 62.0F;

    public BloodWaveRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public ResourceLocation getTextureLocation(BloodWaveEntity entity) {
        return TEX;
    }

    @Override
    public void render(BloodWaveEntity entity, float yaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int packedLight) {
        pose.pushPose();
        // 注意：实体的 yaw 字段这里存的是 atan2(dir.x, dir.z)，
        // 要让"本地 +Z"正好指向 dir，旋转角必须取**正**值。
        // 之前写成负值 -> 特效整体朝反方向（用户实测反馈）。
        pose.mulPose(Axis.YP.rotationDegrees(entity.getYRot()));

        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        if (entity.isStar()) {
            renderStar(entity, pose);
        } else {
            renderWave(entity, pose);
        }

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        pose.popPose();

        super.render(entity, yaw, partialTick, pose, buffers, packedLight);
    }

    // ================================================================ 星爆（光翼连斩）

    private void renderStar(BloodWaveEntity entity, PoseStack pose) {
        int tick = entity.tickCount;

        // ---- 生长进度：0 -> 1（用 17 刻从根部蔓延到最远处）
        float g = Math.min(1.0F, tick / (float) BloodWaveEntity.GROW_TICKS);
        // 缓出：一开始快、后面慢，像刃光"刷"地铺开
        float grow = (float) (1.0D - Math.pow(1.0D - g, 2.2D));

        // ---- 保持 + 淡出：grow 完成后先保持 3 刻，再用剩下的时间淡出
        int fadeStart = BloodWaveEntity.GROW_TICKS + 3;
        float fade = tick <= fadeStart
                ? 1.0F
                : Math.max(0.0F, 1.0F - (tick - fadeStart)
                        / (float) (BloodWaveEntity.STAR_LIFE - fadeStart));

        float alphaAll = fade;

        // ---- 玻璃感部分：普通透明混合
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);

        // 地面扇形光域：跟着生长一起铺开
        groundFan(pose, DOME_RADIUS * grow, FAN_HALF_DEG, 0.62F, 0.86F, 1.0F,
                0.20F * alphaAll);

        // ---- 晶体刃：**从根部向外生长**（长度随时间增加，根部固定不动）
        float[] shardAngles = {-40.0F, -24.0F, -8.0F, 8.0F, 24.0F, 40.0F};
        float fullLength = 26.0F;                       // 最终铺到 26 格
        float curLength = fullLength * grow;
        float shardA = 0.60F * alphaAll;
        for (float a : shardAngles) {
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(a));
            shard(pose, curLength, 2.6F, 0.66F, 0.88F, 1.0F, shardA);
            pose.popPose();
        }

        // ---- 发光部分：加色混合
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE);

        // 刃芯（白热棱线），同样从根部生长
        for (float a : shardAngles) {
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(a));
            shard(pose, curLength * 0.97F, 0.75F, 0.95F, 1.0F, 1.0F, 0.75F * alphaAll);
            pose.popPose();
        }

        // 金色光翼已按用户要求删除（头顶那团金色特效）

        // ---- 金色漩涡环：生长"扫过"它们时才出现（而不是一开始全在）
        for (int i = 0; i < 3; i++) {
            float ringPos = 5.0F + i * 5.5F;
            if (curLength < ringPos) {
                continue;                               // 光还没铺到，这一环不显示
            }
            pose.pushPose();
            pose.translate(0.0F, -1.1F, ringPos);
            pose.mulPose(Axis.YP.rotationDegrees(tick * 14.0F + i * 40.0F));
            swirlRing(pose, 2.2F + i * 0.5F, alphaAll * 0.85F);
            pose.popPose();
        }

        // 白色爆闪已按用户要求删除（头顶那团白色特效）
    }

    // ================================================================ 几何体

    /** 地面扇形（三角扇）。 */
    private void groundFan(PoseStack pose, float radius, float halfDeg,
                           float r, float g, float b, float a) {
        Matrix4f mat = pose.last().pose();
        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = tess.getBuilder();
        buf.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);

        buf.vertex(mat, 0.0F, -1.15F, 0.0F).color(r, g, b, a).endVertex();
        int seg = 40;
        for (int i = 0; i <= seg; i++) {
            double ang = Math.toRadians(-halfDeg + 2.0D * halfDeg * i / seg);
            float x = (float) Math.sin(ang) * radius;
            float z = (float) Math.cos(ang) * radius;
            buf.vertex(mat, x, -1.15F, z).color(r, g, b, a * 0.35F).endVertex();
        }
        BufferUploader.drawWithShader(buf.end());
    }

    /** 半球壳：用若干层水平圆环近似，越往上越细。 */
    private void dome(PoseStack pose, float radius, float alpha) {
        int layers = 7;
        for (int i = 0; i < layers; i++) {
            float t = i / (float) layers;
            float y = (float) Math.sin(t * Math.PI / 2.0D) * radius * 0.85F;
            float rr = (float) Math.cos(t * Math.PI / 2.0D) * radius;
            groundRing(pose, y, rr, 0.62F, 0.86F, 1.0F, alpha * 0.5F, 0.35F);
        }
    }

    /** 水平圆环。 */
    private void groundRing(PoseStack pose, float y, float radius,
                            float r, float g, float b, float a, float thick) {
        Matrix4f mat = pose.last().pose();
        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = tess.getBuilder();
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        int seg = 56;
        for (int i = 0; i < seg; i++) {
            double a0 = Math.PI * 2.0D * i / seg;
            double a1 = Math.PI * 2.0D * (i + 1) / seg;
            float x0 = (float) Math.cos(a0) * radius;
            float z0 = (float) Math.sin(a0) * radius;
            float x1 = (float) Math.cos(a1) * radius;
            float z1 = (float) Math.sin(a1) * radius;
            float x0i = (float) Math.cos(a0) * (radius - thick);
            float z0i = (float) Math.sin(a0) * (radius - thick);
            float x1i = (float) Math.cos(a1) * (radius - thick);
            float z1i = (float) Math.sin(a1) * (radius - thick);
            buf.vertex(mat, x0, y, z0).color(r, g, b, a).endVertex();
            buf.vertex(mat, x1, y, z1).color(r, g, b, a).endVertex();
            buf.vertex(mat, x1i, y, z1i).color(r, g, b, a).endVertex();
            buf.vertex(mat, x0i, y, z0i).color(r, g, b, a).endVertex();
        }
        BufferUploader.drawWithShader(buf.end());
    }

    /** 晶体刃：两头尖、中间宽的叶形（沿 +Z 伸出）。 */
    private void shard(PoseStack pose, float length, float width,
                       float r, float g, float b, float a) {
        if (a <= 0.01F) {
            return;
        }
        Matrix4f mat = pose.last().pose();
        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = tess.getBuilder();
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        int seg = 18;
        for (int i = 0; i < seg; i++) {
            float t0 = i / (float) seg;
            float t1 = (i + 1) / (float) seg;
            // sin 曲线：两端收尖
            float wA = width * (float) Math.sin(t0 * Math.PI);
            float wB = width * (float) Math.sin(t1 * Math.PI);
            float zA = t0 * length;
            float zB = t1 * length;
            float yA = (float) (Math.sin(t0 * Math.PI) * 0.15D);
            float yB = (float) (Math.sin(t1 * Math.PI) * 0.15D);
            buf.vertex(mat, -wA, yA, zA).color(r, g, b, a * 0.85F).endVertex();
            buf.vertex(mat, wA, yA, zA).color(r, g, b, a * 0.85F).endVertex();
            buf.vertex(mat, wB, yB, zB).color(r, g, b, a).endVertex();
            buf.vertex(mat, -wB, yB, zB).color(r, g, b, a).endVertex();
        }
        BufferUploader.drawWithShader(buf.end());
    }

    /**
     * 金色光翼：一侧 7 根羽刃，从玩家背后向斜上方张开（羽尖朝后上）。
     *
     * @param side +1 = 右翼，-1 = 左翼
     */
    private void wing(PoseStack pose, float side, float alpha) {
        if (alpha <= 0.01F) {
            return;
        }
        for (int i = 0; i < 7; i++) {
            pose.pushPose();
            // 羽刃从身体两侧向后上方散开
            pose.mulPose(Axis.YP.rotationDegrees(side * (14.0F + i * 11.0F)));
            pose.mulPose(Axis.ZP.rotationDegrees(-(28.0F + i * 9.0F)));
            pose.translate(0.0F, 0.4F + i * 0.18F, -0.2F);
            float len = 7.4F - i * 0.5F;
            float w = 0.85F + i * 0.10F;
            // 外焰
            feather(pose, len, w, 1.0F, 0.82F, 0.42F, alpha * 0.55F);
            // 内芯
            feather(pose, len * 0.9F, w * 0.45F, 1.0F, 0.98F, 0.85F, alpha * 0.85F);
            pose.popPose();
        }
    }

    /** 单根羽刃：向 -Z（身后）伸展、两头尖的细长片。 */
    private void feather(PoseStack pose, float length, float width,
                         float r, float g, float b, float a) {
        Matrix4f mat = pose.last().pose();
        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = tess.getBuilder();
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        int seg = 10;
        for (int i = 0; i < seg; i++) {
            float t0 = i / (float) seg;
            float t1 = (i + 1) / (float) seg;
            float wA = width * (float) Math.sin(t0 * Math.PI);
            float wB = width * (float) Math.sin(t1 * Math.PI);
            float zA = -t0 * length;
            float zB = -t1 * length;
            buf.vertex(mat, -wA, 0.0F, zA).color(r, g, b, a * 0.8F).endVertex();
            buf.vertex(mat, wA, 0.0F, zA).color(r, g, b, a * 0.8F).endVertex();
            buf.vertex(mat, wB, 0.0F, zB).color(r, g, b, a).endVertex();
            buf.vertex(mat, -wB, 0.0F, zB).color(r, g, b, a).endVertex();
        }
        BufferUploader.drawWithShader(buf.end());
    }

    /** 金色漩涡环：两层旋转圆环 + 中心亮环。 */
    private void swirlRing(PoseStack pose, float radius, float alpha) {
        if (alpha <= 0.01F) {
            return;
        }
        groundRing(pose, 0.0F, radius, 1.0F, 0.86F, 0.5F, alpha, 0.22F);
        groundRing(pose, 0.02F, radius * 0.72F, 1.0F, 0.95F, 0.75F, alpha * 0.8F, 0.16F);
        groundRing(pose, 0.04F, radius * 0.44F, 1.0F, 1.0F, 0.9F, alpha * 0.6F, 0.12F);
    }

    /** 中心爆闪。 */
    private void flash(PoseStack pose, float size, float alpha) {
        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = tess.getBuilder();
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (int plane = 0; plane < 2; plane++) {
            pose.pushPose();
            if (plane == 1) {
                pose.mulPose(Axis.ZP.rotationDegrees(90.0F));
            }
            Matrix4f m = pose.last().pose();
            float s = size;
            buf.vertex(m, -s, -s, 0.0F).color(1.0F, 1.0F, 0.95F, alpha).endVertex();
            buf.vertex(m, s, -s, 0.0F).color(1.0F, 1.0F, 0.95F, alpha).endVertex();
            buf.vertex(m, s, s, 0.0F).color(1.0F, 1.0F, 0.95F, alpha).endVertex();
            buf.vertex(m, -s, s, 0.0F).color(1.0F, 1.0F, 0.95F, alpha).endVertex();
            pose.popPose();
        }
        BufferUploader.drawWithShader(buf.end());
    }

    // ================================================================ 飞行刀光（沿用）

    // ================================================================ 半月剑气

    /**
     * 半月剑气：**竖直的半圆弧刀光**向前飞行，后面拖三段残影。
     *
     * <p>形状：外弧半径 R、内弧半径 R-w(t)，t 从 -75° 到 +75°，
     * w(t) 在中间最厚、两端收尖 —— 就是一轮弯月。
     * 颜色：青（0.55, 0.95, 1.0）为体，金（1.0, 0.85, 0.45）为刃缘。
     */
    private void renderWave(BloodWaveEntity entity, PoseStack pose) {
        int tick = entity.tickCount;
        float fade = tick > BloodWaveEntity.LIFE - 5
                ? (BloodWaveEntity.LIFE - tick) / 5.0F
                : 1.0F;

        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE);

        // 拖尾：向后偏移三段残影
        for (int i = 3; i >= 1; i--) {
            pose.pushPose();
            pose.translate(0.0F, 0.0F, -i * 1.8F);
            float ta = fade * (0.20F / i);
            crescentH(pose, 1.05F - i * 0.05F, 1.4F, 0.55F, 0.95F, 1.0F, ta);
            crescentV(pose, 1.05F - i * 0.05F, 1.4F, 0.55F, 0.95F, 1.0F, ta);
            pose.popPose();
        }

        // ---- 横月牙（原来的剑气）
        crescentH(pose, 1.35F, 2.20F, 0.55F, 0.95F, 1.0F, 0.58F * fade);   // 青体
        crescentH(pose, 1.45F, 0.60F, 1.00F, 0.85F, 0.45F, 0.85F * fade);  // 金缘
        crescentH(pose, 1.36F, 0.22F, 1.00F, 1.00F, 0.98F, 0.95F * fade);  // 白热芯

        // ---- 竖月牙（和横月牙交叉成星形）：按用户要求**加大**
        crescentV(pose, 1.85F, 2.90F, 0.55F, 0.95F, 1.0F, 0.58F * fade);   // 青体
        crescentV(pose, 1.98F, 0.75F, 1.00F, 0.85F, 0.45F, 0.85F * fade);  // 金缘
        crescentV(pose, 1.86F, 0.28F, 1.00F, 1.00F, 0.98F, 0.95F * fade);  // 白热芯
    }

    /** 横月牙：躺在水平面上的弧（像一道横扫的刀光）。 */
    private void crescentH(PoseStack pose, float scale, float thick,
                           float r, float g, float b, float a) {
        if (a <= 0.01F) {
            return;
        }
        Matrix4f mat = pose.last().pose();
        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = tess.getBuilder();
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        int seg = 32;
        double half = Math.toRadians(80.0D);
        float radius = 5.0F * scale;
        for (int i = 0; i < seg; i++) {
            double t0 = -half + 2.0D * half * i / seg;
            double t1 = -half + 2.0D * half * (i + 1) / seg;
            double w0 = thick * Math.cos(t0 / half * (Math.PI / 2.0D));
            double w1 = thick * Math.cos(t1 / half * (Math.PI / 2.0D));

            // 水平弧：x 方向摆动，z 方向前进
            float ox0 = (float) (Math.sin(t0) * radius);
            float oz0 = (float) (Math.cos(t0) * radius);
            float ox1 = (float) (Math.sin(t1) * radius);
            float oz1 = (float) (Math.cos(t1) * radius);
            float ix0 = (float) (Math.sin(t0) * (radius - w0));
            float iz0 = (float) (Math.cos(t0) * (radius - w0));
            float ix1 = (float) (Math.sin(t1) * (radius - w1));
            float iz1 = (float) (Math.cos(t1) * (radius - w1));

            buf.vertex(mat, ox0, 0.0F, oz0).color(r, g, b, a).endVertex();
            buf.vertex(mat, ox1, 0.0F, oz1).color(r, g, b, a).endVertex();
            buf.vertex(mat, ix1, 0.0F, iz1).color(r * 0.5F, g * 0.5F, b * 0.6F, a * 0.25F).endVertex();
            buf.vertex(mat, ix0, 0.0F, iz0).color(r * 0.5F, g * 0.5F, b * 0.6F, a * 0.25F).endVertex();
        }
        BufferUploader.drawWithShader(buf.end());
    }

    /** 竖月牙：立起来的弧（新增的那道）。 */
    private void crescentV(PoseStack pose, float scale, float thick,
                           float r, float g, float b, float a) {
        if (a <= 0.01F) {
            return;
        }
        Matrix4f mat = pose.last().pose();
        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = tess.getBuilder();
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        int seg = 32;
        double half = Math.toRadians(80.0D);
        float radius = 5.0F * scale;
        for (int i = 0; i < seg; i++) {
            double t0 = -half + 2.0D * half * i / seg;
            double t1 = -half + 2.0D * half * (i + 1) / seg;
            double w0 = thick * Math.cos(t0 / half * (Math.PI / 2.0D));
            double w1 = thick * Math.cos(t1 / half * (Math.PI / 2.0D));

            // 竖直弧：x 方向摆动，y 方向向上
            float ox0 = (float) (Math.sin(t0) * radius);
            float oy0 = (float) (Math.cos(t0) * radius);
            float ox1 = (float) (Math.sin(t1) * radius);
            float oy1 = (float) (Math.cos(t1) * radius);
            float ix0 = (float) (Math.sin(t0) * (radius - w0));
            float iy0 = (float) (Math.cos(t0) * (radius - w0));
            float ix1 = (float) (Math.sin(t1) * (radius - w1));
            float iy1 = (float) (Math.cos(t1) * (radius - w1));

            buf.vertex(mat, ox0, oy0, 0.0F).color(r, g, b, a).endVertex();
            buf.vertex(mat, ox1, oy1, 0.0F).color(r, g, b, a).endVertex();
            buf.vertex(mat, ix1, iy1, 0.0F).color(r * 0.5F, g * 0.5F, b * 0.6F, a * 0.25F).endVertex();
            buf.vertex(mat, ix0, iy0, 0.0F).color(r * 0.5F, g * 0.5F, b * 0.6F, a * 0.25F).endVertex();
        }
        BufferUploader.drawWithShader(buf.end());
    }

    private void blade(PoseStack pose, float scale, float widthK,
                       float r, float g, float b, float a) {
        if (a <= 0.01F) {
            return;
        }
        Matrix4f mat = pose.last().pose();
        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = tess.getBuilder();
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        float len = BLADE_LENGTH * scale;
        float w0 = BLADE_WIDTH * widthK * scale;
        for (int i = 0; i < SEGMENTS; i++) {
            float t0 = i / (float) SEGMENTS;
            float t1 = (i + 1) / (float) SEGMENTS;
            float wA = w0 * (float) Math.pow(1.0F - t0, 0.75D);
            float wB = w0 * (float) Math.pow(1.0F - t1, 0.75D);
            float yA = (float) (Math.sin(t0 * Math.PI) * 0.55D) * scale;
            float yB = (float) (Math.sin(t1 * Math.PI) * 0.55D) * scale;
            float zA = t0 * len;
            float zB = t1 * len;
            float aA = a * (float) Math.min(1.0D, (1.0D - t0) * 3.0D);
            float aB = a * (float) Math.min(1.0D, (1.0D - t1) * 3.0D);
            buf.vertex(mat, -wA, yA, zA).color(r, g, b, aA).endVertex();
            buf.vertex(mat, wA, yA, zA).color(r, g, b, aA).endVertex();
            buf.vertex(mat, wB, yB, zB).color(r, g, b, aB).endVertex();
            buf.vertex(mat, -wB, yB, zB).color(r, g, b, aB).endVertex();
        }
        BufferUploader.drawWithShader(buf.end());
    }
}
