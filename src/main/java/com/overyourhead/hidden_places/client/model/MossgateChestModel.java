package com.overyourhead.hidden_places.client.model;

import com.overyourhead.hidden_places.HiddenPlacesMod;
import com.overyourhead.hidden_places.common.block.entity.MossgateChestBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class MossgateChestModel extends GeoModel<MossgateChestBlockEntity> {
    @Override
    public ResourceLocation getModelResource(MossgateChestBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(HiddenPlacesMod.MOD_ID, "geo/block/mossgate_chest.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(MossgateChestBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(HiddenPlacesMod.MOD_ID, "textures/block/mossgate_chest.png");
    }

    @Override
    public ResourceLocation getAnimationResource(MossgateChestBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(HiddenPlacesMod.MOD_ID, "animations/block/mossgate_chest.animation.json");
    }
}
