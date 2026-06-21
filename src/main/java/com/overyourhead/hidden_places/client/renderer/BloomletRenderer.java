package com.overyourhead.hidden_places.client.renderer;

import com.overyourhead.hidden_places.client.model.BloomletModel;
import com.overyourhead.hidden_places.common.entity.BloomletEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class BloomletRenderer extends GeoEntityRenderer<BloomletEntity> {
    public BloomletRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new BloomletModel());
        this.shadowRadius = 0.35F;
    }
}
