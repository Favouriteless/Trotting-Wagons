package net.favouriteless.trotting_wagons.common.network.packets;

import io.netty.buffer.ByteBuf;
import net.favouriteless.trotting_wagons.TrottingWagons;
import net.favouriteless.trotting_wagons.common.entities.BaseWagon;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ClearBaseBuildPayload(int wagonId) implements CustomPacketPayload {

    public static final Type<ClearBaseBuildPayload> TYPE = new Type<>(TrottingWagons.id("clear_base_build"));
    public static final StreamCodec<ByteBuf, ClearBaseBuildPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, ClearBaseBuildPayload::wagonId,
            ClearBaseBuildPayload::new
    );

    public static void handle(ClearBaseBuildPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Player player = ctx.player();
            if(!(player.level().getEntity(payload.wagonId()) instanceof BaseWagon wagon))
                return;
            if(player.distanceToSqr(wagon) > 64.0D)
                return;
            wagon.clearSelection(player);
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}
