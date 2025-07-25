package net.favouriteless.trotting_wagons.common.network.packets;

import net.favouriteless.trotting_wagons.common.entities.base.AbstractGeckolibVehicle;
import net.favouriteless.trotting_wagons.common.network.TWPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraftforge.network.NetworkEvent.Context;

public class VehicleSteerPacket implements TWPacket {

    private int vehicle;
    private float impulse;

    public VehicleSteerPacket(int vehicle, float impulse) {
        this.vehicle = vehicle;
        this.impulse = impulse;
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeInt(vehicle);
        buffer.writeFloat(impulse);
    }

    public static VehicleSteerPacket decode(FriendlyByteBuf buffer) {
        return new VehicleSteerPacket(buffer.readInt(), buffer.readFloat());
    }

    @Override
    public void handle(Context ctx) {
        ctx.enqueueWork(() -> {
            if(ctx.getSender().level().getEntity(vehicle) instanceof AbstractGeckolibVehicle vehicle) {
                if(ctx.getSender() != vehicle.getControllingPassenger())
                    return;
                vehicle.setSteeringImpulse(Mth.clamp(impulse, -1, 1));
            }
        });
        ctx.setPacketHandled(true);
    }

}
