package net.favouriteless.trotting_wagons.common;

import net.favouriteless.trotting_wagons.TrottingWagons;
import net.favouriteless.trotting_wagons.common.entities.base.AbstractInventoryWagon;
import net.favouriteless.trotting_wagons.common.entities.wagons.LogWagon;
import net.favouriteless.trotting_wagons.common.init.TWEntityTypes;
import net.favouriteless.trotting_wagons.common.network.packets.ClearBaseBuildPayload;
import net.favouriteless.trotting_wagons.common.network.packets.EjectSeatPayload;
import net.favouriteless.trotting_wagons.common.network.packets.HitchAnimalPayload;
import net.favouriteless.trotting_wagons.common.network.packets.MoveAnimalPayload;
import net.favouriteless.trotting_wagons.common.network.packets.OpenWagonMenuPayload;
import net.favouriteless.trotting_wagons.common.network.packets.SelectSeatPayload;
import net.favouriteless.trotting_wagons.common.network.packets.SelectBaseBuildPayload;
import net.favouriteless.trotting_wagons.common.network.packets.SelectConestogaPagePayload;
import net.favouriteless.trotting_wagons.common.network.packets.SelectWareTabPayload;
import net.favouriteless.trotting_wagons.common.network.packets.SteeringPayload;
import net.favouriteless.trotting_wagons.common.network.packets.SyncSeatsPayload;
import net.favouriteless.trotting_wagons.common.network.packets.KibitkaRespawnPayload;
import net.favouriteless.trotting_wagons.common.network.packets.KibitkaSleepPayload;
import net.favouriteless.trotting_wagons.common.network.packets.ToggleKibitkaCampPayload;
import net.favouriteless.trotting_wagons.common.network.packets.ToggleLadderPayload;
import net.favouriteless.trotting_wagons.common.network.packets.ToggleSeatLockPayload;
import net.favouriteless.trotting_wagons.common.network.packets.ToggleWagonLockPayload;
import net.favouriteless.trotting_wagons.common.network.packets.WhipLowerGearPayload;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities.ItemHandler;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = TrottingWagons.MOD_ID)
public class CommonSetupEvents {

    @SubscribeEvent
    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.registerEntity(ItemHandler.ENTITY, TWEntityTypes.CONESTOGA_WAGON.get(), AbstractInventoryWagon::getItemCapability);
        event.registerEntity(ItemHandler.ENTITY, TWEntityTypes.ROYAL_WAGON.get(), AbstractInventoryWagon::getItemCapability);
        event.registerEntity(ItemHandler.ENTITY, TWEntityTypes.DEBUG_WAGON.get(), AbstractInventoryWagon::getItemCapability);
        event.registerEntity(ItemHandler.ENTITY, TWEntityTypes.LOG_WAGON.get(), LogWagon::getItemCapability);
        event.registerEntity(ItemHandler.ENTITY, TWEntityTypes.BIG_CART_WAGON.get(), AbstractInventoryWagon::getItemCapability);
    }

    @SubscribeEvent
    public static void onRegisterPayloadHandlers(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(SteeringPayload.TYPE, SteeringPayload.CODEC, SteeringPayload::handle);
        registrar.playToServer(HitchAnimalPayload.TYPE, HitchAnimalPayload.CODEC, HitchAnimalPayload::handle);
        registrar.playToServer(OpenWagonMenuPayload.TYPE, OpenWagonMenuPayload.CODEC, OpenWagonMenuPayload::handle);
        registrar.playToServer(SelectSeatPayload.TYPE, SelectSeatPayload.CODEC, SelectSeatPayload::handle);
        registrar.playToServer(SelectBaseBuildPayload.TYPE, SelectBaseBuildPayload.CODEC, SelectBaseBuildPayload::handle);
        registrar.playToServer(ClearBaseBuildPayload.TYPE, ClearBaseBuildPayload.CODEC, ClearBaseBuildPayload::handle);
        registrar.playToServer(SelectWareTabPayload.TYPE, SelectWareTabPayload.CODEC, SelectWareTabPayload::handle);
        registrar.playToServer(SelectConestogaPagePayload.TYPE, SelectConestogaPagePayload.CODEC, SelectConestogaPagePayload::handle);
        registrar.playToServer(EjectSeatPayload.TYPE, EjectSeatPayload.CODEC, EjectSeatPayload::handle);
        registrar.playToServer(MoveAnimalPayload.TYPE, MoveAnimalPayload.CODEC, MoveAnimalPayload::handle);
        registrar.playToServer(ToggleSeatLockPayload.TYPE, ToggleSeatLockPayload.CODEC, ToggleSeatLockPayload::handle);
        registrar.playToServer(ToggleWagonLockPayload.TYPE, ToggleWagonLockPayload.CODEC, ToggleWagonLockPayload::handle);
        registrar.playToServer(ToggleLadderPayload.TYPE, ToggleLadderPayload.CODEC, ToggleLadderPayload::handle);
        registrar.playToServer(ToggleKibitkaCampPayload.TYPE, ToggleKibitkaCampPayload.CODEC, ToggleKibitkaCampPayload::handle);
        registrar.playToServer(KibitkaSleepPayload.TYPE, KibitkaSleepPayload.CODEC, KibitkaSleepPayload::handle);
        registrar.playToServer(KibitkaRespawnPayload.TYPE, KibitkaRespawnPayload.CODEC, KibitkaRespawnPayload::handle);
        registrar.playToServer(WhipLowerGearPayload.TYPE, WhipLowerGearPayload.CODEC, WhipLowerGearPayload::handle);
        registrar.playToClient(SyncSeatsPayload.TYPE, SyncSeatsPayload.CODEC, SyncSeatsPayload::handle);
    }
}
