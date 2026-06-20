package com.overyourhead.hidden_places.client.network;

import com.overyourhead.hidden_places.client.screen.TestWayfinderTradeScreen;
import com.overyourhead.hidden_places.common.entity.TestWayfinderEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

public final class OpenWayfinderTradeClientHandler {
    private OpenWayfinderTradeClientHandler() {
    }

    public static void open(int entityId) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        Entity entity = minecraft.level.getEntity(entityId);
        if (entity instanceof TestWayfinderEntity wayfinder && wayfinder.hasTrade()) {
            minecraft.setScreen(new TestWayfinderTradeScreen(entityId, wayfinder.getProfile()));
        }
    }
}
