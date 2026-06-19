package com.overyourhead.hidden_places.common.item;

import com.overyourhead.hidden_places.HiddenPlacesMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

public class SealedSanctumMapItem extends Item {
    private static final TagKey<Structure> SEALED_SANCTUM_MAP_STRUCTURES = TagKey.create(
            Registries.STRUCTURE,
            ResourceLocation.fromNamespaceAndPath(HiddenPlacesMod.MOD_ID, "on_sealed_sanctum_maps")
    );

    /**
     * Radius is in chunks. Keep it high because sealed_sanctum is intended to be rare.
     */
    private static final int SEARCH_RADIUS = 512;
    private static final byte MAP_SCALE = 2;

    public SealedSanctumMapItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }

        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResultHolder.pass(stack);
        }

        BlockPos targetPos = serverLevel.findNearestMapStructure(
                SEALED_SANCTUM_MAP_STRUCTURES,
                player.blockPosition(),
                SEARCH_RADIUS,
                false
        );

        if (targetPos == null) {
            player.displayClientMessage(Component.translatable("message.hidden_places.sealed_sanctum_map.not_found"), true);
            return InteractionResultHolder.fail(stack);
        }

        ItemStack mapStack = MapItem.create(serverLevel, targetPos.getX(), targetPos.getZ(), MAP_SCALE, true, true);
        MapItem.renderBiomePreviewMap(serverLevel, mapStack);
        MapItemSavedData.addTargetDecoration(mapStack, targetPos, "+", MapDecorationTypes.TARGET_POINT);
        mapStack.set(DataComponents.CUSTOM_NAME, Component.translatable("filled_map.hidden_places.sealed_sanctum"));

        if (player.getAbilities().instabuild) {
            if (!player.getInventory().add(mapStack)) {
                player.drop(mapStack, false);
            }
            return InteractionResultHolder.success(stack);
        }

        stack.shrink(1);

        if (stack.isEmpty()) {
            return InteractionResultHolder.success(mapStack);
        }

        if (!player.getInventory().add(mapStack)) {
            player.drop(mapStack, false);
        }

        return InteractionResultHolder.success(stack);
    }
}
