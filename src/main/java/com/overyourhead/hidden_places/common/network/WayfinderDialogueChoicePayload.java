package com.overyourhead.hidden_places.common.network;

import com.overyourhead.hidden_places.HiddenPlacesMod;
import com.overyourhead.hidden_places.common.entity.TestWayfinderEntity;
import com.overyourhead.hidden_places.common.npc.WayfinderDialogueStage;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record WayfinderDialogueChoicePayload(int entityId, int choiceId) implements CustomPacketPayload {
    public static final int CHOICE_OPEN_TRADE = 1;
    public static final int CHOICE_TROUBLE = 2;
    public static final int CHOICE_STAGE_INTRO = 10;
    public static final int CHOICE_STAGE_WHO = 11;
    public static final int CHOICE_STAGE_SANCTUM = 12;
    public static final int CHOICE_STAGE_TRADE = 13;
    public static final int CHOICE_STAGE_HOSTILE_DEAD_END = 14;
    public static final int CHOICE_STAGE_TRADE_COMPLETED = 15;
    public static final int CHOICE_CLOSE = 20;

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

            switch (payload.choiceId()) {
                case CHOICE_OPEN_TRADE -> {
                    WayfinderDialogueStage currentStage = wayfinder.getDialogueStage(player);
                    if (!wayfinder.isHostile()
                            && wayfinder.hasTrade()
                            && currentStage != WayfinderDialogueStage.TRADE_COMPLETED
                            && currentStage != WayfinderDialogueStage.HOSTILE_DEAD_END) {
                        wayfinder.setDialogueStage(player, WayfinderDialogueStage.TRADE);
                        wayfinder.startConversation(player);
                    }
                }
                case CHOICE_TROUBLE -> wayfinder.provoke(player);
                case CHOICE_STAGE_INTRO -> setStage(player, wayfinder, WayfinderDialogueStage.INTRO);
                case CHOICE_STAGE_WHO -> setStage(player, wayfinder, WayfinderDialogueStage.WHO);
                case CHOICE_STAGE_SANCTUM -> setStage(player, wayfinder, WayfinderDialogueStage.SANCTUM);
                case CHOICE_STAGE_TRADE -> setStage(player, wayfinder, WayfinderDialogueStage.TRADE);
                case CHOICE_STAGE_HOSTILE_DEAD_END -> setStage(player, wayfinder, WayfinderDialogueStage.HOSTILE_DEAD_END);
                case CHOICE_STAGE_TRADE_COMPLETED -> setStage(player, wayfinder, WayfinderDialogueStage.TRADE_COMPLETED);
                case CHOICE_CLOSE -> wayfinder.stopConversation(player);
                default -> {
                }
            }
        });
    }

    private static void setStage(ServerPlayer player, TestWayfinderEntity wayfinder, WayfinderDialogueStage stage) {
        if (wayfinder.isHostile()) {
            return;
        }

        WayfinderDialogueStage currentStage = wayfinder.getDialogueStage(player);
        if (!canMoveToStage(currentStage, stage)) {
            return;
        }

        wayfinder.setDialogueStage(player, stage);
        wayfinder.startConversation(player);
    }

    private static boolean canMoveToStage(WayfinderDialogueStage currentStage, WayfinderDialogueStage requestedStage) {
        if (currentStage == WayfinderDialogueStage.TRADE_COMPLETED) {
            return requestedStage == WayfinderDialogueStage.TRADE_COMPLETED;
        }

        if (currentStage == WayfinderDialogueStage.HOSTILE_DEAD_END) {
            return requestedStage == WayfinderDialogueStage.HOSTILE_DEAD_END;
        }

        if (requestedStage == WayfinderDialogueStage.INTRO && currentStage != WayfinderDialogueStage.INTRO) {
            return false;
        }

        return true;
    }
}
