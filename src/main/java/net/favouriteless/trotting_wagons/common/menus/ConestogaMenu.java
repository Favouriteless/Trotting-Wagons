package net.favouriteless.trotting_wagons.common.menus;

import net.favouriteless.trotting_wagons.common.entities.base.AbstractInventoryWagon;
import net.favouriteless.trotting_wagons.common.init.TWMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

public class ConestogaMenu extends AbstractWagonMenu {

    public ConestogaMenu(int id, Inventory inventory, AbstractInventoryWagon wagon) {
        super(TWMenuTypes.CONESTOGA.get(), id, inventory, wagon, 17, 80, 140);
    }

    public ConestogaMenu(int id, Inventory inventory, FriendlyByteBuf data) {
        this(id, inventory, getWagon(inventory, data));
    }

}
