package com.overyourhead.hidden_places.common.block.entity;

import com.overyourhead.hidden_places.common.block.JunglePathTileBlock;
import com.overyourhead.hidden_places.core.registry.HPBlockEntities;
import com.overyourhead.hidden_places.core.registry.HPBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class JunglePathControllerBlockEntity extends BlockEntity {
    public static final int TOTAL_STEPS = 10;
    private static final int SEARCH_RADIUS = 16;
    private static final int TILE_RESET_RADIUS = 16;
    private static final int PEDESTAL_RADIUS = 18;
    private static final int FAIL_COOLDOWN_TICKS = 20;

    private int currentStep;
    private boolean solved;
    private int failCooldown;

    public JunglePathControllerBlockEntity(BlockPos pos, BlockState blockState) {
        super(HPBlockEntities.JUNGLE_PATH_CONTROLLER.get(), pos, blockState);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, JunglePathControllerBlockEntity controller) {
        if (controller.failCooldown > 0) {
            controller.failCooldown--;
        }
    }

    public static Optional<JunglePathControllerBlockEntity> findNearest(ServerLevel level, BlockPos tilePos) {
        JunglePathControllerBlockEntity nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (BlockPos checkPos : BlockPos.betweenClosed(
                tilePos.offset(-SEARCH_RADIUS, -4, -SEARCH_RADIUS),
                tilePos.offset(SEARCH_RADIUS, 4, SEARCH_RADIUS))) {
            BlockEntity blockEntity = level.getBlockEntity(checkPos);
            if (blockEntity instanceof JunglePathControllerBlockEntity controller && !controller.solved) {
                double distance = checkPos.distSqr(tilePos);
                if (distance < nearestDistance) {
                    nearest = controller;
                    nearestDistance = distance;
                }
            }
        }

        return Optional.ofNullable(nearest);
    }

    public void handleTileStep(ServerPlayer player, BlockPos tilePos, BlockState tileState) {
        if (!(this.level instanceof ServerLevel serverLevel) || this.solved) {
            return;
        }

        int tileStep = tileState.getValue(JunglePathTileBlock.STEP);
        int expectedStep = this.currentStep + 1;

        if (tileStep == expectedStep) {
            acceptStep(serverLevel, player, tilePos, tileState);
            return;
        }

        if (this.failCooldown <= 0) {
            fail(serverLevel, player, tilePos, tileState);
        }
    }

    private void acceptStep(ServerLevel serverLevel, ServerPlayer player, BlockPos tilePos, BlockState tileState) {
        this.currentStep++;
        serverLevel.setBlock(tilePos, tileState.setValue(JunglePathTileBlock.LIT, true), Block.UPDATE_ALL);
        this.setChangedAndSync();

        double x = tilePos.getX() + 0.5D;
        double y = tilePos.getY() + 1.08D;
        double z = tilePos.getZ() + 0.5D;
        serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, x, y, z, 10, 0.28D, 0.06D, 0.28D, 0.02D);
        serverLevel.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, x, y, z, 8, 0.25D, 0.05D, 0.25D, 0.0D);
        serverLevel.playSound(null, tilePos, sound(SoundEvents.AMETHYST_BLOCK_CHIME), SoundSource.BLOCKS, 0.55F, 1.1F + this.currentStep * 0.08F);

        if (this.currentStep >= TOTAL_STEPS) {
            solve(serverLevel, player);
        }
    }

    private void fail(ServerLevel serverLevel, ServerPlayer player, BlockPos tilePos, BlockState tileState) {
        this.failCooldown = FAIL_COOLDOWN_TICKS;
        this.currentStep = 0;
        this.setChangedAndSync();
        this.clearLitTiles(serverLevel);
        this.applyRandomFailureEffect(player);

        double x = tilePos.getX() + 0.5D;
        double y = tilePos.getY() + 1.04D;
        double z = tilePos.getZ() + 0.5D;
        serverLevel.sendParticles(ParticleTypes.SMOKE, x, y, z, 18, 0.35D, 0.05D, 0.35D, 0.01D);
        serverLevel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, tileState), x, y, z, 18, 0.28D, 0.06D, 0.28D, 0.03D);
        serverLevel.playSound(null, tilePos, sound(SoundEvents.STONE_BREAK), SoundSource.BLOCKS, 0.65F, 0.7F);

        teleportToReset(player);
    }

    private void solve(ServerLevel serverLevel, ServerPlayer player) {
        this.solved = true;
        this.currentStep = TOTAL_STEPS;
        this.setChangedAndSync();

        double x = this.worldPosition.getX() + 0.5D;
        double y = this.worldPosition.getY() + 2.1D;
        double z = this.worldPosition.getZ() + 0.5D;
        serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, x, y, z, 35, 0.65D, 0.25D, 0.65D, 0.04D);
        serverLevel.sendParticles(ParticleTypes.ENCHANT, x, y, z, 25, 0.7D, 0.25D, 0.7D, 0.06D);
        serverLevel.playSound(null, this.worldPosition, sound(SoundEvents.ENCHANTMENT_TABLE_USE), SoundSource.BLOCKS, 0.85F, 1.1F);

        this.activateNearbyPedestals(serverLevel);
    }

    private void clearLitTiles(ServerLevel serverLevel) {
        for (BlockPos checkPos : BlockPos.betweenClosed(
                this.worldPosition.offset(-TILE_RESET_RADIUS, -2, -TILE_RESET_RADIUS),
                this.worldPosition.offset(TILE_RESET_RADIUS, 4, TILE_RESET_RADIUS))) {
            BlockState state = serverLevel.getBlockState(checkPos);
            if (state.is(HPBlocks.JUNGLE_PATH_TILE.get()) && state.getValue(JunglePathTileBlock.LIT)) {
                serverLevel.setBlock(checkPos, state.setValue(JunglePathTileBlock.LIT, false), Block.UPDATE_ALL);
            }
        }
    }

    private void activateNearbyPedestals(ServerLevel serverLevel) {
        for (BlockPos checkPos : BlockPos.betweenClosed(
                this.worldPosition.offset(-PEDESTAL_RADIUS, -2, -PEDESTAL_RADIUS),
                this.worldPosition.offset(PEDESTAL_RADIUS, 5, PEDESTAL_RADIUS))) {
            BlockEntity blockEntity = serverLevel.getBlockEntity(checkPos);
            if (blockEntity instanceof JungleOfferingPedestalBlockEntity pedestal) {
                pedestal.setTrialMode(true);
                pedestal.setProtectedMode(true);
                serverLevel.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR,
                        checkPos.getX() + 0.5D,
                        checkPos.getY() + 1.2D,
                        checkPos.getZ() + 0.5D,
                        18, 0.3D, 0.14D, 0.3D, 0.01D);
                serverLevel.playSound(null, checkPos, sound(SoundEvents.AMETHYST_BLOCK_CHIME), SoundSource.BLOCKS, 0.8F, 0.85F);
            }
        }
    }

    private void applyRandomFailureEffect(ServerPlayer player) {
        int pick = player.serverLevel().random.nextInt(5);
        switch (pick) {
            case 0 -> player.addEffect(new MobEffectInstance(MobEffects.POISON, 70, 0));
            case 1 -> player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 45, 0));
            case 2 -> player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 90, 0));
            case 3 -> player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 70, 0));
            default -> player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 90, 0));
        }
    }

    private void teleportToReset(ServerPlayer player) {
        double x = this.worldPosition.getX() + 0.5D;
        double y = this.worldPosition.getY() + 2.08D;
        double z = this.worldPosition.getZ() + 0.5D;
        player.teleportTo(x, y, z);
    }

    public boolean isSolved() {
        return this.solved;
    }

    public int getCurrentStep() {
        return this.currentStep;
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
        tag.putInt("CurrentStep", this.currentStep);
        tag.putBoolean("Solved", this.solved);
        tag.putInt("FailCooldown", this.failCooldown);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.currentStep = tag.getInt("CurrentStep");
        this.solved = tag.getBoolean("Solved");
        this.failCooldown = tag.getInt("FailCooldown");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    @Override
    public @Nullable ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
