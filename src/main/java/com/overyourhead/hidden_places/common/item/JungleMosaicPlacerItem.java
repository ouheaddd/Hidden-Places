package com.overyourhead.hidden_places.common.item;

import com.overyourhead.hidden_places.common.block.JungleMosaicTileBlock;
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
import net.minecraft.world.level.block.state.BlockState;

public class JungleMosaicPlacerItem extends Item {
    public JungleMosaicPlacerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Direction facing = context.getClickedFace();
        Player player = context.getPlayer();

        if (facing.getAxis().isVertical()) {
            if (player != null && !level.isClientSide) {
                player.displayClientMessage(Component.literal("Use this on a vertical wall face."), true);
            }
            return InteractionResult.FAIL;
        }

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        BlockPos center = context.getClickedPos().relative(facing);
        Direction right = facing.getClockWise();
        BlockPos topLeft = center.relative(right, -JungleMosaicTileBlock.WIDTH / 2).above(JungleMosaicTileBlock.HEIGHT / 2);
        boolean solvedPreview = player != null && player.isShiftKeyDown();

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

        level.playSound(null, center, sound(SoundEvents.STONE_PLACE), SoundSource.BLOCKS, 0.8F, solvedPreview ? 1.2F : 0.9F);
        if (player != null) {
            String mode = solvedPreview ? "solved preview" : "randomized puzzle";
            player.displayClientMessage(Component.literal("Placed 7x5 jungle mosaic (" + mode + ")."), true);
        }
        return InteractionResult.CONSUME;
    }

    private static SoundEvent sound(SoundEvent soundEvent) {
        return soundEvent;
    }

    private static SoundEvent sound(Holder<SoundEvent> soundEventHolder) {
        return soundEventHolder.value();
    }
}
