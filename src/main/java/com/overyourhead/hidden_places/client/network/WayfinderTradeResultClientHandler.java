package com.overyourhead.hidden_places.client.network;

import com.overyourhead.hidden_places.client.screen.TestWayfinderTradeResultScreen;
import com.overyourhead.hidden_places.client.screen.TestWayfinderTradeScreen;
import com.overyourhead.hidden_places.common.entity.TestWayfinderEntity;
import com.overyourhead.hidden_places.common.npc.WayfinderProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

public final class WayfinderTradeResultClientHandler {
    private WayfinderTradeResultClientHandler() {
    }

    public static void open(int entityId, int resultCode) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.screen instanceof TestWayfinderTradeScreen tradeScreen) {
            tradeScreen.openResult(resultCode);
            return;
        }

        WayfinderProfile profile = WayfinderProfile.TEST_WAYFINDER;
        if (minecraft.level != null) {
            Entity entity = minecraft.level.getEntity(entityId);
            if (entity instanceof TestWayfinderEntity wayfinder) {
                profile = wayfinder.getProfile();
            }
        }

        minecraft.setScreen(new TestWayfinderTradeResultScreen(entityId, profile, resultCode));
    }
}
