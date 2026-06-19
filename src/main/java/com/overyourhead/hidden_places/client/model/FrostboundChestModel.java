package com.overyourhead.hidden_places.client.model;

import com.overyourhead.hidden_places.HiddenPlacesMod;
import com.overyourhead.hidden_places.common.block.entity.FrostboundChestBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class FrostboundChestModel extends GeoModel<FrostboundChestBlockEntity> {
    @Override
    public ResourceLocation getModelResource(FrostboundChestBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(HiddenPlacesMod.MOD_ID, "geo/block/frostbound_chest.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(FrostboundChestBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(HiddenPlacesMod.MOD_ID, "textures/block/frostbound_chest.png");
    }

    @Override
    public ResourceLocation getAnimationResource(FrostboundChestBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(HiddenPlacesMod.MOD_ID, "animations/block/frostbound_chest.animation.json");
    }
}
