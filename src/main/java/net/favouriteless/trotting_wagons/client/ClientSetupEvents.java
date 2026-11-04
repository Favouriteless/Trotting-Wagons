package net.favouriteless.trotting_wagons.client;

import net.favouriteless.trotting_wagons.TrottingWagons;
import net.favouriteless.trotting_wagons.client.render.WagonItemRenderer;
import net.favouriteless.trotting_wagons.client.render.entity.BaseWagonRenderer;
import net.favouriteless.trotting_wagons.client.render.entity.ChassisRenderer;
import net.favouriteless.trotting_wagons.client.render.entity.WagonRenderer;
import net.favouriteless.trotting_wagons.client.screens.AnimalScreen;
import net.favouriteless.trotting_wagons.client.screens.BaseBuildScreen;
import net.favouriteless.trotting_wagons.client.screens.BarrelScreen;
import net.favouriteless.trotting_wagons.client.screens.BigCartScreen;
import net.favouriteless.trotting_wagons.client.screens.ArmoredScreen;
import net.favouriteless.trotting_wagons.client.screens.ConestogaScreen;
import net.favouriteless.trotting_wagons.client.screens.DebugScreen;
import net.favouriteless.trotting_wagons.client.screens.KibitkaScreen;
import net.favouriteless.trotting_wagons.client.screens.LogScreen;
import net.favouriteless.trotting_wagons.client.screens.PassengerScreen;
import net.favouriteless.trotting_wagons.client.screens.RoyalScreen;
import net.favouriteless.trotting_wagons.client.screens.WareScreen;
import net.favouriteless.trotting_wagons.common.entities.base.AbstractWagon;
import net.favouriteless.trotting_wagons.common.init.TWEntityTypes;
import net.favouriteless.trotting_wagons.common.init.TWItems;
import net.favouriteless.trotting_wagons.common.init.TWMenuTypes;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.entity.EntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

import java.util.function.Supplier;

@EventBusSubscriber(modid = TrottingWagons.MOD_ID, value = Dist.CLIENT)
public class ClientSetupEvents {

    @SubscribeEvent
    public static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(wagonExtension(TWEntityTypes.ARMORED_WAGON.get()), TWItems.ARMORED_WAGON.get());
        event.registerItem(wagonExtension(TWEntityTypes.CONESTOGA_WAGON.get()), TWItems.CONESTOGA_WAGON.get());
        event.registerItem(wagonExtension(TWEntityTypes.ROYAL_WAGON.get()), TWItems.ROYAL_WAGON.get());
        event.registerItem(wagonExtension(TWEntityTypes.DEBUG_WAGON.get()), TWItems.DEBUG_WAGON.get());
        event.registerItem(wagonExtension(TWEntityTypes.ANIMAL_WAGON.get()), TWItems.ANIMAL_WAGON.get());
        event.registerItem(wagonExtension(TWEntityTypes.LOG_WAGON.get()), TWItems.LOG_WAGON.get());
        event.registerItem(wagonExtension(TWEntityTypes.BARREL_WAGON.get()), TWItems.BARREL_WAGON.get());
        event.registerItem(wagonExtension(TWEntityTypes.PASSENGER_WAGON.get()), TWItems.PASSENGER_WAGON.get());
        event.registerItem(wagonExtension(TWEntityTypes.WARE_WAGON.get()), TWItems.WARE_WAGON.get());
        event.registerItem(wagonExtension(TWEntityTypes.KIBITKA_WAGON.get()), TWItems.KIBITKA_WAGON.get());
        event.registerItem(wagonExtension(TWEntityTypes.BIG_CART_WAGON.get()), TWItems.BIG_CART_WAGON.get());
    }

    @SubscribeEvent
    public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(TWMenuTypes.CONESTOGA.get(), ConestogaScreen::new);
        event.register(TWMenuTypes.ROYAL.get(), RoyalScreen::new);
        event.register(TWMenuTypes.ARMORED.get(), ArmoredScreen::new);
        event.register(TWMenuTypes.DEBUG.get(), DebugScreen::new);
        event.register(TWMenuTypes.ANIMAL.get(), AnimalScreen::new);
        event.register(TWMenuTypes.LOG.get(), LogScreen::new);
        event.register(TWMenuTypes.BARREL.get(), BarrelScreen::new);
        event.register(TWMenuTypes.PASSENGER.get(), PassengerScreen::new);
        event.register(TWMenuTypes.WARE.get(), WareScreen::new);
        event.register(TWMenuTypes.KIBITKA.get(), KibitkaScreen::new);
        event.register(TWMenuTypes.BIG_CART.get(), BigCartScreen::new);
        event.register(TWMenuTypes.BASE_BUILD.get(), BaseBuildScreen::new);
    }

    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(TWClientEvents.HITCH_KEY);
    }

    @SubscribeEvent
    public static void onRegisterEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(TWEntityTypes.CONESTOGA_WAGON.get(), context -> new WagonRenderer<>(context, TrottingWagons.id("conestoga_wagon")));
        event.registerEntityRenderer(TWEntityTypes.ROYAL_WAGON.get(), context -> new WagonRenderer<>(context, TrottingWagons.id("royal_wagon")));
        event.registerEntityRenderer(TWEntityTypes.ARMORED_WAGON.get(), context -> new WagonRenderer<>(context, TrottingWagons.id("armored_wagon")));
        event.registerEntityRenderer(TWEntityTypes.DEBUG_WAGON.get(), context -> new WagonRenderer<>(context, TrottingWagons.id("debug_wagon")));
        event.registerEntityRenderer(TWEntityTypes.ANIMAL_WAGON.get(), context -> new WagonRenderer<>(context, TrottingWagons.id("animal_wagon")));
        event.registerEntityRenderer(TWEntityTypes.LOG_WAGON.get(), context -> new WagonRenderer<>(context, TrottingWagons.id("log_wagon")));
        event.registerEntityRenderer(TWEntityTypes.BARREL_WAGON.get(), context -> new WagonRenderer<>(context, TrottingWagons.id("barrel_wagon")));
        event.registerEntityRenderer(TWEntityTypes.PASSENGER_WAGON.get(), context -> new WagonRenderer<>(context, TrottingWagons.id("passenger_wagon")));
        event.registerEntityRenderer(TWEntityTypes.WARE_WAGON.get(), context -> new WagonRenderer<>(context, TrottingWagons.id("ware_wagon")));
        event.registerEntityRenderer(TWEntityTypes.KIBITKA_WAGON.get(), context -> new WagonRenderer<>(context, TrottingWagons.id("kibitka_wagon")));
        event.registerEntityRenderer(TWEntityTypes.BIG_CART_WAGON.get(), context -> new WagonRenderer<>(context, TrottingWagons.id("big_cart")));
        event.registerEntityRenderer(TWEntityTypes.CHASSIS.get(), ChassisRenderer::new);
        event.registerEntityRenderer(TWEntityTypes.BASE_WAGON.get(), BaseWagonRenderer::new);
    }

    public static IClientItemExtensions wagonExtension(EntityType<? extends AbstractWagon> entityType) {
        return new IClientItemExtensions() {

            private final WagonItemRenderer renderer = new WagonItemRenderer(entityType);

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return renderer;
            }

        };
    }

}
