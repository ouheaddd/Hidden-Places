package com.overyourhead.hidden_places.common.entity.projectile;

import com.overyourhead.hidden_places.core.registry.HPEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class BloomletPetalProjectileEntity extends ThrowableItemProjectile {
    @Nullable
    private UUID targetUuid;

    public BloomletPetalProjectileEntity(EntityType<? extends BloomletPetalProjectileEntity> entityType, Level level) {
        super(entityType, level);
    }

    public BloomletPetalProjectileEntity(Level level, LivingEntity shooter, LivingEntity target) {
        super(HPEntities.BLOOMLET_PETAL_PROJECTILE.get(), shooter, level);
        this.targetUuid = target.getUUID();
        Vec3 initial = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D)
                .subtract(shooter.position().add(0.0D, 0.65D, 0.0D))
                .normalize()
                .scale(0.45D);
        this.setDeltaMovement(initial);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.tickCount > 200) {
            this.discard();
            return;
        }

        if (!this.level().isClientSide) {
            LivingEntity target = this.getTrackedTarget();
            if (target != null && target.isAlive()) {
                Vec3 targetPos = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
                Vec3 desired = targetPos.subtract(this.position()).normalize().scale(0.45D);
                Vec3 adjusted = this.getDeltaMovement().scale(0.82D).add(desired.scale(0.30D));
                double maxSpeed = 0.55D;
                if (adjusted.lengthSqr() > maxSpeed * maxSpeed) {
                    adjusted = adjusted.normalize().scale(maxSpeed);
                }
                this.setDeltaMovement(adjusted);
                this.hasImpulse = true;
            }

            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, this.getX(), this.getY(), this.getZ(), 1, 0.01D, 0.01D, 0.01D, 0.0D);
            }
        } else {
            this.level().addParticle(ParticleTypes.SPORE_BLOSSOM_AIR, this.getX(), this.getY(), this.getZ(), 0.0D, 0.0D, 0.0D);
        }

        Vec3 motion = this.getDeltaMovement();
        double horizontal = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
        this.setYRot((float) (Mth.atan2(motion.x, motion.z) * (180F / Math.PI)));
        this.setXRot((float) (Mth.atan2(motion.y, horizontal) * (180F / Math.PI)));
    }

    @Nullable
    private LivingEntity getTrackedTarget() {
        if (this.targetUuid == null || !(this.level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        Entity entity = serverLevel.getEntity(this.targetUuid);
        return entity instanceof LivingEntity living ? living : null;
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        Entity target = result.getEntity();
        Entity owner = this.getOwner();
        if (target == owner) {
            return;
        }
        if (owner != null && owner.isAlliedTo(target)) {
            return;
        }
        if (target.hurt(this.damageSources().mobProjectile(this, owner instanceof LivingEntity living ? living : null), 3.0F)
                && target instanceof LivingEntity livingTarget) {
            livingTarget.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 0), owner);
        }
        this.discard();
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (result.getType() != HitResult.Type.ENTITY) {
            this.discard();
        }
    }

    @Override
    protected Item getDefaultItem() {
        return Items.PINK_PETALS;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.targetUuid != null) {
            tag.putUUID("Target", this.targetUuid);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Target")) {
            this.targetUuid = tag.getUUID("Target");
        }
    }
}
