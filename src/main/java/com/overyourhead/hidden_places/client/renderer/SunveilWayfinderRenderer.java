package com.overyourhead.hidden_places.client.renderer;

import com.overyourhead.hidden_places.client.model.SunveilWayfinderModel;
import com.overyourhead.hidden_places.common.entity.SunveilWayfinderEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class SunveilWayfinderRenderer extends GeoEntityRenderer<SunveilWayfinderEntity> {
    public SunveilWayfinderRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new SunveilWayfinderModel());
        this.shadowRadius = 0.45F;
    }
}
