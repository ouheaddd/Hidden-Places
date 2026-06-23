package com.overyourhead.hidden_places.core.registry;

import com.overyourhead.hidden_places.HiddenPlacesMod;
import com.overyourhead.hidden_places.common.entity.BloomletEntity;
import com.overyourhead.hidden_places.common.entity.MossgateWayfinderEntity;
import com.overyourhead.hidden_places.common.entity.SunveilWayfinderEntity;
import com.overyourhead.hidden_places.common.entity.TestWayfinderEntity;
import com.overyourhead.hidden_places.common.entity.projectile.BloomletPetalProjectileEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class HPEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, HiddenPlacesMod.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<TestWayfinderEntity>> TEST_WAYFINDER =
            ENTITY_TYPES.register("test_wayfinder", () -> EntityType.Builder
                    .of(TestWayfinderEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.95F)
                    .eyeHeight(1.74F)
                    .clientTrackingRange(8)
                    .updateInterval(3)
                    .build("test_wayfinder"));

    public static final DeferredHolder<EntityType<?>, EntityType<MossgateWayfinderEntity>> MOSSGATE_WAYFINDER =
            ENTITY_TYPES.register("mossgate_wayfinder", () -> EntityType.Builder
                    .of(MossgateWayfinderEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.95F)
                    .eyeHeight(1.74F)
                    .clientTrackingRange(8)
                    .updateInterval(3)
                    .build("mossgate_wayfinder"));

    public static final DeferredHolder<EntityType<?>, EntityType<SunveilWayfinderEntity>> SUNVEIL_WAYFINDER =
            ENTITY_TYPES.register("sunveil_wayfinder", () -> EntityType.Builder
                    .of(SunveilWayfinderEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.95F)
                    .eyeHeight(1.74F)
                    .clientTrackingRange(8)
                    .updateInterval(3)
                    .build("sunveil_wayfinder"));

    public static final DeferredHolder<EntityType<?>, EntityType<BloomletEntity>> BLOOMLET =
            ENTITY_TYPES.register("bloomlet", () -> EntityType.Builder
                    .of(BloomletEntity::new, MobCategory.CREATURE)
                    .sized(0.8F, 1.0F)
                    .eyeHeight(0.65F)
                    .clientTrackingRange(8)
                    .updateInterval(3)
                    .build("bloomlet"));

    public static final DeferredHolder<EntityType<?>, EntityType<BloomletPetalProjectileEntity>> BLOOMLET_PETAL_PROJECTILE =
            ENTITY_TYPES.register("bloomlet_petal_projectile", () -> EntityType.Builder
                    .<BloomletPetalProjectileEntity>of(BloomletPetalProjectileEntity::new, MobCategory.MISC)
                    .sized(0.55F, 0.55F)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .build("bloomlet_petal_projectile"));

    private HPEntities() {
    }

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }

    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(TEST_WAYFINDER.get(), TestWayfinderEntity.createAttributes().build());
        event.put(MOSSGATE_WAYFINDER.get(), MossgateWayfinderEntity.createAttributes().build());
        event.put(SUNVEIL_WAYFINDER.get(), SunveilWayfinderEntity.createAttributes().build());
        event.put(BLOOMLET.get(), BloomletEntity.createAttributes().build());
    }

    public static void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        // Test Wayfinder is spawned manually for now through spawn egg, /summon, or structures.
    }
}
