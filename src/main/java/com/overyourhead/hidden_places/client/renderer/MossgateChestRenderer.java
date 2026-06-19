package com.overyourhead.hidden_places.client.renderer;

import com.overyourhead.hidden_places.client.model.MossgateChestModel;
import com.overyourhead.hidden_places.common.block.entity.MossgateChestBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class MossgateChestRenderer extends GeoBlockRenderer<MossgateChestBlockEntity> {
    public MossgateChestRenderer(BlockEntityRendererProvider.Context context) {
        super(new MossgateChestModel());
    }
}
