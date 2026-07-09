package com.overyourhead.hidden_places.common.item;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.block.Blocks;

public class FrostboundCleaverItem extends Item {
    private static final double ATTACK_DAMAGE_TOOLTIP_VALUE = 9.0D;
    private static final double ATTACK_SPEED_TOOLTIP_VALUE = 0.7D;
    private static final double PLAYER_BASE_ATTACK_DAMAGE = 1.0D;
    private static final double PLAYER_BASE_ATTACK_SPEED = 4.0D;

    private static final float FREEZE_PROC_CHANCE = 0.35F;
    private static final int EFFECT_DURATION_TICKS = 20 * 4;
    private static final int FREEZING_TICKS = 20 * 6;

    public FrostboundCleaverItem(Properties properties) {
        super(properties.attributes(createAttributes()));
    }

    private static ItemAttributeModifiers createAttributes() {
        return ItemAttributeModifiers.builder()
                .add(
                        Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(
                                BASE_ATTACK_DAMAGE_ID,
                                ATTACK_DAMAGE_TOOLTIP_VALUE - PLAYER_BASE_ATTACK_DAMAGE,
                                AttributeModifier.Operation.ADD_VALUE
                        ),
                        EquipmentSlotGroup.MAINHAND
                )
                .add(
                        Attributes.ATTACK_SPEED,
                        new AttributeModifier(
                                BASE_ATTACK_SPEED_ID,
                                ATTACK_SPEED_TOOLTIP_VALUE - PLAYER_BASE_ATTACK_SPEED,
                                AttributeModifier.Operation.ADD_VALUE
                        ),
                        EquipmentSlotGroup.MAINHAND
                )
                .build();
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.level().isClientSide) {
            stack.hurtAndBreak(1, attacker, EquipmentSlot.MAINHAND);

            if (attacker.getRandom().nextFloat() < FREEZE_PROC_CHANCE) {
                applyFrostProc(target, attacker);
            }
        }

        return true;
    }

    private static void applyFrostProc(LivingEntity target, LivingEntity attacker) {
        target.addEffect(new MobEffectInstance(
                MobEffects.MOVEMENT_SLOWDOWN,
                EFFECT_DURATION_TICKS,
                1,
                true,
                false,
                false
        ), attacker);
        target.addEffect(new MobEffectInstance(
                MobEffects.DIG_SLOWDOWN,
                EFFECT_DURATION_TICKS,
                0,
                true,
                false,
                false
        ), attacker);
        target.setTicksFrozen(Math.max(target.getTicksFrozen(), FREEZING_TICKS));

        if (attacker.level() instanceof ServerLevel serverLevel) {
            double x = target.getX();
            double y = target.getY() + target.getBbHeight() * 0.55D;
            double z = target.getZ();
            double horizontalSpread = Math.max(0.18D, target.getBbWidth() * 0.35D);
            double verticalSpread = Math.max(0.25D, target.getBbHeight() * 0.25D);

            serverLevel.sendParticles(
                    ParticleTypes.SNOWFLAKE,
                    x,
                    y + 0.25D,
                    z,
                    16,
                    horizontalSpread,
                    verticalSpread,
                    horizontalSpread,
                    0.015D
            );
            serverLevel.sendParticles(
                    new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ICE.defaultBlockState()),
                    x,
                    y,
                    z,
                    8,
                    horizontalSpread * 0.75D,
                    verticalSpread * 0.5D,
                    horizontalSpread * 0.75D,
                    0.06D
            );
            serverLevel.playSound(
                    null,
                    target.getX(),
                    target.getY(),
                    target.getZ(),
                    SoundEvents.GLASS_HIT,
                    SoundSource.PLAYERS,
                    0.18F,
                    1.45F + serverLevel.random.nextFloat() * 0.15F
            );
        }
    }
}
