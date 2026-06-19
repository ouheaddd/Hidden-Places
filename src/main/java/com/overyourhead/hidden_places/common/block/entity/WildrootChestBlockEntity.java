package com.overyourhead.hidden_places.common.block.entity;

import com.overyourhead.hidden_places.common.menu.WildrootChestMenu;
import com.overyourhead.hidden_places.core.registry.HPBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class WildrootChestBlockEntity extends MossgateChestBlockEntity {
    public WildrootChestBlockEntity(BlockPos pos, BlockState blockState) {
        super(HPBlockEntities.WILDROOT_CHEST.get(), pos, blockState);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hidden_places.wildroot_chest");
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory playerInventory) {
        Level level = this.getLevel();
        ContainerLevelAccess access = level == null
                ? ContainerLevelAccess.NULL
                : ContainerLevelAccess.create(level, this.getBlockPos());
        return new WildrootChestMenu(containerId, playerInventory, this, access);
    }
}
