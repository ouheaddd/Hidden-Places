package com.overyourhead.hidden_places.client.screen;

import com.overyourhead.hidden_places.HiddenPlacesMod;
import com.overyourhead.hidden_places.client.screen.widget.WayfinderButton;
import com.overyourhead.hidden_places.common.entity.TestWayfinderEntity;
import com.overyourhead.hidden_places.common.network.WayfinderDialogueChoicePayload;
import com.overyourhead.hidden_places.common.npc.WayfinderDialogueStage;
import com.overyourhead.hidden_places.common.npc.WayfinderProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

public class TestWayfinderDialogueScreen extends Screen {
    private static final int PANEL_WIDTH = 256;
    private static final int PANEL_HEIGHT = 188;
    private static final int BUTTON_WIDTH = 224;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_SPACING = 24;
    private static final ResourceLocation PANEL_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            HiddenPlacesMod.MOD_ID,
            "textures/gui/wayfinder_panel.png"
    );

    private final int entityId;
    private final WayfinderProfile profile;
    private WayfinderDialogueStage dialogueStage;
    private Component bodyText;
    private boolean suppressClosePacket;

    public TestWayfinderDialogueScreen(TestWayfinderEntity wayfinder) {
        this(wayfinder, wayfinder.getDialogueStageType());
    }

    public TestWayfinderDialogueScreen(TestWayfinderEntity wayfinder, WayfinderDialogueStage initialStage) {
        super(Component.translatable("screen.hidden_places.test_wayfinder.title"));
        this.entityId = wayfinder.getId();
        this.profile = wayfinder.getProfile();
        this.dialogueStage = initialStage;
        this.bodyText = this.bodyForStage(this.dialogueStage);
    }

    @Override
    protected void init() {
        this.showStage(this.dialogueStage, false);
    }

    private void showStage(WayfinderDialogueStage stage, boolean notifyServer) {
        this.clearWidgets();
        this.dialogueStage = stage;
        this.bodyText = this.bodyForStage(stage);

        if (notifyServer) {
            this.sendStageToServer(stage);
        }

        switch (stage) {
            case WHO -> this.showWhoOptions();
            case SANCTUM -> this.showSanctumOptions();
            case TRADE -> this.showTradeOptions();
            case HOSTILE_DEAD_END -> this.showHostileOptions();
            case TRADE_COMPLETED -> this.showTradeCompletedOptions();
            case INTRO -> this.showIntroOptions();
        }
    }

    private Component bodyForStage(WayfinderDialogueStage stage) {
        return Component.translatable(stage.bodyTranslationKey(this.profile));
    }

    private void showIntroOptions() {
        this.addDialogueButton(0, Component.translatable("screen.hidden_places.test_wayfinder.option.who"),
                button -> this.showStage(WayfinderDialogueStage.WHO, true));
        this.addDialogueButton(1, Component.translatable("screen.hidden_places.test_wayfinder.option.sanctum"),
                button -> this.showStage(WayfinderDialogueStage.SANCTUM, true));
        if (this.profile.hasTrade()) {
            this.addDialogueButton(2, Component.translatable("screen.hidden_places.test_wayfinder.option.trade"),
                    button -> this.openTrade());
        }
        this.addDialogueButton(3, Component.translatable("screen.hidden_places.test_wayfinder.option.insult"),
                button -> this.showStage(WayfinderDialogueStage.HOSTILE_DEAD_END, true));
    }

    private void showWhoOptions() {
        if (this.profile.hasTrade()) {
            this.addDialogueButton(0, Component.translatable("screen.hidden_places.test_wayfinder.option.trade"),
                    button -> this.openTrade());
        }
        this.addDialogueButton(1, Component.translatable("screen.hidden_places.test_wayfinder.option.leave"),
                button -> Minecraft.getInstance().setScreen(null));
    }

    private void showSanctumOptions() {
        if (this.profile.hasTrade()) {
            this.addDialogueButton(0, Component.translatable("screen.hidden_places.test_wayfinder.option.trade"),
                    button -> this.openTrade());
        }
        this.addDialogueButton(1, Component.translatable("screen.hidden_places.test_wayfinder.option.leave"),
                button -> Minecraft.getInstance().setScreen(null));
    }

    private void showTradeOptions() {
        if (this.profile.hasTrade()) {
            this.addDialogueButton(0, Component.translatable("screen.hidden_places.test_wayfinder.option.trade_open"),
                    button -> this.openTrade());
        }
        this.addDialogueButton(1, Component.translatable("screen.hidden_places.test_wayfinder.option.leave"),
                button -> Minecraft.getInstance().setScreen(null));
    }

    private void showTradeCompletedOptions() {
        this.addDialogueButton(0, Component.translatable("screen.hidden_places.test_wayfinder.option.leave"),
                button -> Minecraft.getInstance().setScreen(null));
    }

    private void showHostileOptions() {
        if (this.profile.canBecomeHostileFromDialogue()) {
            this.addDialogueButton(0, Component.translatable("screen.hidden_places.test_wayfinder.option.trouble"),
                    button -> this.startFight());
        }
        this.addDialogueButton(1, Component.translatable("screen.hidden_places.test_wayfinder.option.leave"),
                button -> Minecraft.getInstance().setScreen(null));
    }

    private void openTrade() {
        if (!this.profile.hasTrade()) {
            return;
        }

        PacketDistributor.sendToServer(new WayfinderDialogueChoicePayload(
                this.entityId,
                WayfinderDialogueChoicePayload.CHOICE_OPEN_TRADE
        ));
        this.suppressClosePacket = true;
        Minecraft.getInstance().setScreen(new TestWayfinderTradeScreen(this.entityId, this.profile));
    }

    private void startFight() {
        this.suppressClosePacket = true;
        PacketDistributor.sendToServer(new WayfinderDialogueChoicePayload(
                this.entityId,
                WayfinderDialogueChoicePayload.CHOICE_TROUBLE
        ));
        Minecraft.getInstance().setScreen(null);
    }

    private void sendStageToServer(WayfinderDialogueStage stage) {
        int choice = switch (stage) {
            case WHO -> WayfinderDialogueChoicePayload.CHOICE_STAGE_WHO;
            case SANCTUM -> WayfinderDialogueChoicePayload.CHOICE_STAGE_SANCTUM;
            case TRADE -> WayfinderDialogueChoicePayload.CHOICE_STAGE_TRADE;
            case HOSTILE_DEAD_END -> WayfinderDialogueChoicePayload.CHOICE_STAGE_HOSTILE_DEAD_END;
            case TRADE_COMPLETED -> WayfinderDialogueChoicePayload.CHOICE_STAGE_TRADE_COMPLETED;
            case INTRO -> WayfinderDialogueChoicePayload.CHOICE_STAGE_INTRO;
        };

        PacketDistributor.sendToServer(new WayfinderDialogueChoicePayload(this.entityId, choice));
    }

    private void addDialogueButton(int index, Component text, Button.OnPress onPress) {
        int left = (this.width - PANEL_WIDTH) / 2;
        int top = (this.height - PANEL_HEIGHT) / 2;
        int buttonX = left + 16;
        int buttonY = top + 76 + index * BUTTON_SPACING;

        this.addRenderableWidget(new WayfinderButton(buttonX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT, text, onPress));
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
        graphics.drawWordWrap(this.font, this.bodyText, left + 12, top + 38, PANEL_WIDTH - 24, 0xFFE0DDD0);

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
