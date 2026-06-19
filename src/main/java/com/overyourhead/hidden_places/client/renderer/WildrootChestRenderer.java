package com.overyourhead.hidden_places.client.renderer;

import com.overyourhead.hidden_places.client.model.WildrootChestModel;
import com.overyourhead.hidden_places.common.block.entity.WildrootChestBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class WildrootChestRenderer extends GeoBlockRenderer<WildrootChestBlockEntity> {
    public WildrootChestRenderer(BlockEntityRendererProvider.Context context) {
        super(new WildrootChestModel());
    }
}
