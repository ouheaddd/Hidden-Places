package com.overyourhead.hidden_places.common.entity;

import com.overyourhead.hidden_places.common.network.OpenWayfinderDialoguePayload;
import com.overyourhead.hidden_places.common.npc.WayfinderDialogueStage;
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
    public static final int DIALOGUE_STAGE_INTRO = WayfinderDialogueStage.INTRO.id();
    public static final int DIALOGUE_STAGE_WHO = WayfinderDialogueStage.WHO.id();
    public static final int DIALOGUE_STAGE_SANCTUM = WayfinderDialogueStage.SANCTUM.id();
    public static final int DIALOGUE_STAGE_TRADE = WayfinderDialogueStage.TRADE.id();
    public static final int DIALOGUE_STAGE_HOSTILE_DEAD_END = WayfinderDialogueStage.HOSTILE_DEAD_END.id();
    public static final int DIALOGUE_STAGE_TRADE_COMPLETED = WayfinderDialogueStage.TRADE_COMPLETED.id();

    private static final EntityDataAccessor<Boolean> HOSTILE = SynchedEntityData.defineId(
            TestWayfinderEntity.class,
            EntityDataSerializers.BOOLEAN
    );

    private static final EntityDataAccessor<Boolean> IN_CONVERSATION = SynchedEntityData.defineId(
            TestWayfinderEntity.class,
            EntityDataSerializers.BOOLEAN
    );

    private static final EntityDataAccessor<Integer> DIALOGUE_STAGE = SynchedEntityData.defineId(
            TestWayfinderEntity.class,
            EntityDataSerializers.INT
    );

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");

    private final AnimatableInstanceCache animatableInstanceCache = GeckoLibUtil.createInstanceCache(this);
    private final Map<UUID, WayfinderDialogueStage> playerDialogueStages = new HashMap<>();
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
        builder.define(DIALOGUE_STAGE, this.getProfile().defaultDialogueStage().id());
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

    public int getDialogueStage() {
        return this.entityData.get(DIALOGUE_STAGE);
    }

    public WayfinderDialogueStage getDialogueStageType() {
        return WayfinderDialogueStage.byId(this.getDialogueStage());
    }

    public WayfinderDialogueStage getDialogueStage(Player player) {
        return this.playerDialogueStages.getOrDefault(player.getUUID(), this.getProfile().defaultDialogueStage());
    }

    public void setDialogueStage(int dialogueStage) {
        this.setDialogueStage(WayfinderDialogueStage.byId(dialogueStage));
    }

    public void setDialogueStage(WayfinderDialogueStage dialogueStage) {
        this.entityData.set(DIALOGUE_STAGE, dialogueStage.id());
    }

    public void setDialogueStage(Player player, WayfinderDialogueStage dialogueStage) {
        this.playerDialogueStages.put(player.getUUID(), dialogueStage);
        this.entityData.set(DIALOGUE_STAGE, dialogueStage.id());
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
        this.setDialogueStage(player, WayfinderDialogueStage.TRADE_COMPLETED);
    }

    public void provoke(Player player) {
        if (!this.getProfile().canBecomeHostileFromDialogue() || this.hasPlayerTraded(player)) {
            return;
        }

        this.clearConversations();
        this.setDialogueStage(player, WayfinderDialogueStage.HOSTILE_DEAD_END);
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
                this.setDialogueStage(player, WayfinderDialogueStage.TRADE_COMPLETED);
                this.startConversation(player);
                if (player instanceof ServerPlayer serverPlayer) {
                    PacketDistributor.sendToPlayer(serverPlayer, new OpenWayfinderDialoguePayload(
                            this.getId(),
                            WayfinderDialogueStage.TRADE_COMPLETED.id()
                    ));
                }
            }
            return InteractionResult.CONSUME;
        }

        if (this.hasPlayerTraded(player)) {
            this.setDialogueStage(player, WayfinderDialogueStage.TRADE_COMPLETED);
        }

        this.startConversation(player);
        if (player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, new OpenWayfinderDialoguePayload(
                    this.getId(),
                    this.getDialogueStage(player).id()
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
        tag.putInt("DialogueStage", this.getDialogueStage());

        ListTag stages = new ListTag();
        this.playerDialogueStages.forEach((playerId, stage) -> {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("Player", playerId);
            entry.putInt("Stage", stage.id());
            stages.add(entry);
        });
        tag.put("PlayerDialogueStages", stages);

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
        if (tag.contains("DialogueStage")) {
            this.setDialogueStage(tag.getInt("DialogueStage"));
        } else {
            this.setDialogueStage(this.getProfile().defaultDialogueStage());
        }

        this.playerDialogueStages.clear();
        ListTag stages = tag.getList("PlayerDialogueStages", Tag.TAG_COMPOUND);
        for (int i = 0; i < stages.size(); i++) {
            CompoundTag entry = stages.getCompound(i);
            if (entry.hasUUID("Player")) {
                this.playerDialogueStages.put(
                        entry.getUUID("Player"),
                        WayfinderDialogueStage.byId(entry.getInt("Stage"))
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
        controllers.add(new AnimationController<>(this, "test_wayfinder_controller", 5, this::handleAnimations));
    }

    private <E extends TestWayfinderEntity> PlayState handleAnimations(AnimationState<E> animationState) {
        return animationState.setAndContinue(animationState.isMoving() ? WALK : IDLE);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.animatableInstanceCache;
    }
}
