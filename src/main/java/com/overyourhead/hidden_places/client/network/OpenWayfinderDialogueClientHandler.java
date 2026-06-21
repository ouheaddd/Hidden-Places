package com.overyourhead.hidden_places.client.network;

import com.overyourhead.hidden_places.client.screen.TestWayfinderDialogueScreen;
import com.overyourhead.hidden_places.common.entity.TestWayfinderEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

public final class OpenWayfinderDialogueClientHandler {
    private OpenWayfinderDialogueClientHandler() {
    }

    public static void open(int entityId, String nodeId) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        Entity entity = minecraft.level.getEntity(entityId);
        if (entity instanceof TestWayfinderEntity wayfinder) {
            minecraft.setScreen(new TestWayfinderDialogueScreen(wayfinder, nodeId));
        }
    }
}
