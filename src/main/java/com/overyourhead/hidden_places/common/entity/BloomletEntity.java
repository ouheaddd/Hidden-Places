package com.overyourhead.hidden_places.common.entity;

import com.overyourhead.hidden_places.common.entity.ai.BloomletRangedAttackGoal;
import com.overyourhead.hidden_places.common.entity.projectile.BloomletPetalProjectileEntity;
import com.overyourhead.hidden_places.core.registry.HPEntities;
import com.overyourhead.hidden_places.core.registry.HPItems;
import com.overyourhead.hidden_places.core.registry.HPSoundEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
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
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

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

    // Tune these to the real Blockbench attack timing.
    private static final int ATTACK_TOTAL_TICKS = 100;
    private static final int PROJECTILE_RELEASE_DELAY_TICKS = 70;
    private static final int ATTACK_COOLDOWN_TICKS = 140;

    private final AnimatableInstanceCache animatableInstanceCache = GeckoLibUtil.createInstanceCache(this);

    private int sitDownTicks;
    private int standUpTicks;
    private int attackTicks;
    private int projectileReleaseTicks;
    private int attackCooldownTicks;
    private boolean projectileReleased;
    @Nullable
    private UUID pendingAttackTargetUuid;

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
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.35D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(2, new BloomletRangedAttackGoal(this));
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
        if (this.attackCooldownTicks > 0) {
            this.attackCooldownTicks--;
        }

        if (this.attackTicks > 0) {
            this.attackTicks--;
            this.getNavigation().stop();
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.2D, 1.0D, 0.2D));

            LivingEntity pendingTarget = this.getPendingAttackTarget();
            if (pendingTarget != null && pendingTarget.isAlive()) {
                this.faceAttackTarget(pendingTarget);
            }

            if (!this.level().isClientSide) {
                if (this.projectileReleaseTicks > 0) {
                    this.projectileReleaseTicks--;
                }

                if (!this.projectileReleased && this.projectileReleaseTicks <= 0) {
                    LivingEntity target = this.getPendingAttackTarget();
                    if (target != null && target.isAlive()) {
                        this.spawnBudProjectile(target);
                    }
                    this.projectileReleased = true;
                }

                if (this.attackTicks <= 0) {
                    this.attackCooldownTicks = ATTACK_COOLDOWN_TICKS;
                    this.pendingAttackTargetUuid = null;
                    this.projectileReleaseTicks = 0;
                    this.projectileReleased = false;
                }
            }
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
                    this.playBloomletSound(HPSoundEvents.BLOOMLET_TAME_SUCCESS.get(), 0.9F, 1.0F + (this.random.nextFloat() - 0.5F) * 0.08F);
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
                this.cancelPendingAttack();
                this.level().broadcastEntityEvent(this, sit ? EVENT_SIT_DOWN : EVENT_STAND_UP);
                this.playBloomletSound(sit ? HPSoundEvents.BLOOMLET_SIT.get() : HPSoundEvents.BLOOMLET_STAND_UP.get(), 0.65F, 0.95F + this.random.nextFloat() * 0.12F);
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
            this.attackTicks = ATTACK_TOTAL_TICKS;
            this.sitDownTicks = 0;
            this.standUpTicks = 0;
            return;
        }
        super.handleEntityEvent(id);
    }

    @Override
    public void performRangedAttack(LivingEntity target, float velocity) {
        this.startBudAttack(target);
    }

    public boolean canStartBudAttack(LivingEntity target) {
        return !this.level().isClientSide
                && target != null
                && target.isAlive()
                && !this.isOrderedToSit()
                && this.attackTicks <= 0
                && this.attackCooldownTicks <= 0;
    }

    public void startBudAttack(LivingEntity target) {
        if (!this.canStartBudAttack(target)) {
            return;
        }

        this.pendingAttackTargetUuid = target.getUUID();
        this.attackTicks = ATTACK_TOTAL_TICKS;
        this.projectileReleaseTicks = PROJECTILE_RELEASE_DELAY_TICKS;
        this.projectileReleased = false;
        this.getNavigation().stop();
        this.faceAttackTarget(target);
        this.playBloomletSound(HPSoundEvents.BLOOMLET_ATTACK_CHARGE.get(), 0.85F, 0.95F + this.random.nextFloat() * 0.08F);
        this.level().broadcastEntityEvent(this, EVENT_ATTACK);
    }

    private void faceAttackTarget(LivingEntity target) {
        double dx = target.getX() - this.getX();
        double dz = target.getZ() - this.getZ();
        if (dx * dx + dz * dz > 1.0E-6D) {
            float yaw = (float) (Mth.atan2(dz, dx) * (180.0F / Math.PI)) - 90.0F;
            this.setYRot(yaw);
            this.yBodyRot = yaw;
            this.yHeadRot = yaw;
            this.yRotO = yaw;
            this.yBodyRotO = yaw;
            this.yHeadRotO = yaw;
        }

        this.getLookControl().setLookAt(target, 60.0F, 60.0F);
    }

    @Nullable
    private LivingEntity getPendingAttackTarget() {
        if (this.pendingAttackTargetUuid == null || !(this.level() instanceof ServerLevel serverLevel)) {
            return null;
        }

        Entity entity = serverLevel.getEntity(this.pendingAttackTargetUuid);
        return entity instanceof LivingEntity living ? living : null;
    }

    private void spawnBudProjectile(LivingEntity target) {
        Vec3 targetDirection = target.position().subtract(this.position());
        Vec3 horizontalDirection = new Vec3(targetDirection.x, 0.0D, targetDirection.z);
        if (horizontalDirection.lengthSqr() < 1.0E-6D) {
            horizontalDirection = this.getLookAngle();
        }
        horizontalDirection = horizontalDirection.normalize();

        Vec3 spawnPos = this.position()
                .add(0.0D, 0.88D, 0.0D)
                .add(horizontalDirection.x * 0.55D, 0.0D, horizontalDirection.z * 0.55D);

        BloomletPetalProjectileEntity projectile = new BloomletPetalProjectileEntity(this.level(), this, target);
        projectile.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
        this.level().addFreshEntity(projectile);
        this.playBloomletSound(HPSoundEvents.BLOOMLET_PROJECTILE_LAUNCH.get(), 0.8F, 0.95F + this.random.nextFloat() * 0.12F);
    }

    private void playBloomletSound(SoundEvent sound, float volume, float pitch) {
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(), sound, SoundSource.NEUTRAL, volume, pitch);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return HPSoundEvents.BLOOMLET_IDLE.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return HPSoundEvents.BLOOMLET_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return HPSoundEvents.BLOOMLET_DEATH.get();
    }

    @Override
    protected float getSoundVolume() {
        return 0.75F;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 360;
    }

    private void cancelPendingAttack() {
        this.attackTicks = 0;
        this.projectileReleaseTicks = 0;
        this.attackCooldownTicks = 0;
        this.projectileReleased = false;
        this.pendingAttackTargetUuid = null;
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
        this.cancelPendingAttack();
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
