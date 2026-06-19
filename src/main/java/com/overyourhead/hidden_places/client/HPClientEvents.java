package com.overyourhead.hidden_places.client;

import com.overyourhead.hidden_places.HiddenPlacesMod;
import com.overyourhead.hidden_places.client.renderer.MossgateChestRenderer;
import com.overyourhead.hidden_places.client.screen.MossgateChestScreen;
import com.overyourhead.hidden_places.core.registry.HPBlockEntities;
import com.overyourhead.hidden_places.core.registry.HPMenuTypes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = HiddenPlacesMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class HPClientEvents {
    private HPClientEvents() {
    }

    @SubscribeEvent
    public static void registerMenuScreens(RegisterMenuScreensEvent event) {
        event.register(HPMenuTypes.MOSSGATE_CHEST.get(), MossgateChestScreen::new);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(HPBlockEntities.MOSSGATE_CHEST.get(), MossgateChestRenderer::new);
    }
}
