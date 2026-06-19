package com.overyourhead.hidden_places.core.registry;

import com.overyourhead.hidden_places.HiddenPlacesMod;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class HPItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(HiddenPlacesMod.MOD_ID);

    /*
     * Add normal items here. Everything registered in ITEMS is added to the
     * Hidden Places creative tab automatically by HPCreativeTabs.
     *
     * Example:
     * public static final DeferredItem<Item> ANCIENT_KEY =
     *         registerSimpleItem("ancient_key");
     */

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
