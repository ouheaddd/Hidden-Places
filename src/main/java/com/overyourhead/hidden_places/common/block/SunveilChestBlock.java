package com.overyourhead.hidden_places.common.block;

import com.mojang.serialization.MapCodec;
import com.overyourhead.hidden_places.common.block.entity.SunveilChestBlockEntity;
import com.overyourhead.hidden_places.core.registry.HPItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class SunveilChestBlock extends MossgateChestBlock {
    public static final MapCodec<SunveilChestBlock> CODEC = simpleCodec(SunveilChestBlock::new);

    public SunveilChestBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SunveilChestBlockEntity(pos, state);
    }

    @Override
    protected Item getRequiredKey() {
        return HPItems.SUNVEIL_KEY.get();
    }

    @Override
    protected Component getLockedMessage() {
        return Component.translatable("message.hidden_places.sunveil_chest.locked");
    }
}
