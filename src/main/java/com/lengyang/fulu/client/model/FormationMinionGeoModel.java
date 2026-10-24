package com.lengyang.fulu.client.model;

import com.lengyang.fulu.entity.FormationMinionEntity;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * 阵仆模型：直接复用阵魔那套骨骼与动画，靠渲染器缩到 0.42 倍。
 *
 * <p>这样阵仆和阵魔是同一套美术，不会出现"大的用新模型、小的还是方块人"的割裂感。
 */
public class FormationMinionGeoModel extends GeoModel<FormationMinionEntity> {

    @Override
    public ResourceLocation getModelResource(FormationMinionEntity entity) {
        return new ResourceLocation("fulu", "geo/formation_demon.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(FormationMinionEntity entity) {
        return new ResourceLocation("fulu", "textures/entity/formation_demon.png");
    }

    @Override
    public ResourceLocation getAnimationResource(FormationMinionEntity entity) {
        return new ResourceLocation("fulu", "animations/formation_demon.animation.json");
    }
}
