package com.overyourhead.hidden_places.common.network;

import com.overyourhead.hidden_places.HiddenPlacesMod;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class HPNetwork {
    private HPNetwork() {
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(HiddenPlacesMod.MOD_ID);

        registrar.playToClient(
                OpenWayfinderDialoguePayload.TYPE,
                OpenWayfinderDialoguePayload.STREAM_CODEC,
                OpenWayfinderDialoguePayload::handle
        );

        registrar.playToClient(
                OpenWayfinderTradePayload.TYPE,
                OpenWayfinderTradePayload.STREAM_CODEC,
                OpenWayfinderTradePayload::handle
        );

        registrar.playToClient(
                WayfinderTradeResultPayload.TYPE,
                WayfinderTradeResultPayload.STREAM_CODEC,
                WayfinderTradeResultPayload::handle
        );

        registrar.playToServer(
                WayfinderDialogueChoicePayload.TYPE,
                WayfinderDialogueChoicePayload.STREAM_CODEC,
                WayfinderDialogueChoicePayload::handle
        );

        registrar.playToServer(
                WayfinderTradePayload.TYPE,
                WayfinderTradePayload.STREAM_CODEC,
                WayfinderTradePayload::handle
        );

        registrar.playToServer(
                HearthboundHatTeleportPayload.TYPE,
                HearthboundHatTeleportPayload.STREAM_CODEC,
                HearthboundHatTeleportPayload::handle
        );
    }
}
