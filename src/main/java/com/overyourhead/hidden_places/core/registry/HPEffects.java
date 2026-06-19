package com.overyourhead.hidden_places.core.registry;

import com.overyourhead.hidden_places.HiddenPlacesMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class HPEffects {
    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, HiddenPlacesMod.MOD_ID);

    private HPEffects() {
    }

    public static void register(IEventBus eventBus) {
        EFFECTS.register(eventBus);
    }
}
