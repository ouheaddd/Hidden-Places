package com.overyourhead.hidden_places.client.model;

import com.overyourhead.hidden_places.HiddenPlacesMod;
import com.overyourhead.hidden_places.common.entity.SunveilWayfinderEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class SunveilWayfinderModel extends GeoModel<SunveilWayfinderEntity> {
    @Override
    public ResourceLocation getModelResource(SunveilWayfinderEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(HiddenPlacesMod.MOD_ID, "geo/entity/sunveil_wayfinder.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(SunveilWayfinderEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(HiddenPlacesMod.MOD_ID, "textures/entity/sunveil_wayfinder.png");
    }

    @Override
    public ResourceLocation getAnimationResource(SunveilWayfinderEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(HiddenPlacesMod.MOD_ID, "animations/entity/sunveil_wayfinder.animation.json");
    }
}
