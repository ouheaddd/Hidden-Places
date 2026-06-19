package com.overyourhead.hidden_places.core.registry;

import com.overyourhead.hidden_places.HiddenPlacesMod;
import com.overyourhead.hidden_places.common.block.entity.MossgateChestBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Arrays;

public final class HPBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, HiddenPlacesMod.MOD_ID);


    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MossgateChestBlockEntity>> MOSSGATE_CHEST =
            register("mossgate_chest", MossgateChestBlockEntity::new, HPBlocks.MOSSGATE_CHEST);

    private HPBlockEntities() {
    }

    @SafeVarargs
    public static <T extends BlockEntity> DeferredHolder<BlockEntityType<?>, BlockEntityType<T>> register(
            String name,
            BlockEntityType.BlockEntitySupplier<T> factory,
            DeferredBlock<? extends Block>... validBlocks
    ) {
        return BLOCK_ENTITY_TYPES.register(name, () -> BlockEntityType.Builder
                .of(factory, Arrays.stream(validBlocks).map(DeferredBlock::get).toArray(Block[]::new))
                .build(null)
        );
    }

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITY_TYPES.register(eventBus);
    }
}
