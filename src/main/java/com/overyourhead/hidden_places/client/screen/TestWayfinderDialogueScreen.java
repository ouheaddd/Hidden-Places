package com.overyourhead.hidden_places.client.screen;

import com.overyourhead.hidden_places.HiddenPlacesMod;
import com.overyourhead.hidden_places.client.screen.widget.WayfinderButton;
import com.overyourhead.hidden_places.common.entity.TestWayfinderEntity;
import com.overyourhead.hidden_places.common.network.WayfinderDialogueChoicePayload;
import com.overyourhead.hidden_places.common.npc.WayfinderDialogueAction;
import com.overyourhead.hidden_places.common.npc.WayfinderDialogueChoice;
import com.overyourhead.hidden_places.common.npc.WayfinderDialogueNode;
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
    private String nodeId;
    private WayfinderDialogueNode node;
    private Component bodyText;
    private boolean suppressClosePacket;

    public TestWayfinderDialogueScreen(TestWayfinderEntity wayfinder) {
        this(wayfinder, wayfinder.getProfile().defaultNodeId());
    }

    public TestWayfinderDialogueScreen(TestWayfinderEntity wayfinder, String initialNodeId) {
        super(Component.translatable("screen.hidden_places." + wayfinder.getProfile().translationKey() + ".title"));
        this.entityId = wayfinder.getId();
        this.profile = wayfinder.getProfile();
        this.nodeId = this.profile.hasDialogueNode(initialNodeId) ? initialNodeId : this.profile.defaultNodeId();
        this.node = this.profile.dialogueNode(this.nodeId);
        this.bodyText = this.bodyForNode(this.node);
    }

    @Override
    protected void init() {
        this.showNode(this.nodeId);
    }

    private void showNode(String newNodeId) {
        this.clearWidgets();
        this.nodeId = this.profile.hasDialogueNode(newNodeId) ? newNodeId : this.profile.defaultNodeId();
        this.node = this.profile.dialogueNode(this.nodeId);
        this.bodyText = this.bodyForNode(this.node);

        int index = 0;
        for (WayfinderDialogueChoice choice : this.node.choices()) {
            final int choiceIndex = index;
            this.addDialogueButton(index, Component.translatable(choice.textTranslationKey(this.profile)), button -> this.choose(choiceIndex));
            index++;
        }
    }

    private Component bodyForNode(WayfinderDialogueNode node) {
        return Component.translatable(node.bodyTranslationKey(this.profile));
    }

    private void choose(int choiceIndex) {
        if (choiceIndex < 0 || choiceIndex >= this.node.choices().size()) {
            return;
        }

        WayfinderDialogueChoice choice = this.node.choices().get(choiceIndex);
        PacketDistributor.sendToServer(new WayfinderDialogueChoicePayload(this.entityId, choiceIndex));

        if (choice.action() == WayfinderDialogueAction.GOTO_NODE && choice.targetNodeId() != null) {
            this.showNode(choice.targetNodeId());
            return;
        }

        if (choice.action() == WayfinderDialogueAction.OPEN_TRADE) {
            this.openTrade();
            return;
        }

        if (choice.action() == WayfinderDialogueAction.START_FIGHT
                || choice.action() == WayfinderDialogueAction.START_SUNVEIL_TRIAL
                || choice.action() == WayfinderDialogueAction.CLOSE) {
            this.suppressClosePacket = true;
            Minecraft.getInstance().setScreen(null);
            return;
        }

        if (choice.action() == WayfinderDialogueAction.COMPLETE) {
            this.showNode(this.profile.completedNodeId());
        }
    }

    private void openTrade() {
        if (!this.profile.hasTrade()) {
            return;
        }

        this.suppressClosePacket = true;
        Minecraft.getInstance().setScreen(new TestWayfinderTradeScreen(this.entityId, this.profile));
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
