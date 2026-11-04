package net.favouriteless.trotting_wagons.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.favouriteless.trotting_wagons.TrottingWagons;
import net.favouriteless.trotting_wagons.common.entities.Chassis;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class ChassisRenderer extends GeoEntityRenderer<Chassis> {

    /** Hammer mesh and texture from aleki's Nifty Ships (alekiponi), MIT. */
    private static final ResourceLocation HAMMER = TrottingWagons.id("textures/entity/wagon_base/construction.png");

    private final ModelPart hammer;

    public ChassisRenderer(Context context) {
        super(context, new ChassisModel());
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("bone", CubeListBuilder.create()
                        .texOffs(0, 3).addBox(-0.5F, -3.5F, -0.5F, 1.0F, 9.0F, 1.0F)
                        .texOffs(0, 0).addBox(-1.5F, -2.5F, -2.5F, 3.0F, 3.0F, 5.0F),
                PartPose.offsetAndRotation(0.0F, 16.9187F, 0.6048F, -0.3927F, 0.0F, 0.0F));
        this.hammer = LayerDefinition.create(mesh, 16, 16).bakeRoot();
    }

    @Override
    public void render(Chassis chassis, float entityYaw, float partialTick, PoseStack pose, MultiBufferSource buffer, int packedLight) {
        super.render(chassis, entityYaw, partialTick, pose, buffer, packedLight);
        pose.pushPose();
        pose.translate(0.0D, 1.75D, 0.0D);
        pose.mulPose(Axis.YP.rotation((chassis.tickCount + partialTick) / 20.0F));
        pose.mulPose(Axis.ZP.rotation(Mth.DEG_TO_RAD * 180.0F));
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(HAMMER));
        hammer.render(pose, consumer, packedLight, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }

    @Override
    public void preRender(PoseStack pose, Chassis chassis, BakedGeoModel model, MultiBufferSource bufferSource, VertexConsumer buffer,
                          boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        float yaw = -Mth.rotLerp(partialTick, chassis.yRotO, chassis.getYRot());
        pose.mulPose(Axis.YP.rotationDegrees(yaw));
        super.preRender(pose, chassis, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
    }

}
