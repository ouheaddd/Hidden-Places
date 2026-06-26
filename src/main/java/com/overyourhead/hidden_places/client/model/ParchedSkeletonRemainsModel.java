package com.overyourhead.hidden_places.client.model;

import com.overyourhead.hidden_places.HiddenPlacesMod;
import com.overyourhead.hidden_places.common.entity.ParchedSkeletonRemainsEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class ParchedSkeletonRemainsModel extends GeoModel<ParchedSkeletonRemainsEntity> {
    @Override
    public ResourceLocation getModelResource(ParchedSkeletonRemainsEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(
                HiddenPlacesMod.MOD_ID,
                "geo/entity/parched_skeleton_" + animatable.getVariant() + ".geo.json"
        );
    }

    @Override
    public ResourceLocation getTextureResource(ParchedSkeletonRemainsEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(HiddenPlacesMod.MOD_ID, "textures/entity/parched_skeleton.png");
    }

    @Override
    public ResourceLocation getAnimationResource(ParchedSkeletonRemainsEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(HiddenPlacesMod.MOD_ID, "animations/entity/parched_skeleton.animation.json");
    }
}
