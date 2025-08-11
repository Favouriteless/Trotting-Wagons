package net.favouriteless.trotting_wagons.common.init;

import net.favouriteless.trotting_wagons.TrottingWagons;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class TWSoundEvents {

    public static final DeferredRegister<SoundEvent> REGISTRY = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, TrottingWagons.MOD_ID);

    public static final RegistryObject<SoundEvent> WAGON = REGISTRY.register("wagon", () -> SoundEvent.createVariableRangeEvent(TrottingWagons.id("wagon")));
    public static final RegistryObject<SoundEvent> WHIP = REGISTRY.register("whip", () -> SoundEvent.createVariableRangeEvent(TrottingWagons.id("whip")));

}
