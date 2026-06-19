package com.overyourhead.hidden_places.common.event;

import com.overyourhead.hidden_places.HiddenPlacesMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public final class SealedSanctumProtectionEvents {
    private static final TagKey<Structure> SEALED_SANCTUM_STRUCTURES = TagKey.create(
            Registries.STRUCTURE,
            ResourceLocation.fromNamespaceAndPath(HiddenPlacesMod.MOD_ID, "on_sealed_sanctum_maps")
    );

    private static final ResourceLocation REQUIRED_ADVANCEMENT = ResourceLocation.fromNamespaceAndPath(
            HiddenPlacesMod.MOD_ID,
            "hidden_places/crafted_sealed_sanctum_map"
    );

    /**
     * Placeholder radius in blocks. Increase to 128+ when the final structure becomes larger.
     */
    private static final int PROTECTION_RADIUS_BLOCKS = 48;
    private static final int PUSH_OUT_MARGIN_BLOCKS = 6;
    private static final int CHECK_INTERVAL_TICKS = 10;
    private static final int SEARCH_RADIUS_CHUNKS = Math.max(8, (PROTECTION_RADIUS_BLOCKS / 16) + 8);
    private static final int SHIELD_PARTICLE_COLUMNS = 9;
    private static final int SHIELD_PARTICLE_ROWS = 4;

    private SealedSanctumProtectionEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (player.tickCount % CHECK_INTERVAL_TICKS != 0) {
            return;
        }

        if (player.isCreative() || player.isSpectator()) {
            return;
        }

        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        if (hasSealedSanctumAccess(player)) {
            return;
        }

        BlockPos playerPos = player.blockPosition();
        StructureStart currentStructure = serverLevel.structureManager().getStructureWithPieceAt(playerPos, SEALED_SANCTUM_STRUCTURES);

        if (currentStructure != StructureStart.INVALID_START && currentStructure.isValid()) {
            ejectFromStructure(serverLevel, player, currentStructure.getBoundingBox());
            showRejectMessage(player);
            return;
        }

        BlockPos nearestSanctum = serverLevel.findNearestMapStructure(
                SEALED_SANCTUM_STRUCTURES,
                playerPos,
                SEARCH_RADIUS_CHUNKS,
                false
        );

        if (nearestSanctum == null) {
            return;
        }

        pushAwayFromSanctum(serverLevel, player, nearestSanctum);
    }

    private static boolean hasSealedSanctumAccess(ServerPlayer player) {
        var advancement = player.server.getAdvancements().get(REQUIRED_ADVANCEMENT);
        return advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone();
    }

    private static void pushAwayFromSanctum(ServerLevel serverLevel, ServerPlayer player, BlockPos sanctumPos) {
        double centerX = sanctumPos.getX() + 0.5D;
        double centerZ = sanctumPos.getZ() + 0.5D;
        double dx = player.getX() - centerX;
        double dz = player.getZ() - centerZ;
        double distanceSqr = dx * dx + dz * dz;
        double radius = PROTECTION_RADIUS_BLOCKS;

        if (distanceSqr > radius * radius) {
            return;
        }

        double distance = Math.sqrt(distanceSqr);
        if (distance < 0.001D) {
            Vec3 look = player.getLookAngle();
            dx = -look.x;
            dz = -look.z;
            distance = Math.sqrt(dx * dx + dz * dz);

            if (distance < 0.001D) {
                dx = 1.0D;
                dz = 0.0D;
                distance = 1.0D;
            }
        }

        double normalX = dx / distance;
        double normalZ = dz / distance;
        double proximity = 1.0D - Math.min(1.0D, distance / radius);
        double strength = 0.22D + proximity * 0.65D;

        player.push(normalX * strength, 0.08D, normalZ * strength);
        player.hurtMarked = true;
        spawnShieldImpactParticles(serverLevel, player, sanctumPos, normalX, normalZ);
        showRejectMessage(player);
    }

    private static void ejectFromStructure(ServerLevel serverLevel, ServerPlayer player, BoundingBox box) {
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();

        double westDistance = Math.abs(x - box.minX());
        double eastDistance = Math.abs(box.maxX() - x);
        double northDistance = Math.abs(z - box.minZ());
        double southDistance = Math.abs(box.maxZ() - z);

        double targetX = x;
        double targetZ = z;
        double bestDistance = westDistance;
        targetX = box.minX() - PUSH_OUT_MARGIN_BLOCKS + 0.5D;

        if (eastDistance < bestDistance) {
            bestDistance = eastDistance;
            targetX = box.maxX() + PUSH_OUT_MARGIN_BLOCKS + 0.5D;
            targetZ = z;
        }

        if (northDistance < bestDistance) {
            bestDistance = northDistance;
            targetX = x;
            targetZ = box.minZ() - PUSH_OUT_MARGIN_BLOCKS + 0.5D;
        }

        if (southDistance < bestDistance) {
            targetX = x;
            targetZ = box.maxZ() + PUSH_OUT_MARGIN_BLOCKS + 0.5D;
        }

        player.teleportTo(targetX, y + 0.1D, targetZ);
        player.setDeltaMovement(Vec3.ZERO);
        player.hurtMarked = true;
        spawnEjectParticles(serverLevel, targetX, y + 1.0D, targetZ);
    }

    private static void spawnShieldImpactParticles(ServerLevel level, ServerPlayer player, BlockPos sanctumPos, double normalX, double normalZ) {
        double centerX = sanctumPos.getX() + 0.5D;
        double centerZ = sanctumPos.getZ() + 0.5D;
        double impactX = centerX + normalX * PROTECTION_RADIUS_BLOCKS;
        double impactZ = centerZ + normalZ * PROTECTION_RADIUS_BLOCKS;
        double baseY = player.getY() + 1.0D;

        double tangentX = -normalZ;
        double tangentZ = normalX;
        double halfColumns = (SHIELD_PARTICLE_COLUMNS - 1) / 2.0D;

        for (int row = 0; row < SHIELD_PARTICLE_ROWS; row++) {
            double yOffset = (row - (SHIELD_PARTICLE_ROWS - 1) / 2.0D) * 0.42D;

            for (int column = 0; column < SHIELD_PARTICLE_COLUMNS; column++) {
                double sideOffset = (column - halfColumns) * 0.34D;
                double ripple = Math.sin((player.tickCount + column * 5 + row * 9) * 0.22D) * 0.12D;
                double x = impactX + tangentX * sideOffset + normalX * ripple;
                double y = baseY + yOffset;
                double z = impactZ + tangentZ * sideOffset + normalZ * ripple;

                level.sendParticles(ParticleTypes.END_ROD, x, y, z, 1, 0.015D, 0.015D, 0.015D, 0.0D);
            }
        }

        level.sendParticles(ParticleTypes.ENCHANT, impactX, baseY, impactZ, 18, 0.65D, 0.8D, 0.65D, 0.04D);
    }

    private static void spawnEjectParticles(ServerLevel level, double x, double y, double z) {
        level.sendParticles(ParticleTypes.END_ROD, x, y, z, 28, 0.75D, 0.9D, 0.75D, 0.02D);
        level.sendParticles(ParticleTypes.ENCHANT, x, y, z, 32, 1.0D, 1.0D, 1.0D, 0.06D);
    }

    private static void showRejectMessage(ServerPlayer player) {
        if (player.tickCount % 20 == 0) {
            player.displayClientMessage(Component.translatable("message.hidden_places.sealed_sanctum.rejected"), true);
        }
    }
}
