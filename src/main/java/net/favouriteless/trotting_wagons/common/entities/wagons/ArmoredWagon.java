package net.favouriteless.trotting_wagons.common.entities.wagons;

import net.favouriteless.trotting_wagons.ServerConfig;
import net.favouriteless.trotting_wagons.common.entities.base.AbstractWagon;
import net.favouriteless.trotting_wagons.common.init.TWItems;
import net.favouriteless.trotting_wagons.common.items.WagonItem;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class ArmoredWagon extends AbstractWagon {

    public static final Vec3[] RIDERS = new Vec3[] {
            new Vec3(0, 1.2D, 2.1D),
            new Vec3(-0.5D, 1.18D, 1.1D),
            new Vec3(0.5D, 1.18D, 1.1D),
            new Vec3(-0.5D, 1.18D, 0.0D),
            new Vec3(0.5D, 1.18D, 0.0D),
            new Vec3(-0.5D, 1.18D, -1.1D),
            new Vec3(0.5D, 1.18D, -1.1D)
    };

    public ArmoredWagon(EntityType<ArmoredWagon> type, Level level) {
        super(type, level, 1, 1, RIDERS, ServerConfig.INSTANCE.armoredSpeed.get(), ServerConfig.INSTANCE.armoredAcceleration.get(),
                ServerConfig.INSTANCE.armoredTurnRate.get(), ServerConfig.INSTANCE.armoredHealth.get(), 0.6D, 3.6D, 13);
    }

    @Override
    public ItemStack getPickResult() {
        return WagonItem.setupNbt(new ItemStack(TWItems.ARMORED_WAGON.get()), getColor());
    }

    @Override
    protected DyeColor getDefaultColor() {
        return null;
    }

}
