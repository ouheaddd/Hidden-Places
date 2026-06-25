package com.overyourhead.hidden_places.common.entity;

import com.overyourhead.hidden_places.core.registry.HPItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumSet;

public class MirageCreeperEntity extends Monster implements GeoEntity {
    private static final EntityDataAccessor<Integer> DATA_SWELL_DIR = SynchedEntityData.defineId(MirageCreeperEntity.class, EntityDataSerializers.INT);

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.creeper.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.creeper.walk");

    private static final int MAX_SWELL_TICKS = 30;
    private static final int FAKE_EXPLOSION_COOLDOWN_TICKS = 20;
    private static final float FAKE_EXPLOSION_CHANCE = 0.32F;
    private static final float EXPLOSION_RADIUS = 3.6F;

    private final AnimatableInstanceCache animatableInstanceCache = GeckoLibUtil.createInstanceCache(this);

    private int oldSwell;
    private int swell;
    private int fakeExplosionCooldownTicks;

    public MirageCreeperEntity(EntityType<? extends MirageCreeperEntity> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 34.0D)
                .add(Attributes.ARMOR, 2.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.27D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.15D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MirageCreeperSwellGoal(this));
        this.goalSelector.addGoal(3, new AvoidEntityGoal<>(this, Ocelot.class, 6.0F, 1.0D, 1.2D));
        this.goalSelector.addGoal(3, new AvoidEntityGoal<>(this, Cat.class, 6.0F, 1.0D, 1.2D));
        this.goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.0D, false));
        this.goalSelector.addGoal(5, new RandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_SWELL_DIR, -1);
    }

    @Override
    public void tick() {
        if (this.isAlive()) {
            this.oldSwell = this.swell;

            if (this.fakeExplosionCooldownTicks > 0) {
                this.fakeExplosionCooldownTicks--;
                this.setSwellDir(-1);
            }

            int swellDir = this.getSwellDir();
            if (swellDir > 0 && this.swell == 0) {
                this.playSound(SoundEvents.CREEPER_PRIMED, 1.0F, 0.5F);
            }

            this.swell += swellDir;
            if (this.swell < 0) {
                this.swell = 0;
            }

            if (this.swell >= MAX_SWELL_TICKS) {
                this.swell = MAX_SWELL_TICKS;
                if (!this.level().isClientSide) {
                    if (this.shouldFakeExplosion()) {
                        this.fakeExplosion();
                    } else {
                        this.explodeMirageCreeper();
                    }
                }
            }
        }

        super.tick();
    }

    public float getSwelling(float partialTick) {
        return Mth.lerp(partialTick, (float) this.oldSwell, (float) this.swell) / (float) (MAX_SWELL_TICKS - 2);
    }

    public int getSwellDir() {
        return this.entityData.get(DATA_SWELL_DIR);
    }

    public void setSwellDir(int state) {
        this.entityData.set(DATA_SWELL_DIR, state);
    }

    public boolean canStartSwell() {
        return this.fakeExplosionCooldownTicks <= 0;
    }

    private boolean shouldFakeExplosion() {
        return this.random.nextFloat() < FAKE_EXPLOSION_CHANCE;
    }

    private void fakeExplosion() {
        double oldX = this.getX();
        double oldY = this.getY();
        double oldZ = this.getZ();

        this.swell = 0;
        this.oldSwell = 0;
        this.fakeExplosionCooldownTicks = FAKE_EXPLOSION_COOLDOWN_TICKS;
        this.setSwellDir(-1);

        boolean teleported = this.teleportBehindAggroedPlayer();

        this.level().playSound(null, oldX, oldY, oldZ, SoundEvents.CREEPER_HURT, SoundSource.HOSTILE, 0.9F, 0.6F + this.random.nextFloat() * 0.15F);

        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.POOF, oldX, oldY + 0.55D, oldZ, 18, 0.45D, 0.45D, 0.45D, 0.04D);
            serverLevel.sendParticles(ParticleTypes.SMOKE, oldX, oldY + 0.55D, oldZ, 10, 0.35D, 0.35D, 0.35D, 0.02D);

            if (teleported) {
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 0.65F, 1.35F + this.random.nextFloat() * 0.15F);
                serverLevel.sendParticles(ParticleTypes.POOF, this.getX(), this.getY() + 0.55D, this.getZ(), 22, 0.35D, 0.45D, 0.35D, 0.04D);
                serverLevel.sendParticles(ParticleTypes.SMOKE, this.getX(), this.getY() + 0.55D, this.getZ(), 8, 0.25D, 0.35D, 0.25D, 0.02D);
            }
        }
    }

    private boolean teleportBehindAggroedPlayer() {
        LivingEntity target = this.getTarget();
        if (!(target instanceof Player) || !target.isAlive()) {
            return false;
        }

        Vec3 look = target.getLookAngle();
        Vec3 horizontalLook = new Vec3(look.x, 0.0D, look.z);
        if (horizontalLook.lengthSqr() < 1.0E-4D) {
            horizontalLook = Vec3.directionFromRotation(0.0F, target.getYRot());
        } else {
            horizontalLook = horizontalLook.normalize();
        }

        int baseY = Mth.floor(target.getY());
        for (int yOffset : new int[] {0, 1, -1, 2, -2}) {
            double y = baseY + yOffset;

            for (int distanceStep = 0; distanceStep < 3; distanceStep++) {
                double distance = 2.25D + distanceStep * 0.7D;
                double x = target.getX() - horizontalLook.x * distance;
                double z = target.getZ() - horizontalLook.z * distance;

                if (this.tryTeleportToSafeSpot(x, y, z, target)) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean tryTeleportToSafeSpot(double x, double y, double z, LivingEntity target) {
        BlockPos feetPos = BlockPos.containing(x, y, z);
        if (!this.level().getWorldBorder().isWithinBounds(feetPos)) {
            return false;
        }

        BlockPos groundPos = feetPos.below();
        if (!this.level().getBlockState(groundPos).isFaceSturdy(this.level(), groundPos, Direction.UP)) {
            return false;
        }

        BlockPos headPos = feetPos.above();
        if (!this.level().getBlockState(feetPos).getCollisionShape(this.level(), feetPos).isEmpty()) {
            return false;
        }
        if (!this.level().getBlockState(headPos).getCollisionShape(this.level(), headPos).isEmpty()) {
            return false;
        }

        double targetX = feetPos.getX() + 0.5D;
        double targetY = feetPos.getY();
        double targetZ = feetPos.getZ() + 0.5D;
        if (target.distanceToSqr(targetX, targetY, targetZ) < 1.0D) {
            return false;
        }

        if (!this.level().noCollision(this, this.getBoundingBox().move(targetX - this.getX(), targetY - this.getY(), targetZ - this.getZ()))) {
            return false;
        }

        this.teleportTo(targetX, targetY, targetZ);
        this.getNavigation().stop();
        this.lookAt(target, 30.0F, 30.0F);
        return true;
    }

    private void explodeMirageCreeper() {
        if (this.isRemoved()) {
            return;
        }

        this.level().explode(this, this.getX(), this.getY(), this.getZ(), EXPLOSION_RADIUS, Level.ExplosionInteraction.NONE);
        this.discard();
    }

    @Override
    protected SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource damageSource) {
        return SoundEvents.CREEPER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.CREEPER_DEATH;
    }

    @Override
    protected float getSoundVolume() {
        return 0.8F;
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(HPItems.MIRAGE_CREEPER_SPAWN_EGG.get());
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putShort("Fuse", (short) this.swell);
        tag.putShort("FakeCooldown", (short) this.fakeExplosionCooldownTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.swell = tag.getShort("Fuse");
        this.oldSwell = this.swell;
        this.fakeExplosionCooldownTicks = tag.getShort("FakeCooldown");
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "mirage_creeper_controller", 4, this::handleAnimations));
    }

    private <E extends MirageCreeperEntity> PlayState handleAnimations(AnimationState<E> animationState) {
        boolean moving = this.getDeltaMovement().horizontalDistanceSqr() > 1.0E-6D || animationState.isMoving();
        return animationState.setAndContinue(moving ? WALK : IDLE);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.animatableInstanceCache;
    }

    private static class MirageCreeperSwellGoal extends Goal {
        private final MirageCreeperEntity creeper;
        private LivingEntity target;

        private MirageCreeperSwellGoal(MirageCreeperEntity creeper) {
            this.creeper = creeper;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            LivingEntity currentTarget = this.creeper.getTarget();
            return this.creeper.canStartSwell()
                    && (this.creeper.getSwellDir() > 0 || currentTarget != null && this.creeperDistanceToSqr(currentTarget) < 9.0D);
        }

        @Override
        public void start() {
            this.creeper.getNavigation().stop();
            this.target = this.creeper.getTarget();
        }

        @Override
        public void stop() {
            this.target = null;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (this.target == null) {
                this.creeper.setSwellDir(-1);
            } else if (!this.creeper.canStartSwell()) {
                this.creeper.setSwellDir(-1);
            } else if (this.creeperDistanceToSqr(this.target) > 49.0D) {
                this.creeper.setSwellDir(-1);
            } else if (!this.creeper.getSensing().hasLineOfSight(this.target)) {
                this.creeper.setSwellDir(-1);
            } else {
                this.creeper.setSwellDir(1);
            }
        }

        private double creeperDistanceToSqr(LivingEntity entity) {
            return this.creeper.distanceToSqr(entity);
        }
    }
}
