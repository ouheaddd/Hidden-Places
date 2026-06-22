package com.overyourhead.hidden_places.common.entity.ai;

import com.overyourhead.hidden_places.common.entity.BloomletEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public class BloomletRangedAttackGoal extends Goal {
    private final BloomletEntity bloomlet;

    public BloomletRangedAttackGoal(BloomletEntity bloomlet) {
        this.bloomlet = bloomlet;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = this.bloomlet.getTarget();
        return target != null
                && target.isAlive()
                && !this.bloomlet.isOrderedToSit()
                && this.bloomlet.canStartBudAttack(target);
    }

    @Override
    public boolean canContinueToUse() {
        return false;
    }

    @Override
    public void start() {
        LivingEntity target = this.bloomlet.getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }

        this.bloomlet.getNavigation().stop();
        this.bloomlet.getLookControl().setLookAt(target, 30.0F, 30.0F);
        this.bloomlet.startBudAttack(target);
    }
}
