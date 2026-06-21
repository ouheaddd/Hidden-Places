package com.overyourhead.hidden_places.client.screen;

import com.overyourhead.hidden_places.HiddenPlacesMod;
import com.overyourhead.hidden_places.client.screen.widget.WayfinderButton;
import com.overyourhead.hidden_places.common.entity.TestWayfinderEntity;
import com.overyourhead.hidden_places.common.network.WayfinderDialogueChoicePayload;
import com.overyourhead.hidden_places.common.network.WayfinderTradePayload;
import com.overyourhead.hidden_places.common.npc.WayfinderProfile;
import com.overyourhead.hidden_places.common.npc.WayfinderTradeOffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.PacketDistributor;

public class TestWayfinderTradeScreen extends Screen {
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
    private final WayfinderTradeOffer offer;
    private boolean suppressClosePacket;

    public TestWayfinderTradeScreen(int entityId, WayfinderProfile profile) {
        super(Component.translatable("screen.hidden_places." + profile.translationKey() + ".trade.title"));
        this.entityId = entityId;
        this.profile = profile;
        this.offer = profile.tradeOffer();
    }

    @Override
    protected void init() {
        int left = (this.width - PANEL_WIDTH) / 2;
        int top = (this.height - PANEL_HEIGHT) / 2;
        int buttonX = left + 16;
        int buttonY = top + 104;

        if (this.offer != null) {
            this.addRenderableWidget(new WayfinderButton(
                    buttonX,
                    buttonY,
                    BUTTON_WIDTH,
                    BUTTON_HEIGHT,
                    Component.translatable(this.offer.buyButtonTranslationKey(this.profile)),
                    button -> PacketDistributor.sendToServer(new WayfinderTradePayload(
                            this.entityId,
                            this.offer.id()
                    ))
            ));
        }

        this.addRenderableWidget(new WayfinderButton(
                buttonX,
                buttonY + 24,
                BUTTON_WIDTH,
                BUTTON_HEIGHT,
                Component.translatable(this.translationKey("trade.back")),
                button -> this.returnToDialogue()
        ));
    }


    public void openResult(int resultCode) {
        this.suppressClosePacket = true;
        Minecraft.getInstance().setScreen(new TestWayfinderTradeResultScreen(this.entityId, this.profile, resultCode));
    }

    private void returnToDialogue() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            minecraft.setScreen(null);
            return;
        }

        Entity entity = minecraft.level.getEntity(this.entityId);
        if (entity instanceof TestWayfinderEntity wayfinder) {
            this.suppressClosePacket = true;
            minecraft.setScreen(new TestWayfinderDialogueScreen(wayfinder, this.profile.tradeNodeId()));
            return;
        }

        minecraft.setScreen(null);
    }

    private String translationKey(String suffix) {
        return "screen.hidden_places." + this.profile.translationKey() + "." + suffix;
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
                Component.translatable(this.translationKey("trade.body")),
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
