package com.overyourhead.hidden_places.common.block;

import com.mojang.serialization.MapCodec;
import com.overyourhead.hidden_places.common.block.entity.JungleOfferingPedestalBlockEntity;
import com.overyourhead.hidden_places.core.registry.HPBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class JungleOfferingPedestalBlock extends BaseEntityBlock {
    public static final MapCodec<JungleOfferingPedestalBlock> CODEC = simpleCodec(JungleOfferingPedestalBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    private static final VoxelShape SHAPE = Block.box(1.0D, 0.0D, 1.0D, 15.0D, 18.0D, 15.0D);

    public JungleOfferingPedestalBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public static Properties createProperties() {
        return Properties.of()
                .mapColor(MapColor.STONE)
                .strength(3.0F, 12.0F)
                .sound(SoundType.STONE)
                .noOcclusion();
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new JungleOfferingPedestalBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide
                ? null
                : createTickerHelper(type, HPBlockEntities.JUNGLE_OFFERING_PEDESTAL.get(), JungleOfferingPedestalBlockEntity::serverTick);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hitResult) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof JungleOfferingPedestalBlockEntity pedestal) || stack.isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }

        if (!pedestal.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.hidden_places.jungle_offering_pedestal.occupied"), true);
            return ItemInteractionResult.CONSUME;
        }

        ItemStack placed = stack.copy();
        placed.setCount(1);
        pedestal.setDisplayedItem(placed);

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        return ItemInteractionResult.CONSUME;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof JungleOfferingPedestalBlockEntity pedestal)) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        ItemStack removed = pedestal.removeDisplayedItem();
        if (!removed.isEmpty()) {
            if (!player.addItem(removed)) {
                player.drop(removed, false);
            }
            return InteractionResult.CONSUME;
        }

        if (player.isCreative() && player.isShiftKeyDown()) {
            pedestal.toggleTrialMode();
            player.displayClientMessage(Component.literal("Jungle offering pedestal trial mode: " + pedestal.isTrialMode()), true);
            return InteractionResult.CONSUME;
        }

        return InteractionResult.CONSUME;
    }

    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!player.isCreative() && blockEntity instanceof JungleOfferingPedestalBlockEntity pedestal && pedestal.isProtectedMode()) {
            return 0.0F;
        }
        return super.getDestroyProgress(state, player, level, pos);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof JungleOfferingPedestalBlockEntity pedestal) {
                pedestal.dropStoredItem();
            }
        }

        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
