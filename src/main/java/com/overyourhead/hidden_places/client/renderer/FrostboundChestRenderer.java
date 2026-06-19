package com.overyourhead.hidden_places.client.renderer;

import com.overyourhead.hidden_places.client.model.FrostboundChestModel;
import com.overyourhead.hidden_places.common.block.entity.FrostboundChestBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class FrostboundChestRenderer extends GeoBlockRenderer<FrostboundChestBlockEntity> {
    public FrostboundChestRenderer(BlockEntityRendererProvider.Context context) {
        super(new FrostboundChestModel());
    }
}
