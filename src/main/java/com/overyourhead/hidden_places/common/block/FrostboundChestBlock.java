package com.overyourhead.hidden_places.common.block;

import com.mojang.serialization.MapCodec;
import com.overyourhead.hidden_places.common.block.entity.FrostboundChestBlockEntity;
import com.overyourhead.hidden_places.core.registry.HPItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class FrostboundChestBlock extends MossgateChestBlock {
    public static final MapCodec<FrostboundChestBlock> CODEC = simpleCodec(FrostboundChestBlock::new);

    public FrostboundChestBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FrostboundChestBlockEntity(pos, state);
    }

    @Override
    protected Item getRequiredKey() {
        return HPItems.FROSTBOUND_KEY.get();
    }

    @Override
    protected Component getLockedMessage() {
        return Component.translatable("message.hidden_places.frostbound_chest.locked");
    }
}
