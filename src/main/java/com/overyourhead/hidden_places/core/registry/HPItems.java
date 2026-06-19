package com.overyourhead.hidden_places.core.registry;

import com.overyourhead.hidden_places.HiddenPlacesMod;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
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
