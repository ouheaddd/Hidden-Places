package com.overyourhead.hidden_places.client.renderer;

import com.overyourhead.hidden_places.client.model.MossgateWayfinderModel;
import com.overyourhead.hidden_places.common.entity.MossgateWayfinderEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class MossgateWayfinderRenderer extends GeoEntityRenderer<MossgateWayfinderEntity> {
    public MossgateWayfinderRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new MossgateWayfinderModel());
        this.shadowRadius = 0.45F;
    }
}
