package com.overyourhead.hidden_places.common.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class SunveilSpawnMarkerBlock extends Block {
    public static final MapCodec<SunveilSpawnMarkerBlock> CODEC = simpleCodec(SunveilSpawnMarkerBlock::new);

    public SunveilSpawnMarkerBlock(Properties properties) {
        super(properties);
    }

    public static Properties createProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.NONE)
                .strength(-1.0F, 3600000.0F)
                .sound(SoundType.STONE)
                .noCollission()
                .noOcclusion();
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }
}
