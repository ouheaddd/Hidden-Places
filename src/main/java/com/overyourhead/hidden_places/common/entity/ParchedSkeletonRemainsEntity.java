package com.overyourhead.hidden_places.common.entity;

import com.overyourhead.hidden_places.core.registry.HPItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public class ParchedSkeletonRemainsEntity extends Entity implements GeoEntity {
    public static final int CEILING_VARIANT = 3;
    public static final int[] FLOOR_VARIANTS = new int[] {1, 2, 4, 5};

    private static final EntityDataAccessor<Integer> DATA_VARIANT = SynchedEntityData.defineId(ParchedSkeletonRemainsEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_CEILING = SynchedEntityData.defineId(ParchedSkeletonRemainsEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> DATA_PLACED_YAW = SynchedEntityData.defineId(ParchedSkeletonRemainsEntity.class, EntityDataSerializers.FLOAT);

    private final AnimatableInstanceCache animatableInstanceCache = GeckoLibUtil.createInstanceCache(this);

    public ParchedSkeletonRemainsEntity(EntityType<? extends ParchedSkeletonRemainsEntity> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_VARIANT, 1);
        builder.define(DATA_CEILING, false);
        builder.define(DATA_PLACED_YAW, 0.0F);
    }

    @Override
    public void tick() {
        this.noPhysics = true;
        this.setNoGravity(true);
        super.tick();
    }

    public int getVariant() {
        return this.entityData.get(DATA_VARIANT);
    }

    public void setVariant(int variant) {
        this.entityData.set(DATA_VARIANT, clampVariant(variant));
        this.refreshDimensions();
    }

    public boolean isCeilingMounted() {
        return this.entityData.get(DATA_CEILING);
    }

    public void setCeilingMounted(boolean ceilingMounted) {
        this.entityData.set(DATA_CEILING, ceilingMounted);
        if (ceilingMounted) {
            this.setVariant(CEILING_VARIANT);
        }
        this.refreshDimensions();
    }

    public float getPlacedYaw() {
        return this.entityData.get(DATA_PLACED_YAW);
    }

    public void setPlacedYaw(float yaw) {
        float wrappedYaw = Mth.wrapDegrees(yaw);
        this.entityData.set(DATA_PLACED_YAW, wrappedYaw);
        this.setYRot(0.0F);
        this.yRotO = 0.0F;
    }

    private static int clampVariant(int variant) {
        return switch (variant) {
            case 1, 2, 3, 4, 5 -> variant;
            default -> 1;
        };
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return this.isCeilingMounted() ? EntityDimensions.fixed(0.9F, 1.15F) : EntityDimensions.fixed(1.45F, 0.32F);
    }

    @Override
    protected AABB makeBoundingBox() {
        if (this.isCeilingMounted()) {
            double halfWidth = 0.45D;
            return new AABB(
                    this.getX() - halfWidth,
                    this.getY() - 1.15D,
                    this.getZ() - halfWidth,
                    this.getX() + halfWidth,
                    this.getY(),
                    this.getZ() + halfWidth
            );
        }

        double halfWidth = 0.725D;
        return new AABB(
                this.getX() - halfWidth,
                this.getY(),
                this.getZ() - halfWidth,
                this.getX() + halfWidth,
                this.getY() + 0.32D,
                this.getZ() + halfWidth
        );
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.level().isClientSide || this.isRemoved()) {
            return true;
        }

        if (source.getEntity() instanceof Player player && player.getAbilities().instabuild) {
            this.discard();
            this.playBreakSound();
            return true;
        }

        this.spawnAtLocation(new ItemStack(HPItems.PARCHED_SKELETON_REMAINS.get()));
        this.discard();
        this.playBreakSound();
        return true;
    }

    private void playBreakSound() {
        this.level().playSound(null, this.blockPosition(), SoundEvents.BONE_BLOCK_BREAK, SoundSource.BLOCKS, 0.75F, 0.85F + this.random.nextFloat() * 0.2F);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Variant", this.getVariant());
        tag.putBoolean("Ceiling", this.isCeilingMounted());
        tag.putFloat("Yaw", this.getPlacedYaw());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.setVariant(tag.getInt("Variant"));
        this.setCeilingMounted(tag.getBoolean("Ceiling"));
        if (tag.contains("Yaw")) {
            this.setPlacedYaw(tag.getFloat("Yaw"));
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Static decoration. Animations can be added later without changing the entity logic.
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.animatableInstanceCache;
    }
}
