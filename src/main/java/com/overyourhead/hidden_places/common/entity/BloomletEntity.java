package com.overyourhead.hidden_places.common.entity;

import com.overyourhead.hidden_places.common.entity.projectile.BloomletPetalProjectileEntity;
import com.overyourhead.hidden_places.core.registry.HPEntities;
import com.overyourhead.hidden_places.core.registry.HPItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class BloomletEntity extends TamableAnimal implements GeoEntity, net.minecraft.world.entity.monster.RangedAttackMob {
    private static final EntityDataAccessor<Boolean> SITTING = SynchedEntityData.defineId(BloomletEntity.class, EntityDataSerializers.BOOLEAN);

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("attack");
    private static final RawAnimation SIT = RawAnimation.begin().thenLoop("sit");
    private static final RawAnimation SIT_DOWN = RawAnimation.begin().thenPlay("sitDown");
    private static final RawAnimation STAND_UP = RawAnimation.begin().thenPlay("standUp");

    private static final byte EVENT_SIT_DOWN = 60;
    private static final byte EVENT_STAND_UP = 61;
    private static final byte EVENT_ATTACK = 62;

    private static final int SIT_TRANSITION_TICKS = 10;
    private static final int ATTACK_ANIMATION_TICKS = 12;

    private final AnimatableInstanceCache animatableInstanceCache = GeckoLibUtil.createInstanceCache(this);

    private int sitDownTicks;
    private int standUpTicks;
    private int attackTicks;

    public BloomletEntity(EntityType<? extends BloomletEntity> entityType, Level level) {
        super(entityType, level);
        this.setTame(false, false);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TamableAnimal.createLivingAttributes()
                .add(Attributes.MAX_HEALTH, 32.0D)
                .add(Attributes.ARMOR, 4.0D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.30D)
                .add(Attributes.FOLLOW_RANGE, 24.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.35D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(2, new RangedAttackGoal(this, 1.15D, 22, 12.0F));
        this.goalSelector.addGoal(3, new FollowOwnerGoal(this, 1.1D, 8.0F, 2.0F));
        this.goalSelector.addGoal(5, new RandomStrollGoal(this, 0.9D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SITTING, false);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.sitDownTicks > 0) {
            this.sitDownTicks--;
        }
        if (this.standUpTicks > 0) {
            this.standUpTicks--;
        }
        if (this.attackTicks > 0) {
            this.attackTicks--;
        }
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
    public boolean hurt(DamageSource source, float amount) {
        if (this.isTame() && source.getEntity() != null && source.getEntity().getUUID().equals(this.getOwnerUUID())) {
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return target != this.getOwner() && super.canAttack(target);
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(ItemTags.FLOWERS);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!this.isTame()) {
            if (!stack.is(ItemTags.FLOWERS)) {
                return super.mobInteract(player, hand);
            }

            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }

            if (!this.level().isClientSide) {
                if (this.random.nextInt(3) == 0) {
                    this.tame(player);
                    this.setOrderedToSit(true);
                    this.setSitting(true);
                    this.getNavigation().stop();
                    this.setTarget(null);
                    this.setHealth(this.getMaxHealth());
                    this.level().broadcastEntityEvent(this, (byte) 7);
                    this.level().broadcastEntityEvent(this, EVENT_SIT_DOWN);
                } else {
                    this.level().broadcastEntityEvent(this, (byte) 6);
                }
            }

            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }

        if (this.isOwnedBy(player)) {
            if (stack.is(ItemTags.FLOWERS) && this.getHealth() < this.getMaxHealth()) {
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                this.heal(4.0F);
                this.level().addParticle(ParticleTypes.HEART, this.getX(), this.getY() + 0.8D, this.getZ(), 0.0D, 0.1D, 0.0D);
                return InteractionResult.sidedSuccess(this.level().isClientSide);
            }

            if (!this.level().isClientSide) {
                boolean sit = !this.isOrderedToSit();
                this.setOrderedToSit(sit);
                this.setSitting(sit);
                this.getNavigation().stop();
                this.setTarget(null);
                this.level().broadcastEntityEvent(this, sit ? EVENT_SIT_DOWN : EVENT_STAND_UP);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }

        return super.mobInteract(player, hand);
    }

    private void setSitting(boolean sitting) {
        this.entityData.set(SITTING, sitting);
        this.setInSittingPose(sitting);
    }

    @Override
    public void setOrderedToSit(boolean sitting) {
        super.setOrderedToSit(sitting);
        this.setSitting(sitting);
    }

    public boolean isOrderedToSit() {
        return this.entityData.get(SITTING);
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == EVENT_SIT_DOWN) {
            this.sitDownTicks = SIT_TRANSITION_TICKS;
            this.standUpTicks = 0;
            return;
        }
        if (id == EVENT_STAND_UP) {
            this.standUpTicks = SIT_TRANSITION_TICKS;
            this.sitDownTicks = 0;
            return;
        }
        if (id == EVENT_ATTACK) {
            this.attackTicks = ATTACK_ANIMATION_TICKS;
            return;
        }
        super.handleEntityEvent(id);
    }

    @Override
    public void performRangedAttack(LivingEntity target, float velocity) {
        if (!this.level().isClientSide) {
            BloomletPetalProjectileEntity projectile = new BloomletPetalProjectileEntity(this.level(), this, target);
            projectile.setPos(this.getX(), this.getY() + 0.65D, this.getZ());
            this.level().addFreshEntity(projectile);
            this.level().broadcastEntityEvent(this, EVENT_ATTACK);
            this.attackTicks = ATTACK_ANIMATION_TICKS;
        }
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        BloomletEntity bloomlet = HPEntities.BLOOMLET.get().create(level);
        if (bloomlet != null && this.isTame()) {
            bloomlet.setOwnerUUID(this.getOwnerUUID());
            bloomlet.setTame(true, true);
        }
        return bloomlet;
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(HPItems.BLOOMLET_SPAWN_EGG.get());
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("OrderedToSit", this.isOrderedToSit());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setOrderedToSit(tag.getBoolean("OrderedToSit"));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "bloomlet_controller", 4, this::handleAnimations));
    }

    private <E extends BloomletEntity> PlayState handleAnimations(AnimationState<E> animationState) {
        if (this.attackTicks > 0) {
            return animationState.setAndContinue(ATTACK);
        }
        if (this.standUpTicks > 0) {
            return animationState.setAndContinue(STAND_UP);
        }
        if (this.sitDownTicks > 0) {
            return animationState.setAndContinue(SIT_DOWN);
        }
        if (this.isOrderedToSit() || this.isInSittingPose()) {
            return animationState.setAndContinue(SIT);
        }

        boolean moving = this.getDeltaMovement().horizontalDistanceSqr() > 1.0E-6D || animationState.isMoving();
        return animationState.setAndContinue(moving ? WALK : IDLE);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.animatableInstanceCache;
    }
}
