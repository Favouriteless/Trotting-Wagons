package net.favouriteless.trotting_wagons.common.init;

import net.favouriteless.trotting_wagons.TrottingWagons;
import net.favouriteless.trotting_wagons.common.menus.ConestogaMenu;
import net.favouriteless.trotting_wagons.common.menus.RoyalMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class TWMenuTypes {

    public static final DeferredRegister<MenuType<?>> REGISTRY = DeferredRegister.create(ForgeRegistries.MENU_TYPES, TrottingWagons.MOD_ID);

    public static final RegistryObject<MenuType<ConestogaMenu>> CONESTOGA = REGISTRY.register("conestoga_wagon", () -> IForgeMenuType.create(ConestogaMenu::new));
    public static final RegistryObject<MenuType<RoyalMenu>> ROYAL = REGISTRY.register("royal_wagon", () -> IForgeMenuType.create(RoyalMenu::new));

}
