package net.favouriteless.trotting_wagons.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.favouriteless.trotting_wagons.common.entities.base.AbstractWagon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class WagonItemRenderer extends BlockEntityWithoutLevelRenderer {

    private final EntityType<? extends AbstractWagon> type;
    private AbstractWagon wagon = null;

    public WagonItemRenderer(EntityType<? extends AbstractWagon> type) {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
        this.type = type;
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if(wagon == null)
            wagon = type.create(Minecraft.getInstance().level);

        wagon.setColor(stack.get(DataComponents.BASE_COLOR));

        Minecraft.getInstance().getEntityRenderDispatcher().render(wagon, 0, 0, 0, 0, 0, pose, buffer, packedLight);
    }

}
