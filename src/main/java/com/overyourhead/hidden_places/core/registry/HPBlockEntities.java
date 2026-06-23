package com.overyourhead.hidden_places.core.registry;

import com.overyourhead.hidden_places.HiddenPlacesMod;
import com.overyourhead.hidden_places.common.block.entity.FrostboundChestBlockEntity;
import com.overyourhead.hidden_places.common.block.entity.JungleOfferingPedestalBlockEntity;
import com.overyourhead.hidden_places.common.block.entity.JunglePathControllerBlockEntity;
import com.overyourhead.hidden_places.common.block.entity.JungleTrialMasterControllerBlockEntity;
import com.overyourhead.hidden_places.common.block.entity.MossgateChestBlockEntity;
import com.overyourhead.hidden_places.common.block.entity.SunveilChestBlockEntity;
import com.overyourhead.hidden_places.common.block.entity.WildrootChestBlockEntity;
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

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FrostboundChestBlockEntity>> FROSTBOUND_CHEST =
            register("frostbound_chest", FrostboundChestBlockEntity::new, HPBlocks.FROSTBOUND_CHEST);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SunveilChestBlockEntity>> SUNVEIL_CHEST =
            register("sunveil_chest", SunveilChestBlockEntity::new, HPBlocks.SUNVEIL_CHEST);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WildrootChestBlockEntity>> WILDROOT_CHEST =
            register("wildroot_chest", WildrootChestBlockEntity::new, HPBlocks.WILDROOT_CHEST);


    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<JungleOfferingPedestalBlockEntity>> JUNGLE_OFFERING_PEDESTAL =
            register("jungle_offering_pedestal", JungleOfferingPedestalBlockEntity::new, HPBlocks.JUNGLE_OFFERING_PEDESTAL);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<JunglePathControllerBlockEntity>> JUNGLE_PATH_CONTROLLER =
            register("jungle_path_controller", JunglePathControllerBlockEntity::new, HPBlocks.JUNGLE_PATH_CONTROLLER);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<JungleTrialMasterControllerBlockEntity>> JUNGLE_TRIAL_MASTER_CONTROLLER =
            register("jungle_trial_master_controller", JungleTrialMasterControllerBlockEntity::new, HPBlocks.JUNGLE_TRIAL_MASTER_CONTROLLER);

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
