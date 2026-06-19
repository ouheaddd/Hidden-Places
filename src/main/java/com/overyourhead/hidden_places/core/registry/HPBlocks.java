package com.overyourhead.hidden_places.core.registry;

import com.overyourhead.hidden_places.HiddenPlacesMod;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.BiFunction;
import java.util.function.Supplier;

public final class HPBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(HiddenPlacesMod.MOD_ID);

    /*
     * Add blocks here. registerBlock(...) automatically creates a matching
     * BlockItem in HPItems, and HPCreativeTabs adds it to the mod tab.
     *
     * Example:
     * public static final DeferredBlock<Block> OLD_STONE = registerBlock(
     *         "old_stone",
     *         () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE).mapColor(MapColor.STONE))
     * );
     */

    private HPBlocks() {
    }

    public static <T extends Block> DeferredBlock<T> registerBlock(String name, Supplier<T> blockSupplier) {
        return registerBlock(name, blockSupplier, (block, properties) -> new BlockItem(block, properties));
    }

    public static <T extends Block> DeferredBlock<T> registerBlockWithoutItem(String name, Supplier<T> blockSupplier) {
        return BLOCKS.register(name, blockSupplier);
    }

    public static <T extends Block> DeferredBlock<T> registerBlock(
            String name,
            Supplier<T> blockSupplier,
            BiFunction<T, Item.Properties, ? extends Item> itemFactory
    ) {
        DeferredBlock<T> holder = BLOCKS.register(name, blockSupplier);
        HPItems.ITEMS.register(name, () -> itemFactory.apply(holder.get(), new Item.Properties()));
        return holder;
    }

    public static BlockBehaviour.Properties stoneProperties() {
        return BlockBehaviour.Properties.ofFullCopy(Blocks.STONE).mapColor(MapColor.STONE);
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
