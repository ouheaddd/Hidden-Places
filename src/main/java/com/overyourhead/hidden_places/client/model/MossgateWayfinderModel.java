package com.overyourhead.hidden_places.client.model;

import com.overyourhead.hidden_places.HiddenPlacesMod;
import com.overyourhead.hidden_places.common.entity.MossgateWayfinderEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class MossgateWayfinderModel extends GeoModel<MossgateWayfinderEntity> {
    @Override
    public ResourceLocation getModelResource(MossgateWayfinderEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(HiddenPlacesMod.MOD_ID, "geo/entity/mossgate_wayfinder.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(MossgateWayfinderEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(HiddenPlacesMod.MOD_ID, "textures/entity/mossgate_wayfinder.png");
    }

    @Override
    public ResourceLocation getAnimationResource(MossgateWayfinderEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(HiddenPlacesMod.MOD_ID, "animations/entity/mossgate_wayfinder.animation.json");
    }
}
