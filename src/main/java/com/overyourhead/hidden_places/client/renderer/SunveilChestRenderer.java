package com.overyourhead.hidden_places.client.renderer;

import com.overyourhead.hidden_places.client.model.SunveilChestModel;
import com.overyourhead.hidden_places.common.block.entity.SunveilChestBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class SunveilChestRenderer extends GeoBlockRenderer<SunveilChestBlockEntity> {
    public SunveilChestRenderer(BlockEntityRendererProvider.Context context) {
        super(new SunveilChestModel());
    }
}
