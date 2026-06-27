package com.overyourhead.hidden_places.client.model;

import com.overyourhead.hidden_places.HiddenPlacesMod;
import com.overyourhead.hidden_places.common.item.ParchedSkeletonRemainsItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class ParchedSkeletonRemainsItemModel extends GeoModel<ParchedSkeletonRemainsItem> {
    @Override
    public ResourceLocation getModelResource(ParchedSkeletonRemainsItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(
                HiddenPlacesMod.MOD_ID,
                "geo/item/parched_skeleton_remains_item.geo.json"
        );
    }

    @Override
    public ResourceLocation getTextureResource(ParchedSkeletonRemainsItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(
                HiddenPlacesMod.MOD_ID,
                "textures/item/parched_skeleton_remains.png"
        );
    }

    @Override
    public ResourceLocation getAnimationResource(ParchedSkeletonRemainsItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(
                HiddenPlacesMod.MOD_ID,
                "animations/item/parched_skeleton_remains_item.animation.json"
        );
    }
}
