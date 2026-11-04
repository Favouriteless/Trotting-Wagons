package net.favouriteless.trotting_wagons.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.favouriteless.trotting_wagons.TrottingWagons;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

/** Spinning hammer from aleki's Nifty Ships (alekiponi), MIT. See construction-license.txt. */
public final class BuildHammer {

    private static final ResourceLocation TEXTURE = TrottingWagons.id("textures/entity/wagon_base/construction.png");
    private static ModelPart model;

    private BuildHammer() {}

    public static void render(PoseStack pose, MultiBufferSource buffer, Entity entity, float partialTick, int packedLight, double height) {
        Minecraft minecraft = Minecraft.getInstance();
        if(minecraft.player != null && minecraft.player.distanceToSqr(entity) > 100.0D)
            return;
        if(model == null)
            model = bake();
        pose.pushPose();
        pose.translate(0.0D, height, 0.0D);
        pose.mulPose(Axis.YP.rotation((entity.tickCount + partialTick) / 20.0F));
        pose.mulPose(Axis.ZP.rotation(Mth.DEG_TO_RAD * 180.0F));
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        model.render(pose, consumer, packedLight, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }

    private static ModelPart bake() {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("bone", CubeListBuilder.create()
                        .texOffs(0, 3).addBox(-0.5F, -3.5F, -0.5F, 1.0F, 9.0F, 1.0F)
                        .texOffs(0, 0).addBox(-1.5F, -2.5F, -2.5F, 3.0F, 3.0F, 5.0F),
                PartPose.offsetAndRotation(0.0F, 16.9187F, 0.6048F, -0.3927F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 16, 16).bakeRoot();
    }

}
