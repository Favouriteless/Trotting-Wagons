package net.favouriteless.trotting_wagons.common.network.packets;

import io.netty.buffer.ByteBuf;
import net.favouriteless.trotting_wagons.TrottingWagons;
import net.favouriteless.trotting_wagons.common.menus.ConestogaMenu;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SelectConestogaPagePayload(int page) implements CustomPacketPayload {

    public static final Type<SelectConestogaPagePayload> TYPE = new Type<>(TrottingWagons.id("select_conestoga_page"));
    public static final StreamCodec<ByteBuf, SelectConestogaPagePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SelectConestogaPagePayload::page,
            SelectConestogaPagePayload::new
    );

    public static void handle(SelectConestogaPagePayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Player player = ctx.player();
            if(player.containerMenu instanceof ConestogaMenu menu)
                menu.setPage(payload.page());
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}
