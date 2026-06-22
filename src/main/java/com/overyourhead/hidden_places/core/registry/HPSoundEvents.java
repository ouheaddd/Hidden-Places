package com.overyourhead.hidden_places.core.registry;

import com.overyourhead.hidden_places.HiddenPlacesMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class HPSoundEvents {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, HiddenPlacesMod.MOD_ID);


    public static final DeferredHolder<SoundEvent, SoundEvent> BLOOMLET_TAME_SUCCESS =
            registerSound("entity.bloomlet.tame_success");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOOMLET_ATTACK_CHARGE =
            registerSound("entity.bloomlet.attack_charge");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOOMLET_PROJECTILE_LAUNCH =
            registerSound("entity.bloomlet.projectile_launch");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOOMLET_PROJECTILE_POP =
            registerSound("entity.bloomlet.projectile_pop");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOOMLET_HURT =
            registerSound("entity.bloomlet.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOOMLET_DEATH =
            registerSound("entity.bloomlet.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOOMLET_SIT =
            registerSound("entity.bloomlet.sit");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOOMLET_STAND_UP =
            registerSound("entity.bloomlet.stand_up");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOOMLET_IDLE =
            registerSound("entity.bloomlet.idle");

    private HPSoundEvents() {
    }

    public static DeferredHolder<SoundEvent, SoundEvent> registerSound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(
                ResourceLocation.fromNamespaceAndPath(HiddenPlacesMod.MOD_ID, name)
        ));
    }

    public static void register(IEventBus eventBus) {
        SOUNDS.register(eventBus);
    }
}
