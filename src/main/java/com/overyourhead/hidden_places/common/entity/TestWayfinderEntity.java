package com.overyourhead.hidden_places.common.entity;

import com.overyourhead.hidden_places.common.network.OpenWayfinderDialoguePayload;
import com.overyourhead.hidden_places.common.npc.WayfinderDialogueNode;
import com.overyourhead.hidden_places.common.npc.WayfinderProfile;
import com.overyourhead.hidden_places.common.npc.WayfinderTradeOffer;
import com.overyourhead.hidden_places.core.registry.HPItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class TestWayfinderEntity extends Monster implements GeoEntity {
    private static final EntityDataAccessor<Boolean> HOSTILE = SynchedEntityData.defineId(
            TestWayfinderEntity.class,
            EntityDataSerializers.BOOLEAN
    );

    private static final EntityDataAccessor<Boolean> IN_CONVERSATION = SynchedEntityData.defineId(
            TestWayfinderEntity.class,
            EntityDataSerializers.BOOLEAN
    );

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");

    private final AnimatableInstanceCache animatableInstanceCache = GeckoLibUtil.createInstanceCache(this);
    private final Map<UUID, String> playerDialogueNodes = new HashMap<>();
    private final Set<UUID> tradedPlayers = new HashSet<>();
    private final Set<UUID> activeTalkers = new HashSet<>();

    public TestWayfinderEntity(EntityType<? extends TestWayfinderEntity> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 8;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 28.0D)
                .add(Attributes.ARMOR, 2.0D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.FOLLOW_RANGE, 24.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.1D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.75D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(HOSTILE, false);
        builder.define(IN_CONVERSATION, false);
    }

    public WayfinderProfile getProfile() {
        return WayfinderProfile.TEST_WAYFINDER;
    }

    public boolean isHostile() {
        return this.entityData.get(HOSTILE);
    }

    public void setHostile(boolean hostile) {
        this.entityData.set(HOSTILE, hostile);
        if (hostile) {
            this.clearConversations();
        } else {
            this.setTarget(null);
        }
    }

    public boolean isInConversation() {
        return this.entityData.get(IN_CONVERSATION);
    }

    public void setInConversation(boolean inConversation) {
        if (!inConversation) {
            this.activeTalkers.clear();
        }

        this.entityData.set(IN_CONVERSATION, inConversation);
        if (inConversation && !this.isHostile()) {
            this.stopDialogueMovement();
        }
    }

    public void startConversation(Player player) {
        if (this.isHostile()) {
            return;
        }

        this.activeTalkers.add(player.getUUID());
        this.entityData.set(IN_CONVERSATION, true);
        this.stopDialogueMovement();
    }

    public void stopConversation(Player player) {
        this.activeTalkers.remove(player.getUUID());
        if (this.activeTalkers.isEmpty()) {
            this.entityData.set(IN_CONVERSATION, false);
        }
    }

    private void clearConversations() {
        this.activeTalkers.clear();
        this.entityData.set(IN_CONVERSATION, false);
    }

    public String getDialogueNodeId(Player player) {
        if (this.hasPlayerTraded(player)) {
            return this.getProfile().completedNodeId();
        }

        String nodeId = this.playerDialogueNodes.getOrDefault(player.getUUID(), this.getProfile().defaultNodeId());
        if (!this.getProfile().hasDialogueNode(nodeId)) {
            return this.getProfile().defaultNodeId();
        }

        return nodeId;
    }

    public WayfinderDialogueNode getDialogueNode(Player player) {
        return this.getProfile().dialogueNode(this.getDialogueNodeId(player));
    }

    public void setDialogueNode(Player player, String nodeId) {
        String safeNodeId = this.getProfile().hasDialogueNode(nodeId) ? nodeId : this.getProfile().defaultNodeId();
        this.playerDialogueNodes.put(player.getUUID(), safeNodeId);
    }

    public boolean hasTrade() {
        return this.getProfile().hasTrade();
    }

    public WayfinderTradeOffer getTradeOffer() {
        return this.getProfile().tradeOffer();
    }

    public boolean allowsTradeOffer(WayfinderTradeOffer offer) {
        return this.getProfile().allowsTradeOffer(offer);
    }

    public boolean hasPlayerTraded(Player player) {
        return this.tradedPlayers.contains(player.getUUID());
    }

    public void markPlayerTraded(Player player) {
        this.tradedPlayers.add(player.getUUID());
        this.setDialogueNode(player, this.getProfile().completedNodeId());
    }

    public void provoke(Player player) {
        if (!this.getProfile().canBecomeHostileFromDialogue() || this.hasPlayerTraded(player)) {
            return;
        }

        this.clearConversations();
        if (this.getProfile().hostileNodeId() != null) {
            this.setDialogueNode(player, this.getProfile().hostileNodeId());
        }
        this.setHostile(true);
        this.setTarget(player);
        player.displayClientMessage(Component.translatable("message.hidden_places.test_wayfinder.hostile"), true);
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide && !this.activeTalkers.isEmpty() && this.level() instanceof ServerLevel serverLevel) {
            this.activeTalkers.removeIf(playerId -> {
                Player player = serverLevel.getPlayerByUUID(playerId);
                return player == null || player.isRemoved() || this.distanceToSqr(player) > 100.0D;
            });
            this.entityData.set(IN_CONVERSATION, !this.activeTalkers.isEmpty());
        }

        if (this.isInConversation() && !this.isHostile()) {
            this.stopDialogueMovement();
        }
    }

    private void stopDialogueMovement() {
        this.setTarget(null);
        this.getNavigation().stop();
        this.setDeltaMovement(0.0D, this.getDeltaMovement().y, 0.0D);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() instanceof Player player && this.hasPlayerTraded(player)) {
            if (!this.level().isClientSide) {
                player.displayClientMessage(Component.translatable("message.hidden_places.test_wayfinder.trade_completed_no_fight"), true);
            }
            return false;
        }

        if (!this.isHostile()) {
            if (source.getEntity() instanceof Player player && !this.level().isClientSide) {
                player.displayClientMessage(Component.translatable("message.hidden_places.test_wayfinder.speak_first"), true);
            }
            return false;
        }

        if (source.getEntity() instanceof Player player) {
            this.setTarget(player);
        }

        return super.hurt(source, amount);
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }

        if (this.isHostile()) {
            return InteractionResult.PASS;
        }

        if (this.level().isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (player.isShiftKeyDown()) {
            if (!this.hasPlayerTraded(player)) {
                this.provoke(player);
            } else {
                this.setDialogueNode(player, this.getProfile().completedNodeId());
                this.startConversation(player);
                if (player instanceof ServerPlayer serverPlayer) {
                    PacketDistributor.sendToPlayer(serverPlayer, new OpenWayfinderDialoguePayload(
                            this.getId(),
                            this.getProfile().completedNodeId()
                    ));
                }
            }
            return InteractionResult.CONSUME;
        }

        if (this.hasPlayerTraded(player)) {
            this.setDialogueNode(player, this.getProfile().completedNodeId());
        }

        this.startConversation(player);
        if (player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, new OpenWayfinderDialoguePayload(
                    this.getId(),
                    this.getDialogueNodeId(player)
            ));
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(HPItems.TEST_WAYFINDER_SPAWN_EGG.get());
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Hostile", this.isHostile());

        ListTag nodes = new ListTag();
        this.playerDialogueNodes.forEach((playerId, nodeId) -> {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("Player", playerId);
            entry.putString("Node", nodeId);
            nodes.add(entry);
        });
        tag.put("PlayerDialogueNodes", nodes);

        ListTag trades = new ListTag();
        for (UUID playerId : this.tradedPlayers) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("Player", playerId);
            trades.add(entry);
        }
        tag.put("TradedPlayers", trades);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setHostile(tag.getBoolean("Hostile"));

        this.playerDialogueNodes.clear();
        ListTag nodes = tag.getList("PlayerDialogueNodes", Tag.TAG_COMPOUND);
        for (int i = 0; i < nodes.size(); i++) {
            CompoundTag entry = nodes.getCompound(i);
            if (entry.hasUUID("Player")) {
                String nodeId = entry.getString("Node");
                if (this.getProfile().hasDialogueNode(nodeId)) {
                    this.playerDialogueNodes.put(entry.getUUID("Player"), nodeId);
                }
            }
        }

        ListTag legacyStages = tag.getList("PlayerDialogueStages", Tag.TAG_COMPOUND);
        for (int i = 0; i < legacyStages.size(); i++) {
            CompoundTag entry = legacyStages.getCompound(i);
            if (entry.hasUUID("Player")) {
                this.playerDialogueNodes.put(
                        entry.getUUID("Player"),
                        this.getProfile().nodeIdFromLegacyStage(entry.getInt("Stage"))
                );
            }
        }

        this.tradedPlayers.clear();
        ListTag trades = tag.getList("TradedPlayers", Tag.TAG_COMPOUND);
        for (int i = 0; i < trades.size(); i++) {
            CompoundTag entry = trades.getCompound(i);
            if (entry.hasUUID("Player")) {
                this.tradedPlayers.add(entry.getUUID("Player"));
            }
        }

        this.clearConversations();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "wayfinder_controller", 5, this::handleAnimations));
    }

    private <E extends TestWayfinderEntity> PlayState handleAnimations(AnimationState<E> animationState) {
        boolean moving = this.getDeltaMovement().horizontalDistanceSqr() > 1.0E-6D || animationState.isMoving();
        return animationState.setAndContinue(moving ? WALK : IDLE);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.animatableInstanceCache;
    }
}
