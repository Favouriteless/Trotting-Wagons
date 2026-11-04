package net.favouriteless.trotting_wagons.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.favouriteless.trotting_wagons.common.entities.Chassis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.util.Mth;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class ChassisRenderer extends GeoEntityRenderer<Chassis> {

    public ChassisRenderer(Context context) {
        super(context, new ChassisModel());
    }

    @Override
    public void render(Chassis chassis, float entityYaw, float partialTick, PoseStack pose, MultiBufferSource buffer, int packedLight) {
        super.render(chassis, entityYaw, partialTick, pose, buffer, packedLight);
        BuildHammer.render(pose, buffer, chassis, partialTick, packedLight, 1.75D);
    }

    @Override
    public void preRender(PoseStack pose, Chassis chassis, BakedGeoModel model, MultiBufferSource bufferSource, VertexConsumer buffer,
                          boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        float yaw = -Mth.rotLerp(partialTick, chassis.yRotO, chassis.getYRot());
        pose.mulPose(Axis.YP.rotationDegrees(yaw));
        super.preRender(pose, chassis, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
    }

}
