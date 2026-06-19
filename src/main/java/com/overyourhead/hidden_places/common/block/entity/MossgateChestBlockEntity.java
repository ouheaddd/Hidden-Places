package com.overyourhead.hidden_places.common.block.entity;

import com.overyourhead.hidden_places.common.block.MossgateChestBlock;
import com.overyourhead.hidden_places.common.menu.MossgateChestMenu;
import com.overyourhead.hidden_places.core.registry.HPBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class MossgateChestBlockEntity extends RandomizableContainerBlockEntity implements GeoBlockEntity {
    public static final int ROWS = 2;
    public static final int COLUMNS = 9;
    public static final int CONTAINER_SIZE = ROWS * COLUMNS;

    private static final RawAnimation IDLE_CLOSED = RawAnimation.begin().thenLoop("idle_closed");
    private static final RawAnimation IDLE_OPEN = RawAnimation.begin().thenLoop("idle_open");
    private static final RawAnimation OPENING = RawAnimation.begin()
            .thenPlay("opening")
            .thenLoop("idle_open");

    private static final RawAnimation CLOSING = RawAnimation.begin()
            .thenPlay("closing")
            .thenLoop("idle_closed");

    private static final int TRANSITION_RENDER_TICKS = 20;

    private final AnimatableInstanceCache animatableInstanceCache = GeckoLibUtil.createInstanceCache(this);
    private NonNullList<ItemStack> items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
    private int openCount;
    private boolean lastOpenState;
    private int transitionRenderTicks;

    public MossgateChestBlockEntity(BlockPos pos, BlockState blockState) {
        super(HPBlockEntities.MOSSGATE_CHEST.get(), pos, blockState);
        this.lastOpenState = isOpen(blockState);
    }

    @Override
    public int getContainerSize() {
        return CONTAINER_SIZE;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hidden_places.mossgate_chest");
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    public boolean hasAnyItem() {
        for (ItemStack stack : this.items) {
            if (!stack.isEmpty()) {
                return true;
            }
        }

        return false;
    }

    public ItemContainerContents createItemContainerContents() {
        return ItemContainerContents.fromItems(this.items);
    }

    @Override
    protected void applyImplicitComponents(BlockEntity.DataComponentInput componentInput) {
        super.applyImplicitComponents(componentInput);
        componentInput.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(this.items);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        components.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(this.items));
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        super.removeComponentsFromTag(tag);
        tag.remove("Items");
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory playerInventory) {
        Level level = this.getLevel();
        ContainerLevelAccess access = level == null
                ? ContainerLevelAccess.NULL
                : ContainerLevelAccess.create(level, this.getBlockPos());
        return new MossgateChestMenu(containerId, playerInventory, this, access);
    }

    @Override
    public void startOpen(Player player) {
        if (!this.isRemoved() && !player.isSpectator()) {
            if (this.openCount++ == 0) {
                this.updateOpenState(true);
            }
        }
    }

    @Override
    public void stopOpen(Player player) {
        if (!this.isRemoved() && !player.isSpectator()) {
            this.openCount = Math.max(0, this.openCount - 1);
            if (this.openCount == 0) {
                this.updateOpenState(false);
            }
        }
    }

    private void updateOpenState(boolean open) {
        Level level = this.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }

        BlockState state = this.getBlockState();
        if (state.hasProperty(MossgateChestBlock.OPEN) && state.getValue(MossgateChestBlock.OPEN) != open) {
            level.setBlock(this.getBlockPos(), state.setValue(MossgateChestBlock.OPEN, open), Block.UPDATE_CLIENTS);
        }
    }

    private static boolean isOpen(BlockState state) {
        return state.hasProperty(MossgateChestBlock.OPEN) && state.getValue(MossgateChestBlock.OPEN);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "mossgate_chest_controller", 0, this::handleAnimations));
    }

    private <E extends MossgateChestBlockEntity> PlayState handleAnimations(AnimationState<E> animationState) {
        boolean open = isOpen(this.getBlockState());

        if (open != this.lastOpenState) {
            this.lastOpenState = open;
            this.transitionRenderTicks = TRANSITION_RENDER_TICKS;
            animationState.getController().forceAnimationReset();
        }

        if (this.transitionRenderTicks > 0) {
            this.transitionRenderTicks--;
            return animationState.setAndContinue(open ? OPENING : CLOSING);
        }

        return animationState.setAndContinue(open ? IDLE_OPEN : IDLE_CLOSED);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.animatableInstanceCache;
    }
}
