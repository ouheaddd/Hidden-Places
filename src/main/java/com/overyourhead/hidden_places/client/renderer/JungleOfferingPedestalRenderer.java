package com.overyourhead.hidden_places.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.overyourhead.hidden_places.client.model.JungleOfferingPedestalModel;
import com.overyourhead.hidden_places.common.block.entity.JungleOfferingPedestalBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class JungleOfferingPedestalRenderer extends GeoBlockRenderer<JungleOfferingPedestalBlockEntity> {
    public JungleOfferingPedestalRenderer(BlockEntityRendererProvider.Context context) {
        super(new JungleOfferingPedestalModel());
    }

    @Override
    public void render(JungleOfferingPedestalBlockEntity animatable, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        super.render(animatable, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
        this.renderDisplayedItem(animatable, partialTick, poseStack, bufferSource, packedLight);
    }

    private void renderDisplayedItem(JungleOfferingPedestalBlockEntity pedestal, float partialTick, PoseStack poseStack,
                                     MultiBufferSource bufferSource, int packedLight) {
        ItemStack stack = pedestal.getDisplayedItem();
        if (stack.isEmpty()) {
            return;
        }

        double gameTime = pedestal.getLevel() == null ? 0.0D : pedestal.getLevel().getGameTime() + partialTick;
        float rotation = (float) ((gameTime * 2.0D) % 360.0D);
        float bob = (float) Math.sin(gameTime * 0.12D) * 0.035F;

        poseStack.pushPose();
        poseStack.translate(0.5D, 1.23D + bob, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation));
        poseStack.scale(0.55F, 0.55F, 0.55F);

        Minecraft.getInstance().getItemRenderer().renderStatic(
                stack,
                ItemDisplayContext.GROUND,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                poseStack,
                bufferSource,
                pedestal.getLevel(),
                0
        );

        poseStack.popPose();
    }
}
