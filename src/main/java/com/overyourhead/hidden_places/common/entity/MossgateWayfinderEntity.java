package com.overyourhead.hidden_places.common.entity;

import com.overyourhead.hidden_places.common.npc.WayfinderProfile;
import com.overyourhead.hidden_places.core.registry.HPItems;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class MossgateWayfinderEntity extends TestWayfinderEntity {
    public MossgateWayfinderEntity(EntityType<? extends MossgateWayfinderEntity> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.ARMOR, 4.0D)
                .add(Attributes.ATTACK_DAMAGE, 0.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.16D)
                .add(Attributes.FOLLOW_RANGE, 16.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    public WayfinderProfile getProfile() {
        return WayfinderProfile.MOSSGATE_WAYFINDER;
    }

    @Override
    public void setHostile(boolean hostile) {
        super.setHostile(false);
    }

    @Override
    public void provoke(net.minecraft.world.entity.player.Player player) {
        this.setDialogueNode(player, this.getProfile().tradeNodeId());
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() instanceof net.minecraft.world.entity.player.Player player && !this.level().isClientSide) {
            player.displayClientMessage(Component.translatable("message.hidden_places.mossgate_wayfinder.peaceful"), true);
        }
        return false;
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(HPItems.MOSSGATE_WAYFINDER_SPAWN_EGG.get());
    }
}
