package com.overyourhead.hidden_places.common.item;

import com.overyourhead.hidden_places.common.block.JungleMosaicTileBlock;
import com.overyourhead.hidden_places.common.block.JunglePathTileBlock;
import com.overyourhead.hidden_places.common.block.entity.JungleOfferingPedestalBlockEntity;
import com.overyourhead.hidden_places.common.block.entity.JunglePathControllerBlockEntity;
import com.overyourhead.hidden_places.common.block.entity.JungleTrialMasterControllerBlockEntity;
import com.overyourhead.hidden_places.core.registry.HPBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class JungleTrialRoomPlacerItem extends Item {
    private static final int PATH_WIDTH = 5;
    private static final int PATH_LENGTH = 10;
    private static final int RESET_DISTANCE_FROM_MOSAIC = 2;
    private static final int PATH_START_DISTANCE_FROM_RESET = 2;
    private static final int PEDESTAL_DISTANCE_AFTER_PATH = 4;

    private static final int[][] PATH = {
            {0, 2, JunglePathTileBlock.SYMBOL_LEAF},
            {1, 2, JunglePathTileBlock.SYMBOL_SUN},
            {2, 3, JunglePathTileBlock.SYMBOL_WATER},
            {3, 3, JunglePathTileBlock.SYMBOL_SNAKE},
            {4, 1, JunglePathTileBlock.SYMBOL_FLOWER},
            {5, 1, JunglePathTileBlock.SYMBOL_LEAF},
            {6, 0, JunglePathTileBlock.SYMBOL_WATER},
            {7, 2, JunglePathTileBlock.SYMBOL_SUN},
            {8, 4, JunglePathTileBlock.SYMBOL_SNAKE},
            {9, 4, JunglePathTileBlock.SYMBOL_FLOWER}
    };

    public JungleTrialRoomPlacerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        Direction facing = context.getClickedFace();

        if (facing.getAxis().isVertical()) {
            if (player != null && !level.isClientSide) {
                player.displayClientMessage(Component.literal("Use this on a vertical wall face at the mosaic center."), true);
            }
            return InteractionResult.FAIL;
        }

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        Direction mosaicForward = facing;
        Direction trialForward = facing.getOpposite();
        Direction right = facing.getClockWise();
        BlockPos mosaicCenter = context.getClickedPos().relative(facing);
        BlockPos trialBaseCenter = mosaicCenter.below();
        boolean solvedPreview = player != null && player.isShiftKeyDown();

        this.placeMosaic(level, mosaicCenter, mosaicForward, right, solvedPreview);

        BlockPos markerFloorPos = trialBaseCenter
                .relative(trialForward, RESET_DISTANCE_FROM_MOSAIC)
                .below(JungleMosaicTileBlock.HEIGHT / 2);
        BlockPos pathControllerPos = markerFloorPos.below();
        BlockPos masterControllerPos = pathControllerPos.below();

        BlockPos pathTopLeft = markerFloorPos.relative(trialForward, PATH_START_DISTANCE_FROM_RESET).relative(right, -PATH_WIDTH / 2);
        BlockPos pathBottomRight = pathTopLeft.relative(trialForward, PATH_LENGTH - 1).relative(right, PATH_WIDTH - 1);
        BlockPos pedestalPos = markerFloorPos.relative(trialForward, PATH_START_DISTANCE_FROM_RESET + PATH_LENGTH - 1 + PEDESTAL_DISTANCE_AFTER_PATH).above();
        BlockPos rewardTeleportPos = mosaicCenter.relative(mosaicForward, 1).below(JungleMosaicTileBlock.HEIGHT / 2).above();

        BlockPos mosaicTopLeft = mosaicCenter.relative(right, -JungleMosaicTileBlock.WIDTH / 2).above(JungleMosaicTileBlock.HEIGHT / 2);
        BlockPos mosaicBottomRight = mosaicTopLeft.relative(right, JungleMosaicTileBlock.WIDTH - 1).below(JungleMosaicTileBlock.HEIGHT - 1);
        BlockPos boundsMin = paddedMin(3, 3, 3, mosaicTopLeft, mosaicBottomRight, markerFloorPos, pathTopLeft, pathBottomRight, pedestalPos, rewardTeleportPos);
        BlockPos boundsMax = paddedMax(3, 6, 3, mosaicTopLeft, mosaicBottomRight, markerFloorPos, pathTopLeft, pathBottomRight, pedestalPos, rewardTeleportPos);

        level.setBlock(markerFloorPos, Blocks.MOSSY_STONE_BRICKS.defaultBlockState(), 3);
        level.setBlock(masterControllerPos, HPBlocks.JUNGLE_TRIAL_MASTER_CONTROLLER.get().defaultBlockState(), 3);
        level.setBlock(pathControllerPos, HPBlocks.JUNGLE_PATH_CONTROLLER.get().defaultBlockState(), 3);

        if (level.getBlockEntity(pathControllerPos) instanceof JunglePathControllerBlockEntity pathController) {
            pathController.setMasterControllerPos(masterControllerPos);
        }

        if (level.getBlockEntity(masterControllerPos) instanceof JungleTrialMasterControllerBlockEntity masterController) {
            masterController.configureLinkedTrial(mosaicCenter, mosaicForward, pathControllerPos, pedestalPos, rewardTeleportPos, boundsMin, boundsMax);
            if (solvedPreview) {
                masterController.setMosaicSolved((net.minecraft.server.level.ServerLevel) level, mosaicCenter);
            }
        }

        this.placePath(level, markerFloorPos, trialForward, right);

        level.setBlock(pedestalPos, HPBlocks.JUNGLE_OFFERING_PEDESTAL.get().defaultBlockState()
                .setValue(com.overyourhead.hidden_places.common.block.JungleOfferingPedestalBlock.FACING, trialForward.getOpposite()), 3);
        if (level.getBlockEntity(pedestalPos) instanceof JungleOfferingPedestalBlockEntity pedestal) {
            pedestal.setTrialMode(false);
            pedestal.setProtectedMode(true);
            pedestal.setMasterControllerPos(masterControllerPos);
        }

        level.playSound(null, mosaicCenter, sound(SoundEvents.STONE_PLACE), SoundSource.BLOCKS, 0.95F, 0.9F);
        if (player != null) {
            player.displayClientMessage(Component.literal("Placed linked jungle trial behind the mosaic: reset -> gap -> path -> pedestal."), true);
        }

        return InteractionResult.CONSUME;
    }

    private void placeMosaic(Level level, BlockPos center, Direction facing, Direction right, boolean solvedPreview) {
        BlockPos topLeft = center.relative(right, -JungleMosaicTileBlock.WIDTH / 2).above(JungleMosaicTileBlock.HEIGHT / 2);

        for (int row = 0; row < JungleMosaicTileBlock.HEIGHT; row++) {
            for (int col = 0; col < JungleMosaicTileBlock.WIDTH; col++) {
                int piece = JungleMosaicTileBlock.pieceForGridPosition(row, col);
                int rotation = solvedPreview ? 0 : level.random.nextInt(4);
                BlockPos tilePos = topLeft.relative(right, col).below(row);
                BlockState tileState = HPBlocks.JUNGLE_MOSAIC_TILE.get().defaultBlockState()
                        .setValue(JungleMosaicTileBlock.FACING, facing)
                        .setValue(JungleMosaicTileBlock.PIECE, piece)
                        .setValue(JungleMosaicTileBlock.IMAGE_ROTATION, rotation)
                        .setValue(JungleMosaicTileBlock.SOLVED, false)
                        .setValue(JungleMosaicTileBlock.CRUMBLING, false);
                level.setBlock(tilePos, tileState, 3);
            }
        }
    }

    private void placePath(Level level, BlockPos markerFloorPos, Direction forward, Direction right) {
        BlockPos topLeft = markerFloorPos.relative(forward, PATH_START_DISTANCE_FROM_RESET).relative(right, -PATH_WIDTH / 2);

        for (int row = 0; row < PATH_LENGTH; row++) {
            for (int col = 0; col < PATH_WIDTH; col++) {
                int symbol = fallbackSymbol(row, col);
                int step = 0;

                for (int i = 0; i < PATH.length; i++) {
                    if (PATH[i][0] == row && PATH[i][1] == col) {
                        symbol = PATH[i][2];
                        step = i + 1;
                        break;
                    }
                }

                BlockPos tilePos = topLeft.relative(forward, row).relative(right, col);
                BlockState tileState = HPBlocks.JUNGLE_PATH_TILE.get().defaultBlockState()
                        .setValue(JunglePathTileBlock.SYMBOL, symbol)
                        .setValue(JunglePathTileBlock.STEP, step)
                        .setValue(JunglePathTileBlock.LIT, false);
                level.setBlock(tilePos, tileState, 3);
            }
        }
    }

    private static int fallbackSymbol(int row, int col) {
        return Math.floorMod(row * 2 + col * 3, 5);
    }

    private static BlockPos paddedMin(int padX, int padY, int padZ, BlockPos... positions) {
        int minX = positions[0].getX();
        int minY = positions[0].getY();
        int minZ = positions[0].getZ();
        for (BlockPos pos : positions) {
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
        }
        return new BlockPos(minX - padX, minY - padY, minZ - padZ);
    }

    private static BlockPos paddedMax(int padX, int padY, int padZ, BlockPos... positions) {
        int maxX = positions[0].getX();
        int maxY = positions[0].getY();
        int maxZ = positions[0].getZ();
        for (BlockPos pos : positions) {
            maxX = Math.max(maxX, pos.getX());
            maxY = Math.max(maxY, pos.getY());
            maxZ = Math.max(maxZ, pos.getZ());
        }
        return new BlockPos(maxX + padX, maxY + padY, maxZ + padZ);
    }

    private static SoundEvent sound(SoundEvent soundEvent) {
        return soundEvent;
    }

    private static SoundEvent sound(Holder<SoundEvent> soundEventHolder) {
        return soundEventHolder.value();
    }
}
