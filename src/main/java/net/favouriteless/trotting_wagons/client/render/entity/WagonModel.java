package net.favouriteless.trotting_wagons.client.render.entity;

import net.favouriteless.trotting_wagons.TrottingWagons;
import net.favouriteless.trotting_wagons.common.entities.base.AbstractWagon;
import net.favouriteless.trotting_wagons.common.entities.wagons.AnimalWagon;
import net.favouriteless.trotting_wagons.common.entities.wagons.BarrelWagon;
import net.favouriteless.trotting_wagons.common.entities.wagons.BigCartWagon;
import net.favouriteless.trotting_wagons.common.entities.wagons.ConestogaWagon;
import net.favouriteless.trotting_wagons.common.entities.wagons.DebugWagon;
import net.favouriteless.trotting_wagons.common.entities.wagons.KibitkaWagon;
import net.favouriteless.trotting_wagons.common.entities.wagons.LogWagon;
import net.favouriteless.trotting_wagons.common.entities.wagons.PassengerWagon;
import net.favouriteless.trotting_wagons.common.entities.wagons.RoyalWagon;
import net.favouriteless.trotting_wagons.common.entities.wagons.WareWagon;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;
import software.bernie.geckolib.animation.AnimationProcessor;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class WagonModel<T extends AbstractWagon> extends DefaultedEntityGeoModel<T> {

    private final String texturePathPrefix;

    public WagonModel(ResourceLocation assetSubpath) {
        super(assetSubpath, false);
        this.texturePathPrefix = "textures/entity/" + assetSubpath.getPath() + "/";
    }

    @Override
    public ResourceLocation getTextureResource(T wagon) {
        if(wagon instanceof DebugWagon || wagon instanceof AnimalWagon || wagon instanceof LogWagon || wagon instanceof BarrelWagon || wagon instanceof PassengerWagon || wagon instanceof WareWagon || wagon instanceof KibitkaWagon || wagon instanceof BigCartWagon)
            return TrottingWagons.id(texturePathPrefix + "none.png");
        DyeColor color = wagon.getColor();
        return TrottingWagons.id(texturePathPrefix + (color != null ? color.getName() : "none") + ".png");
    }

    @Override
    public void setCustomAnimations(T wagon, long instanceId, AnimationState<T> state) {
        super.setCustomAnimations(wagon, instanceId, state);

        AnimationProcessor<T> processor = getAnimationProcessor();
        float partialTick = (float)state.animationTick - (int)state.animationTick;
        float rot = wagon.getWheelRot(partialTick);

        spinAxle(processor, "FrontWheels", -rot);
        spinAxle(processor, "RearWheels", -rot);
        spinAxle(processor, "FrontWheels_Wide", -rot);
        spinAxle(processor, "RearWheels_Wide", -rot);
        spinAxle(processor, "FrontWheels_Iron", -rot);
        spinAxle(processor, "RearWheels_Iron", -rot);
        spinAxle(processor, "FrontIronWheels", -rot);
        spinAxle(processor, "RearIronWheels", -rot);
        spinAxle(processor, "FrontAxis", -rot);
        spinAxle(processor, "RearAxis", -rot);
        if(wagon instanceof DebugWagon || wagon instanceof AnimalWagon || wagon instanceof LogWagon || wagon instanceof BarrelWagon || wagon instanceof PassengerWagon || wagon instanceof WareWagon || wagon instanceof KibitkaWagon || wagon instanceof BigCartWagon)
            hideWheelVariants(processor, wagon, DEBUG_WHEEL_VARIANTS);
        else if(wagon instanceof ConestogaWagon)
            hideWheelVariants(processor, wagon, CONESTOGA_WHEEL_VARIANTS);
        else if(wagon instanceof RoyalWagon)
            hideWheelVariants(processor, wagon, ROYAL_WHEEL_VARIANTS);
        else
            hideWheelVariants(processor, wagon, ARMORED_WHEEL_VARIANTS);
        if(wagon instanceof ConestogaWagon)
            hideBone(processor, "barrels", true);
        if(wagon instanceof LogWagon logWagon) {
            hideLogCargo(processor, logWagon);
            levelLogs(processor);
        }
        if(wagon instanceof BarrelWagon barrelWagon)
            hideBarrels(processor, barrelWagon);
        if(wagon instanceof PassengerWagon passengerWagon)
            hideLamps(processor, passengerWagon);
        if(wagon instanceof WareWagon wareWagon)
            hideWare(processor, wareWagon);
        if(wagon instanceof KibitkaWagon kibitka)
            poseKibitka(processor, kibitka, partialTick);
        if(wagon instanceof AnimalWagon animal)
            poseAnimalGate(processor, animal, partialTick);
        if(wagon instanceof BigCartWagon bigCart)
            hideBone(processor, "Lamp", !bigCart.hasLantern());

        AbstractWagon.WheelSag sag = wagon.wheelSag();
        float bodyPitch = wagon.getXRot(partialTick) + sag.pitch();
        int harness = wagon.hitchSlots();
        hideBone(processor, "Tongue", harness != 2);
        hideBone(processor, "TongueSingle", harness != 1);
        float tongue = wagon.tongueAngle(bodyPitch, sag.roll(), sag.drop());
        spinTongue(processor, "Tongue", tongue);
        spinTongue(processor, "TongueSingle", tongue);
        if(wagon instanceof DebugWagon debugWagon) {
            GeoBone ladder = processor.getBone("Ladder");
            if(ladder != null)
                ladder.setRotZ(debugWagon.ladderRender(partialTick) * DebugWagon.LADDER_DROP);
            hidePumpkins(processor, debugWagon.getPumpkinCount());
            boolean bare = debugWagon.getBannerStack().isEmpty();
            GeoBone banner = processor.getBone("banner");
            GeoBone stand = processor.getBone("stand");
            GeoBone lamp = processor.getBone("Lamp");
            if(banner != null)
                banner.setHidden(bare);
            if(stand != null)
                stand.setHidden(bare);
            if(lamp != null)
                lamp.setHidden(!debugWagon.hasLantern());
        }
    }

    private static void spinAxle(AnimationProcessor<?> processor, String name, float rot) {
        GeoBone bone = processor.getBone(name);
        if(bone != null)
            bone.setRotX(rot);
    }

    private static void spinTongue(AnimationProcessor<?> processor, String name, float rot) {
        GeoBone bone = processor.getBone(name);
        if(bone != null)
            bone.setRotX(rot);
    }

    /** Empty corner hides every mesh. A filled corner shows regular, wide, or reinforced. */
    private static void hideWheelVariants(AnimationProcessor<?> processor, AbstractWagon wagon, String[][] variants) {
        for(int i = 0; i < variants.length; i++) {
            int show = wagon.getWheelKind(i).ordinal() - 1;
            String[] names = variants[i];
            for(int variant = 0; variant < names.length; variant++)
                hideBone(processor, names[variant], variant != show);
        }
    }

    /** Front left, front right, rear left, rear right. Each trio is regular, wide, reinforced. */
    private static final String[][] CONESTOGA_WHEEL_VARIANTS = {
            {"wheelleft", "LeftFrontWheel2", "wheelleft2"},
            {"wheelright", "RightFrontWheel2", "wheelright2"},
            {"bone", "LeftRearWheel2", "LeftRearWheel3"},
            {"bone2", "RightRearWheel2", "RightRearWheel3"}
    };
    private static final String[][] ROYAL_WHEEL_VARIANTS = {
            {"octagon2", "LeftFrontWheel2", "octagon5"},
            {"octagon", "RightFrontWheel2", "octagon6"},
            {"octagon4", "LeftFrontWheel3", "octagon9"},
            {"octagon3", "RightFrontWheel3", "octagon10"}
    };
    private static final String[][] ARMORED_WHEEL_VARIANTS = {
            {"bone3", "LeftFrontWheel2", "LeftFrontWheel3"},
            {"bone4", "RightFrontWheel2", "RightFrontWheel3"},
            {"bone", "LeftRearWheel2", "LeftRearWheel3"},
            {"bone2", "RightRearWheel2", "RightRearWheel3"}
    };
    private static final String[][] DEBUG_WHEEL_VARIANTS = {
            {"LeftFrontWheel", "LeftFrontWheel2", "LeftFrontWheel3"},
            {"RightFrontWheel", "RightFrontWheel2", "RightFrontWheel3"},
            {"LeftRearWheel", "LeftRearWheel2", "LeftRearWheel3"},
            {"RightRearWheel", "RightRearWheel2", "RightRearWheel3"}
    };

    /** Cart pitch is the inner rotation. Undoing it on the pile leaves only the side roll. */
    private static void levelLogs(AnimationProcessor<?> processor) {
        GeoBone pile = processor.getBone("Logs");
        GeoBone cart = processor.getBone("cart");
        if(pile != null && cart != null)
            pile.setRotX(-cart.getRotX());
    }

    private static void hideLogCargo(AnimationProcessor<?> processor, LogWagon wagon) {
        int logs = LogWagon.visibleLogs(wagon.syncedCargo());
        for(int i = 1; i <= LogWagon.LOG_BONES; i++)
            hideBone(processor, "Log_" + i, i > logs);
    }

    private static void hideBarrels(AnimationProcessor<?> processor, BarrelWagon wagon) {
        int count = wagon.barrelCount();
        for(int i = 1; i <= BarrelWagon.BARRELS; i++)
            hideBone(processor, "Barrel_" + i, i > count);
    }

    private static void hideBone(AnimationProcessor<?> processor, String name, boolean hidden) {
        GeoBone bone = processor.getBone(name);
        if(bone != null)
            bone.setHidden(hidden);
    }

    private static void poseAnimalGate(AnimationProcessor<?> processor, AnimalWagon wagon, float partialTick) {
        float open = wagon.gateOpen(partialTick) * AnimalWagon.GATE_OPEN;
        setGate(processor, "LeftGate", -open);
        setGate(processor, "RightGate", open);
    }

    private static void setGate(AnimationProcessor<?> processor, String name, float yaw) {
        GeoBone bone = processor.getBone(name);
        if(bone != null)
            bone.setRotY(yaw);
    }

    private static void poseKibitka(AnimationProcessor<?> processor, KibitkaWagon wagon, float partialTick) {
        hideBone(processor, "Lamp", !wagon.hasLantern());
        float linear = Mth.clamp(wagon.campRender(partialTick) / KibitkaWagon.CAMP_TICKS, 0, 1);
        float t = linear * linear * (3 - 2 * linear);
        if(KibitkaWagon.DOOR_OPENS) {
            GeoBone door = processor.getBone("Door");
            if(door != null)
                door.setRotY(t * KibitkaWagon.DOOR_OPEN);
        }
        poseTent(processor, "Tent_CLOSED", 1 - t);
        poseTent(processor, "Tent_OPEN", t);
    }

    /** Crossfade. A scale under 0.02 is hidden and reset so the closed tent does not leave a speck. */
    private static void poseTent(AnimationProcessor<?> processor, String name, float scale) {
        GeoBone bone = processor.getBone(name);
        if(bone == null)
            return;
        boolean hide = scale < 0.02F;
        bone.setHidden(hide);
        bone.updateScale(hide ? 1 : scale, hide ? 1 : scale, hide ? 1 : scale);
    }

    private static void hideWare(AnimationProcessor<?> processor, WareWagon wagon) {
        hideBone(processor, "Lamp", !wagon.hasLantern());
        int chests = wagon.chestCount();
        for(int i = 1; i <= WareWagon.CHESTS; i++)
            hideBone(processor, "chest" + i, i > chests);
    }

    private static void hideLamps(AnimationProcessor<?> processor, PassengerWagon wagon) {
        for(int i = 0; i < PassengerWagon.LANTERNS; i++)
            hideBone(processor, "Lamp" + (i + 1), !wagon.hasLantern(i));
    }

    private static void hidePumpkins(AnimationProcessor<?> processor, int count) {
        for(int i = 0; i < DebugWagon.PUMPKIN_SLOTS; i++) {
            GeoBone bone = processor.getBone("pumpkin" + (i + 1));
            if(bone != null)
                bone.setHidden(i >= count);
        }
    }

}
