package net.favouriteless.trotting_wagons.client;

import net.favouriteless.trotting_wagons.common.entities.base.AbstractGeckolibVehicle;
import net.favouriteless.trotting_wagons.common.entities.base.AbstractWagon;
import net.favouriteless.trotting_wagons.common.init.TWPackets;
import net.favouriteless.trotting_wagons.common.network.packets.VehicleSteerPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public class TWClient {

    public static void createWagonSound(AbstractWagon wagon) {
        Minecraft.getInstance().getSoundManager().play(new WagonSoundInstance(wagon));
    }

    public static void controlVehicle(AbstractGeckolibVehicle vehicle) {
        LocalPlayer player = Minecraft.getInstance().player;
        if(player == null)
            return;

        TWPackets.INSTANCE.sendToServer(new VehicleSteerPacket(vehicle.getId(), player.input.leftImpulse));
    }

}
