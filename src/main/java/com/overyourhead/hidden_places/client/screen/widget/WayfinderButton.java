package com.overyourhead.hidden_places.client.screen.widget;

import com.overyourhead.hidden_places.HiddenPlacesMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Button.OnPress;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class WayfinderButton extends Button {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            HiddenPlacesMod.MOD_ID,
            "textures/gui/wayfinder_button.png"
    );

    private static final int TEXTURE_WIDTH = 224;
    private static final int TEXTURE_HEIGHT = 20;

    public WayfinderButton(int x, int y, int width, int height, Component message, OnPress onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.blit(TEXTURE, this.getX(), this.getY(), 0, 0, this.getWidth(), this.getHeight(), TEXTURE_WIDTH, TEXTURE_HEIGHT);

        if (this.isHoveredOrFocused()) {
            graphics.fill(this.getX() + 2, this.getY() + 2, this.getX() + this.getWidth() - 2, this.getY() + this.getHeight() - 2, 0x26FFFFFF);
        }

        if (!this.active) {
            graphics.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), 0x88000000);
        }

        int textColor = this.active
                ? (this.isHoveredOrFocused() ? 0xFFFFF2B8 : 0xFFE6D3A3)
                : 0xFF8A8170;

        Minecraft minecraft = Minecraft.getInstance();
        int textY = this.getY() + (this.getHeight() - 8) / 2;
        graphics.drawCenteredString(minecraft.font, this.getMessage(), this.getX() + this.getWidth() / 2, textY, textColor);
    }
}
