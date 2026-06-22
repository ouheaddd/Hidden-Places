package com.overyourhead.hidden_places.client.renderer;

import com.overyourhead.hidden_places.client.model.BloomletPetalProjectileModel;
import com.overyourhead.hidden_places.common.entity.projectile.BloomletPetalProjectileEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class BloomletPetalProjectileRenderer extends GeoEntityRenderer<BloomletPetalProjectileEntity> {
    public BloomletPetalProjectileRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new BloomletPetalProjectileModel());
        this.shadowRadius = 0.2F;
    }
}
