package com.overyourhead.hidden_places.client.model;

import com.overyourhead.hidden_places.HiddenPlacesMod;
import com.overyourhead.hidden_places.common.entity.projectile.BloomletPetalProjectileEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class BloomletPetalProjectileModel extends GeoModel<BloomletPetalProjectileEntity> {
    @Override
    public ResourceLocation getModelResource(BloomletPetalProjectileEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(HiddenPlacesMod.MOD_ID, "geo/entity/bloomlet_petal_projectile.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(BloomletPetalProjectileEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(HiddenPlacesMod.MOD_ID, "textures/entity/bloomlet_petal_projectile.png");
    }

    @Override
    public ResourceLocation getAnimationResource(BloomletPetalProjectileEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(HiddenPlacesMod.MOD_ID, "animations/entity/bloomlet_petal_projectile.animation.json");
    }
}
