package net.favouriteless.trotting_wagons.common.entities.base;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.ItemStackHandler;

public abstract class AbstractInventoryWagon extends AbstractWagon implements MenuProvider {

    private final NonNullList<ItemStack> inventory;
    private final ItemStackHandler itemHandler;

    public AbstractInventoryWagon(EntityType<? extends AbstractWagon> type, Level level, double wheelWidth, double wheelLength, Vec3[] riders,
                                  double speed, double acceleration, double turnRate, int maxHealth, double horseX, double horseY, float shaftAngle, int capacity) {
        super(type, level, wheelWidth, wheelLength, riders, speed, acceleration, turnRate, maxHealth, horseX, horseY, shaftAngle);
        this.inventory = NonNullList.withSize(capacity, ItemStack.EMPTY);
        this.itemHandler = new ItemStackHandler(inventory);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        InteractionResult superResult = super.interact(player, hand);
        if(superResult != InteractionResult.PASS)
            return superResult;

        if(isAlive() && !level().isClientSide && player.isSecondaryUseActive()) {
            player.openMenu(this, buf -> buf.writeInt(getId()));
            return InteractionResult.CONSUME;
        }

        return InteractionResult.PASS;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        ContainerHelper.saveAllItems(nbt, inventory, level().registryAccess());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        ContainerHelper.loadAllItems(nbt, inventory, level().registryAccess());
    }

    @Override
    protected void onDestroyed(boolean dropItem) {
        super.onDestroyed(dropItem);
        if(!level().isClientSide)
            Containers.dropContents(level(), blockPosition(), inventory);
    }

    public ItemStackHandler getItems() {
        return itemHandler;
    }

    public static ItemStackHandler getItemCapability(AbstractInventoryWagon wagon, Void ctx) {
        return wagon.getItems();
    }

}
