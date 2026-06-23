package com.overyourhead.hidden_places.common.block.entity;

import com.overyourhead.hidden_places.common.block.JungleMosaicTileBlock;
import com.overyourhead.hidden_places.core.registry.HPBlockEntities;
import com.overyourhead.hidden_places.core.registry.HPBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class JungleTrialMasterControllerBlockEntity extends BlockEntity {
    private static final int SEARCH_RADIUS = 48;
    private static final int PEDESTAL_RADIUS = 32;
    private static final int REWARD_RESET_DELAY_TICKS = 100;

    private boolean mosaicSolved;
    private boolean pathSolved;
    private boolean offeringAccepted;
    private int rewardResetTicks;

    @Nullable
    private BlockPos mosaicCenter;
    @Nullable
    private Direction mosaicFacing;
    @Nullable
    private BlockPos pathControllerPos;
    @Nullable
    private BlockPos pedestalPos;
    @Nullable
    private BlockPos rewardTeleportPos;
    @Nullable
    private BlockPos trialBoundsMin;
    @Nullable
    private BlockPos trialBoundsMax;

    public JungleTrialMasterControllerBlockEntity(BlockPos pos, BlockState blockState) {
        super(HPBlockEntities.JUNGLE_TRIAL_MASTER_CONTROLLER.get(), pos, blockState);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, JungleTrialMasterControllerBlockEntity controller) {
        if (!(level instanceof ServerLevel serverLevel) || controller.rewardResetTicks <= 0) {
            return;
        }

        controller.rewardResetTicks--;
        if (controller.rewardResetTicks <= 0) {
            controller.finishRewardReset(serverLevel);
        } else {
            controller.setChangedAndSync();
        }
    }

    public static Optional<JungleTrialMasterControllerBlockEntity> findNearest(ServerLevel level, BlockPos origin) {
        JungleTrialMasterControllerBlockEntity nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (BlockPos checkPos : BlockPos.betweenClosed(
                origin.offset(-SEARCH_RADIUS, -8, -SEARCH_RADIUS),
                origin.offset(SEARCH_RADIUS, 8, SEARCH_RADIUS))) {
            BlockEntity blockEntity = level.getBlockEntity(checkPos);
            if (blockEntity instanceof JungleTrialMasterControllerBlockEntity controller) {
                double distance = checkPos.distSqr(origin);
                if (distance < nearestDistance) {
                    nearest = controller;
                    nearestDistance = distance;
                }
            }
        }

        return Optional.ofNullable(nearest);
    }

    public void configureLinkedTrial(BlockPos mosaicCenter, Direction mosaicFacing, BlockPos pathControllerPos, BlockPos pedestalPos,
                                     BlockPos rewardTeleportPos, BlockPos trialBoundsMin, BlockPos trialBoundsMax) {
        this.mosaicCenter = mosaicCenter.immutable();
        this.mosaicFacing = mosaicFacing;
        this.pathControllerPos = pathControllerPos.immutable();
        this.pedestalPos = pedestalPos.immutable();
        this.rewardTeleportPos = rewardTeleportPos.immutable();
        this.trialBoundsMin = trialBoundsMin.immutable();
        this.trialBoundsMax = trialBoundsMax.immutable();
        this.setChangedAndSync();
    }

    public boolean isPathUnlocked() {
        return this.mosaicSolved;
    }

    public boolean isPedestalUnlocked() {
        return this.pathSolved;
    }

    public boolean isOfferingAccepted() {
        return this.offeringAccepted;
    }

    public void setMosaicSolved(ServerLevel level, BlockPos sourcePos) {
        if (this.mosaicSolved) {
            return;
        }

        this.mosaicSolved = true;
        this.setChangedAndSync();
        level.playSound(null, sourcePos, sound(SoundEvents.AMETHYST_BLOCK_CHIME), SoundSource.BLOCKS, 1.0F, 1.35F);
        level.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR,
                sourcePos.getX() + 0.5D,
                sourcePos.getY() + 0.5D,
                sourcePos.getZ() + 0.5D,
                28, 0.75D, 0.45D, 0.75D, 0.02D);
    }

    public void setPathSolved(ServerLevel level, BlockPos sourcePos) {
        if (this.pathSolved) {
            return;
        }

        this.pathSolved = true;
        this.setChangedAndSync();
        level.playSound(null, sourcePos, sound(SoundEvents.ENCHANTMENT_TABLE_USE), SoundSource.BLOCKS, 0.95F, 1.05F);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                sourcePos.getX() + 0.5D,
                sourcePos.getY() + 1.0D,
                sourcePos.getZ() + 0.5D,
                30, 0.75D, 0.25D, 0.75D, 0.04D);
        this.activateNearbyPedestals(level);
    }

    public void setOfferingAccepted(ServerLevel level, BlockPos sourcePos) {
        if (this.offeringAccepted) {
            return;
        }

        this.offeringAccepted = true;
        this.setChangedAndSync();
        level.playSound(null, sourcePos, sound(SoundEvents.AMETHYST_BLOCK_BREAK), SoundSource.BLOCKS, 0.85F, 0.65F);
        level.sendParticles(ParticleTypes.ENCHANT,
                sourcePos.getX() + 0.5D,
                sourcePos.getY() + 1.35D,
                sourcePos.getZ() + 0.5D,
                30, 0.35D, 0.35D, 0.35D, 0.05D);
    }

    public void startRewardReset(ServerLevel level, BlockPos sourcePos) {
        if (this.rewardResetTicks > 0) {
            return;
        }

        this.rewardResetTicks = REWARD_RESET_DELAY_TICKS;
        this.setChangedAndSync();
        level.playSound(null, sourcePos, sound(SoundEvents.BEACON_DEACTIVATE), SoundSource.BLOCKS, 0.75F, 1.2F);
        level.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR,
                sourcePos.getX() + 0.5D,
                sourcePos.getY() + 1.2D,
                sourcePos.getZ() + 0.5D,
                28, 0.4D, 0.25D, 0.4D, 0.02D);
    }

    private void finishRewardReset(ServerLevel level) {
        this.teleportPlayersOut(level);
        this.restoreMosaic(level);
        this.resetPathController(level);
        this.resetPedestal(level);

        this.mosaicSolved = false;
        this.pathSolved = false;
        this.offeringAccepted = false;
        this.rewardResetTicks = 0;
        this.setChangedAndSync();

        BlockPos effectPos = this.rewardTeleportPos != null ? this.rewardTeleportPos : this.worldPosition.above(2);
        level.playSound(null, effectPos, sound(SoundEvents.ENDERMAN_TELEPORT), SoundSource.BLOCKS, 0.75F, 0.9F);
        level.sendParticles(ParticleTypes.ENCHANT,
                effectPos.getX() + 0.5D,
                effectPos.getY() + 0.6D,
                effectPos.getZ() + 0.5D,
                30, 0.35D, 0.35D, 0.35D, 0.04D);
    }

    private void restoreMosaic(ServerLevel level) {
        if (this.mosaicCenter == null || this.mosaicFacing == null) {
            return;
        }

        Direction right = this.mosaicFacing.getClockWise();
        BlockPos topLeft = this.mosaicCenter.relative(right, -JungleMosaicTileBlock.WIDTH / 2).above(JungleMosaicTileBlock.HEIGHT / 2);

        for (int row = 0; row < JungleMosaicTileBlock.HEIGHT; row++) {
            for (int col = 0; col < JungleMosaicTileBlock.WIDTH; col++) {
                int piece = JungleMosaicTileBlock.pieceForGridPosition(row, col);
                int rotation = level.random.nextInt(4);
                BlockPos tilePos = topLeft.relative(right, col).below(row);
                BlockState tileState = HPBlocks.JUNGLE_MOSAIC_TILE.get().defaultBlockState()
                        .setValue(JungleMosaicTileBlock.FACING, this.mosaicFacing)
                        .setValue(JungleMosaicTileBlock.PIECE, piece)
                        .setValue(JungleMosaicTileBlock.IMAGE_ROTATION, rotation)
                        .setValue(JungleMosaicTileBlock.SOLVED, false)
                        .setValue(JungleMosaicTileBlock.CRUMBLING, false);
                level.setBlock(tilePos, tileState, Block.UPDATE_ALL);
            }
        }
    }

    private void resetPathController(ServerLevel level) {
        if (this.pathControllerPos == null) {
            return;
        }

        BlockEntity blockEntity = level.getBlockEntity(this.pathControllerPos);
        if (blockEntity instanceof JunglePathControllerBlockEntity pathController) {
            pathController.resetTrial(level);
        }
    }

    private void resetPedestal(ServerLevel level) {
        if (this.pedestalPos == null) {
            return;
        }

        BlockEntity blockEntity = level.getBlockEntity(this.pedestalPos);
        if (blockEntity instanceof JungleOfferingPedestalBlockEntity pedestal) {
            pedestal.resetForTrial();
        }
    }

    private void teleportPlayersOut(ServerLevel level) {
        BlockPos target = this.rewardTeleportPos != null ? this.rewardTeleportPos : this.worldPosition.above(2);
        for (ServerPlayer player : level.players()) {
            if (this.isInsideTrialBounds(player.blockPosition())) {
                player.teleportTo(target.getX() + 0.5D, target.getY() + 0.08D, target.getZ() + 0.5D);
            }
        }
    }

    private boolean isInsideTrialBounds(BlockPos pos) {
        BlockPos min = this.trialBoundsMin != null ? this.trialBoundsMin : this.worldPosition.offset(-SEARCH_RADIUS, -8, -SEARCH_RADIUS);
        BlockPos max = this.trialBoundsMax != null ? this.trialBoundsMax : this.worldPosition.offset(SEARCH_RADIUS, 16, SEARCH_RADIUS);

        int minX = Math.min(min.getX(), max.getX());
        int minY = Math.min(min.getY(), max.getY());
        int minZ = Math.min(min.getZ(), max.getZ());
        int maxX = Math.max(min.getX(), max.getX());
        int maxY = Math.max(min.getY(), max.getY());
        int maxZ = Math.max(min.getZ(), max.getZ());

        return pos.getX() >= minX && pos.getX() <= maxX
                && pos.getY() >= minY && pos.getY() <= maxY
                && pos.getZ() >= minZ && pos.getZ() <= maxZ;
    }

    private void activateNearbyPedestals(ServerLevel level) {
        for (BlockPos checkPos : BlockPos.betweenClosed(
                this.worldPosition.offset(-PEDESTAL_RADIUS, -4, -PEDESTAL_RADIUS),
                this.worldPosition.offset(PEDESTAL_RADIUS, 8, PEDESTAL_RADIUS))) {
            BlockEntity blockEntity = level.getBlockEntity(checkPos);
            if (blockEntity instanceof JungleOfferingPedestalBlockEntity pedestal && pedestal.isProtectedMode()) {
                pedestal.setTrialMode(true);
                pedestal.setProtectedMode(true);
                level.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR,
                        checkPos.getX() + 0.5D,
                        checkPos.getY() + 1.25D,
                        checkPos.getZ() + 0.5D,
                        22, 0.3D, 0.16D, 0.3D, 0.01D);
                level.playSound(null, checkPos, sound(SoundEvents.AMETHYST_BLOCK_CHIME), SoundSource.BLOCKS, 0.85F, 0.85F);
            }
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
        tag.putBoolean("MosaicSolved", this.mosaicSolved);
        tag.putBoolean("PathSolved", this.pathSolved);
        tag.putBoolean("OfferingAccepted", this.offeringAccepted);
        tag.putInt("RewardResetTicks", this.rewardResetTicks);
        if (this.mosaicCenter != null) {
            tag.putLong("MosaicCenter", this.mosaicCenter.asLong());
        }
        if (this.mosaicFacing != null) {
            tag.putInt("MosaicFacing", this.mosaicFacing.get3DDataValue());
        }
        if (this.pathControllerPos != null) {
            tag.putLong("PathControllerPos", this.pathControllerPos.asLong());
        }
        if (this.pedestalPos != null) {
            tag.putLong("PedestalPos", this.pedestalPos.asLong());
        }
        if (this.rewardTeleportPos != null) {
            tag.putLong("RewardTeleportPos", this.rewardTeleportPos.asLong());
        }
        if (this.trialBoundsMin != null) {
            tag.putLong("TrialBoundsMin", this.trialBoundsMin.asLong());
        }
        if (this.trialBoundsMax != null) {
            tag.putLong("TrialBoundsMax", this.trialBoundsMax.asLong());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.mosaicSolved = tag.getBoolean("MosaicSolved");
        this.pathSolved = tag.getBoolean("PathSolved");
        this.offeringAccepted = tag.getBoolean("OfferingAccepted");
        this.rewardResetTicks = tag.getInt("RewardResetTicks");
        this.mosaicCenter = tag.contains("MosaicCenter") ? BlockPos.of(tag.getLong("MosaicCenter")) : null;
        this.mosaicFacing = tag.contains("MosaicFacing") ? Direction.from3DDataValue(tag.getInt("MosaicFacing")) : null;
        this.pathControllerPos = tag.contains("PathControllerPos") ? BlockPos.of(tag.getLong("PathControllerPos")) : null;
        this.pedestalPos = tag.contains("PedestalPos") ? BlockPos.of(tag.getLong("PedestalPos")) : null;
        this.rewardTeleportPos = tag.contains("RewardTeleportPos") ? BlockPos.of(tag.getLong("RewardTeleportPos")) : null;
        this.trialBoundsMin = tag.contains("TrialBoundsMin") ? BlockPos.of(tag.getLong("TrialBoundsMin")) : null;
        this.trialBoundsMax = tag.contains("TrialBoundsMax") ? BlockPos.of(tag.getLong("TrialBoundsMax")) : null;
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
