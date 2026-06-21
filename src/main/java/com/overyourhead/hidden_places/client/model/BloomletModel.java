package com.overyourhead.hidden_places.client.model;

import com.overyourhead.hidden_places.HiddenPlacesMod;
import com.overyourhead.hidden_places.common.entity.BloomletEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class BloomletModel extends GeoModel<BloomletEntity> {
    @Override
    public ResourceLocation getModelResource(BloomletEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(HiddenPlacesMod.MOD_ID, "geo/entity/bloomlet.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(BloomletEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(HiddenPlacesMod.MOD_ID, "textures/entity/bloomlet.png");
    }

    @Override
    public ResourceLocation getAnimationResource(BloomletEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(HiddenPlacesMod.MOD_ID, "animations/entity/bloomlet.animation.json");
    }
}
