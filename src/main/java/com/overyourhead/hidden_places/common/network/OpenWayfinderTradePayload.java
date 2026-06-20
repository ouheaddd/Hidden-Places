package com.overyourhead.hidden_places.common.network;

import com.overyourhead.hidden_places.HiddenPlacesMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record OpenWayfinderTradePayload(int entityId) implements CustomPacketPayload {
    public static final Type<OpenWayfinderTradePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(HiddenPlacesMod.MOD_ID, "open_wayfinder_trade")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenWayfinderTradePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            OpenWayfinderTradePayload::entityId,
            OpenWayfinderTradePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenWayfinderTradePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> openClientScreen(payload.entityId()));
    }

    private static void openClientScreen(int entityId) {
        try {
            Class<?> handler = Class.forName("com.overyourhead.hidden_places.client.network.OpenWayfinderTradeClientHandler");
            handler.getMethod("open", int.class).invoke(null, entityId);
        } catch (ReflectiveOperationException exception) {
            HiddenPlacesMod.LOGGER.error("Failed to open Test Wayfinder trade screen", exception);
        }
    }
}
