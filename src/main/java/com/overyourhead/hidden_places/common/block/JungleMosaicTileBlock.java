package com.overyourhead.hidden_places.common.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class JungleMosaicTileBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<JungleMosaicTileBlock> CODEC = simpleCodec(JungleMosaicTileBlock::new);

    public static final int WIDTH = 7;
    public static final int HEIGHT = 5;
    public static final int PIECE_COUNT = WIDTH * HEIGHT;

    public static final IntegerProperty PIECE = IntegerProperty.create("piece", 0, PIECE_COUNT - 1);
    public static final IntegerProperty IMAGE_ROTATION = IntegerProperty.create("image_rotation", 0, 3);
    public static final BooleanProperty SOLVED = BooleanProperty.create("solved");
    public static final BooleanProperty CRUMBLING = BooleanProperty.create("crumbling");

    public JungleMosaicTileBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(PIECE, 0)
                .setValue(IMAGE_ROTATION, 0)
                .setValue(SOLVED, false)
                .setValue(CRUMBLING, false));
    }

    public static Properties createProperties() {
        return Properties.of()
                .mapColor(MapColor.STONE)
                .strength(-1.0F, 3600000.0F)
                .sound(SoundType.STONE)
                .pushReaction(PushReaction.BLOCK)
                .requiresCorrectToolForDrops();
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(PIECE, 0)
                .setValue(IMAGE_ROTATION, 0)
                .setValue(SOLVED, false)
                .setValue(CRUMBLING, false);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (state.getValue(SOLVED)) {
            return InteractionResult.CONSUME;
        }

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        int nextRotation = (state.getValue(IMAGE_ROTATION) + 1) & 3;
        BlockState rotated = state.setValue(IMAGE_ROTATION, nextRotation);
        level.setBlock(pos, rotated, 3);
        level.playSound(null, pos, sound(SoundEvents.STONE_BUTTON_CLICK_ON), SoundSource.BLOCKS, 0.45F, 0.8F + level.random.nextFloat() * 0.25F);

        trySolve((ServerLevel) level, pos, rotated);
        return InteractionResult.CONSUME;
    }

    private void trySolve(ServerLevel level, BlockPos changedPos, BlockState changedState) {
        BlockPos topLeft = getTopLeft(changedPos, changedState);
        Direction facing = changedState.getValue(FACING);
        Direction right = facing.getClockWise();

        for (int row = 0; row < HEIGHT; row++) {
            for (int col = 0; col < WIDTH; col++) {
                int expectedPiece = pieceForGridPosition(row, col);
                BlockPos tilePos = topLeft.relative(right, col).below(row);
                BlockState tileState = level.getBlockState(tilePos);

                if (!tileState.is(this)
                        || tileState.getValue(FACING) != facing
                        || tileState.getValue(PIECE) != expectedPiece
                        || tileState.getValue(IMAGE_ROTATION) != 0
                        || tileState.getValue(SOLVED)) {
                    return;
                }
            }
        }

        startSolvedSequence(level, topLeft, facing);
    }

    private BlockPos getTopLeft(BlockPos pos, BlockState state) {
        int piece = state.getValue(PIECE);
        int col = gridColForPiece(piece);
        int row = gridRowForPiece(piece);
        Direction right = state.getValue(FACING).getClockWise();
        return pos.relative(right, -col).above(row);
    }

    public static int pieceForGridPosition(int row, int col) {
        return row * WIDTH + (WIDTH - 1 - col);
    }

    public static int gridColForPiece(int piece) {
        return WIDTH - 1 - (piece % WIDTH);
    }

    public static int gridRowForPiece(int piece) {
        return piece / WIDTH;
    }

    private void startSolvedSequence(ServerLevel level, BlockPos topLeft, Direction facing) {
        Direction right = facing.getClockWise();
        BlockPos center = topLeft.relative(right, WIDTH / 2).below(HEIGHT / 2);

        level.playSound(null, center, sound(SoundEvents.AMETHYST_BLOCK_CHIME), SoundSource.BLOCKS, 1.1F, 0.75F);
        level.playSound(null, center, sound(SoundEvents.GENERIC_EXPLODE), SoundSource.BLOCKS, 0.45F, 1.6F);
        spawnMosaicDust(level, center, 34, 0.45D, 0.35D, 0.45D, 0.03D);

        for (int row = 0; row < HEIGHT; row++) {
            for (int col = 0; col < WIDTH; col++) {
                BlockPos tilePos = topLeft.relative(right, col).below(row);
                BlockState tileState = level.getBlockState(tilePos);
                if (!tileState.is(this)) {
                    continue;
                }

                double dx = col - WIDTH / 2.0D;
                double dy = row - HEIGHT / 2.0D;
                int waveDelay = 4 + (int) Math.round(Math.sqrt(dx * dx + dy * dy) * 4.0D);
                level.setBlock(tilePos, tileState
                        .setValue(IMAGE_ROTATION, 0)
                        .setValue(SOLVED, true)
                        .setValue(CRUMBLING, false), 3);
                level.scheduleTick(tilePos, this, waveDelay);
            }
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.is(this) || !state.getValue(SOLVED)) {
            return;
        }

        if (!state.getValue(CRUMBLING)) {
            spawnShockwaveTileEffect(level, pos, random);
            level.setBlock(pos, state.setValue(CRUMBLING, true), 3);
            int row = gridRowForPiece(state.getValue(PIECE));
            level.scheduleTick(pos, this, 18 + row * 5);
            return;
        }

        spawnCrumbleTileEffect(level, pos, random);
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
    }

    private void spawnShockwaveTileEffect(ServerLevel level, BlockPos pos, RandomSource random) {
        level.playSound(null, pos, sound(SoundEvents.DEEPSLATE_BRICKS_PLACE), SoundSource.BLOCKS, 0.55F, 0.55F + random.nextFloat() * 0.25F);
        spawnMosaicDust(level, pos, 8, 0.22D, 0.18D, 0.22D, 0.015D);
    }

    private void spawnCrumbleTileEffect(ServerLevel level, BlockPos pos, RandomSource random) {
        level.playSound(null, pos, sound(SoundEvents.STONE_BREAK), SoundSource.BLOCKS, 0.75F, 0.75F + random.nextFloat() * 0.25F);
        level.levelEvent(2001, pos, Block.getId(Blocks.MOSSY_STONE_BRICKS.defaultBlockState()));
        spawnMosaicDust(level, pos, 18, 0.35D, 0.30D, 0.35D, 0.025D);
    }

    private void spawnMosaicDust(ServerLevel level, BlockPos pos, int count, double xSpread, double ySpread, double zSpread, double speed) {
        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 0.5D;
        double z = pos.getZ() + 0.5D;
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.MOSSY_STONE_BRICKS.defaultBlockState()),
                x, y, z, count, xSpread, ySpread, zSpread, speed);
        level.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, x, y, z, Math.max(2, count / 4), xSpread, ySpread, zSpread, speed * 0.5D);
    }


    private static SoundEvent sound(SoundEvent soundEvent) {
        return soundEvent;
    }

    private static SoundEvent sound(Holder<SoundEvent> soundEventHolder) {
        return soundEventHolder.value();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PIECE, IMAGE_ROTATION, SOLVED, CRUMBLING);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }
}
