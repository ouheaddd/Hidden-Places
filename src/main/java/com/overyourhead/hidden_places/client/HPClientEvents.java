package com.overyourhead.hidden_places.client;

import com.overyourhead.hidden_places.HiddenPlacesMod;
import com.overyourhead.hidden_places.client.particle.BloomletSporeParticle;
import com.overyourhead.hidden_places.client.renderer.BloomletPetalProjectileRenderer;
import com.overyourhead.hidden_places.client.renderer.BloomletRenderer;
import com.overyourhead.hidden_places.client.renderer.FrostboundChestRenderer;
import com.overyourhead.hidden_places.client.renderer.JungleOfferingPedestalRenderer;
import com.overyourhead.hidden_places.client.renderer.MirageCreeperRenderer;
import com.overyourhead.hidden_places.client.renderer.MossgateChestRenderer;
import com.overyourhead.hidden_places.client.renderer.MossgateWayfinderRenderer;
import com.overyourhead.hidden_places.client.renderer.SunveilChestRenderer;
import com.overyourhead.hidden_places.client.renderer.SunveilWayfinderRenderer;
import com.overyourhead.hidden_places.client.renderer.TestWayfinderRenderer;
import com.overyourhead.hidden_places.client.renderer.WildrootChestRenderer;
import com.overyourhead.hidden_places.client.screen.FrostboundChestScreen;
import com.overyourhead.hidden_places.client.screen.MossgateChestScreen;
import com.overyourhead.hidden_places.client.screen.SunveilChestScreen;
import com.overyourhead.hidden_places.client.screen.WildrootChestScreen;
import com.overyourhead.hidden_places.core.registry.HPBlockEntities;
import com.overyourhead.hidden_places.core.registry.HPEntities;
import com.overyourhead.hidden_places.core.registry.HPMenuTypes;
import com.overyourhead.hidden_places.core.registry.HPParticleTypes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = HiddenPlacesMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class HPClientEvents {
    private HPClientEvents() {
    }

    @SubscribeEvent
    public static void registerMenuScreens(RegisterMenuScreensEvent event) {
        event.register(HPMenuTypes.MOSSGATE_CHEST.get(), MossgateChestScreen::new);
        event.register(HPMenuTypes.FROSTBOUND_CHEST.get(), FrostboundChestScreen::new);
        event.register(HPMenuTypes.SUNVEIL_CHEST.get(), SunveilChestScreen::new);
        event.register(HPMenuTypes.WILDROOT_CHEST.get(), WildrootChestScreen::new);
    }


    @SubscribeEvent
    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(HPParticleTypes.BLOOMLET_SPORE.get(), BloomletSporeParticle.Provider::new);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(HPBlockEntities.MOSSGATE_CHEST.get(), MossgateChestRenderer::new);
        event.registerBlockEntityRenderer(HPBlockEntities.FROSTBOUND_CHEST.get(), FrostboundChestRenderer::new);
        event.registerBlockEntityRenderer(HPBlockEntities.SUNVEIL_CHEST.get(), SunveilChestRenderer::new);
        event.registerBlockEntityRenderer(HPBlockEntities.WILDROOT_CHEST.get(), WildrootChestRenderer::new);
        event.registerBlockEntityRenderer(HPBlockEntities.JUNGLE_OFFERING_PEDESTAL.get(), JungleOfferingPedestalRenderer::new);

        event.registerEntityRenderer(HPEntities.TEST_WAYFINDER.get(), TestWayfinderRenderer::new);
        event.registerEntityRenderer(HPEntities.MOSSGATE_WAYFINDER.get(), MossgateWayfinderRenderer::new);
        event.registerEntityRenderer(HPEntities.SUNVEIL_WAYFINDER.get(), SunveilWayfinderRenderer::new);
        event.registerEntityRenderer(HPEntities.BLOOMLET.get(), BloomletRenderer::new);
        event.registerEntityRenderer(HPEntities.MIRAGE_CREEPER.get(), MirageCreeperRenderer::new);
        event.registerEntityRenderer(HPEntities.BLOOMLET_PETAL_PROJECTILE.get(), BloomletPetalProjectileRenderer::new);
    }
}
