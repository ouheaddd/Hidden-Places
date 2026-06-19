package com.overyourhead.hidden_places.client.model;

import com.overyourhead.hidden_places.HiddenPlacesMod;
import com.overyourhead.hidden_places.common.block.entity.WildrootChestBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class WildrootChestModel extends GeoModel<WildrootChestBlockEntity> {
    @Override
    public ResourceLocation getModelResource(WildrootChestBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(HiddenPlacesMod.MOD_ID, "geo/block/wildroot_chest.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(WildrootChestBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(HiddenPlacesMod.MOD_ID, "textures/block/wildroot_chest.png");
    }

    @Override
    public ResourceLocation getAnimationResource(WildrootChestBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(HiddenPlacesMod.MOD_ID, "animations/block/wildroot_chest.animation.json");
    }
}
