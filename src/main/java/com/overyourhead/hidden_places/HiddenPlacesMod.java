package com.overyourhead.hidden_places;

import com.mojang.logging.LogUtils;
import com.overyourhead.hidden_places.core.registry.HPBlockEntities;
import com.overyourhead.hidden_places.core.registry.HPBlocks;
import com.overyourhead.hidden_places.core.registry.HPCreativeTabs;
import com.overyourhead.hidden_places.core.registry.HPEffects;
import com.overyourhead.hidden_places.core.registry.HPEntities;
import com.overyourhead.hidden_places.core.registry.HPItems;
import com.overyourhead.hidden_places.core.registry.HPParticleTypes;
import com.overyourhead.hidden_places.core.registry.HPSoundEvents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.slf4j.Logger;

@Mod(HiddenPlacesMod.MOD_ID)
public class HiddenPlacesMod {
    public static final String MOD_ID = "hidden_places";

    /**
     * Kept as a compatibility alias for old template code that used MODID.
     * Prefer MOD_ID in new code.
     */
    @Deprecated(forRemoval = false)
    public static final String MODID = MOD_ID;

    public static final Logger LOGGER = LogUtils.getLogger();

    public HiddenPlacesMod(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(HPEntities::registerAttributes);
        modEventBus.addListener(HPEntities::registerSpawnPlacements);

        HPBlocks.register(modEventBus);
        HPBlockEntities.register(modEventBus);
        HPEntities.register(modEventBus);
        HPItems.register(modEventBus);
        HPEffects.register(modEventBus);
        HPParticleTypes.register(modEventBus);
        HPSoundEvents.register(modEventBus);
        HPCreativeTabs.register(modEventBus);

        NeoForge.EVENT_BUS.register(this);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Hidden Places common setup complete.");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("Hidden Places server starting.");
    }
}
