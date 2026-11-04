package net.favouriteless.trotting_wagons.common.network.packets;

import io.netty.buffer.ByteBuf;
import net.favouriteless.trotting_wagons.TrottingWagons;
import net.favouriteless.trotting_wagons.common.entities.BaseWagon;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SelectBaseBuildPayload(int wagonId, String choice) implements CustomPacketPayload {

    public static final Type<SelectBaseBuildPayload> TYPE = new Type<>(TrottingWagons.id("select_base_build"));
    public static final StreamCodec<ByteBuf, SelectBaseBuildPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, SelectBaseBuildPayload::wagonId,
            ByteBufCodecs.STRING_UTF8, SelectBaseBuildPayload::choice,
            SelectBaseBuildPayload::new
    );

    public static void handle(SelectBaseBuildPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Player player = ctx.player();
            if(!(player.level().getEntity(payload.wagonId()) instanceof BaseWagon wagon))
                return;
            if(player.distanceToSqr(wagon) > 64.0D || !BaseWagon.allowed(payload.choice()))
                return;
            wagon.choose(payload.choice(), player);
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}
