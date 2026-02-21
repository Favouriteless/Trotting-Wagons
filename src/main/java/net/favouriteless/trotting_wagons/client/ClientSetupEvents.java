package net.favouriteless.trotting_wagons.client;

import net.favouriteless.trotting_wagons.TrottingWagons;
import net.favouriteless.trotting_wagons.client.render.WagonItemRenderer;
import net.favouriteless.trotting_wagons.client.render.entity.WagonRenderer;
import net.favouriteless.trotting_wagons.client.screens.ConestogaScreen;
import net.favouriteless.trotting_wagons.client.screens.RoyalScreen;
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
    }

    @SubscribeEvent
    public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(TWMenuTypes.CONESTOGA.get(), ConestogaScreen::new);
        event.register(TWMenuTypes.ROYAL.get(), RoyalScreen::new);
    }

    @SubscribeEvent
    public static void onRegisterEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(TWEntityTypes.CONESTOGA_WAGON.get(), context -> new WagonRenderer<>(context, TrottingWagons.id("conestoga_wagon")));
        event.registerEntityRenderer(TWEntityTypes.ROYAL_WAGON.get(), context -> new WagonRenderer<>(context, TrottingWagons.id("royal_wagon")));
        event.registerEntityRenderer(TWEntityTypes.ARMORED_WAGON.get(), context -> new WagonRenderer<>(context, TrottingWagons.id("armored_wagon")));
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
