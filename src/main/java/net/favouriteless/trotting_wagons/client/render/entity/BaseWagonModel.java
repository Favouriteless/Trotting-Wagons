package net.favouriteless.trotting_wagons.client.render.entity;

import net.favouriteless.trotting_wagons.TrottingWagons;
import net.favouriteless.trotting_wagons.common.entities.BaseWagon;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class BaseWagonModel extends DefaultedEntityGeoModel<BaseWagon> {

    public BaseWagonModel() {
        super(TrottingWagons.id("wagon_base"), false);
    }

    @Override
    public ResourceLocation getTextureResource(BaseWagon wagon) {
        return TrottingWagons.id("textures/entity/wagon_base/none.png");
    }

    @Override
    public void setCustomAnimations(BaseWagon wagon, long instanceId, AnimationState<BaseWagon> state) {
        super.setCustomAnimations(wagon, instanceId, state);
        show("Chassis");
        show("cart");
        show("hitbox");
    }

    private void show(String name) {
        GeoBone bone = getAnimationProcessor().getBone(name);
        if(bone != null)
            bone.setHidden(false);
    }

}
