package net.favouriteless.trotting_wagons.common.menus;

import net.favouriteless.trotting_wagons.common.entities.wagons.ConestogaWagon;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/** One cell of a conestoga chest page. The hold refuses items while a cabin seat is taken. */
public class ConestogaCargoSlot extends SlotItemHandler {

    private final ConestogaMenu menu;
    private final ConestogaWagon wagon;
    private final boolean hold;

    public ConestogaCargoSlot(ConestogaMenu menu, ConestogaWagon wagon, IItemHandler handler, int index, int x, int y, boolean hold) {
        super(handler, index, x, y);
        this.menu = menu;
        this.wagon = wagon;
        this.hold = hold;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return (!hold || wagon == null || !wagon.hasCabinPassenger()) && super.mayPlace(stack);
    }

    @Override
    public boolean isActive() {
        return menu.page() == (hold ? 1 : 0);
    }

}
