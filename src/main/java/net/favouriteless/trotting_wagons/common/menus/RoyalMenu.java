package net.favouriteless.trotting_wagons.common.menus;

import net.favouriteless.trotting_wagons.common.entities.base.AbstractInventoryWagon;
import net.favouriteless.trotting_wagons.common.init.TWMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

public class RoyalMenu extends AbstractWagonMenu {

    public RoyalMenu(int id, Inventory inventory, AbstractInventoryWagon wagon) {
        super(TWMenuTypes.ROYAL.get(), id, inventory, wagon, 9, 8, 140);
    }

    public RoyalMenu(int id, Inventory inventory, FriendlyByteBuf data) {
        this(id, inventory, getWagon(inventory, data));
    }

}
