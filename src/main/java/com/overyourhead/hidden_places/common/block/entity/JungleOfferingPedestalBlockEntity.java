package com.overyourhead.hidden_places.common.block.entity;

import com.overyourhead.hidden_places.core.registry.HPBlockEntities;
import com.overyourhead.hidden_places.core.registry.HPItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class JungleOfferingPedestalBlockEntity extends BlockEntity implements GeoBlockEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final int TRANSFORM_DELAY_TICKS = 35;

    private final AnimatableInstanceCache animatableInstanceCache = GeckoLibUtil.createInstanceCache(this);
    private NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);
    private boolean trialMode;
    private boolean protectedMode;
    private boolean accepted;
    private int transformTicks;

    public JungleOfferingPedestalBlockEntity(BlockPos pos, BlockState blockState) {
        super(HPBlockEntities.JUNGLE_OFFERING_PEDESTAL.get(), pos, blockState);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, JungleOfferingPedestalBlockEntity pedestal) {
        pedestal.tickServer();
    }

    private void tickServer() {
        if (!(this.level instanceof ServerLevel serverLevel)) {
            return;
        }

        if (!this.trialMode || this.accepted || this.getDisplayedItem().isEmpty()) {
            this.transformTicks = 0;
            return;
        }

        if (!this.isCorrectOffering(this.getDisplayedItem())) {
            this.transformTicks = 0;
            return;
        }

        if (this.transformTicks <= 0) {
            this.transformTicks = TRANSFORM_DELAY_TICKS;
            serverLevel.playSound(null, this.worldPosition, sound(SoundEvents.AMETHYST_BLOCK_CHIME), SoundSource.BLOCKS, 0.75F, 0.75F);
        }

        this.spawnOfferingParticles(serverLevel);
        this.transformTicks--;

        if (this.transformTicks <= 0) {
            this.items.set(0, new ItemStack(HPItems.WILDROOT_KEY.get()));
            this.accepted = true;
            this.setChangedAndSync();
            serverLevel.playSound(null, this.worldPosition, sound(SoundEvents.ENCHANTMENT_TABLE_USE), SoundSource.BLOCKS, 0.9F, 1.1F);
            serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    this.worldPosition.getX() + 0.5D,
                    this.worldPosition.getY() + 1.25D,
                    this.worldPosition.getZ() + 0.5D,
                    18, 0.25D, 0.15D, 0.25D, 0.02D);
        }
    }

    private boolean isCorrectOffering(ItemStack stack) {
        return stack.is(Items.TORCHFLOWER);
    }

    private void spawnOfferingParticles(ServerLevel serverLevel) {
        double x = this.worldPosition.getX() + 0.5D;
        double y = this.worldPosition.getY() + 1.22D;
        double z = this.worldPosition.getZ() + 0.5D;
        serverLevel.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, x, y, z, 2, 0.12D, 0.05D, 0.12D, 0.0D);
        if (this.transformTicks % 5 == 0) {
            serverLevel.sendParticles(ParticleTypes.ENCHANT, x, y, z, 3, 0.18D, 0.08D, 0.18D, 0.03D);
        }
    }

    public ItemStack getDisplayedItem() {
        return this.items.getFirst();
    }

    public boolean isEmpty() {
        return this.getDisplayedItem().isEmpty();
    }

    public void setDisplayedItem(ItemStack stack) {
        ItemStack copy = stack.copy();
        copy.setCount(Math.min(1, copy.getCount()));
        this.items.set(0, copy);
        this.transformTicks = 0;
        this.setChangedAndSync();
        this.playPlaceSound();
    }

    public ItemStack removeDisplayedItem() {
        ItemStack stack = this.getDisplayedItem().copy();
        this.items.set(0, ItemStack.EMPTY);
        this.transformTicks = 0;
        this.setChangedAndSync();
        this.playPlaceSound();
        return stack;
    }

    public void dropStoredItem() {
        if (this.level == null || this.level.isClientSide) {
            return;
        }

        ItemStack stack = this.getDisplayedItem();
        if (!stack.isEmpty()) {
            Block.popResource(this.level, this.worldPosition, stack.copy());
            this.items.set(0, ItemStack.EMPTY);
        }
    }

    public boolean isTrialMode() {
        return this.trialMode;
    }

    public boolean isProtectedMode() {
        return this.protectedMode;
    }

    public void setTrialMode(boolean trialMode) {
        this.trialMode = trialMode;
        this.setChangedAndSync();
    }

    public void setProtectedMode(boolean protectedMode) {
        this.protectedMode = protectedMode;
        this.setChangedAndSync();
    }

    public void toggleTrialMode() {
        this.trialMode = !this.trialMode;
        this.setChangedAndSync();
    }

    private void playPlaceSound() {
        if (this.level != null && !this.level.isClientSide) {
            this.level.playSound(null, this.worldPosition, sound(SoundEvents.DECORATED_POT_INSERT), SoundSource.BLOCKS, 0.55F, 0.9F);
        }
    }


    private static SoundEvent sound(SoundEvent soundEvent) {
        return soundEvent;
    }

    private static SoundEvent sound(Holder<SoundEvent> soundEventHolder) {
        return soundEventHolder.value();
    }

    private void setChangedAndSync() {
        this.setChanged();
        if (this.level != null) {
            BlockState state = this.getBlockState();
            this.level.sendBlockUpdated(this.worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, this.items, registries);
        tag.putBoolean("TrialMode", this.trialMode);
        tag.putBoolean("ProtectedMode", this.protectedMode);
        tag.putBoolean("Accepted", this.accepted);
        tag.putInt("TransformTicks", this.transformTicks);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.items = NonNullList.withSize(1, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, this.items, registries);
        this.trialMode = tag.getBoolean("TrialMode");
        this.protectedMode = tag.getBoolean("ProtectedMode");
        this.accepted = tag.getBoolean("Accepted");
        this.transformTicks = tag.getInt("TransformTicks");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    @Override
    public @Nullable ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "jungle_offering_pedestal_controller", 0, this::handleAnimations));
    }

    private <E extends JungleOfferingPedestalBlockEntity> PlayState handleAnimations(AnimationState<E> animationState) {
        return animationState.setAndContinue(IDLE);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.animatableInstanceCache;
    }
}
