package com.overyourhead.hidden_places.common.block;

import com.mojang.serialization.MapCodec;
import com.overyourhead.hidden_places.common.block.entity.JunglePathControllerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;

public class JunglePathTileBlock extends Block {
    public static final MapCodec<JunglePathTileBlock> CODEC = simpleCodec(JunglePathTileBlock::new);

    public static final int SYMBOL_LEAF = 0;
    public static final int SYMBOL_SUN = 1;
    public static final int SYMBOL_WATER = 2;
    public static final int SYMBOL_SNAKE = 3;
    public static final int SYMBOL_FLOWER = 4;

    public static final IntegerProperty SYMBOL = IntegerProperty.create("symbol", 0, 4);
    public static final IntegerProperty STEP = IntegerProperty.create("step", 0, JunglePathControllerBlockEntity.TOTAL_STEPS);
    public static final BooleanProperty LIT = BooleanProperty.create("lit");

    public JunglePathTileBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(SYMBOL, SYMBOL_LEAF)
                .setValue(STEP, 0)
                .setValue(LIT, false));
    }

    public static Properties createProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE)
                .strength(2.5F, 10.0F)
                .sound(SoundType.STONE);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SYMBOL, STEP, LIT);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        super.stepOn(level, pos, state, entity);

        if (!(level instanceof ServerLevel serverLevel) || !(entity instanceof ServerPlayer player)) {
            return;
        }

        if (state.getValue(LIT)) {
            return;
        }

        JunglePathControllerBlockEntity.findNearest(serverLevel, pos)
                .ifPresent(controller -> controller.handleTileStep(player, pos, state));
    }
}
