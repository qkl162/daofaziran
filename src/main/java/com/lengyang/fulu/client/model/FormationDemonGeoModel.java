package com.lengyang.fulu.client.model;

import com.lengyang.fulu.entity.FormationDemonEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/** 阵魔 —— GeckoLib 骨骼模型（破阵灵）。 */
public class FormationDemonGeoModel extends GeoModel<FormationDemonEntity> {

    @Override
    public ResourceLocation getModelResource(FormationDemonEntity entity) {
        return new ResourceLocation("fulu", "geo/formation_demon.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(FormationDemonEntity entity) {
        return new ResourceLocation("fulu", "textures/entity/formation_demon.png");
    }

    @Override
    public ResourceLocation getAnimationResource(FormationDemonEntity entity) {
        return new ResourceLocation("fulu", "animations/formation_demon.animation.json");
    }
}
