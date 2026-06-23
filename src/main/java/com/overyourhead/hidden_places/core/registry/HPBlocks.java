package com.overyourhead.hidden_places.core.registry;

import com.overyourhead.hidden_places.HiddenPlacesMod;
import com.overyourhead.hidden_places.common.block.FrostboundChestBlock;
import com.overyourhead.hidden_places.common.block.JungleMosaicTileBlock;
import com.overyourhead.hidden_places.common.block.JunglePathControllerBlock;
import com.overyourhead.hidden_places.common.block.JunglePathTileBlock;
import com.overyourhead.hidden_places.common.block.JungleTrialMasterControllerBlock;
import com.overyourhead.hidden_places.common.block.JungleOfferingPedestalBlock;
import com.overyourhead.hidden_places.common.block.MossgateChestBlock;
import com.overyourhead.hidden_places.common.block.SunveilChestBlock;
import com.overyourhead.hidden_places.common.block.WildrootChestBlock;
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

    public static final DeferredBlock<FrostboundChestBlock> FROSTBOUND_CHEST = registerBlock(
            "frostbound_chest",
            () -> new FrostboundChestBlock(MossgateChestBlock.createProperties())
    );

    public static final DeferredBlock<SunveilChestBlock> SUNVEIL_CHEST = registerBlock(
            "sunveil_chest",
            () -> new SunveilChestBlock(MossgateChestBlock.createProperties())
    );

    public static final DeferredBlock<WildrootChestBlock> WILDROOT_CHEST = registerBlock(
            "wildroot_chest",
            () -> new WildrootChestBlock(MossgateChestBlock.createProperties())
    );

    public static final DeferredBlock<MossgateChestBlock> MOSSGATE_CHEST = registerBlock(
            "mossgate_chest",
            () -> new MossgateChestBlock(MossgateChestBlock.createProperties())
    );


    public static final DeferredBlock<JungleMosaicTileBlock> JUNGLE_MOSAIC_TILE = registerBlock(
            "jungle_mosaic_tile",
            () -> new JungleMosaicTileBlock(JungleMosaicTileBlock.createProperties())
    );

    public static final DeferredBlock<JungleOfferingPedestalBlock> JUNGLE_OFFERING_PEDESTAL = registerBlock(
            "jungle_offering_pedestal",
            () -> new JungleOfferingPedestalBlock(JungleOfferingPedestalBlock.createProperties())
    );

    public static final DeferredBlock<JunglePathTileBlock> JUNGLE_PATH_TILE = registerBlock(
            "jungle_path_tile",
            () -> new JunglePathTileBlock(JunglePathTileBlock.createProperties())
    );

    public static final DeferredBlock<JunglePathControllerBlock> JUNGLE_PATH_CONTROLLER = registerBlockWithoutItem(
            "jungle_path_controller",
            () -> new JunglePathControllerBlock(JunglePathControllerBlock.createProperties())
    );

    public static final DeferredBlock<JungleTrialMasterControllerBlock> JUNGLE_TRIAL_MASTER_CONTROLLER = registerBlockWithoutItem(
            "jungle_trial_master_controller",
            () -> new JungleTrialMasterControllerBlock(JungleTrialMasterControllerBlock.createProperties())
    );

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
