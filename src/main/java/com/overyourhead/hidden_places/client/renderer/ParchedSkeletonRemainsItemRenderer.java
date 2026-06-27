package com.overyourhead.hidden_places.client.renderer;

import com.overyourhead.hidden_places.client.model.ParchedSkeletonRemainsItemModel;
import com.overyourhead.hidden_places.common.item.ParchedSkeletonRemainsItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class ParchedSkeletonRemainsItemRenderer extends GeoItemRenderer<ParchedSkeletonRemainsItem> {
    public ParchedSkeletonRemainsItemRenderer() {
        super(new ParchedSkeletonRemainsItemModel());
    }
}
