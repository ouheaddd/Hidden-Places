package com.overyourhead.hidden_places.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.overyourhead.hidden_places.client.model.ParchedSkeletonRemainsModel;
import com.overyourhead.hidden_places.common.entity.ParchedSkeletonRemainsEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class ParchedSkeletonRemainsRenderer extends GeoEntityRenderer<ParchedSkeletonRemainsEntity> {
    public ParchedSkeletonRemainsRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new ParchedSkeletonRemainsModel());
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(ParchedSkeletonRemainsEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        poseStack.popPose();
    }
}
