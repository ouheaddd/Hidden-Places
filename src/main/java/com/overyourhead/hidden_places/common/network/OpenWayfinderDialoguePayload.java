package com.overyourhead.hidden_places.common.network;

import com.overyourhead.hidden_places.HiddenPlacesMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record OpenWayfinderDialoguePayload(int entityId, int stageId) implements CustomPacketPayload {
    public static final Type<OpenWayfinderDialoguePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(HiddenPlacesMod.MOD_ID, "open_wayfinder_dialogue")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenWayfinderDialoguePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            OpenWayfinderDialoguePayload::entityId,
            ByteBufCodecs.VAR_INT,
            OpenWayfinderDialoguePayload::stageId,
            OpenWayfinderDialoguePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenWayfinderDialoguePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> openClientScreen(payload.entityId(), payload.stageId()));
    }

    private static void openClientScreen(int entityId, int stageId) {
        try {
            Class<?> handler = Class.forName("com.overyourhead.hidden_places.client.network.OpenWayfinderDialogueClientHandler");
            handler.getMethod("open", int.class, int.class).invoke(null, entityId, stageId);
        } catch (ReflectiveOperationException exception) {
            HiddenPlacesMod.LOGGER.error("Failed to open Test Wayfinder dialogue screen", exception);
        }
    }
}
