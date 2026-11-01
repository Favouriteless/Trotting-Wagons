package net.favouriteless.trotting_wagons.common.init;

import net.favouriteless.trotting_wagons.TrottingWagons;
import net.favouriteless.trotting_wagons.common.entities.wagons.AnimalWagon;
import net.favouriteless.trotting_wagons.common.entities.wagons.BarrelWagon;
import net.favouriteless.trotting_wagons.common.entities.wagons.BigCartWagon;
import net.favouriteless.trotting_wagons.common.entities.wagons.ArmoredWagon;
import net.favouriteless.trotting_wagons.common.entities.wagons.ConestogaWagon;
import net.favouriteless.trotting_wagons.common.entities.wagons.DebugWagon;
import net.favouriteless.trotting_wagons.common.entities.wagons.KibitkaWagon;
import net.favouriteless.trotting_wagons.common.entities.wagons.LogWagon;
import net.favouriteless.trotting_wagons.common.entities.wagons.PassengerWagon;
import net.favouriteless.trotting_wagons.common.entities.wagons.RoyalWagon;
import net.favouriteless.trotting_wagons.common.entities.wagons.WareWagon;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class TWEntityTypes {

    public static final DeferredRegister<EntityType<?>> REGISTRY = DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, TrottingWagons.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<ConestogaWagon>> CONESTOGA_WAGON = REGISTRY.register("conestoga_wagon", () ->
            EntityType.Builder.of(ConestogaWagon::new, MobCategory.MISC).sized(3.0F, 2.9F).build("conestoga_wagon"));

    public static final DeferredHolder<EntityType<?>, EntityType<RoyalWagon>> ROYAL_WAGON = REGISTRY.register("royal_wagon", () ->
            EntityType.Builder.of(RoyalWagon::new, MobCategory.MISC).sized(3.0F, 2.9F).build("royal_wagon"));

    public static final DeferredHolder<EntityType<?>, EntityType<ArmoredWagon>> ARMORED_WAGON = REGISTRY.register("armored_wagon", () ->
            EntityType.Builder.of(ArmoredWagon::new, MobCategory.MISC).sized(3.0F, 2.9F).build("armored_wagon"));

    public static final DeferredHolder<EntityType<?>, EntityType<DebugWagon>> DEBUG_WAGON = REGISTRY.register("debug_wagon", () ->
            EntityType.Builder.of(DebugWagon::new, MobCategory.MISC).sized(3.5F, 4.0F).build("debug_wagon"));

    public static final DeferredHolder<EntityType<?>, EntityType<AnimalWagon>> ANIMAL_WAGON = REGISTRY.register("animal_wagon", () ->
            EntityType.Builder.of(AnimalWagon::new, MobCategory.MISC).sized(3.5F, 2.75F).build("animal_wagon"));

    public static final DeferredHolder<EntityType<?>, EntityType<LogWagon>> LOG_WAGON = REGISTRY.register("log_wagon", () ->
            EntityType.Builder.of(LogWagon::new, MobCategory.MISC).sized(4.5F, 3.25F).build("log_wagon"));

    public static final DeferredHolder<EntityType<?>, EntityType<BarrelWagon>> BARREL_WAGON = REGISTRY.register("barrel_wagon", () ->
            EntityType.Builder.of(BarrelWagon::new, MobCategory.MISC).sized(4.5F, 3.25F).build("barrel_wagon"));

    public static final DeferredHolder<EntityType<?>, EntityType<PassengerWagon>> PASSENGER_WAGON = REGISTRY.register("passenger_wagon", () ->
            EntityType.Builder.of(PassengerWagon::new, MobCategory.MISC).sized(3.0F, 3.5F).build("passenger_wagon"));

    public static final DeferredHolder<EntityType<?>, EntityType<WareWagon>> WARE_WAGON = REGISTRY.register("ware_wagon", () ->
            EntityType.Builder.of(WareWagon::new, MobCategory.MISC).sized(3.5F, 2.75F).build("ware_wagon"));

    public static final DeferredHolder<EntityType<?>, EntityType<KibitkaWagon>> KIBITKA_WAGON = REGISTRY.register("kibitka_wagon", () ->
            EntityType.Builder.of(KibitkaWagon::new, MobCategory.MISC).sized(3.5F, 3.85F).build("kibitka_wagon"));

    public static final DeferredHolder<EntityType<?>, EntityType<BigCartWagon>> BIG_CART_WAGON = REGISTRY.register("big_cart", () ->
            EntityType.Builder.of(BigCartWagon::new, MobCategory.MISC).sized(4.5F, 2.5F).build("big_cart"));

}
