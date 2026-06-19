package com.overyourhead.hidden_places.common.menu;

import com.overyourhead.hidden_places.common.block.entity.MossgateChestBlockEntity;
import com.overyourhead.hidden_places.core.registry.HPBlocks;
import com.overyourhead.hidden_places.core.registry.HPMenuTypes;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class SunveilChestMenu extends AbstractContainerMenu {
    private static final int CHEST_SLOT_COUNT = MossgateChestBlockEntity.CONTAINER_SIZE;
    private static final int PLAYER_INVENTORY_SLOT_COUNT = 27;
    private static final int PLAYER_HOTBAR_SLOT_COUNT = 9;
    private static final int PLAYER_SLOT_COUNT = PLAYER_INVENTORY_SLOT_COUNT + PLAYER_HOTBAR_SLOT_COUNT;
    private static final int TOTAL_SLOT_COUNT = CHEST_SLOT_COUNT + PLAYER_SLOT_COUNT;

    private final Container container;
    private final ContainerLevelAccess access;

    public SunveilChestMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(CHEST_SLOT_COUNT), ContainerLevelAccess.NULL);
    }

    public SunveilChestMenu(int containerId, Inventory playerInventory, Container container, ContainerLevelAccess access) {
        super(HPMenuTypes.SUNVEIL_CHEST.get(), containerId);
        checkContainerSize(container, CHEST_SLOT_COUNT);

        this.container = container;
        this.access = access;
        this.container.startOpen(playerInventory.player);

        addChestSlots(container);
        addPlayerInventorySlots(playerInventory);
        addPlayerHotbarSlots(playerInventory);
    }

    private void addChestSlots(Container container) {
        for (int row = 0; row < MossgateChestBlockEntity.ROWS; row++) {
            for (int column = 0; column < MossgateChestBlockEntity.COLUMNS; column++) {
                int index = column + row * MossgateChestBlockEntity.COLUMNS;
                int x = 8 + column * 18;
                int y = 18 + row * 18;
                this.addSlot(new Slot(container, index, x, y));
            }
        }
    }

    private void addPlayerInventorySlots(Inventory playerInventory) {
        int playerInventoryY = 32 + MossgateChestBlockEntity.ROWS * 18;

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                int index = column + row * 9 + 9;
                int x = 8 + column * 18;
                int y = playerInventoryY + row * 18;
                this.addSlot(new Slot(playerInventory, index, x, y));
            }
        }
    }

    private void addPlayerHotbarSlots(Inventory playerInventory) {
        int hotbarY = 90 + MossgateChestBlockEntity.ROWS * 18;

        for (int column = 0; column < 9; column++) {
            int x = 8 + column * 18;
            this.addSlot(new Slot(playerInventory, column, x, hotbarY));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int quickMovedSlotIndex) {
        ItemStack quickMovedStack = ItemStack.EMPTY;
        Slot quickMovedSlot = this.slots.get(quickMovedSlotIndex);

        if (quickMovedSlot != null && quickMovedSlot.hasItem()) {
            ItemStack rawStack = quickMovedSlot.getItem();
            quickMovedStack = rawStack.copy();

            if (quickMovedSlotIndex < CHEST_SLOT_COUNT) {
                if (!this.moveItemStackTo(rawStack, CHEST_SLOT_COUNT, TOTAL_SLOT_COUNT, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(rawStack, 0, CHEST_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }

            if (rawStack.isEmpty()) {
                quickMovedSlot.set(ItemStack.EMPTY);
            } else {
                quickMovedSlot.setChanged();
            }

            quickMovedSlot.onTake(player, rawStack);
        }

        return quickMovedStack;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, HPBlocks.SUNVEIL_CHEST.get());
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.container.stopOpen(player);
    }
}
