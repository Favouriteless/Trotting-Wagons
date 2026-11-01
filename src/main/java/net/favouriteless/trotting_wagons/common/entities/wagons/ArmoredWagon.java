package net.favouriteless.trotting_wagons.common.entities.wagons;

import net.favouriteless.trotting_wagons.ServerConfig;
import net.favouriteless.trotting_wagons.common.entities.base.AbstractWagon;
import net.favouriteless.trotting_wagons.common.entities.base.WagonSeat;
import net.favouriteless.trotting_wagons.common.init.TWItems;
import net.favouriteless.trotting_wagons.common.items.WagonItem;
import net.favouriteless.trotting_wagons.common.menus.ArmoredMenu;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.ItemStackHandler;

public class ArmoredWagon extends AbstractWagon implements MenuProvider {

    /** 0 none, 1 the tongue item (TongueSingle), 2 the shafts item (Tongue). */
    private static final EntityDataAccessor<Integer> DATA_HARNESS = SynchedEntityData.defineId(ArmoredWagon.class, EntityDataSerializers.INT);

    private final NonNullList<ItemStack> harness = NonNullList.withSize(1, ItemStack.EMPTY);
    private final ItemStackHandler harnessItems = new ItemStackHandler(harness) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return TWItems.isHarness(stack);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        protected void onContentsChanged(int slot) {
            syncHarness();
        }
    };

    public static final WagonSeat[] SEATS = new WagonSeat[] {
            new WagonSeat(new Vec3(0, 1.2D, 2.1D), WagonSeat.Kind.DRIVER),
            new WagonSeat(new Vec3(-0.5D, 1.18D, 1.1D), WagonSeat.Kind.PLAYER),
            new WagonSeat(new Vec3(0.5D, 1.18D, 1.1D), WagonSeat.Kind.PLAYER),
            new WagonSeat(new Vec3(-0.5D, 1.18D, 0.0D), WagonSeat.Kind.PLAYER),
            new WagonSeat(new Vec3(0.5D, 1.18D, 0.0D), WagonSeat.Kind.PLAYER),
            new WagonSeat(new Vec3(-0.5D, 1.18D, -1.1D), WagonSeat.Kind.PLAYER),
            new WagonSeat(new Vec3(0.5D, 1.18D, -1.1D), WagonSeat.Kind.PLAYER)
    };

    public ArmoredWagon(EntityType<ArmoredWagon> type, Level level) {
        super(type, level, 1, 1, SEATS, ServerConfig.INSTANCE.armoredSpeed.get(),
                ServerConfig.INSTANCE.armoredTurnRate.get(), ServerConfig.INSTANCE.armoredHealth.get(), 0.6D, 3.6D, 13);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_HARNESS, 0);
    }

    @Override
    public int hitchSlots() {
        int slots = entityData.get(DATA_HARNESS);
        return slots < 0 ? 0 : Math.min(slots, 2);
    }

    @Override
    protected Vec3 hitchOffset(Side side) {
        if(hitchSlots() >= 2)
            return super.hitchOffset(side);
        return new Vec3(0, 0, 3.6D);
    }

    @Override
    public ItemStackHandler getHarnessItems() {
        return harnessItems;
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        if(nbt.contains("Harness", Tag.TAG_COMPOUND)) {
            harness.set(0, ItemStack.EMPTY);
            ContainerHelper.loadAllItems(nbt.getCompound("Harness"), harness, registryAccess());
        }
        syncHarness();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        CompoundTag harnessTag = new CompoundTag();
        ContainerHelper.saveAllItems(harnessTag, harness, registryAccess());
        nbt.put("Harness", harnessTag);
    }

    private void syncHarness() {
        ItemStack stack = harness.get(0);
        int slots = stack.is(TWItems.SHAFTS.get()) ? 2 : stack.is(TWItems.TONGUE.get()) ? 1 : 0;
        entityData.set(DATA_HARNESS, slots);
    }

    @Override
    protected double tongueLength() {
        return 61.12D / 16D;
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ArmoredMenu(id, inventory, this);
    }

    @Override
    public ItemStack getPickResult() {
        return WagonItem.setupComponents(new ItemStack(TWItems.ARMORED_WAGON.get()), getColor());
    }

    @Override
    protected DyeColor getDefaultColor() {
        return null;
    }

}
