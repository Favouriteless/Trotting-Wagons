package net.favouriteless.trotting_wagons.common.init;

import net.favouriteless.trotting_wagons.TrottingWagons;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class TWSoundEvents {

    public static final DeferredRegister<SoundEvent> REGISTRY = DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, TrottingWagons.MOD_ID);

    public static final DeferredHolder<SoundEvent, SoundEvent> WAGON = REGISTRY.register("wagon", () -> SoundEvent.createVariableRangeEvent(TrottingWagons.id("wagon")));
    public static final DeferredHolder<SoundEvent, SoundEvent> WHIP = REGISTRY.register("whip", () -> SoundEvent.createVariableRangeEvent(TrottingWagons.id("whip")));

}
