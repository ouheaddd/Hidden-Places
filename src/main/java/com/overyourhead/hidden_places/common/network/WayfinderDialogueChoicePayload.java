package com.overyourhead.hidden_places.common.network;

import com.overyourhead.hidden_places.HiddenPlacesMod;
import com.overyourhead.hidden_places.common.entity.TestWayfinderEntity;
import com.overyourhead.hidden_places.common.npc.WayfinderDialogueAction;
import com.overyourhead.hidden_places.common.npc.WayfinderDialogueChoice;
import com.overyourhead.hidden_places.common.npc.WayfinderDialogueNode;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record WayfinderDialogueChoicePayload(int entityId, int choiceId) implements CustomPacketPayload {
    public static final int CHOICE_CLOSE = -1;

    public static final Type<WayfinderDialogueChoicePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(HiddenPlacesMod.MOD_ID, "wayfinder_dialogue_choice")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, WayfinderDialogueChoicePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            WayfinderDialogueChoicePayload::entityId,
            ByteBufCodecs.VAR_INT,
            WayfinderDialogueChoicePayload::choiceId,
            WayfinderDialogueChoicePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(WayfinderDialogueChoicePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }

            Entity entity = player.level().getEntity(payload.entityId());
            if (!(entity instanceof TestWayfinderEntity wayfinder)) {
                return;
            }

            if (wayfinder.distanceToSqr(player) > 64.0D) {
                wayfinder.stopConversation(player);
                return;
            }

            if (payload.choiceId() == CHOICE_CLOSE) {
                wayfinder.stopConversation(player);
                return;
            }

            if (wayfinder.isHostile()) {
                return;
            }

            WayfinderDialogueNode node = wayfinder.getDialogueNode(player);
            if (payload.choiceId() < 0 || payload.choiceId() >= node.choices().size()) {
                return;
            }

            WayfinderDialogueChoice choice = node.choices().get(payload.choiceId());
            applyChoice(player, wayfinder, choice);
        });
    }

    private static void applyChoice(ServerPlayer player, TestWayfinderEntity wayfinder, WayfinderDialogueChoice choice) {
        WayfinderDialogueAction action = choice.action();

        if (action == WayfinderDialogueAction.GOTO_NODE && choice.targetNodeId() != null) {
            wayfinder.setDialogueNode(player, choice.targetNodeId());
            wayfinder.startConversation(player);
            return;
        }

        if (action == WayfinderDialogueAction.OPEN_TRADE) {
            if (wayfinder.hasTrade() && !wayfinder.hasPlayerTraded(player)) {
                if (wayfinder.getProfile().tradeNodeId() != null) {
                    wayfinder.setDialogueNode(player, wayfinder.getProfile().tradeNodeId());
                }
                wayfinder.startConversation(player);
            }
            return;
        }

        if (action == WayfinderDialogueAction.START_FIGHT) {
            wayfinder.provoke(player);
            return;
        }

        if (action == WayfinderDialogueAction.COMPLETE) {
            wayfinder.setDialogueNode(player, wayfinder.getProfile().completedNodeId());
            wayfinder.startConversation(player);
            return;
        }

        if (action == WayfinderDialogueAction.CLOSE) {
            wayfinder.stopConversation(player);
        }
    }
}
