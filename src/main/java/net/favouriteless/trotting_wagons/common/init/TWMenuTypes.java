package net.favouriteless.trotting_wagons.common.init;

import net.favouriteless.trotting_wagons.TrottingWagons;
import net.favouriteless.trotting_wagons.common.menus.AnimalMenu;
import net.favouriteless.trotting_wagons.common.menus.BaseBuildMenu;
import net.favouriteless.trotting_wagons.common.menus.BarrelMenu;
import net.favouriteless.trotting_wagons.common.menus.BigCartMenu;
import net.favouriteless.trotting_wagons.common.menus.ArmoredMenu;
import net.favouriteless.trotting_wagons.common.menus.ConestogaMenu;
import net.favouriteless.trotting_wagons.common.menus.DebugMenu;
import net.favouriteless.trotting_wagons.common.menus.KibitkaMenu;
import net.favouriteless.trotting_wagons.common.menus.LogMenu;
import net.favouriteless.trotting_wagons.common.menus.PassengerMenu;
import net.favouriteless.trotting_wagons.common.menus.RoyalMenu;
import net.favouriteless.trotting_wagons.common.menus.WareMenu;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;


public class TWMenuTypes {

    public static final DeferredRegister<MenuType<?>> REGISTRY = DeferredRegister.create(BuiltInRegistries.MENU, TrottingWagons.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<ConestogaMenu>> CONESTOGA = REGISTRY.register("conestoga_wagon", () -> IMenuTypeExtension.create(ConestogaMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<RoyalMenu>> ROYAL = REGISTRY.register("royal_wagon", () -> IMenuTypeExtension.create(RoyalMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<ArmoredMenu>> ARMORED = REGISTRY.register("armored_wagon", () -> IMenuTypeExtension.create(ArmoredMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<DebugMenu>> DEBUG = REGISTRY.register("debug_wagon", () -> IMenuTypeExtension.create(DebugMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<AnimalMenu>> ANIMAL = REGISTRY.register("animal_wagon", () -> IMenuTypeExtension.create(AnimalMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<LogMenu>> LOG = REGISTRY.register("log_wagon", () -> IMenuTypeExtension.create(LogMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<BarrelMenu>> BARREL = REGISTRY.register("barrel_wagon", () -> IMenuTypeExtension.create(BarrelMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<PassengerMenu>> PASSENGER = REGISTRY.register("passenger_wagon", () -> IMenuTypeExtension.create(PassengerMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<WareMenu>> WARE = REGISTRY.register("ware_wagon", () -> IMenuTypeExtension.create(WareMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<KibitkaMenu>> KIBITKA = REGISTRY.register("kibitka_wagon", () -> IMenuTypeExtension.create(KibitkaMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<BigCartMenu>> BIG_CART = REGISTRY.register("big_cart", () -> IMenuTypeExtension.create(BigCartMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<BaseBuildMenu>> BASE_BUILD = REGISTRY.register("base_build", () -> IMenuTypeExtension.create(BaseBuildMenu::new));

}
