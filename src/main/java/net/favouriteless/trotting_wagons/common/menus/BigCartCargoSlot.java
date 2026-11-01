package net.favouriteless.trotting_wagons.common.menus;

import net.favouriteless.trotting_wagons.common.entities.wagons.BigCartWagon;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/** Cargo cell. Hidden and closed while an animal is in the bed. */
public class BigCartCargoSlot extends SlotItemHandler {

    private final BigCartWagon wagon;

    public BigCartCargoSlot(BigCartWagon wagon, IItemHandler handler, int index, int x, int y) {
        super(handler, index, x, y);
        this.wagon = wagon;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return (wagon == null || !wagon.hasCabinPassenger()) && super.mayPlace(stack);
    }

    @Override
    public boolean isActive() {
        return wagon == null || !wagon.hasCabinPassenger();
    }

}
