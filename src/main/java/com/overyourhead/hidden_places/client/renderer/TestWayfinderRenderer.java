package com.overyourhead.hidden_places.client.renderer;

import com.overyourhead.hidden_places.client.model.TestWayfinderModel;
import com.overyourhead.hidden_places.common.entity.TestWayfinderEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class TestWayfinderRenderer extends GeoEntityRenderer<TestWayfinderEntity> {
    public TestWayfinderRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new TestWayfinderModel());
        this.shadowRadius = 0.45F;
    }
}
