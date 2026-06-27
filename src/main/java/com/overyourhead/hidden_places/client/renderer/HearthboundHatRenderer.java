package com.overyourhead.hidden_places.client.renderer;

import com.overyourhead.hidden_places.HiddenPlacesMod;
import com.overyourhead.hidden_places.common.item.HearthboundHatItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedItemGeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class HearthboundHatRenderer extends GeoArmorRenderer<HearthboundHatItem> {
    public HearthboundHatRenderer() {
        super(new DefaultedItemGeoModel<>(ResourceLocation.fromNamespaceAndPath(HiddenPlacesMod.MOD_ID, "armor/hearthbound_hat")));
    }
}
