package com.overyourhead.hidden_places.client.screen;

import com.overyourhead.hidden_places.HiddenPlacesMod;
import com.overyourhead.hidden_places.client.screen.widget.WayfinderButton;
import com.overyourhead.hidden_places.common.network.WayfinderDialogueChoicePayload;
import com.overyourhead.hidden_places.common.network.WayfinderTradeResultPayload;
import com.overyourhead.hidden_places.common.npc.WayfinderProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

public class TestWayfinderTradeResultScreen extends Screen {
    private static final int PANEL_WIDTH = 256;
    private static final int PANEL_HEIGHT = 188;
    private static final int BUTTON_WIDTH = 224;
    private static final int BUTTON_HEIGHT = 20;
    private static final ResourceLocation PANEL_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            HiddenPlacesMod.MOD_ID,
            "textures/gui/wayfinder_panel.png"
    );

    private final int entityId;
    private final WayfinderProfile profile;
    private final int resultCode;
    private boolean suppressClosePacket;

    public TestWayfinderTradeResultScreen(int entityId, WayfinderProfile profile, int resultCode) {
        super(Component.translatable("screen.hidden_places." + profile.translationKey() + ".trade.result.title"));
        this.entityId = entityId;
        this.profile = profile;
        this.resultCode = resultCode;
    }

    @Override
    protected void init() {
        int left = (this.width - PANEL_WIDTH) / 2;
        int top = (this.height - PANEL_HEIGHT) / 2;
        int buttonX = left + 16;
        int buttonY = top + 104;

        if (this.canReturnToTrade()) {
            this.addRenderableWidget(new WayfinderButton(
                    buttonX,
                    buttonY,
                    BUTTON_WIDTH,
                    BUTTON_HEIGHT,
                    Component.translatable(this.translationKey("trade.result.back_trade")),
                    button -> this.returnToTrade()
            ));

            this.addRenderableWidget(new WayfinderButton(
                    buttonX,
                    buttonY + 24,
                    BUTTON_WIDTH,
                    BUTTON_HEIGHT,
                    Component.translatable(this.translationKey("trade.result.leave")),
                    button -> Minecraft.getInstance().setScreen(null)
            ));
            return;
        }

        this.addRenderableWidget(new WayfinderButton(
                buttonX,
                buttonY,
                BUTTON_WIDTH,
                BUTTON_HEIGHT,
                Component.translatable(this.translationKey("trade.result.done")),
                button -> Minecraft.getInstance().setScreen(null)
        ));
    }

    private boolean canReturnToTrade() {
        return this.resultCode == WayfinderTradeResultPayload.NO_PAYMENT
                || this.resultCode == WayfinderTradeResultPayload.INVENTORY_FULL;
    }

    private void returnToTrade() {
        this.suppressClosePacket = true;
        Minecraft.getInstance().setScreen(new TestWayfinderTradeScreen(this.entityId, this.profile));
    }

    private String translationKey(String suffix) {
        return "screen.hidden_places." + this.profile.translationKey() + "." + suffix;
    }

    private Component resultMessage() {
        return Component.translatable(switch (this.resultCode) {
            case WayfinderTradeResultPayload.SUCCESS -> this.translationKey("trade.result.success");
            case WayfinderTradeResultPayload.NO_PAYMENT -> this.translationKey("trade.result.no_payment");
            case WayfinderTradeResultPayload.INVENTORY_FULL -> this.translationKey("trade.result.inventory_full");
            case WayfinderTradeResultPayload.ALREADY_TRADED -> this.translationKey("trade.result.already_traded");
            default -> this.translationKey("trade.result.failed");
        });
    }

    @Override
    protected void renderBlurredBackground(float partialTick) {
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int left = (this.width - PANEL_WIDTH) / 2;
        int top = (this.height - PANEL_HEIGHT) / 2;

        graphics.blit(PANEL_TEXTURE, left, top, 0, 0, PANEL_WIDTH, PANEL_HEIGHT, PANEL_WIDTH, PANEL_HEIGHT);

        graphics.drawString(this.font, this.title, left + 12, top + 12, 0xFFE6D3A3, false);
        graphics.drawWordWrap(
                this.font,
                this.resultMessage(),
                left + 12,
                top + 38,
                PANEL_WIDTH - 24,
                0xFFE0DDD0
        );

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        if (!this.suppressClosePacket) {
            PacketDistributor.sendToServer(new WayfinderDialogueChoicePayload(
                    this.entityId,
                    WayfinderDialogueChoicePayload.CHOICE_CLOSE
            ));
        }
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
