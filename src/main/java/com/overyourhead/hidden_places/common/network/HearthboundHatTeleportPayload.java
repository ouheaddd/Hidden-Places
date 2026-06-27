package com.overyourhead.hidden_places.common.network;

import com.overyourhead.hidden_places.HiddenPlacesMod;
import com.overyourhead.hidden_places.common.item.HearthboundHatItem;
import com.overyourhead.hidden_places.core.registry.HPItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public record HearthboundHatTeleportPayload() implements CustomPacketPayload {
    private static final Map<UUID, Long> COOLDOWNS = new HashMap<>();
    public static final Type<HearthboundHatTeleportPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(HiddenPlacesMod.MOD_ID, "hearthbound_hat_teleport")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, HearthboundHatTeleportPayload> STREAM_CODEC =
            StreamCodec.unit(new HearthboundHatTeleportPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(HearthboundHatTeleportPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                tryTeleportHome(player);
            }
        });
    }

    private static void tryTeleportHome(ServerPlayer player) {
        if (!player.getItemBySlot(EquipmentSlot.HEAD).is(HPItems.HEARTHBOUND_HAT.get())) {
            return;
        }

        long currentTick = player.server.overworld().getGameTime();
        long readyTick = COOLDOWNS.getOrDefault(player.getUUID(), 0L);
        if (currentTick < readyTick) {
            int secondsLeft = (int) Math.ceil((readyTick - currentTick) / 20.0D);
            player.displayClientMessage(Component.translatable("message.hidden_places.hearthbound_hat.cooldown", secondsLeft), true);
            player.level().playSound(null, player.blockPosition(), sound(SoundEvents.NOTE_BLOCK_CHIME), SoundSource.PLAYERS, 0.35F, 0.7F);
            return;
        }

        ServerLevel targetLevel = getTargetLevel(player);
        BlockPos targetBlockPos = getTargetBlockPos(player, targetLevel);
        Vec3 target = findSafeTarget(player, targetLevel, targetBlockPos);
        int cost = calculateLevelCost(player, targetLevel, target);

        if (!player.isCreative() && player.experienceLevel < cost) {
            player.displayClientMessage(Component.translatable("message.hidden_places.hearthbound_hat.not_enough_xp", cost), true);
            player.level().playSound(null, player.blockPosition(), sound(SoundEvents.NOTE_BLOCK_BASS), SoundSource.PLAYERS, 0.45F, 0.7F);
            return;
        }

        if (!player.isCreative()) {
            player.giveExperienceLevels(-cost);
        }

        COOLDOWNS.put(player.getUUID(), currentTick + HearthboundHatItem.COOLDOWN_TICKS);
        player.getCooldowns().addCooldown(HPItems.HEARTHBOUND_HAT.get(), HearthboundHatItem.COOLDOWN_TICKS);

        player.level().playSound(null, player.blockPosition(), sound(SoundEvents.RESPAWN_ANCHOR_DEPLETE), SoundSource.PLAYERS, 0.75F, 1.0F);
        player.teleportTo(targetLevel, target.x, target.y, target.z, Set.of(), player.getYRot(), player.getXRot());
        targetLevel.playSound(null, BlockPos.containing(target), sound(SoundEvents.ENDERMAN_TELEPORT), SoundSource.PLAYERS, 0.85F, 1.1F);
        player.displayClientMessage(Component.translatable("message.hidden_places.hearthbound_hat.teleported", cost), true);
    }

    private static ServerLevel getTargetLevel(ServerPlayer player) {
        ResourceKey<Level> respawnDimension = player.getRespawnDimension();
        ServerLevel targetLevel = player.server.getLevel(respawnDimension);
        return targetLevel != null ? targetLevel : player.server.overworld();
    }

    private static BlockPos getTargetBlockPos(ServerPlayer player, ServerLevel targetLevel) {
        BlockPos respawnPosition = player.getRespawnPosition();
        if (respawnPosition != null) {
            return respawnPosition;
        }

        return targetLevel.getSharedSpawnPos();
    }

    private static Vec3 findSafeTarget(ServerPlayer player, ServerLevel targetLevel, BlockPos targetBlockPos) {
        BlockPos respawnPosition = player.getRespawnPosition();
        if (respawnPosition != null) {
            Vec3 safeRespawn = findNearbySafePosition(targetLevel, respawnPosition);
            if (safeRespawn != null) {
                return safeRespawn;
            }
        }

        Vec3 safeSpawn = findNearbySafePosition(targetLevel, targetBlockPos);
        if (safeSpawn != null) {
            return safeSpawn;
        }

        return Vec3.atBottomCenterOf(targetBlockPos.above());
    }

    private static Vec3 findNearbySafePosition(ServerLevel level, BlockPos origin) {
        for (int yOffset = -1; yOffset <= 2; yOffset++) {
            for (int radius = 0; radius <= 3; radius++) {
                for (int xOffset = -radius; xOffset <= radius; xOffset++) {
                    for (int zOffset = -radius; zOffset <= radius; zOffset++) {
                        if (Math.abs(xOffset) != radius && Math.abs(zOffset) != radius) {
                            continue;
                        }

                        BlockPos candidate = origin.offset(xOffset, yOffset, zOffset);
                        if (isSafeStandPosition(level, candidate)) {
                            return Vec3.atBottomCenterOf(candidate);
                        }
                    }
                }
            }
        }

        return null;
    }

    private static boolean isSafeStandPosition(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).isAir()
                && level.getBlockState(pos.above()).isAir()
                && !level.getBlockState(pos.below()).isAir();
    }

    private static int calculateLevelCost(ServerPlayer player, ServerLevel targetLevel, Vec3 target) {
        if (player.level().dimension() != Level.OVERWORLD || targetLevel.dimension() != player.level().dimension()) {
            return HearthboundHatItem.CROSS_DIMENSION_COST_LEVELS;
        }

        double distance = player.position().distanceTo(target);
        int cost = Math.max(1, (int) Math.ceil(distance / HearthboundHatItem.BLOCKS_PER_LEVEL));
        return Math.min(HearthboundHatItem.MAX_COST_LEVELS, cost);
    }

    private static SoundEvent sound(SoundEvent soundEvent) {
        return soundEvent;
    }

    private static SoundEvent sound(Holder<SoundEvent> soundEventHolder) {
        return soundEventHolder.value();
    }
}
