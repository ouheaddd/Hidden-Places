package com.overyourhead.hidden_places.common.block.entity;

import com.overyourhead.hidden_places.common.entity.SunveilWayfinderEntity;
import com.overyourhead.hidden_places.core.registry.HPBlockEntities;
import com.overyourhead.hidden_places.core.registry.HPItems;
import com.overyourhead.hidden_places.core.registry.HPSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class SunveilTrialControllerBlockEntity extends BlockEntity {
    private static final int SEARCH_RADIUS = 48;
    private static final int START_DELAY_TICKS = 22;
    private static final int END_DELAY_TICKS = 24;
    private static final int FAILURE_DISTANCE = 34;
    private static final int MAX_ACTIVE_MOBS = 10;
    private static final int SPAWN_COOLDOWN_TICKS = 22;
    private static final int FLUTE_REFRESH_TICKS = 1800;
    private static final int[] WAVE_COUNTS = {4, 7, 7, 9, 3};
    private static final int TOTAL_REQUIRED_KILLS = 30;
    private static final int MINING_FATIGUE_AMPLIFIER = 2; // Fatigue III
    private static final int DOOR_HORIZONTAL_DISTANCE = 5;
    private static final int[] DOOR_Y_OFFSETS = {4, 5};

    private final ServerBossEvent bossEvent = new ServerBossEvent(
            Component.translatable("bossbar.hidden_places.sunveil_trial"),
            BossEvent.BossBarColor.YELLOW,
            BossEvent.BossBarOverlay.PROGRESS
    );

    private final List<BlockPos> spawnMarkerPositions = new ArrayList<>();
    private final List<UUID> activeMobIds = new ArrayList<>();
    private final List<BlockPos> gatePositions = new ArrayList<>();

    private boolean running;
    private boolean finishing;
    private int startDelayTicks;
    private int finishDelayTicks;
    private int waveIndex;
    private int spawnedInWave;
    private int killsDone;
    private int spawnCooldown;
    private int fluteRefreshTicks;
    private int spawnSequenceIndex;

    @Nullable
    private UUID starterPlayerId;
    @Nullable
    private BlockPos npcPos;
    @Nullable
    private BlockPos rewardChestPos;
    @Nullable
    private BlockPos boundsMin;
    @Nullable
    private BlockPos boundsMax;

    public SunveilTrialControllerBlockEntity(BlockPos pos, BlockState blockState) {
        super(HPBlockEntities.SUNVEIL_TRIAL_CONTROLLER.get(), pos, blockState);
        this.bossEvent.setVisible(false);
        this.bossEvent.setProgress(0.0F);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SunveilTrialControllerBlockEntity controller) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        controller.tickBossbarPlayers(serverLevel);

        if (controller.finishing) {
            controller.finishDelayTicks--;
            if (controller.finishDelayTicks <= 0) {
                controller.finishSuccess(serverLevel);
            }
            return;
        }

        if (!controller.running) {
            return;
        }

        controller.applyTrialEffects(serverLevel);

        if (controller.startDelayTicks > 0) {
            controller.startDelayTicks--;
            controller.playFluteLoopIfNeeded(serverLevel);
            return;
        }

        if (controller.shouldFail(serverLevel)) {
            controller.failTrial(serverLevel);
            return;
        }

        controller.playFluteLoopIfNeeded(serverLevel);
        controller.pruneAndCountDefeatedMobs(serverLevel);
        controller.spawnNextMobIfReady(serverLevel);
        controller.updateBossbar();

        if (controller.killsDone >= TOTAL_REQUIRED_KILLS && controller.activeMobIds.isEmpty()) {
            controller.startSuccess(serverLevel);
        }
    }

    public static Optional<SunveilTrialControllerBlockEntity> findNearest(ServerLevel level, BlockPos origin) {
        SunveilTrialControllerBlockEntity nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (BlockPos checkPos : BlockPos.betweenClosed(
                origin.offset(-SEARCH_RADIUS, -10, -SEARCH_RADIUS),
                origin.offset(SEARCH_RADIUS, 12, SEARCH_RADIUS))) {
            BlockEntity blockEntity = level.getBlockEntity(checkPos);
            if (blockEntity instanceof SunveilTrialControllerBlockEntity controller) {
                double distance = checkPos.distSqr(origin);
                if (distance < nearestDistance) {
                    nearest = controller;
                    nearestDistance = distance;
                }
            }
        }

        return Optional.ofNullable(nearest);
    }

    public void configure(BlockPos npcPos, BlockPos rewardChestPos, List<BlockPos> spawnMarkerPositions,
                          BlockPos boundsMin, BlockPos boundsMax) {
        this.npcPos = npcPos.immutable();
        this.rewardChestPos = rewardChestPos.immutable();
        this.spawnMarkerPositions.clear();
        for (BlockPos spawnMarkerPosition : spawnMarkerPositions) {
            this.spawnMarkerPositions.add(spawnMarkerPosition.immutable());
        }
        this.boundsMin = boundsMin.immutable();
        this.boundsMax = boundsMax.immutable();
        this.rebuildGatePositions();
        this.setChangedAndSync();
    }

    public boolean isRunning() {
        return this.running || this.finishing;
    }

    public boolean startTrial(ServerLevel level, ServerPlayer starter, SunveilWayfinderEntity wayfinder) {
        if (this.isRunning() || this.spawnMarkerPositions.isEmpty()) {
            return false;
        }

        this.stopFluteLoopForPlayers(level);
        this.running = true;
        this.finishing = false;
        this.startDelayTicks = START_DELAY_TICKS;
        this.finishDelayTicks = 0;
        this.waveIndex = 0;
        this.spawnedInWave = 0;
        this.killsDone = 0;
        this.spawnCooldown = 0;
        this.fluteRefreshTicks = 0;
        this.spawnSequenceIndex = level.random.nextInt(Math.max(1, this.spawnMarkerPositions.size()));
        this.starterPlayerId = starter.getUUID();
        this.activeMobIds.clear();
        this.bossEvent.setVisible(true);
        this.updateBossbar();
        this.tickBossbarPlayers(level);
        this.closeEntrance(level);
        wayfinder.startFlutePerformance();
        this.playFluteLoop(level);
        level.playSound(null, wayfinder.blockPosition(), sound(SoundEvents.NOTE_BLOCK_FLUTE), SoundSource.NEUTRAL, 1.0F, 0.75F);
        this.setChangedAndSync();
        return true;
    }

    private void spawnNextMobIfReady(ServerLevel level) {
        if (this.waveIndex >= WAVE_COUNTS.length || this.activeMobIds.size() >= MAX_ACTIVE_MOBS) {
            return;
        }

        if (this.spawnCooldown > 0) {
            this.spawnCooldown--;
            return;
        }

        int waveSize = WAVE_COUNTS[this.waveIndex];
        if (this.spawnedInWave >= waveSize) {
            if (this.activeMobIds.isEmpty()) {
                this.waveIndex++;
                this.spawnedInWave = 0;
                this.spawnCooldown = 34;
                level.playSound(null, this.worldPosition, sound(SoundEvents.NOTE_BLOCK_CHIME), SoundSource.BLOCKS, 0.85F, 0.85F + this.waveIndex * 0.08F);
            }
            return;
        }

        BlockPos markerPos = this.pickNextSpawnMarker();
        EntityType<? extends Mob> entityType = this.entityTypeForWave(this.waveIndex, this.spawnedInWave);
        Mob mob = entityType.create(level);
        if (mob == null) {
            this.spawnedInWave++;
            return;
        }

        double inwardX = inwardOffset(markerPos.getX(), this.worldPosition.getX());
        double inwardZ = inwardOffset(markerPos.getZ(), this.worldPosition.getZ());
        double spawnX = markerPos.getX() + 0.5D + inwardX;
        double spawnY = markerPos.getY() + 2.05D;
        double spawnZ = markerPos.getZ() + 0.5D + inwardZ;
        mob.moveTo(spawnX, spawnY, spawnZ, level.random.nextFloat() * 360.0F, 0.0F);
        mob.setPersistenceRequired();
        mob.getPersistentData().putBoolean("HiddenPlacesSunveilTrialMob", true);
        level.addFreshEntity(mob);
        this.activeMobIds.add(mob.getUUID());
        this.spawnedInWave++;
        this.spawnCooldown = SPAWN_COOLDOWN_TICKS;

        level.sendParticles(ParticleTypes.POOF,
                spawnX,
                spawnY + 0.2D,
                spawnZ,
                14, 0.35D, 0.2D, 0.35D, 0.02D);
        level.playSound(null, BlockPos.containing(spawnX, spawnY, spawnZ), sound(SoundEvents.SAND_BREAK), SoundSource.HOSTILE, 0.9F, 0.65F + level.random.nextFloat() * 0.25F);
        this.setChangedAndSync();
    }

    private BlockPos pickNextSpawnMarker() {
        int index = Math.floorMod(this.spawnSequenceIndex, this.spawnMarkerPositions.size());
        this.spawnSequenceIndex++;
        return this.spawnMarkerPositions.get(index);
    }

    private static double inwardOffset(int markerCoord, int centerCoord) {
        if (markerCoord < centerCoord) {
            return 1.0D;
        }
        if (markerCoord > centerCoord) {
            return -1.0D;
        }
        return 0.0D;
    }

    private EntityType<? extends Mob> entityTypeForWave(int wave, int index) {
        if (wave == 0) {
            return EntityType.ZOMBIE;
        }
        if (wave == 1) {
            return index < 4 ? EntityType.ZOMBIE : EntityType.SILVERFISH;
        }
        if (wave == 2) {
            return index < 4 ? EntityType.SKELETON : EntityType.ZOMBIE;
        }
        if (wave == 3) {
            return index < 5 ? EntityType.ZOMBIE : EntityType.SILVERFISH;
        }
        return EntityType.SKELETON;
    }

    private void pruneAndCountDefeatedMobs(ServerLevel level) {
        Iterator<UUID> iterator = this.activeMobIds.iterator();
        while (iterator.hasNext()) {
            UUID mobId = iterator.next();
            Entity entity = level.getEntity(mobId);
            if (!(entity instanceof LivingEntity livingEntity) || entity.isRemoved() || !livingEntity.isAlive()) {
                iterator.remove();
                this.killsDone = Math.min(TOTAL_REQUIRED_KILLS, this.killsDone + 1);
                this.setChangedAndSync();
            }
        }
    }

    private void startSuccess(ServerLevel level) {
        this.running = false;
        this.finishing = true;
        this.finishDelayTicks = END_DELAY_TICKS;
        this.updateBossbar();
        this.cleanupMobs(level, false);
        this.getWayfinder(level).ifPresent(SunveilWayfinderEntity::stopFlutePerformance);
        this.stopFluteLoopForPlayers(level);
        BlockPos soundPos = this.rewardChestPos != null ? this.rewardChestPos : this.worldPosition;
        level.playSound(null, soundPos, sound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE), SoundSource.BLOCKS, 1.0F, 1.0F);
        level.sendParticles(ParticleTypes.FLAME,
                soundPos.getX() + 0.5D,
                soundPos.getY() + 1.2D,
                soundPos.getZ() + 0.5D,
                35, 0.45D, 0.35D, 0.45D, 0.04D);
        this.setChangedAndSync();
    }

    private void finishSuccess(ServerLevel level) {
        this.finishing = false;
        this.bossEvent.removeAllPlayers();
        this.bossEvent.setVisible(false);
        this.openEntrance(level);
        this.spawnReward(level);
        ServerPlayer starter = this.starterPlayerId == null ? null : level.getServer().getPlayerList().getPlayer(this.starterPlayerId);
        if (starter != null) {
            this.getWayfinder(level).ifPresent(wayfinder -> wayfinder.markPlayerTraded(starter));
        }
        this.starterPlayerId = null;
        this.resetRuntimeState();
        this.setChangedAndSync();
    }

    public void failTrial(ServerLevel level) {
        ServerPlayer starter = this.starterPlayerId == null ? null : level.getServer().getPlayerList().getPlayer(this.starterPlayerId);
        Optional<SunveilWayfinderEntity> wayfinder = this.getWayfinder(level);

        this.cleanupMobs(level, true);
        this.running = false;
        this.finishing = false;
        this.bossEvent.removeAllPlayers();
        this.bossEvent.setVisible(false);
        this.openEntrance(level);
        this.stopFluteLoopForPlayers(level);
        wayfinder.ifPresent(SunveilWayfinderEntity::stopFlutePerformance);

        if (starter != null) {
            wayfinder.ifPresent(sunveilWayfinder -> {
                sunveilWayfinder.clearPlayerTraded(starter);
                sunveilWayfinder.setDialogueNode(starter, "ask_trial");
            });
        }

        BlockPos pos = this.npcPos != null ? this.npcPos : this.worldPosition;
        level.playSound(null, pos, sound(SoundEvents.BEACON_DEACTIVATE), SoundSource.BLOCKS, 0.85F, 0.95F);
        this.starterPlayerId = null;
        this.resetRuntimeState();
        this.setChangedAndSync();
    }

    private void resetRuntimeState() {
        this.running = false;
        this.finishing = false;
        this.startDelayTicks = 0;
        this.finishDelayTicks = 0;
        this.waveIndex = 0;
        this.spawnedInWave = 0;
        this.killsDone = 0;
        this.spawnCooldown = 0;
        this.fluteRefreshTicks = 0;
        this.spawnSequenceIndex = 0;
        this.activeMobIds.clear();
        this.bossEvent.setProgress(0.0F);
    }

    private void spawnReward(ServerLevel level) {
        BlockPos pos = this.rewardChestPos != null ? this.rewardChestPos : this.worldPosition.above();
        ItemEntity itemEntity = new ItemEntity(level, pos.getX() + 0.5D, pos.getY() + 1.2D, pos.getZ() + 0.5D, new ItemStack(HPItems.SUNVEIL_KEY.get()));
        itemEntity.setDefaultPickUpDelay();
        level.addFreshEntity(itemEntity);
        level.playSound(null, pos, sound(SoundEvents.AMETHYST_BLOCK_CHIME), SoundSource.BLOCKS, 1.0F, 1.35F);
        level.sendParticles(ParticleTypes.ENCHANT,
                pos.getX() + 0.5D,
                pos.getY() + 1.25D,
                pos.getZ() + 0.5D,
                45, 0.45D, 0.35D, 0.45D, 0.06D);
    }

    private boolean shouldFail(ServerLevel level) {
        ServerPlayer starter = this.starterPlayerId == null ? null : level.getServer().getPlayerList().getPlayer(this.starterPlayerId);
        if (starter == null || starter.isDeadOrDying() || starter.isRemoved()) {
            return true;
        }
        return starter.blockPosition().distSqr(this.worldPosition) > FAILURE_DISTANCE * FAILURE_DISTANCE;
    }

    private void cleanupMobs(ServerLevel level, boolean particles) {
        for (UUID mobId : new ArrayList<>(this.activeMobIds)) {
            Entity entity = level.getEntity(mobId);
            if (entity != null) {
                if (particles) {
                    level.sendParticles(ParticleTypes.SMOKE,
                            entity.getX(), entity.getY() + 0.4D, entity.getZ(),
                            12, 0.2D, 0.2D, 0.2D, 0.01D);
                }
                entity.discard();
            }
        }
        this.activeMobIds.clear();
    }

    private void tickBossbarPlayers(ServerLevel level) {
        if (!this.running && !this.finishing) {
            this.bossEvent.removeAllPlayers();
            return;
        }

        for (ServerPlayer player : level.players()) {
            if (this.isInsideBounds(player.blockPosition())) {
                this.bossEvent.addPlayer(player);
            } else {
                this.bossEvent.removePlayer(player);
            }
        }
    }

    private void applyTrialEffects(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            if (this.isInsideBounds(player.blockPosition())) {
                player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 40, MINING_FATIGUE_AMPLIFIER, true, false, true));
            }
        }
    }

    private boolean isInsideBounds(BlockPos pos) {
        BlockPos min = this.boundsMin != null ? this.boundsMin : this.worldPosition.offset(-20, -6, -20);
        BlockPos max = this.boundsMax != null ? this.boundsMax : this.worldPosition.offset(20, 10, 20);
        int minX = Math.min(min.getX(), max.getX());
        int minY = Math.min(min.getY(), max.getY());
        int minZ = Math.min(min.getZ(), max.getZ());
        int maxX = Math.max(min.getX(), max.getX());
        int maxY = Math.max(min.getY(), max.getY());
        int maxZ = Math.max(min.getZ(), max.getZ());
        return pos.getX() >= minX && pos.getX() <= maxX
                && pos.getY() >= minY && pos.getY() <= maxY
                && pos.getZ() >= minZ && pos.getZ() <= maxZ;
    }

    private void updateBossbar() {
        this.bossEvent.setProgress(Math.min(1.0F, this.killsDone / (float) TOTAL_REQUIRED_KILLS));
        this.bossEvent.setName(Component.translatable("bossbar.hidden_places.sunveil_trial.progress", this.killsDone, TOTAL_REQUIRED_KILLS));
    }

    private void stopFluteLoopForPlayers(ServerLevel level) {
        ClientboundStopSoundPacket stopPacket = new ClientboundStopSoundPacket(HPSoundEvents.SUNVEIL_FLUTE_LOOP.getId(), SoundSource.RECORDS);
        for (ServerPlayer player : level.players()) {
            if (this.isInsideBounds(player.blockPosition()) || (this.npcPos != null && player.blockPosition().distSqr(this.npcPos) <= SEARCH_RADIUS * SEARCH_RADIUS)) {
                player.connection.send(stopPacket);
            }
        }
        this.fluteRefreshTicks = FLUTE_REFRESH_TICKS;
    }

    private void playFluteLoopIfNeeded(ServerLevel level) {
        if (this.fluteRefreshTicks > 0) {
            this.fluteRefreshTicks--;
            return;
        }
        this.playFluteLoop(level);
    }

    private void playFluteLoop(ServerLevel level) {
        BlockPos soundPos = this.npcPos != null ? this.npcPos : this.worldPosition;
        level.playSound(null, soundPos, HPSoundEvents.SUNVEIL_FLUTE_LOOP.get(), SoundSource.RECORDS, 1.0F, 1.0F);
        this.fluteRefreshTicks = FLUTE_REFRESH_TICKS;
    }

    public Optional<SunveilWayfinderEntity> getWayfinder(ServerLevel level) {
        if (this.npcPos == null) {
            return Optional.empty();
        }
        List<SunveilWayfinderEntity> wayfinders = level.getEntitiesOfClass(SunveilWayfinderEntity.class, new AABB(this.npcPos).inflate(2.0D, 2.0D, 2.0D));
        return wayfinders.isEmpty() ? Optional.empty() : Optional.of(wayfinders.get(0));
    }

    private void rebuildGatePositions() {
        this.gatePositions.clear();
        if (this.npcPos == null) {
            return;
        }

        int dx = this.npcPos.getX() - this.worldPosition.getX();
        int dz = this.npcPos.getZ() - this.worldPosition.getZ();
        Direction npcDirection;
        if (Math.abs(dx) >= Math.abs(dz)) {
            npcDirection = dx >= 0 ? Direction.EAST : Direction.WEST;
        } else {
            npcDirection = dz >= 0 ? Direction.SOUTH : Direction.NORTH;
        }

        Direction doorwayDirection = npcDirection.getOpposite();
        BlockPos doorwayBase = this.worldPosition.relative(doorwayDirection, DOOR_HORIZONTAL_DISTANCE);
        for (int yOffset : DOOR_Y_OFFSETS) {
            this.gatePositions.add(doorwayBase.above(yOffset).immutable());
        }
    }

    private void closeEntrance(ServerLevel level) {
        if (this.gatePositions.isEmpty()) {
            this.rebuildGatePositions();
        }
        BlockState doorBlock = Blocks.SMOOTH_SANDSTONE.defaultBlockState();
        for (BlockPos gatePos : this.gatePositions) {
            level.setBlock(gatePos, doorBlock, Block.UPDATE_ALL);
        }
    }

    private void openEntrance(ServerLevel level) {
        for (BlockPos gatePos : this.gatePositions) {
            level.setBlock(gatePos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    private static SoundEvent sound(SoundEvent soundEvent) {
        return soundEvent;
    }

    private static SoundEvent sound(Holder<SoundEvent> soundEventHolder) {
        return soundEventHolder.value();
    }

    private void setChangedAndSync() {
        this.setChanged();
        if (this.level != null) {
            BlockState state = this.getBlockState();
            this.level.sendBlockUpdated(this.worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("Running", this.running);
        tag.putBoolean("Finishing", this.finishing);
        tag.putInt("StartDelayTicks", this.startDelayTicks);
        tag.putInt("FinishDelayTicks", this.finishDelayTicks);
        tag.putInt("WaveIndex", this.waveIndex);
        tag.putInt("SpawnedInWave", this.spawnedInWave);
        tag.putInt("KillsDone", this.killsDone);
        tag.putInt("SpawnCooldown", this.spawnCooldown);
        tag.putInt("FluteRefreshTicks", this.fluteRefreshTicks);
        tag.putInt("SpawnSequenceIndex", this.spawnSequenceIndex);
        if (this.starterPlayerId != null) {
            tag.putUUID("StarterPlayer", this.starterPlayerId);
        }
        if (this.npcPos != null) {
            tag.putLong("NpcPos", this.npcPos.asLong());
        }
        if (this.rewardChestPos != null) {
            tag.putLong("RewardChestPos", this.rewardChestPos.asLong());
        }
        if (this.boundsMin != null) {
            tag.putLong("BoundsMin", this.boundsMin.asLong());
        }
        if (this.boundsMax != null) {
            tag.putLong("BoundsMax", this.boundsMax.asLong());
        }

        ListTag spawnMarkers = new ListTag();
        for (BlockPos spawnMarkerPosition : this.spawnMarkerPositions) {
            CompoundTag markerTag = new CompoundTag();
            markerTag.putLong("Pos", spawnMarkerPosition.asLong());
            spawnMarkers.add(markerTag);
        }
        tag.put("SpawnMarkers", spawnMarkers);

        ListTag gateList = new ListTag();
        for (BlockPos gatePos : this.gatePositions) {
            CompoundTag gateTag = new CompoundTag();
            gateTag.putLong("Pos", gatePos.asLong());
            gateList.add(gateTag);
        }
        tag.put("GatePositions", gateList);

        ListTag activeMobs = new ListTag();
        for (UUID activeMobId : this.activeMobIds) {
            CompoundTag mobTag = new CompoundTag();
            mobTag.putUUID("Id", activeMobId);
            activeMobs.add(mobTag);
        }
        tag.put("ActiveMobs", activeMobs);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.running = tag.getBoolean("Running");
        this.finishing = tag.getBoolean("Finishing");
        this.startDelayTicks = tag.getInt("StartDelayTicks");
        this.finishDelayTicks = tag.getInt("FinishDelayTicks");
        this.waveIndex = tag.getInt("WaveIndex");
        this.spawnedInWave = tag.getInt("SpawnedInWave");
        this.killsDone = tag.getInt("KillsDone");
        this.spawnCooldown = tag.getInt("SpawnCooldown");
        this.fluteRefreshTicks = tag.getInt("FluteRefreshTicks");
        this.spawnSequenceIndex = tag.getInt("SpawnSequenceIndex");
        this.starterPlayerId = tag.hasUUID("StarterPlayer") ? tag.getUUID("StarterPlayer") : null;
        this.npcPos = tag.contains("NpcPos") ? BlockPos.of(tag.getLong("NpcPos")) : null;
        this.rewardChestPos = tag.contains("RewardChestPos") ? BlockPos.of(tag.getLong("RewardChestPos")) : null;
        this.boundsMin = tag.contains("BoundsMin") ? BlockPos.of(tag.getLong("BoundsMin")) : null;
        this.boundsMax = tag.contains("BoundsMax") ? BlockPos.of(tag.getLong("BoundsMax")) : null;

        this.spawnMarkerPositions.clear();
        ListTag spawnMarkers = tag.getList("SpawnMarkers", Tag.TAG_COMPOUND);
        for (int i = 0; i < spawnMarkers.size(); i++) {
            this.spawnMarkerPositions.add(BlockPos.of(spawnMarkers.getCompound(i).getLong("Pos")));
        }

        this.gatePositions.clear();
        ListTag gates = tag.getList("GatePositions", Tag.TAG_COMPOUND);
        for (int i = 0; i < gates.size(); i++) {
            this.gatePositions.add(BlockPos.of(gates.getCompound(i).getLong("Pos")));
        }
        if (this.gatePositions.isEmpty()) {
            this.rebuildGatePositions();
        }

        this.activeMobIds.clear();
        ListTag activeMobs = tag.getList("ActiveMobs", Tag.TAG_COMPOUND);
        for (int i = 0; i < activeMobs.size(); i++) {
            CompoundTag mobTag = activeMobs.getCompound(i);
            if (mobTag.hasUUID("Id")) {
                this.activeMobIds.add(mobTag.getUUID("Id"));
            }
        }
        this.updateBossbar();
        this.bossEvent.setVisible(this.running || this.finishing);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    @Override
    public @Nullable ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
