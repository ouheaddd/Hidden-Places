package com.overyourhead.hidden_places.client.model;

import com.overyourhead.hidden_places.HiddenPlacesMod;
import com.overyourhead.hidden_places.common.entity.MirageCreeperEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class MirageCreeperModel extends GeoModel<MirageCreeperEntity> {
    @Override
    public ResourceLocation getModelResource(MirageCreeperEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(HiddenPlacesMod.MOD_ID, "geo/entity/mirage_creeper.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(MirageCreeperEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(HiddenPlacesMod.MOD_ID, "textures/entity/mirage_creeper.png");
    }

    @Override
    public ResourceLocation getAnimationResource(MirageCreeperEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(HiddenPlacesMod.MOD_ID, "animations/entity/mirage_creeper.animation.json");
    }
}
