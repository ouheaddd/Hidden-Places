package com.overyourhead.hidden_places.client.model;

import com.overyourhead.hidden_places.HiddenPlacesMod;
import com.overyourhead.hidden_places.common.entity.TestWayfinderEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class TestWayfinderModel extends GeoModel<TestWayfinderEntity> {
    @Override
    public ResourceLocation getModelResource(TestWayfinderEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(HiddenPlacesMod.MOD_ID, "geo/entity/test_wayfinder.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(TestWayfinderEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(HiddenPlacesMod.MOD_ID, "textures/entity/test_wayfinder.png");
    }

    @Override
    public ResourceLocation getAnimationResource(TestWayfinderEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(HiddenPlacesMod.MOD_ID, "animations/entity/test_wayfinder.animation.json");
    }
}
