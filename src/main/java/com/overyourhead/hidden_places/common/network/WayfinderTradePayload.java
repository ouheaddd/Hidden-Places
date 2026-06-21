package com.overyourhead.hidden_places.common.network;

import com.overyourhead.hidden_places.HiddenPlacesMod;
import com.overyourhead.hidden_places.common.entity.TestWayfinderEntity;
import com.overyourhead.hidden_places.common.npc.WayfinderTradeOffer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record WayfinderTradePayload(int entityId, int offerId) implements CustomPacketPayload {
    public static final int OFFER_MOSSGATE_KEY = WayfinderTradeOffer.MOSSGATE_KEY_FOR_EMERALD.id();
    public static final int OFFER_MOSSGATE_KEY_FOR_TORCHFLOWER = WayfinderTradeOffer.MOSSGATE_KEY_FOR_TORCHFLOWER.id();

    public static final Type<WayfinderTradePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(HiddenPlacesMod.MOD_ID, "wayfinder_trade")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, WayfinderTradePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            WayfinderTradePayload::entityId,
            ByteBufCodecs.VAR_INT,
            WayfinderTradePayload::offerId,
            WayfinderTradePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(WayfinderTradePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }

            Entity entity = player.level().getEntity(payload.entityId());
            if (!(entity instanceof TestWayfinderEntity wayfinder)) {
                return;
            }

            if (wayfinder.distanceToSqr(player) > 64.0D || wayfinder.isHostile() || !wayfinder.hasTrade()) {
                return;
            }

            WayfinderTradeOffer.byId(payload.offerId())
                    .filter(wayfinder::allowsTradeOffer)
                    .ifPresent(offer -> trade(player, wayfinder, offer));
        });
    }

    private static void trade(ServerPlayer player, TestWayfinderEntity wayfinder, WayfinderTradeOffer offer) {
        if (wayfinder.hasPlayerTraded(player)) {
            wayfinder.setDialogueNode(player, wayfinder.getProfile().completedNodeId());
            sendResult(player, wayfinder, WayfinderTradeResultPayload.ALREADY_TRADED);
            return;
        }

        if (!hasEnoughItem(player, offer.costItem(), offer.costCount())) {
            sendResult(player, wayfinder, WayfinderTradeResultPayload.NO_PAYMENT);
            return;
        }

        ItemStack reward = offer.createResultStack();
        if (!hasRoomFor(player, reward)) {
            sendResult(player, wayfinder, WayfinderTradeResultPayload.INVENTORY_FULL);
            return;
        }

        consumeItem(player, offer.costItem(), offer.costCount());
        player.getInventory().add(reward);
        wayfinder.markPlayerTraded(player);
        sendResult(player, wayfinder, WayfinderTradeResultPayload.SUCCESS);
    }

    private static void sendResult(ServerPlayer player, TestWayfinderEntity wayfinder, int resultCode) {
        PacketDistributor.sendToPlayer(player, new WayfinderTradeResultPayload(wayfinder.getId(), resultCode));
    }

    private static boolean hasEnoughItem(ServerPlayer player, Item item, int count) {
        if (player.isCreative()) {
            return true;
        }

        int found = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(item)) {
                found += stack.getCount();
                if (found >= count) {
                    return true;
                }
            }
        }

        return false;
    }

    private static void consumeItem(ServerPlayer player, Item item, int count) {
        if (player.isCreative()) {
            return;
        }

        int remaining = count;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.is(item)) {
                continue;
            }

            int taken = Math.min(remaining, stack.getCount());
            stack.shrink(taken);
            remaining -= taken;

            if (remaining <= 0) {
                player.getInventory().setChanged();
                return;
            }
        }
    }

    private static boolean hasRoomFor(ServerPlayer player, ItemStack reward) {
        if (player.isCreative()) {
            return true;
        }

        int remaining = reward.getCount();
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.isEmpty()) {
                return true;
            }

            if (stack.is(reward.getItem()) && stack.getCount() < stack.getMaxStackSize()) {
                remaining -= stack.getMaxStackSize() - stack.getCount();
                if (remaining <= 0) {
                    return true;
                }
            }
        }

        return false;
    }
}
