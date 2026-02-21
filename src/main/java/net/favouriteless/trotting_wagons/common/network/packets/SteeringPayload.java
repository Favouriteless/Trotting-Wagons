package net.favouriteless.trotting_wagons.common.network.packets;

import io.netty.buffer.ByteBuf;
import net.favouriteless.trotting_wagons.TrottingWagons;
import net.favouriteless.trotting_wagons.common.entities.base.AbstractGeckolibVehicle;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SteeringPayload(int vehicle, float impulse) implements CustomPacketPayload {

    public static final Type<SteeringPayload> TYPE = new Type<>(TrottingWagons.id("vehicle_steer"));

    public static final StreamCodec<ByteBuf, SteeringPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, SteeringPayload::vehicle,
            ByteBufCodecs.FLOAT, SteeringPayload::impulse,
            SteeringPayload::new
    );

    public static void handle(SteeringPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if(ctx.player().level().getEntity(payload.vehicle()) instanceof AbstractGeckolibVehicle vehicle) {
                if(ctx.player() != vehicle.getControllingPassenger())
                    return;
                vehicle.setSteeringImpulse(Mth.clamp(payload.impulse, -1, 1));
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}
