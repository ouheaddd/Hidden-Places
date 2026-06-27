package com.overyourhead.hidden_places.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SleepingBagBlock extends BedBlock {
    public static final DirectionProperty FACING = BedBlock.FACING;
    public static final EnumProperty<BedPart> PART = BlockStateProperties.BED_PART;
    public static final BooleanProperty OCCUPIED = BlockStateProperties.OCCUPIED;

    private static final int REST_TICKS = 60;
    private static final long NIGHT_START = 13000L;
    private static final long NIGHT_END = 24000L;
    private static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 4.0D, 16.0D);
    private static final Map<RestKey, RestEntry> RESTING_PLAYERS = new HashMap<>();

    public SleepingBagBlock(Properties properties) {
        super(DyeColor.BROWN, properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(PART, BedPart.FOOT)
                .setValue(OCCUPIED, false));
    }

    public static BlockBehaviour.Properties createProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOD)
                .strength(0.8F)
                .sound(SoundType.WOOL)
                .noOcclusion();
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return null;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection();
        BlockPos footPos = context.getClickedPos();
        BlockPos headPos = footPos.relative(facing);
        Level level = context.getLevel();

        if (!level.getBlockState(headPos).canBeReplaced(context) || !level.getWorldBorder().isWithinBounds(headPos)) {
            return null;
        }

        return this.defaultBlockState()
                .setValue(FACING, facing)
                .setValue(PART, BedPart.FOOT)
                .setValue(OCCUPIED, false);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable net.minecraft.world.entity.LivingEntity placer, net.minecraft.world.item.ItemStack stack) {
        if (!level.isClientSide) {
            BlockPos headPos = pos.relative(state.getValue(FACING));
            level.setBlock(headPos, state.setValue(PART, BedPart.HEAD), Block.UPDATE_ALL);
            level.blockUpdated(pos, Blocks.AIR);
        }
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        Direction facing = state.getValue(FACING);
        BedPart part = state.getValue(PART);
        Direction otherPartDirection = part == BedPart.FOOT ? facing : facing.getOpposite();

        if (direction == otherPartDirection) {
            boolean validOtherPart = neighborState.is(this)
                    && neighborState.getValue(PART) != part
                    && neighborState.getValue(FACING) == facing;
            return validOtherPart ? state : Blocks.AIR.defaultBlockState();
        }

        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART, OCCUPIED);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (!(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.CONSUME;
        }

        BlockPos footPos = getFootPos(pos, state);
        BlockState footState = level.getBlockState(footPos);
        if (!footState.is(this)) {
            return InteractionResult.CONSUME;
        }

        if (footState.getValue(OCCUPIED)) {
            player.displayClientMessage(Component.translatable("message.hidden_places.sleeping_bag.occupied"), true);
            return InteractionResult.CONSUME;
        }

        long current = serverLevel.getDayTime() % 24000L;
        if (!isNight(current)) {
            player.displayClientMessage(Component.translatable("message.hidden_places.sleeping_bag.night_only"), true);
            return InteractionResult.CONSUME;
        }

        Direction facing = footState.getValue(FACING);
        BlockPos sleepPos = footPos.relative(facing);

        RestKey key = new RestKey(serverLevel.dimension(), footPos.immutable());
        RESTING_PLAYERS.put(key, new RestEntry(serverPlayer.getUUID(), serverLevel.getGameTime() + REST_TICKS));

        setOccupied(serverLevel, footPos, footState, true);

        // Night sleep only. Use the vanilla bed-style sleeping pose and bed direction.
        // Do not call BedBlock's normal interaction, so this remains a temporary sleep
        // and should not save the sleeping bag as a respawn point.
        serverPlayer.startSleeping(sleepPos);

        serverLevel.scheduleTick(footPos, this, 1);
        player.displayClientMessage(Component.translatable("message.hidden_places.sleeping_bag.sleeping_until_morning"), true);

        return InteractionResult.CONSUME;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.is(this) || state.getValue(PART) != BedPart.FOOT) {
            return;
        }

        RestKey key = new RestKey(level.dimension(), pos.immutable());
        RestEntry entry = RESTING_PLAYERS.get(key);
        if (entry == null) {
            setOccupied(level, pos, state, false);
            return;
        }

        Player foundPlayer = level.getPlayerByUUID(entry.playerId());
        if (!(foundPlayer instanceof ServerPlayer player) || player.isRemoved() || player.isShiftKeyDown()) {
            RESTING_PLAYERS.remove(key);
            setOccupied(level, pos, state, false);
            clearRestPose(foundPlayer);
            return;
        }

        if (level.getGameTime() < entry.finishTime()) {
            level.scheduleTick(pos, this, 1);
            return;
        }

        skipToNextRestTarget(level);
        RESTING_PLAYERS.remove(key);
        clearRestPose(player);
        setOccupied(level, pos, state, false);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel serverLevel) {
            BlockPos footPos = getFootPos(pos, state);
            RestEntry entry = RESTING_PLAYERS.remove(new RestKey(serverLevel.dimension(), footPos.immutable()));
            if (entry != null) {
                Player foundPlayer = serverLevel.getPlayerByUUID(entry.playerId());
                clearRestPose(foundPlayer);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    private static BlockPos getFootPos(BlockPos pos, BlockState state) {
        Direction facing = state.getValue(FACING);
        return state.getValue(PART) == BedPart.FOOT ? pos : pos.relative(facing.getOpposite());
    }

    private static void setOccupied(ServerLevel level, BlockPos footPos, BlockState footState, boolean occupied) {
        Direction facing = footState.getValue(FACING);
        BlockPos headPos = footPos.relative(facing);
        BlockState currentFoot = level.getBlockState(footPos);
        BlockState currentHead = level.getBlockState(headPos);

        if (currentFoot.is(footState.getBlock())) {
            level.setBlock(footPos, currentFoot.setValue(OCCUPIED, occupied), Block.UPDATE_ALL);
        }
        if (currentHead.is(footState.getBlock())) {
            level.setBlock(headPos, currentHead.setValue(OCCUPIED, occupied), Block.UPDATE_ALL);
        }
    }


    private static void clearRestPose(@Nullable Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.stopSleeping();
        }
    }

    private static void skipToNextRestTarget(ServerLevel level) {
        long dayTime = level.getDayTime();
        long current = dayTime % 24000L;
        long add = NIGHT_END - current;
        if (add <= 0L) {
            add += 24000L;
        }
        level.setDayTime(dayTime + add);
        level.setWeatherParameters(0, 0, false, false);
    }

    private static boolean isNight(long current) {
        return current >= 12542L && current < NIGHT_END;
    }

    private record RestKey(net.minecraft.resources.ResourceKey<Level> dimension, BlockPos footPos) {
    }

    private record RestEntry(UUID playerId, long finishTime) {
    }
}
