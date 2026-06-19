package com.overyourhead.hidden_places.core.registry;

import com.overyourhead.hidden_places.HiddenPlacesMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class HPEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, HiddenPlacesMod.MOD_ID);

    private HPEntities() {
    }

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }

    public static void registerAttributes(EntityAttributeCreationEvent event) {
        // Add entity attributes here when Hidden Places gets mobs.
    }

    public static void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        // Add entity spawn rules here when needed.
    }
}
