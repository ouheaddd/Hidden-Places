package com.overyourhead.hidden_places.common.item;

import com.overyourhead.hidden_places.common.block.JunglePathTileBlock;
import com.overyourhead.hidden_places.common.block.entity.JungleOfferingPedestalBlockEntity;
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

public class JunglePathTrialPlacerItem extends Item {
    private static final int WIDTH = 5;
    private static final int LENGTH = 10;

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

    public JunglePathTrialPlacerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        Direction forward = player != null ? player.getDirection() : context.getHorizontalDirection();
        Direction right = forward.getClockWise();
        BlockPos markerFloorPos = context.getClickedPos();
        BlockPos controllerPos = markerFloorPos.below();

        level.setBlock(controllerPos, HPBlocks.JUNGLE_PATH_CONTROLLER.get().defaultBlockState(), 3);

        BlockPos topLeft = markerFloorPos.relative(forward, 1).relative(right, -WIDTH / 2);
        for (int row = 0; row < LENGTH; row++) {
            for (int col = 0; col < WIDTH; col++) {
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

        BlockPos pedestalPos = markerFloorPos.relative(forward, LENGTH + 3).above();
        level.setBlock(pedestalPos, HPBlocks.JUNGLE_OFFERING_PEDESTAL.get().defaultBlockState(), 3);
        if (level.getBlockEntity(pedestalPos) instanceof JungleOfferingPedestalBlockEntity pedestal) {
            pedestal.setTrialMode(false);
            pedestal.setProtectedMode(true);
        }

        level.playSound(null, markerFloorPos, sound(SoundEvents.STONE_PLACE), SoundSource.BLOCKS, 0.85F, 0.85F);
        if (player != null) {
            player.displayClientMessage(Component.literal("Placed long jungle path trial. Sequence: leaf -> sun -> water -> snake -> flower -> leaf -> water -> sun -> snake -> flower."), true);
        }

        return InteractionResult.CONSUME;
    }

    private static int fallbackSymbol(int row, int col) {
        return Math.floorMod(row * 2 + col * 3, 5);
    }

    private static SoundEvent sound(SoundEvent soundEvent) {
        return soundEvent;
    }

    private static SoundEvent sound(Holder<SoundEvent> soundEventHolder) {
        return soundEventHolder.value();
    }
}
