package com.overyourhead.hidden_places.common.npc;

import com.overyourhead.hidden_places.core.registry.HPItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Supplier;

public enum WayfinderTradeOffer {
    MOSSGATE_KEY_FOR_EMERALD(
            0,
            () -> Items.EMERALD,
            1,
            () -> HPItems.MOSSGATE_KEY.get(),
            1,
            "mossgate_key"
    ),
    MOSSGATE_KEY_FOR_TORCHFLOWER(
            1,
            () -> Items.TORCHFLOWER,
            1,
            () -> HPItems.MOSSGATE_KEY.get(),
            1,
            "mossgate_key_for_torchflower"
    );

    private final int id;
    private final Supplier<? extends Item> costItem;
    private final int costCount;
    private final Supplier<? extends Item> resultItem;
    private final int resultCount;
    private final String translationKey;

    WayfinderTradeOffer(
            int id,
            Supplier<? extends Item> costItem,
            int costCount,
            Supplier<? extends Item> resultItem,
            int resultCount,
            String translationKey
    ) {
        this.id = id;
        this.costItem = costItem;
        this.costCount = costCount;
        this.resultItem = resultItem;
        this.resultCount = resultCount;
        this.translationKey = translationKey;
    }

    public int id() {
        return this.id;
    }

    public Item costItem() {
        return this.costItem.get();
    }

    public int costCount() {
        return this.costCount;
    }

    public Item resultItem() {
        return this.resultItem.get();
    }

    public int resultCount() {
        return this.resultCount;
    }

    public ItemStack createCostStack() {
        return new ItemStack(this.costItem(), this.costCount);
    }

    public ItemStack createResultStack() {
        return new ItemStack(this.resultItem(), this.resultCount);
    }

    public String buyButtonTranslationKey(WayfinderProfile profile) {
        return "screen.hidden_places." + profile.translationKey() + ".trade.buy_" + this.translationKey;
    }

    public static Optional<WayfinderTradeOffer> byId(int id) {
        return Arrays.stream(values())
                .filter(offer -> offer.id == id)
                .findFirst();
    }
}
