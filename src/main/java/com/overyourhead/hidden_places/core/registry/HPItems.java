package com.overyourhead.hidden_places.core.registry;

import com.overyourhead.hidden_places.HiddenPlacesMod;
import com.overyourhead.hidden_places.common.item.SealedSanctumMapItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class HPItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(HiddenPlacesMod.MOD_ID);

    public static final DeferredItem<Item> ANCIENT_KEY =
            registerSimpleItem("ancient_key");

    public static final DeferredItem<Item> FROSTBOUND_KEY =
            registerSimpleItem("frostbound_key");

    public static final DeferredItem<Item> SUNVEIL_KEY =
            registerSimpleItem("sunveil_key");

    public static final DeferredItem<Item> WILDROOT_KEY =
            registerSimpleItem("wildroot_key");

    public static final DeferredItem<Item> MOSSGATE_KEY =
            registerSimpleItem("mossgate_key");

    public static final DeferredItem<Item> FROSTBOUND_MAP_FRAGMENT =
            registerSimpleItem("frostbound_map_fragment");

    public static final DeferredItem<Item> SUNVEIL_MAP_FRAGMENT =
            registerSimpleItem("sunveil_map_fragment");

    public static final DeferredItem<Item> WILDROOT_MAP_FRAGMENT =
            registerSimpleItem("wildroot_map_fragment");

    public static final DeferredItem<Item> MOSSGATE_MAP_FRAGMENT =
            registerSimpleItem("mossgate_map_fragment");

    public static final DeferredItem<Item> SEALED_SANCTUM_MAP =
            registerItem("sealed_sanctum_map", () -> new SealedSanctumMapItem(new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> TEST_WAYFINDER_SPAWN_EGG =
            ITEMS.register("test_wayfinder_spawn_egg", () -> new DeferredSpawnEggItem(
                    HPEntities.TEST_WAYFINDER,
                    0x7B6A55,
                    0x20BFEF,
                    new Item.Properties()
            ));

    public static final DeferredItem<DeferredSpawnEggItem> MOSSGATE_WAYFINDER_SPAWN_EGG =
            ITEMS.register("mossgate_wayfinder_spawn_egg", () -> new DeferredSpawnEggItem(
                    HPEntities.MOSSGATE_WAYFINDER,
                    0x2F4A2D,
                    0xD9C89A,
                    new Item.Properties()
            ));

    private HPItems() {
    }

    public static DeferredItem<Item> registerSimpleItem(String name) {
        return registerItem(name, () -> new Item(new Item.Properties()));
    }

    public static <T extends Item> DeferredItem<T> registerItem(String name, Supplier<T> itemSupplier) {
        return ITEMS.register(name, itemSupplier);
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
