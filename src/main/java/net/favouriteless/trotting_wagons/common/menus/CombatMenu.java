package net.favouriteless.trotting_wagons.common.menus;

import net.favouriteless.trotting_wagons.common.entities.base.AbstractInventoryWagon;
import net.favouriteless.trotting_wagons.common.init.TWMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

public class CombatMenu extends AbstractWagonMenu {

    public CombatMenu(int id, Inventory inventory, AbstractInventoryWagon wagon) {
        super(TWMenuTypes.COMBAT.get(), id, inventory, wagon, 9, 34, 8, 50);
    }

    public CombatMenu(int id, Inventory inventory, FriendlyByteBuf data) {
        this(id, inventory, getWagon(inventory, data));
    }

}
