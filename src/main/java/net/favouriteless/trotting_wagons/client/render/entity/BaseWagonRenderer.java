package net.favouriteless.trotting_wagons.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.favouriteless.trotting_wagons.common.entities.BaseWagon;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.util.Mth;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class BaseWagonRenderer extends GeoEntityRenderer<BaseWagon> {

    public BaseWagonRenderer(Context context) {
        super(context, new BaseWagonModel());
    }

    @Override
    public void preRender(PoseStack pose, BaseWagon wagon, BakedGeoModel model, MultiBufferSource bufferSource, VertexConsumer buffer,
                          boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        float yaw = -Mth.rotLerp(partialTick, wagon.yRotO, wagon.getYRot());
        pose.mulPose(Axis.YP.rotationDegrees(yaw));
        super.preRender(pose, wagon, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
    }

}
