package com.lengyang.fulu.client.render;

import com.lengyang.fulu.entity.WebPrisonEntity;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

/**
 * 盘丝天牢的渲染：**立体的白色丝线牢笼**（手绘线段，不用粒子）。
 *
 * <p>结构：
 * <ul>
 *   <li>12 根**纵向丝线**：从网底一直拉到 4 格高，向外微张（像笼子）；</li>
 *   <li>4 圈**水平丝环**：高度 0 / 1.3 / 2.6 / 3.9 格；</li>
 *   <li>底部 8 条**放射丝线**：从中心散到半径 5。</li>
 * </ul>
 *
 * <p>线段用极细的四边形画（`POSITION_COLOR`），白色半透明 + 轻微加色，
 * 所以是"会发光的蛛丝"而不是实心墙。
 */
public class WebPrisonRenderer extends EntityRenderer<WebPrisonEntity> {

    private static final ResourceLocation TEX =
            new ResourceLocation("fulu", "textures/entity/blood_wave.png");

    private static final int STRANDS = 12;
    private static final int RINGS = 4;

    public WebPrisonRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public ResourceLocation getTextureLocation(WebPrisonEntity entity) {
        return TEX;
    }

    @Override
    public void render(WebPrisonEntity entity, float yaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int packedLight) {
        int tick = entity.tickCount;
        float alpha = Math.min(1.0F, tick / 8.0F)
                * Math.min(1.0F, (WebPrisonEntity.LIFE - tick) / 20.0F);
        if (alpha <= 0.02F) {
            return;
        }
        float R = WebPrisonEntity.RADIUS;

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE);
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = tess.getBuilder();
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f m = pose.last().pose();

        // 纵向丝线（略向外张开）
        for (int i = 0; i < STRANDS; i++) {
            double a = Math.PI * 2.0D * i / STRANDS;
            double sway = Math.sin(tick * 0.06D + i) * 0.12D;
            float x0 = (float) (Math.cos(a) * (R + sway));
            float z0 = (float) (Math.sin(a) * (R + sway));
            float x1 = (float) (Math.cos(a) * (R * 0.82D));
            float z1 = (float) (Math.sin(a) * (R * 0.82D));
            strand(buf, m, x0, 0.0F, z0, x1, 4.0F, z1, 0.05F, 1.0F, 1.0F, 1.0F, alpha * 0.55F);
        }

        // 水平丝环
        int seg = 40;
        for (int ring = 0; ring < RINGS; ring++) {
            float y = 0.15F + ring * 1.3F;
            float rr = R * (1.0F - ring * 0.055F);
            for (int i = 0; i < seg; i++) {
                double a0 = Math.PI * 2.0D * i / seg;
                double a1 = Math.PI * 2.0D * (i + 1) / seg;
                strand(buf, m,
                        (float) (Math.cos(a0) * rr), y, (float) (Math.sin(a0) * rr),
                        (float) (Math.cos(a1) * rr), y, (float) (Math.sin(a1) * rr),
                        0.04F, 1.0F, 1.0F, 1.0F, alpha * 0.5F);
            }
        }

        // 底部放射丝线
        for (int i = 0; i < 8; i++) {
            double a = Math.PI * 2.0D * i / 8.0D;
            strand(buf, m, 0.0F, 0.05F, 0.0F,
                    (float) (Math.cos(a) * R), 0.05F, (float) (Math.sin(a) * R),
                    0.05F, 1.0F, 1.0F, 1.0F, alpha * 0.6F);
        }

        BufferUploader.drawWithShader(buf.end());

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();

        super.render(entity, yaw, partialTick, pose, buffers, packedLight);
    }

    /** 画一条极细的线段（用四边形）。 */
    private void strand(BufferBuilder buf, Matrix4f m,
                        float x0, float y0, float z0, float x1, float y1, float z1,
                        float w, float r, float g, float b, float a) {
        float dx = x1 - x0;
        float dz = z1 - z0;
        float len = (float) Math.max(0.001D, Math.sqrt(dx * dx + dz * dz));
        float nx = -dz / len * w;
        float nz = dx / len * w;

        buf.vertex(m, x0 - nx, y0, z0 - nz).color(r, g, b, a).endVertex();
        buf.vertex(m, x0 + nx, y0, z0 + nz).color(r, g, b, a).endVertex();
        buf.vertex(m, x1 + nx, y1, z1 + nz).color(r, g, b, a * 0.6F).endVertex();
        buf.vertex(m, x1 - nx, y1, z1 - nz).color(r, g, b, a * 0.6F).endVertex();
    }
}
