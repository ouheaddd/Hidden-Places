package com.overyourhead.hidden_places.common.network;

import com.overyourhead.hidden_places.HiddenPlacesMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record WayfinderTradeResultPayload(int entityId, int resultCode) implements CustomPacketPayload {
    public static final int SUCCESS = 0;
    public static final int NO_PAYMENT = 1;
    public static final int INVENTORY_FULL = 2;
    public static final int ALREADY_TRADED = 3;
    public static final int FAILED = 4;

    public static final Type<WayfinderTradeResultPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(HiddenPlacesMod.MOD_ID, "wayfinder_trade_result")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, WayfinderTradeResultPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            WayfinderTradeResultPayload::entityId,
            ByteBufCodecs.VAR_INT,
            WayfinderTradeResultPayload::resultCode,
            WayfinderTradeResultPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(WayfinderTradeResultPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> openClientResult(payload.entityId(), payload.resultCode()));
    }

    private static void openClientResult(int entityId, int resultCode) {
        try {
            Class<?> handler = Class.forName("com.overyourhead.hidden_places.client.network.WayfinderTradeResultClientHandler");
            handler.getMethod("open", int.class, int.class).invoke(null, entityId, resultCode);
        } catch (ReflectiveOperationException exception) {
            HiddenPlacesMod.LOGGER.error("Failed to open Test Wayfinder trade result screen", exception);
        }
    }
}
