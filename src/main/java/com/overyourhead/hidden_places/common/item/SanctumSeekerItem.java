package com.overyourhead.hidden_places.common.item;

import com.overyourhead.hidden_places.HiddenPlacesMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.EyeOfEnder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.structure.Structure;

public class SanctumSeekerItem extends Item {
    private static final TagKey<Structure> SEALED_SANCTUM_STRUCTURES = TagKey.create(
            Registries.STRUCTURE,
            ResourceLocation.fromNamespaceAndPath(HiddenPlacesMod.MOD_ID, "on_sealed_sanctum_maps")
    );

    /**
     * Radius is in chunks, matching the Sealed Sanctum Map test item.
     */
    private static final int SEARCH_RADIUS = 512;

    public SanctumSeekerItem(Properties properties) {
        super(properties.stacksTo(16));
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
                SEALED_SANCTUM_STRUCTURES,
                player.blockPosition(),
                SEARCH_RADIUS,
                false
        );

        if (targetPos == null) {
            player.displayClientMessage(Component.translatable("message.hidden_places.sanctum_seeker.not_found"), true);
            return InteractionResultHolder.fail(stack);
        }

        EyeOfEnder eye = new EyeOfEnder(level, player.getX(), player.getY(0.5D), player.getZ());
        eye.setItem(stack);
        eye.signalTo(targetPos);
        level.gameEvent(GameEvent.PROJECTILE_SHOOT, eye.position(), GameEvent.Context.of(player));
        level.addFreshEntity(eye);

        level.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.ENDER_EYE_LAUNCH,
                SoundSource.NEUTRAL,
                0.5F,
                0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F)
        );

        player.awardStat(Stats.ITEM_USED.get(this));

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        return InteractionResultHolder.success(stack);
    }
}
