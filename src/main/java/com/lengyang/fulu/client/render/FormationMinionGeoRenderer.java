package com.lengyang.fulu.client.render;

import com.lengyang.fulu.client.model.FormationMinionGeoModel;
import com.lengyang.fulu.entity.FormationMinionEntity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * 阵仆渲染：复用阵魔模型，整体缩到 0.42 倍，并压暗一点当作"影子分身"。
 */
public class FormationMinionGeoRenderer extends GeoEntityRenderer<FormationMinionEntity> {

    public FormationMinionGeoRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new FormationMinionGeoModel());
        this.shadowRadius = 0.32F;
        this.withScale(0.42F);
    }

    @Override
    public ResourceLocation getTextureLocation(FormationMinionEntity entity) {
        return new ResourceLocation("fulu", "textures/entity/formation_demon.png");
    }
}
