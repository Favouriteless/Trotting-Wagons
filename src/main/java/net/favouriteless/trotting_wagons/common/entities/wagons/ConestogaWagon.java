package net.favouriteless.trotting_wagons.common.entities.wagons;

import net.favouriteless.trotting_wagons.ServerConfig;
import net.favouriteless.trotting_wagons.common.entities.base.AbstractInventoryWagon;
import net.favouriteless.trotting_wagons.common.init.TWItems;
import net.favouriteless.trotting_wagons.common.items.WagonItem;
import net.favouriteless.trotting_wagons.common.menus.ConestogaMenu;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class ConestogaWagon extends AbstractInventoryWagon {

    public static final Vec3[] RIDERS = new Vec3[] {
            new Vec3(0, 1.25D, 2.1D),
            new Vec3(-0.4375D, 1.18D, 1.1D),
            new Vec3(0.4375D, 1.18D, 1.1D),
            new Vec3(-0.4375D, 1.18D, 0.1D),
            new Vec3(0.4375D, 1.18D, 0.1D),
            new Vec3(-0, 1.18D, -1.81D)
    };

    public ConestogaWagon(EntityType<ConestogaWagon> type, Level level) {
        super(type, level, 1, 1, RIDERS, ServerConfig.INSTANCE.conestogaSpeed.get(), ServerConfig.INSTANCE.conestogaAcceleration.get(),
                ServerConfig.INSTANCE.conestogaTurnRate.get(), ServerConfig.INSTANCE.conestogaHealth.get(), 0.6D, 3.6D,
                13, 108);
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ConestogaMenu(id, inventory, this);
    }

    @Override
    public ItemStack getPickResult() {
        return WagonItem.setupNbt(new ItemStack(TWItems.CONESTOGA_WAGON.get()), this.getColor());
    }

}
