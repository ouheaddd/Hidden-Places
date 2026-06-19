package com.overyourhead.hidden_places.core.registry;

import com.overyourhead.hidden_places.HiddenPlacesMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.HashSet;
import java.util.Set;

public final class HPCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, HiddenPlacesMod.MOD_ID);

    private static final Set<String> HIDDEN_FROM_CREATIVE = new HashSet<>();

    static {
        HIDDEN_FROM_CREATIVE.add("ancient_key");
    }

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN =
            CREATIVE_MODE_TABS.register("main", () -> CreativeModeTab
                    .builder()
                    .icon(() -> new ItemStack(HPItems.ANCIENT_KEY.get()))
                    .title(Component.translatable("creative_tab.hidden_places.main"))
                    .displayItems((parameters, output) -> {
                        for (DeferredHolder<Item, ? extends Item> holder : HPItems.ITEMS.getEntries()) {
                            if (!HIDDEN_FROM_CREATIVE.contains(holder.getId().getPath())) {
                                output.accept(holder.get());
                            }
                        }
                    })
                    .build()
            );

    private HPCreativeTabs() {
    }

    public static void hideFromCreative(String itemId) {
        HIDDEN_FROM_CREATIVE.add(itemId);
    }

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
