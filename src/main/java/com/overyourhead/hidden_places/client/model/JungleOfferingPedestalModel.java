package com.overyourhead.hidden_places.client.model;

import com.overyourhead.hidden_places.HiddenPlacesMod;
import com.overyourhead.hidden_places.common.block.entity.JungleOfferingPedestalBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class JungleOfferingPedestalModel extends GeoModel<JungleOfferingPedestalBlockEntity> {
    @Override
    public ResourceLocation getModelResource(JungleOfferingPedestalBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(HiddenPlacesMod.MOD_ID, "geo/block/jungle_offering_pedestal.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(JungleOfferingPedestalBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(HiddenPlacesMod.MOD_ID, "textures/block/jungle_offering_pedestal.png");
    }

    @Override
    public ResourceLocation getAnimationResource(JungleOfferingPedestalBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(HiddenPlacesMod.MOD_ID, "animations/block/jungle_offering_pedestal.animation.json");
    }
}
