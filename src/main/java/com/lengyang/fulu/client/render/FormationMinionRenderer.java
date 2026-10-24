package com.lengyang.fulu.client.render;

import com.lengyang.fulu.client.model.FormationDemonModel;
import com.lengyang.fulu.entity.FormationMinionEntity;
import com.lengyang.fulu.registry.ModModelLayers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * 阵仆的渲染：复用阵魔的模型，整体缩小到 0.42 倍。
 *
 * <p>之前这里用的是 {@code EntityRenderer}（没实现 render）—— 等于**什么都没画**，
 * 所以阵仆一直是隐形的。现在换成 {@code MobRenderer} 才会真的画模型。
 */
public class FormationMinionRenderer
        extends MobRenderer<FormationMinionEntity, FormationDemonModel<FormationMinionEntity>> {

    private static final ResourceLocation TEX =
            new ResourceLocation("fulu", "textures/entity/formation_demon.png");

    public FormationMinionRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new FormationDemonModel<>(ctx.bakeLayer(ModModelLayers.FORMATION_MINION)), 0.4F);
    }

    @Override
    protected void scale(FormationMinionEntity entity, PoseStack pose, float partialTick) {
        pose.scale(0.42F, 0.42F, 0.42F);
    }

    @Override
    public ResourceLocation getTextureLocation(FormationMinionEntity entity) {
        return TEX;
    }
}
