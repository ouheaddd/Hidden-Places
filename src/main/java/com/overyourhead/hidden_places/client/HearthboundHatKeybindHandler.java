package com.overyourhead.hidden_places.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.overyourhead.hidden_places.HiddenPlacesMod;
import com.overyourhead.hidden_places.common.item.HearthboundHatItem;
import com.overyourhead.hidden_places.common.network.HearthboundHatTeleportPayload;
import com.overyourhead.hidden_places.core.registry.HPItems;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = HiddenPlacesMod.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
final class HearthboundHatKeyRegister {
    public static final KeyMapping ACTIVATE_HEARTHBOUND_HAT = new KeyMapping(
            "key.hidden_places.activate_hearthbound_hat",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_H,
            "key.categories.hidden_places"
    );

    private HearthboundHatKeyRegister() {
    }

    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(ACTIVATE_HEARTHBOUND_HAT);
    }
}

@EventBusSubscriber(modid = HiddenPlacesMod.MOD_ID, value = Dist.CLIENT)
public final class HearthboundHatKeybindHandler {
    private static int heldTicks;
    private static int soundTicks;
    private static boolean packetSent;
    private static int localCooldownTicks;
    private static int cooldownMessageTicks;
    private static int lastShownCooldownSeconds = -1;

    private HearthboundHatKeybindHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (localCooldownTicks > 0) {
            localCooldownTicks--;
        }

        if (minecraft.player == null || minecraft.level == null || !isWearingHat(minecraft)) {
            reset();
            resetCooldownMessage();
            return;
        }

        boolean keyDown = HearthboundHatKeyRegister.ACTIVATE_HEARTHBOUND_HAT.isDown();
        boolean onCooldown = localCooldownTicks > 0 || minecraft.player.getCooldowns().isOnCooldown(HPItems.HEARTHBOUND_HAT.get());
        if (onCooldown) {
            reset();
            if (keyDown && localCooldownTicks > 0) {
                showCooldownMessage(minecraft);
            } else if (!keyDown) {
                resetCooldownMessage();
            }
            return;
        }

        if (!keyDown) {
            reset();
            resetCooldownMessage();
            return;
        }

        heldTicks++;
        soundTicks++;

        if (heldTicks == 1 || soundTicks >= 10) {
            soundTicks = 0;
            minecraft.player.playSound(SoundEvents.NOTE_BLOCK_CHIME.value(), 0.30F, 1.45F);
        }

        if (!packetSent && heldTicks >= HearthboundHatItem.HOLD_TICKS) {
            packetSent = true;
            localCooldownTicks = HearthboundHatItem.COOLDOWN_TICKS;
            minecraft.player.getCooldowns().addCooldown(HPItems.HEARTHBOUND_HAT.get(), HearthboundHatItem.COOLDOWN_TICKS);
            PacketDistributor.sendToServer(new HearthboundHatTeleportPayload());
        }
    }

    private static boolean isWearingHat(Minecraft minecraft) {
        return minecraft.player.getItemBySlot(EquipmentSlot.HEAD).is(HPItems.HEARTHBOUND_HAT.get());
    }

    private static void showCooldownMessage(Minecraft minecraft) {
        cooldownMessageTicks++;
        int secondsLeft = Math.max(1, (int) Math.ceil(localCooldownTicks / 20.0D));
        if (cooldownMessageTicks == 1 || secondsLeft != lastShownCooldownSeconds && cooldownMessageTicks >= 20) {
            cooldownMessageTicks = 1;
            lastShownCooldownSeconds = secondsLeft;
            minecraft.player.displayClientMessage(Component.translatable("message.hidden_places.hearthbound_hat.cooldown", secondsLeft), true);
            minecraft.player.playSound(SoundEvents.NOTE_BLOCK_CHIME.value(), 0.25F, 0.8F);
        }
    }

    private static void resetCooldownMessage() {
        cooldownMessageTicks = 0;
        lastShownCooldownSeconds = -1;
    }

    private static void reset() {
        heldTicks = 0;
        soundTicks = 0;
        packetSent = false;
    }
}
