package com.overyourhead.hidden_places.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.overyourhead.hidden_places.client.model.MirageCreeperModel;
import com.overyourhead.hidden_places.common.entity.MirageCreeperEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class MirageCreeperRenderer extends GeoEntityRenderer<MirageCreeperEntity> {
    public MirageCreeperRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new MirageCreeperModel());
        this.shadowRadius = 0.5F;
    }

    @Override
    public void render(MirageCreeperEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        float swelling = entity.getSwelling(partialTick);
        float pulse = 1.0F + Mth.sin(swelling * 100.0F) * swelling * 0.01F;
        swelling = Mth.clamp(swelling, 0.0F, 1.0F);
        swelling *= swelling;
        swelling *= swelling;

        float horizontalScale = (1.0F + swelling * 0.4F) * pulse;
        float verticalScale = (1.0F + swelling * 0.1F) / pulse;

        poseStack.pushPose();
        poseStack.scale(horizontalScale, verticalScale, horizontalScale);
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        poseStack.popPose();
    }
}
