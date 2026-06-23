package com.overyourhead.hidden_places.common.item;

import com.overyourhead.hidden_places.HiddenPlacesMod;
import com.overyourhead.hidden_places.common.block.MossgateChestBlock;
import com.overyourhead.hidden_places.common.block.entity.SunveilTrialControllerBlockEntity;
import com.overyourhead.hidden_places.common.entity.SunveilWayfinderEntity;
import com.overyourhead.hidden_places.core.registry.HPBlocks;
import com.overyourhead.hidden_places.core.registry.HPEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SunveilTrialRoomPlacerItem extends Item {
    private static final ResourceLocation STRUCTURE_ID = ResourceLocation.fromNamespaceAndPath(HiddenPlacesMod.MOD_ID, "sunveil_trial_room");
    private static final int SCAN_RADIUS = 36;

    public SunveilTrialRoomPlacerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }

        Optional<StructureTemplate> templateOptional = serverLevel.getStructureManager().get(STRUCTURE_ID);
        if (templateOptional.isEmpty()) {
            if (context.getPlayer() != null) {
                context.getPlayer().displayClientMessage(Component.literal("Missing Hidden Places structure: sunveil_trial_room"), true);
            }
            return InteractionResult.CONSUME;
        }

        StructureTemplate template = templateOptional.get();
        BlockPos origin = context.getClickedPos().above();
        Rotation rotation = rotationForPlayer(context);
        StructurePlaceSettings settings = new StructurePlaceSettings()
                .setMirror(Mirror.NONE)
                .setRotation(rotation)
                .setIgnoreEntities(false);

        template.placeInWorld(serverLevel, origin, origin, settings, serverLevel.random, Block.UPDATE_ALL);
        configurePlacedTrial(serverLevel, origin, template.getSize());

        if (context.getPlayer() != null) {
            context.getPlayer().displayClientMessage(Component.literal("Sunveil trial room placed."), true);
        }
        return InteractionResult.CONSUME;
    }

    private static Rotation rotationForPlayer(UseOnContext context) {
        if (context.getPlayer() == null) {
            return Rotation.NONE;
        }
        return switch (context.getPlayer().getDirection()) {
            case EAST -> Rotation.CLOCKWISE_90;
            case SOUTH -> Rotation.CLOCKWISE_180;
            case WEST -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    private static void configurePlacedTrial(ServerLevel level, BlockPos origin, Vec3i size) {
        BlockPos scanMin = origin.offset(-SCAN_RADIUS, -8, -SCAN_RADIUS);
        BlockPos scanMax = origin.offset(size.getX() + SCAN_RADIUS, size.getY() + 8, size.getZ() + SCAN_RADIUS);

        BlockPos controllerPos = null;
        BlockPos npcPos = null;
        BlockPos chestPos = null;
        List<BlockPos> spawnMarkers = new ArrayList<>();

        for (BlockPos checkPos : BlockPos.betweenClosed(scanMin, scanMax)) {
            BlockState state = level.getBlockState(checkPos);
            if (state.is(Blocks.EMERALD_BLOCK)) {
                controllerPos = checkPos.immutable();
            } else if (state.is(Blocks.GOLD_BLOCK)) {
                npcPos = checkPos.immutable();
            } else if (state.is(Blocks.IRON_BLOCK)) {
                chestPos = checkPos.immutable();
            } else if (state.is(Blocks.RED_WOOL) || state.is(Blocks.BLUE_WOOL) || state.is(Blocks.GREEN_WOOL) || state.is(Blocks.YELLOW_WOOL)) {
                spawnMarkers.add(checkPos.immutable());
            }
        }

        if (controllerPos == null || npcPos == null || chestPos == null || spawnMarkers.isEmpty()) {
            return;
        }

        BlockPos actualControllerPos = controllerPos.below();
        level.setBlock(controllerPos, Blocks.SANDSTONE.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(actualControllerPos, HPBlocks.SUNVEIL_TRIAL_CONTROLLER.get().defaultBlockState(), Block.UPDATE_ALL);

        List<BlockPos> storedSpawnMarkers = new ArrayList<>();
        for (BlockPos spawnMarker : spawnMarkers) {
            BlockPos hiddenMarkerPos = spawnMarker.below();
            level.setBlock(spawnMarker, Blocks.SANDSTONE.defaultBlockState(), Block.UPDATE_ALL);
            level.setBlock(hiddenMarkerPos, HPBlocks.SUNVEIL_SPAWN_MARKER.get().defaultBlockState(), Block.UPDATE_ALL);
            storedSpawnMarkers.add(spawnMarker.immutable());
        }

        level.setBlock(chestPos, HPBlocks.SUNVEIL_CHEST.get().defaultBlockState()
                .setValue(MossgateChestBlock.FACING, net.minecraft.core.Direction.NORTH), Block.UPDATE_ALL);

        level.setBlock(npcPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        SunveilWayfinderEntity wayfinder = HPEntities.SUNVEIL_WAYFINDER.get().create(level);
        if (wayfinder != null) {
            wayfinder.moveTo(Vec3.atCenterOf(npcPos).x, npcPos.getY(), Vec3.atCenterOf(npcPos).z, 0.0F, 0.0F);
            wayfinder.setPersistenceRequired();
            level.addFreshEntity(wayfinder);
        }

        BlockEntity blockEntity = level.getBlockEntity(actualControllerPos);
        if (blockEntity instanceof SunveilTrialControllerBlockEntity controller) {
            controller.configure(
                    npcPos,
                    chestPos,
                    storedSpawnMarkers,
                    scanMin.below(2),
                    scanMax
            );
        }
    }
}
