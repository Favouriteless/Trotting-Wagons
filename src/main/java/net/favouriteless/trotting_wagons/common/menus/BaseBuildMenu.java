package net.favouriteless.trotting_wagons.common.menus;

import net.favouriteless.trotting_wagons.common.entities.BaseWagon;
import net.favouriteless.trotting_wagons.common.init.TWMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public class BaseBuildMenu extends AbstractContainerMenu {

    public static final int WIDTH = 166;
    public static final int HEIGHT = 112;

    private final BaseWagon wagon;

    public BaseBuildMenu(int id, Inventory inventory, BaseWagon wagon) {
        super(TWMenuTypes.BASE_BUILD.get(), id);
        this.wagon = wagon;
    }

    public BaseBuildMenu(int id, Inventory inventory, FriendlyByteBuf data) {
        this(id, inventory, find(inventory, data));
    }

    public BaseWagon getWagon() {
        return wagon;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return wagon != null && wagon.isAlive() && player.distanceToSqr(wagon) < 64.0D;
    }

    private static BaseWagon find(Inventory inventory, FriendlyByteBuf data) {
        return inventory.player.level().getEntity(data.readInt()) instanceof BaseWagon wagon ? wagon : null;
    }

}
