package com.overyourhead.hidden_places.common.entity.projectile;

import com.overyourhead.hidden_places.core.registry.HPEntities;
import com.overyourhead.hidden_places.core.registry.HPParticleTypes;
import com.overyourhead.hidden_places.core.registry.HPSoundEvents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
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

import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;

public class BloomletPetalProjectileEntity extends Entity implements GeoEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");

    private static final double HOMING_SPEED = 0.32D;
    private static final double HOMING_TURN_STRENGTH = 0.28D;
    private static final double HIT_RADIUS = 0.55D;
    private static final double PARTICLE_Y_OFFSET = 0.32D;
    private static final int MAX_LIFETIME_TICKS = 260;

    private final AnimatableInstanceCache animatableInstanceCache = GeckoLibUtil.createInstanceCache(this);

    @Nullable
    private UUID ownerUuid;
    @Nullable
    private UUID targetUuid;

    public BloomletPetalProjectileEntity(EntityType<? extends BloomletPetalProjectileEntity> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public BloomletPetalProjectileEntity(Level level, LivingEntity shooter, LivingEntity target) {
        this(HPEntities.BLOOMLET_PETAL_PROJECTILE.get(), level);
        this.ownerUuid = shooter.getUUID();
        this.targetUuid = target.getUUID();

        Vec3 start = shooter.position().add(0.0D, 0.85D, 0.0D);
        Vec3 targetPos = this.targetPoint(target);
        Vec3 initial = targetPos.subtract(start).normalize().scale(HOMING_SPEED);
        this.setPos(start.x, start.y, start.z);
        this.setDeltaMovement(initial);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        this.noPhysics = true;
        this.setNoGravity(true);
        super.tick();

        if (this.tickCount > MAX_LIFETIME_TICKS) {
            this.breakProjectile();
            return;
        }

        if (!this.level().isClientSide) {
            LivingEntity target = this.getTrackedTarget();
            if (target == null || !target.isAlive()) {
                this.breakProjectile();
                return;
            }

            Vec3 toTarget = this.targetPoint(target).subtract(this.position());
            if (toTarget.lengthSqr() <= HIT_RADIUS * HIT_RADIUS) {
                this.hitTarget(target);
                return;
            }

            Vec3 desired = toTarget.normalize().scale(HOMING_SPEED);
            Vec3 current = this.getDeltaMovement();
            if (current.lengthSqr() < 1.0E-6D) {
                current = desired;
            }

            Vec3 adjusted = current.scale(1.0D - HOMING_TURN_STRENGTH).add(desired.scale(HOMING_TURN_STRENGTH));
            if (adjusted.lengthSqr() < 1.0E-6D) {
                adjusted = desired;
            } else {
                adjusted = adjusted.normalize().scale(HOMING_SPEED);
            }

            this.setDeltaMovement(adjusted);
            this.move(MoverType.SELF, adjusted);
            this.hasImpulse = true;

            Optional<Entity> hit = this.findEntityHit(adjusted);
            if (hit.isPresent() && hit.get() instanceof LivingEntity livingTarget) {
                this.hitTarget(livingTarget);
                return;
            }

            this.spawnTrailParticles();
        } else {
            Vec3 motion = this.getDeltaMovement();
            this.move(MoverType.SELF, motion);
            if (this.tickCount % 2 == 0) {
                Vec3 particlePos = this.particlePoint();
                this.level().addParticle(HPParticleTypes.BLOOMLET_SPORE.get(), particlePos.x, particlePos.y, particlePos.z, 0.0D, 0.005D, 0.0D);
            }
        }

        this.updateRotationFromMovement();
    }

    private Vec3 targetPoint(LivingEntity target) {
        return target.position().add(0.0D, target.getBbHeight() * 0.55D, 0.0D);
    }

    private Optional<Entity> findEntityHit(Vec3 motion) {
        AABB searchBox = this.getBoundingBox().expandTowards(motion).inflate(0.45D);
        return this.level().getEntities(this, searchBox, this::canHitEntity)
                .stream()
                .min(Comparator.comparingDouble(entity -> entity.distanceToSqr(this)));
    }

    private boolean canHitEntity(Entity entity) {
        if (!(entity instanceof LivingEntity living) || !living.isAlive()) {
            return false;
        }
        if (this.ownerUuid != null && entity.getUUID().equals(this.ownerUuid)) {
            return false;
        }
        Entity owner = this.getOwnerEntity();
        return owner == null || !owner.isAlliedTo(entity);
    }

    @Nullable
    private LivingEntity getTrackedTarget() {
        if (this.targetUuid == null || !(this.level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        Entity entity = serverLevel.getEntity(this.targetUuid);
        return entity instanceof LivingEntity living ? living : null;
    }

    @Nullable
    private Entity getOwnerEntity() {
        if (this.ownerUuid == null || !(this.level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        return serverLevel.getEntity(this.ownerUuid);
    }

    private void hitTarget(LivingEntity target) {
        Entity owner = this.getOwnerEntity();
        if (target == owner) {
            return;
        }
        if (owner != null && owner.isAlliedTo(target)) {
            return;
        }

        if (target.hurt(this.damageSources().mobProjectile(this, owner instanceof LivingEntity living ? living : null), 3.0F)) {
            target.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 0), owner);
        }
        this.breakProjectile();
    }

    private Vec3 particlePoint() {
        return this.position().add(0.0D, PARTICLE_Y_OFFSET, 0.0D);
    }

    private void spawnTrailParticles() {
        if (this.level() instanceof ServerLevel serverLevel) {
            Vec3 particlePos = this.particlePoint();
            serverLevel.sendParticles(HPParticleTypes.BLOOMLET_SPORE.get(), particlePos.x, particlePos.y, particlePos.z, 2, 0.03D, 0.03D, 0.03D, 0.01D);
            if (this.tickCount % 3 == 0) {
                serverLevel.sendParticles(HPParticleTypes.BLOOMLET_SPORE.get(), particlePos.x, particlePos.y, particlePos.z, 1, 0.06D, 0.06D, 0.06D, 0.02D);
            }
        }
    }

    private void breakProjectile() {
        if (!this.level().isClientSide && this.level() instanceof ServerLevel serverLevel) {
            Vec3 particlePos = this.particlePoint();
            this.level().playSound(null, particlePos.x, particlePos.y, particlePos.z, HPSoundEvents.BLOOMLET_PROJECTILE_POP.get(), SoundSource.NEUTRAL, 0.75F, 0.9F + this.random.nextFloat() * 0.18F);
            serverLevel.sendParticles(HPParticleTypes.BLOOMLET_SPORE.get(), particlePos.x, particlePos.y, particlePos.z, 18, 0.18D, 0.18D, 0.18D, 0.03D);
            serverLevel.sendParticles(HPParticleTypes.BLOOMLET_SPORE.get(), particlePos.x, particlePos.y, particlePos.z, 10, 0.08D, 0.08D, 0.08D, 0.01D);
        }
        this.discard();
    }

    private void updateRotationFromMovement() {
        Vec3 motion = this.getDeltaMovement();
        if (motion.lengthSqr() <= 1.0E-6D) {
            return;
        }

        double horizontal = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
        this.setYRot((float) (Mth.atan2(motion.x, motion.z) * (180F / Math.PI)));
        this.setXRot((float) (Mth.atan2(motion.y, horizontal) * (180F / Math.PI)));
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean isAttackable() {
        return true;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.level().isClientSide) {
            return true;
        }

        if (source.getEntity() instanceof LivingEntity) {
            this.breakProjectile();
            return true;
        }

        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        if (this.ownerUuid != null) {
            tag.putUUID("Owner", this.ownerUuid);
        }
        if (this.targetUuid != null) {
            tag.putUUID("Target", this.targetUuid);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            this.ownerUuid = tag.getUUID("Owner");
        }
        if (tag.hasUUID("Target")) {
            this.targetUuid = tag.getUUID("Target");
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "bloomlet_petal_controller", 0, this::handleAnimations));
    }

    private <E extends BloomletPetalProjectileEntity> PlayState handleAnimations(AnimationState<E> animationState) {
        return animationState.setAndContinue(IDLE);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.animatableInstanceCache;
    }
}
