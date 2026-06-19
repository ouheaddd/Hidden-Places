package com.overyourhead.hidden_places.client.model;

import com.overyourhead.hidden_places.HiddenPlacesMod;
import com.overyourhead.hidden_places.common.block.entity.SunveilChestBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class SunveilChestModel extends GeoModel<SunveilChestBlockEntity> {
    @Override
    public ResourceLocation getModelResource(SunveilChestBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(HiddenPlacesMod.MOD_ID, "geo/block/sunveil_chest.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(SunveilChestBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(HiddenPlacesMod.MOD_ID, "textures/block/sunveil_chest.png");
    }

    @Override
    public ResourceLocation getAnimationResource(SunveilChestBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(HiddenPlacesMod.MOD_ID, "animations/block/sunveil_chest.animation.json");
    }
}
