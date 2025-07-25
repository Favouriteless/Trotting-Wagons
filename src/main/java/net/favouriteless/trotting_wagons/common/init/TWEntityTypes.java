package net.favouriteless.trotting_wagons.common.init;

import net.favouriteless.trotting_wagons.TrottingWagons;
import net.favouriteless.trotting_wagons.common.entities.wagons.ArmoredWagon;
import net.favouriteless.trotting_wagons.common.entities.wagons.ConestogaWagon;
import net.favouriteless.trotting_wagons.common.entities.wagons.RoyalWagon;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class TWEntityTypes {

    public static final DeferredRegister<EntityType<?>> REGISTRY = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, TrottingWagons.MOD_ID);

    public static final RegistryObject<EntityType<ConestogaWagon>> CONESTOGA_WAGON = REGISTRY.register("conestoga_wagon", () ->
            EntityType.Builder.of(ConestogaWagon::new, MobCategory.MISC).sized(3.0F, 2.9F).build("conestoga_wagon"));

    public static final RegistryObject<EntityType<RoyalWagon>> ROYAL_WAGON = REGISTRY.register("royal_wagon", () ->
            EntityType.Builder.of(RoyalWagon::new, MobCategory.MISC).sized(3.0F, 2.9F).build("royal_wagon"));

    public static final RegistryObject<EntityType<ArmoredWagon>> ARMORED_WAGON = REGISTRY.register("armored_wagon", () ->
            EntityType.Builder.of(ArmoredWagon::new, MobCategory.MISC).sized(3.0F, 2.9F).build("armored_wagon"));

}
