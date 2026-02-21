package net.favouriteless.trotting_wagons.common.init;

import net.favouriteless.trotting_wagons.TrottingWagons;
import net.favouriteless.trotting_wagons.common.menus.ConestogaMenu;
import net.favouriteless.trotting_wagons.common.menus.RoyalMenu;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;


public class TWMenuTypes {

    public static final DeferredRegister<MenuType<?>> REGISTRY = DeferredRegister.create(BuiltInRegistries.MENU, TrottingWagons.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<ConestogaMenu>> CONESTOGA = REGISTRY.register("conestoga_wagon", () -> IMenuTypeExtension.create(ConestogaMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<RoyalMenu>> ROYAL = REGISTRY.register("royal_wagon", () -> IMenuTypeExtension.create(RoyalMenu::new));

}
