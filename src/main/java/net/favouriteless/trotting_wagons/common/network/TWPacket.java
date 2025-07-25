package net.favouriteless.trotting_wagons.common.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent.Context;

public interface TWPacket {

	/**
	 * Write this packet to a {@link FriendlyByteBuf}.
	 * @param buffer The {@link FriendlyByteBuf} to be written to.
	 */
	void encode(FriendlyByteBuf buffer);

	/**
	 * Handle receiving this packet.
	 * @param sender (Server) The player who sent the packet.
	 *               <p>(Client) null.</p>
	 */
	void handle(Context context);

}