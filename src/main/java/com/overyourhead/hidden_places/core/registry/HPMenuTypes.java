package com.overyourhead.hidden_places.core.registry;

import com.overyourhead.hidden_places.HiddenPlacesMod;
import com.overyourhead.hidden_places.common.menu.FrostboundChestMenu;
import com.overyourhead.hidden_places.common.menu.MossgateChestMenu;
import com.overyourhead.hidden_places.common.menu.SunveilChestMenu;
import com.overyourhead.hidden_places.common.menu.WildrootChestMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class HPMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registries.MENU, HiddenPlacesMod.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<MossgateChestMenu>> MOSSGATE_CHEST =
            MENU_TYPES.register("mossgate_chest", () -> new MenuType<>(MossgateChestMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<MenuType<?>, MenuType<FrostboundChestMenu>> FROSTBOUND_CHEST =
            MENU_TYPES.register("frostbound_chest", () -> new MenuType<>(FrostboundChestMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<MenuType<?>, MenuType<SunveilChestMenu>> SUNVEIL_CHEST =
            MENU_TYPES.register("sunveil_chest", () -> new MenuType<>(SunveilChestMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<MenuType<?>, MenuType<WildrootChestMenu>> WILDROOT_CHEST =
            MENU_TYPES.register("wildroot_chest", () -> new MenuType<>(WildrootChestMenu::new, FeatureFlags.DEFAULT_FLAGS));

    private HPMenuTypes() {
    }

    public static void register(IEventBus eventBus) {
        MENU_TYPES.register(eventBus);
    }
}
