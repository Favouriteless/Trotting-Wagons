package net.favouriteless.trotting_wagons.common.entities.wagons;

import net.favouriteless.trotting_wagons.ServerConfig;
import net.favouriteless.trotting_wagons.common.entities.base.AbstractInventoryWagon;
import net.favouriteless.trotting_wagons.common.entities.base.WagonSeat;
import net.favouriteless.trotting_wagons.common.init.TWItems;
import net.favouriteless.trotting_wagons.common.menus.BigCartMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.ItemStackHandler;

/** Open cart. Cargo, one large animal, or the two universal seats. The ladder is only a mesh. */
public class BigCartWagon extends AbstractInventoryWagon {

    public static final int CARGO = 42;
    private static final int FRONT = 1;
    private static final int REAR = 2;
    private static final int LARGE = 3;

    /** Posts and floor, not the wheels, tongue, or side ladder. */
    private static final double BODY_HALF_X = 22D / 16D;
    private static final double BODY_FRONT = 31.25D / 16D;
    private static final double BODY_REAR = 39.5D / 16D;
    private static final double BODY_HEIGHT = 2.5D;
    /** Glass center of LampBase, in entity space. */
    private static final Vec3 LAMP_POS = new Vec3(-20.5D / 16D, 33.1D / 16D, 26.3D / 16D);

    private static final EntityDataAccessor<Integer> DATA_HARNESS = SynchedEntityData.defineId(BigCartWagon.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_LANTERN = SynchedEntityData.defineId(BigCartWagon.class, EntityDataSerializers.BOOLEAN);

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
    private final NonNullList<ItemStack> lantern = NonNullList.withSize(1, ItemStack.EMPTY);
    private final ItemStackHandler lanternItems = new ItemStackHandler(lantern) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.is(Items.LANTERN);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        protected void onContentsChanged(int slot) {
            syncLantern();
        }
    };
    private BlockPos lanternLight;

    /** Floor top is y=15. positionRider drops 0.7, so a standing animal is stored 0.7 higher. */
    public static final WagonSeat[] SEATS = new WagonSeat[] {
            new WagonSeat(new Vec3(0, 1.575D, 1.703125D), WagonSeat.Kind.DRIVER),
            new WagonSeat(new Vec3(0, 1.6375D, 0.2D), WagonSeat.Kind.UNIVERSAL),
            new WagonSeat(new Vec3(0, 1.6375D, -1.3D), WagonSeat.Kind.UNIVERSAL),
            new WagonSeat(new Vec3(0, 1.6375D, -0.3D), WagonSeat.Kind.ANIMALBIG)
    };

    public BigCartWagon(EntityType<BigCartWagon> type, Level level) {
        super(type, level, 1.5D, 1.5D, SEATS, ServerConfig.INSTANCE.bigCartSpeed.get(),
                ServerConfig.INSTANCE.bigCartTurnRate.get(), ServerConfig.INSTANCE.bigCartHealth.get(),
                0.6D, 78D / 16D, 13, CARGO);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_HARNESS, 0);
        builder.define(DATA_LANTERN, false);
    }

    @Override
    public void tick() {
        super.tick();
        setBoundingBox(makeBoundingBox());
        if(!level().isClientSide && tickCount % 10 == 0)
            updateLanternLight();
    }

    @Override
    protected AABB makeBoundingBox() {
        return orientedBody(2.062D, 0.0D, 2.413D, 2.469D, 1.953D);
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
        return new Vec3(0, 0, 78D / 16D);
    }

    @Override
    protected double tongueLength() {
        return 64D / 16D;
    }

    @Override
    protected double tongueHeight() {
        return 8.75D / 16D;
    }

    @Override
    protected boolean acceptsDye() {
        return false;
    }

    @Override
    protected DyeColor getDefaultColor() {
        return null;
    }

    /** Riding pose folds the legs up by about 10px, so a player stored on the floor would hover. The driver stays on the cushion. */
    private static final double PLAYER_SIT = 10D / 16D;

    @Override
    protected Vec3 getRiderOffset(Entity passenger) {
        Vec3 local = super.getRiderOffset(passenger);
        if(local != null && passenger instanceof Player && passenger != getControllingPassenger())
            return local.subtract(0, PLAYER_SIT, 0);
        return local;
    }

    @Override
    public ItemStackHandler getHarnessItems() {
        return harnessItems;
    }

    public ItemStackHandler getLanternItems() {
        return lanternItems;
    }

    public boolean hasLantern() {
        return entityData.get(DATA_LANTERN);
    }

    public boolean cargoHasItems() {
        ItemStackHandler items = getItems();
        for(int i = 0; i < CARGO && i < items.getSlots(); i++) {
            if(!items.getStackInSlot(i).isEmpty())
                return true;
        }
        return false;
    }

    /** Anyone except the driver. Cargo and these seats cannot be used together. */
    public boolean hasCabinPassenger() {
        for(int seat = 1; seat < getSeatCount(); seat++) {
            if(getSeatEntityId(seat) >= 0)
                return true;
        }
        return false;
    }

    /** A missing entity counts as an animal, so the cargo line does not claim the bed holds only players. */
    public boolean bedHasAnimal() {
        for(int seat = 1; seat < getSeatCount(); seat++) {
            int id = getSeatEntityId(seat);
            if(id < 0)
                continue;
            if(!countsAsPlayer(level().getEntity(id)))
                return true;
        }
        return false;
    }

    @Override
    protected boolean acceptsLargeSeat(Entity entity) {
        return isLargePassenger(entity);
    }

    @Override
    public Component seatBlockHint(int index) {
        if(index <= 0 || index >= getSeatCount() || getSeatEntityId(index) >= 0)
            return null;
        if(cargoHasItems())
            return Component.translatable("gui.trotting_wagons.tooltip.cargo_blocks_seats");
        if(index == LARGE && (occupied(FRONT) || occupied(REAR)))
            return Component.translatable("gui.trotting_wagons.tooltip.large_needs_space");
        if((index == FRONT || index == REAR) && occupied(LARGE))
            return Component.translatable("gui.trotting_wagons.tooltip.small_blocked");
        return null;
    }

    @Override
    protected boolean seatOpen(int index, Entity entity) {
        if(index > 0 && cargoHasItems())
            return false;
        if(index == LARGE)
            return !occupied(FRONT, entity) && !occupied(REAR, entity);
        if(index == FRONT || index == REAR)
            return !occupied(LARGE, entity);
        return true;
    }

    private boolean occupied(int seat) {
        return occupied(seat, null);
    }

    private boolean occupied(int seat, Entity moving) {
        int id = getSeatEntityId(seat);
        return id >= 0 && (moving == null || id != moving.getId());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        if(nbt.contains("Harness", Tag.TAG_COMPOUND)) {
            harness.set(0, ItemStack.EMPTY);
            ContainerHelper.loadAllItems(nbt.getCompound("Harness"), harness, registryAccess());
        }
        if(nbt.contains("Lantern", Tag.TAG_COMPOUND)) {
            lantern.set(0, ItemStack.EMPTY);
            ContainerHelper.loadAllItems(nbt.getCompound("Lantern"), lantern, registryAccess());
        }
        syncHarness();
        syncLantern();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        CompoundTag harnessTag = new CompoundTag();
        ContainerHelper.saveAllItems(harnessTag, harness, registryAccess());
        nbt.put("Harness", harnessTag);
        CompoundTag lanternTag = new CompoundTag();
        ContainerHelper.saveAllItems(lanternTag, lantern, registryAccess());
        nbt.put("Lantern", lanternTag);
    }

    @Override
    protected void onDestroyed(boolean dropItem) {
        if(!level().isClientSide) {
            clearLanternLight(true);
            Containers.dropContents(level(), blockPosition(), lantern);
        }
        super.onDestroyed(dropItem);
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new BigCartMenu(id, inventory, this);
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(TWItems.BIG_CART_WAGON.get());
    }

    private void syncHarness() {
        ItemStack stack = harness.get(0);
        int slots = stack.is(TWItems.SHAFTS.get()) ? 2 : stack.is(TWItems.TONGUE.get()) ? 1 : 0;
        entityData.set(DATA_HARNESS, slots);
    }

    private void syncLantern() {
        boolean filled = !lantern.get(0).isEmpty();
        entityData.set(DATA_LANTERN, filled);
        if(!filled && !level().isClientSide)
            clearLanternLight(false);
    }

    private void updateLanternLight() {
        if(!hasLantern()) {
            clearLanternLight(false);
            return;
        }
        BlockPos next = BlockPos.containing(position().add(rotateY(LAMP_POS, getYRot())));
        if(next.equals(lanternLight))
            return;
        if(lanternLight == null && level().getBlockState(next).is(Blocks.LIGHT)) {
            lanternLight = next;
            return;
        }
        clearLanternLight(false);
        if(level().getBlockState(next).isAir()) {
            level().setBlock(next, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 15), 3);
            lanternLight = next;
        }
    }

    private void clearLanternLight(boolean atLamp) {
        if(level().isClientSide)
            return;
        if(lanternLight != null && level().getBlockState(lanternLight).is(Blocks.LIGHT))
            level().setBlock(lanternLight, Blocks.AIR.defaultBlockState(), 3);
        if(atLamp) {
            BlockPos lamp = BlockPos.containing(position().add(rotateY(LAMP_POS, getYRot())));
            if(level().getBlockState(lamp).is(Blocks.LIGHT))
                level().setBlock(lamp, Blocks.AIR.defaultBlockState(), 3);
        }
        lanternLight = null;
    }

}
