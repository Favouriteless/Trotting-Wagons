package net.favouriteless.trotting_wagons.client;

import net.favouriteless.trotting_wagons.TrottingWagons;
import net.favouriteless.trotting_wagons.client.render.entity.WagonRenderer;
import net.favouriteless.trotting_wagons.client.screens.ConestogaScreen;
import net.favouriteless.trotting_wagons.client.screens.RoyalScreen;
import net.favouriteless.trotting_wagons.common.init.TWEntityTypes;
import net.favouriteless.trotting_wagons.common.init.TWMenuTypes;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(modid = TrottingWagons.MOD_ID, bus = Bus.MOD, value = Dist.CLIENT)
public class ClientSetupEvents {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        MenuScreens.register(TWMenuTypes.CONESTOGA.get(), ConestogaScreen::new);
        MenuScreens.register(TWMenuTypes.ROYAL.get(), RoyalScreen::new);
    }

    @SubscribeEvent
    public static void onRegisterEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(TWEntityTypes.CONESTOGA_WAGON.get(), context -> new WagonRenderer<>(context, TrottingWagons.id("conestoga_wagon")));
        event.registerEntityRenderer(TWEntityTypes.ROYAL_WAGON.get(), context -> new WagonRenderer<>(context, TrottingWagons.id("royal_wagon")));
        event.registerEntityRenderer(TWEntityTypes.ARMORED_WAGON.get(), context -> new WagonRenderer<>(context, TrottingWagons.id("armored_wagon")));
    }

}
