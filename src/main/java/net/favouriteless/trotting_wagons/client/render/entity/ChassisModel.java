package net.favouriteless.trotting_wagons.client.render.entity;

import net.favouriteless.trotting_wagons.TrottingWagons;
import net.favouriteless.trotting_wagons.common.entities.Chassis;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animation.AnimationProcessor;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class ChassisModel extends DefaultedEntityGeoModel<Chassis> {

    public ChassisModel() {
        super(TrottingWagons.id("wagon_base"), false);
    }

    @Override
    public ResourceLocation getTextureResource(Chassis chassis) {
        return TrottingWagons.id("textures/entity/wagon_base/none.png");
    }

    @Override
    public void setCustomAnimations(Chassis chassis, long instanceId, AnimationState<Chassis> state) {
        super.setCustomAnimations(chassis, instanceId, state);
        AnimationProcessor<Chassis> processor = getAnimationProcessor();
        hide(processor, "cart");
        hide(processor, "hitbox");
    }

    private static void hide(AnimationProcessor<?> processor, String name) {
        GeoBone bone = processor.getBone(name);
        if(bone != null)
            bone.setHidden(true);
    }

}
