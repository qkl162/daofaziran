package com.lengyang.fulu.client.render;

import com.lengyang.fulu.entity.FireFieldEntity;
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
 * 火海渲染：**用火球贴图画一圈跳动的火焰** + 地面橙色辉光。
 *
 * <p>结构：
 * <ol>
 *   <li>地面辉光：一个 6 格半径的加色圆盘（黄→橙渐变），让整片地看起来在烧；</li>
 *   <li>26 束火焰：每束两片十字交叉的贴图四边形（任何角度都看得到），
 *       位置/大小/相位由实体 id 决定，看起来是随机分布的；</li>
 *   <li>贴图取 {@code fire_0..7.png}，每束按自己的相位循环换帧 → 跳动的火苗。</li>
 * </ol>
 *
 * <p>完全不依赖粒子系统，所以粒子设置再低也能看见。
 */
public class FireFieldRenderer extends EntityRenderer<FireFieldEntity> {

    /** 原版火焰是 16×512 的序列帧条：32 帧，每帧 16 像素高。 */
    private static final ResourceLocation FIRE_TEX =
            new ResourceLocation("fulu", "textures/entity/flame/fire_0.png");
    /** 序列帧数。 */
    private static final int FRAMES = 32;
    /** 染成绿色（原版火焰改色）。 */
    private static final float TINT_R = 0.35F;
    private static final float TINT_G = 1.00F;
    private static final float TINT_B = 0.50F;

    /** 火焰束数量。 */
    private static final int COUNT = 26;

    public FireFieldRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public ResourceLocation getTextureLocation(FireFieldEntity entity) {
        return FIRE_TEX;
    }

    @Override
    public void render(FireFieldEntity entity, float yaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int packedLight) {
        int tick = entity.tickCount;
        float life = FireFieldEntity.LIFE;
        // 前 10 刻淡入，最后 20 刻淡出
        float alpha = Math.min(1.0F, tick / 10.0F)
                * Math.min(1.0F, (life - tick) / 20.0F);
        if (alpha <= 0.02F) {
            return;
        }

        // ---- 瘴气模式（2）：绿色雾柱 + 地面绿雾盘
        if (entity.getMode() == 2) {
            float total = Math.max(1, entity.getLife());
            float a2 = Math.min(1.0F, tick / 10.0F)
                    * Math.min(1.0F, (total - tick) / 30.0F);
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                    GlStateManager.DestFactor.ONE);
            RenderSystem.disableCull();
            RenderSystem.depthMask(false);
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            miasmaFog(pose, tick, a2);
            RenderSystem.depthMask(true);
            RenderSystem.enableCull();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
            super.render(entity, yaw, partialTick, pose, buffers, packedLight);
            return;
        }

        // ---- 泥沼模式：画绿色地面盘 + 明亮边界环（让玩家一眼看清范围）
        if (entity.getMode() == 1) {
            float total = Math.max(1, entity.getLife());
            float a2 = Math.min(1.0F, tick / 10.0F)
                    * Math.min(1.0F, (total - tick) / 30.0F);
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                    GlStateManager.DestFactor.ONE);
            RenderSystem.disableCull();
            RenderSystem.depthMask(false);
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            swampDisc(pose, FireFieldEntity.RADIUS, a2);
            swampRing(pose, FireFieldEntity.RADIUS, a2 * 1.4F);
            swampRing(pose, FireFieldEntity.RADIUS * 0.66F, a2 * 0.8F);
            swampRing(pose, FireFieldEntity.RADIUS * 0.33F, a2 * 0.6F);
            swampDashes(pose, FireFieldEntity.RADIUS, tick, a2);
            RenderSystem.depthMask(true);
            RenderSystem.enableCull();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
            super.render(entity, yaw, partialTick, pose, buffers, packedLight);
            return;
        }

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE);
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);

        // ---- 地面辉光（纯色）
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        floorGlow(pose, FireFieldEntity.RADIUS, alpha * 0.55F);

        // ---- 火焰束（贴图）
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        for (int i = 0; i < COUNT; i++) {
            int seed = entity.getId() * 31 + i * 17;
            // 半径内均匀随机分布（用确定性伪随机，保证每帧位置不跳）
            double a = ((seed * 1103515245L) % 6283L) / 1000.0D;
            double rr = Math.sqrt(((seed * 22695477L) % 1000L) / 1000.0D)
                    * FireFieldEntity.RADIUS;
            double x = Math.cos(a) * rr;
            double z = Math.sin(a) * rr;

            int frame = ((tick / 2) + i) % FRAMES;
            RenderSystem.setShaderTexture(0, FIRE_TEX);

            float size = 1.1F + ((seed >> 3 & 7) / 7.0F) * 0.9F;
            float bob = (float) Math.sin((tick + i * 7) * 0.25D) * 0.12F;

            pose.pushPose();
            pose.translate(x, 0.15D + bob, z);
            pose.mulPose(Axis.YP.rotationDegrees((seed % 360)));
            flameCard(pose, size, alpha, frame);
            pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            flameCard(pose, size, alpha, frame);
            pose.popPose();
        }

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();

        super.render(entity, yaw, partialTick, pose, buffers, packedLight);
    }

    /** 泥沼边界上的旋转短划（让范围边界动起来，更醒目）。 */
    private void swampDashes(PoseStack pose, float radius, int tick, float alpha) {
        Matrix4f m = pose.last().pose();
        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = tess.getBuilder();
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        int count = 24;
        for (int i = 0; i < count; i++) {
            double a = Math.PI * 2.0D * i / count + tick * 0.02D;
            float x = (float) (Math.cos(a) * radius);
            float z = (float) (Math.sin(a) * radius);
            float nx = (float) Math.cos(a) * 0.55F;
            float nz = (float) Math.sin(a) * 0.55F;
            buf.vertex(m, x - nx, 0.12F, z - nz).color(0.75F, 1.0F, 0.6F, alpha).endVertex();
            buf.vertex(m, x + nx, 0.12F, z + nz).color(0.75F, 1.0F, 0.6F, alpha).endVertex();
            buf.vertex(m, x + nx * 0.4F, 0.12F, z + nz * 0.4F)
                    .color(0.3F, 0.7F, 0.25F, 0.0F).endVertex();
            buf.vertex(m, x - nx * 0.4F, 0.12F, z - nz * 0.4F)
                    .color(0.3F, 0.7F, 0.25F, 0.0F).endVertex();
        }
        BufferUploader.drawWithShader(buf.end());
    }

    /** 瘴气雾柱：多层绿色半透明圆盘叠成雾墙。 */
    private void miasmaFog(PoseStack pose, int tick, float alpha) {
        Matrix4f m = pose.last().pose();
        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = tess.getBuilder();
        buf.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);

        // 5 层由低到高的雾盘，半径逐渐收缩（形成雾墙）
        for (int layer = 0; layer < 5; layer++) {
            float y = 0.15F + layer * 0.55F;
            float r = FireFieldEntity.RADIUS * (1.0F - layer * 0.09F);
            float a = alpha * (0.22F - layer * 0.03F);
            float wob = (float) Math.sin(tick * 0.05D + layer) * 0.15F;

            buf.vertex(m, 0.0F, y, 0.0F).color(0.45F, 0.85F, 0.40F, a * 1.6F).endVertex();
            int seg = 40;
            for (int i = 0; i <= seg; i++) {
                double ang = Math.PI * 2.0D * i / seg;
                buf.vertex(m,
                        (float) (Math.cos(ang) * (r + wob)), y,
                        (float) (Math.sin(ang) * (r + wob)))
                        .color(0.12F, 0.35F, 0.10F, 0.0F).endVertex();
            }
        }
        BufferUploader.drawWithShader(buf.end());
    }

    /** 泥沼地面盘：墨绿→透明。 */
    private void swampDisc(PoseStack pose, float radius, float alpha) {
        Matrix4f m = pose.last().pose();
        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = tess.getBuilder();
        buf.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        buf.vertex(m, 0.0F, 0.05F, 0.0F).color(0.35F, 0.65F, 0.30F, alpha * 0.55F).endVertex();
        int seg = 56;
        for (int i = 0; i <= seg; i++) {
            double a = Math.PI * 2.0D * i / seg;
            buf.vertex(m, (float) (Math.cos(a) * radius), 0.05F,
                    (float) (Math.sin(a) * radius)).color(0.15F, 0.35F, 0.12F, 0.0F).endVertex();
        }
        BufferUploader.drawWithShader(buf.end());
    }

    /** 泥沼边界亮环：让半径一目了然。 */
    private void swampRing(PoseStack pose, float radius, float alpha) {
        Matrix4f m = pose.last().pose();
        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = tess.getBuilder();
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        int seg = 72;
        float thick = 0.28F;
        for (int i = 0; i < seg; i++) {
            double a0 = Math.PI * 2.0D * i / seg;
            double a1 = Math.PI * 2.0D * (i + 1) / seg;
            float x0 = (float) (Math.cos(a0) * radius);
            float z0 = (float) (Math.sin(a0) * radius);
            float x1 = (float) (Math.cos(a1) * radius);
            float z1 = (float) (Math.sin(a1) * radius);
            float x0i = (float) (Math.cos(a0) * (radius - thick));
            float z0i = (float) (Math.sin(a0) * (radius - thick));
            float x1i = (float) (Math.cos(a1) * (radius - thick));
            float z1i = (float) (Math.sin(a1) * (radius - thick));
            buf.vertex(m, x0, 0.08F, z0).color(0.55F, 1.0F, 0.45F, alpha).endVertex();
            buf.vertex(m, x1, 0.08F, z1).color(0.55F, 1.0F, 0.45F, alpha).endVertex();
            buf.vertex(m, x1i, 0.08F, z1i).color(0.25F, 0.6F, 0.2F, alpha * 0.3F).endVertex();
            buf.vertex(m, x0i, 0.08F, z0i).color(0.25F, 0.6F, 0.2F, alpha * 0.3F).endVertex();
        }
        BufferUploader.drawWithShader(buf.end());
    }

    /** 地面圆盘辉光：一圈由内到外的橙黄。 */
    private void floorGlow(PoseStack pose, float radius, float alpha) {
        Matrix4f m = pose.last().pose();
        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = tess.getBuilder();
        buf.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);

        buf.vertex(m, 0.0F, 0.03F, 0.0F).color(1.0F, 0.75F, 0.25F, alpha).endVertex();
        int seg = 48;
        for (int i = 0; i <= seg; i++) {
            double a = Math.PI * 2.0D * i / seg;
            float x = (float) Math.cos(a) * radius;
            float z = (float) Math.sin(a) * radius;
            buf.vertex(m, x, 0.03F, z).color(1.0F, 0.28F, 0.05F, 0.0F).endVertex();
        }
        BufferUploader.drawWithShader(buf.end());
    }

    /** 一片火焰四边形：取序列帧条里的一帧，并染成绿色。 */
    private void flameCard(PoseStack pose, float size, float alpha, int frame) {
        Matrix4f m = pose.last().pose();
        float h = size * 0.5F;
        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = tess.getBuilder();
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

        float v0 = frame / (float) FRAMES;
        float v1 = (frame + 1) / (float) FRAMES;
        int cr = (int) (TINT_R * 255);
        int cg = (int) (TINT_G * 255);
        int cb = (int) (TINT_B * 255);
        int ca = (int) (Math.min(1.0F, alpha) * 255);

        buf.vertex(m, -h, 0.0F, 0.0F).uv(0.0F, v1).color(cr, cg, cb, ca).endVertex();
        buf.vertex(m, h, 0.0F, 0.0F).uv(1.0F, v1).color(cr, cg, cb, ca).endVertex();
        buf.vertex(m, h, size, 0.0F).uv(1.0F, v0).color(cr, cg, cb, ca).endVertex();
        buf.vertex(m, -h, size, 0.0F).uv(0.0F, v0).color(cr, cg, cb, ca).endVertex();

        BufferUploader.drawWithShader(buf.end());
    }
}
