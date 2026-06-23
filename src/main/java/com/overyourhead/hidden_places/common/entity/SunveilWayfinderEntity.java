package com.overyourhead.hidden_places.common.entity;

import com.overyourhead.hidden_places.common.block.entity.SunveilTrialControllerBlockEntity;
import com.overyourhead.hidden_places.common.npc.WayfinderProfile;
import com.overyourhead.hidden_places.core.registry.HPItems;
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
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import com.overyourhead.hidden_places.common.network.OpenWayfinderDialoguePayload;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.Optional;

public class SunveilWayfinderEntity extends TestWayfinderEntity {
    public static final int ANIM_IDLE = 0;
    public static final int ANIM_BEFORE_FLUTE = 1;
    public static final int ANIM_PLAY_FLUTE = 2;
    public static final int ANIM_AFTER_FLUTE = 3;

    private static final EntityDataAccessor<Integer> FLUTE_ANIMATION_STATE = SynchedEntityData.defineId(
            SunveilWayfinderEntity.class,
            EntityDataSerializers.INT
    );

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation BEFORE_FLUTE = RawAnimation.begin().thenPlay("before_flute").thenLoop("play_flute");
    private static final RawAnimation PLAY_FLUTE = RawAnimation.begin().thenLoop("play_flute");
    private static final RawAnimation AFTER_FLUTE = RawAnimation.begin().thenPlay("after_flute").thenLoop("idle");

    private int transitionTicks;

    public SunveilWayfinderEntity(EntityType<? extends SunveilWayfinderEntity> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 60.0D)
                .add(Attributes.ARMOR, 8.0D)
                .add(Attributes.ATTACK_DAMAGE, 0.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.FOLLOW_RANGE, 20.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FLUTE_ANIMATION_STATE, ANIM_IDLE);
    }

    @Override
    public WayfinderProfile getProfile() {
        return WayfinderProfile.SUNVEIL_WAYFINDER;
    }

    public int getFluteAnimationState() {
        return this.entityData.get(FLUTE_ANIMATION_STATE);
    }

    private void setFluteAnimationState(int state) {
        this.entityData.set(FLUTE_ANIMATION_STATE, state);
    }

    public void startFlutePerformance() {
        this.setFluteAnimationState(ANIM_BEFORE_FLUTE);
        this.transitionTicks = 20;
    }

    public void stopFlutePerformance() {
        if (this.getFluteAnimationState() != ANIM_IDLE) {
            this.setFluteAnimationState(ANIM_AFTER_FLUTE);
            this.transitionTicks = 18;
        }
    }

    @Override
    public void tick() {
        super.tick();
        this.setTarget(null);
        this.getNavigation().stop();
        this.setDeltaMovement(0.0D, this.getDeltaMovement().y, 0.0D);

        if (!this.level().isClientSide && this.transitionTicks > 0) {
            this.transitionTicks--;
            if (this.transitionTicks <= 0) {
                if (this.getFluteAnimationState() == ANIM_BEFORE_FLUTE) {
                    this.setFluteAnimationState(ANIM_PLAY_FLUTE);
                } else if (this.getFluteAnimationState() == ANIM_AFTER_FLUTE) {
                    this.setFluteAnimationState(ANIM_IDLE);
                }
            }
        }
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }

        if (this.level().isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (this.level() instanceof ServerLevel serverLevel) {
            Optional<SunveilTrialControllerBlockEntity> controller = this.getTrialController(serverLevel);
            if (controller.isPresent() && controller.get().isRunning()) {
                this.setDialogueNode(player, "trial_running");
            } else if (this.hasPlayerTraded(player)) {
                this.setDialogueNode(player, this.getProfile().completedNodeId());
            }
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

    public void startTrialFor(ServerPlayer player) {
        if (this.level() instanceof ServerLevel serverLevel) {
            Optional<SunveilTrialControllerBlockEntity> controller = this.getTrialController(serverLevel);
            if (this.hasPlayerTraded(player)) {
                this.setDialogueNode(player, this.getProfile().completedNodeId());
                this.startConversation(player);
                return;
            }
            if (controller.isEmpty() || controller.get().isRunning() || !controller.get().startTrial(serverLevel, player, this)) {
                this.setDialogueNode(player, "trial_running");
                this.startConversation(player);
                return;
            }

            this.setDialogueNode(player, "trial_started");
            this.startConversation(player);
        }
    }

    public Optional<SunveilTrialControllerBlockEntity> getTrialController(ServerLevel level) {
        return SunveilTrialControllerBlockEntity.findNearest(level, this.blockPosition());
    }

    @Override
    public void setHostile(boolean hostile) {
        super.setHostile(false);
    }

    @Override
    public void provoke(Player player) {
        this.setDialogueNode(player, this.getProfile().defaultNodeId());
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() instanceof Player player && !this.level().isClientSide) {
            player.displayClientMessage(Component.translatable("message.hidden_places.sunveil_wayfinder.immortal"), true);
        }
        return false;
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(HPItems.SUNVEIL_WAYFINDER_SPAWN_EGG.get());
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "sunveil_wayfinder_controller", 3, this::handleSunveilAnimations));
    }

    private <E extends SunveilWayfinderEntity> PlayState handleSunveilAnimations(AnimationState<E> animationState) {
        int state = this.getFluteAnimationState();
        if (state == ANIM_BEFORE_FLUTE) {
            return animationState.setAndContinue(BEFORE_FLUTE);
        }
        if (state == ANIM_PLAY_FLUTE) {
            return animationState.setAndContinue(PLAY_FLUTE);
        }
        if (state == ANIM_AFTER_FLUTE) {
            return animationState.setAndContinue(AFTER_FLUTE);
        }
        return animationState.setAndContinue(IDLE);
    }
}
