package net.favouriteless.trotting_wagons.common;

import net.favouriteless.trotting_wagons.TrottingWagons;
import net.favouriteless.trotting_wagons.common.entities.base.AbstractInventoryWagon;
import net.favouriteless.trotting_wagons.common.init.TWEntityTypes;
import net.favouriteless.trotting_wagons.common.init.TWItems;
import net.favouriteless.trotting_wagons.common.items.WagonItem;
import net.favouriteless.trotting_wagons.common.network.packets.SteeringPayload;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities.ItemHandler;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = TrottingWagons.MOD_ID)
public class CommonSetupEvents {

    @SubscribeEvent
    public static void onCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if(!event.getTabKey().equals(CreativeModeTabs.REDSTONE_BLOCKS))
            return;

        event.accept(TWItems.ARMORED_WAGON.get());
        event.accept(WagonItem.setupComponents(new ItemStack(TWItems.CONESTOGA_WAGON.get()), DyeColor.WHITE));
        event.accept(WagonItem.setupComponents(new ItemStack(TWItems.ROYAL_WAGON.get()), DyeColor.WHITE));
        event.accept(TWItems.HORSE_WHIP.get());
        event.accept(TWItems.WHEEL.get());
    }

    @SubscribeEvent
    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.registerEntity(ItemHandler.ENTITY, TWEntityTypes.CONESTOGA_WAGON.get(), AbstractInventoryWagon::getItemCapability);
        event.registerEntity(ItemHandler.ENTITY, TWEntityTypes.ROYAL_WAGON.get(), AbstractInventoryWagon::getItemCapability);
    }

    @SubscribeEvent
    public static void onRegisterPayloadHandlers(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(SteeringPayload.TYPE, SteeringPayload.CODEC, SteeringPayload::handle);
    }

}
