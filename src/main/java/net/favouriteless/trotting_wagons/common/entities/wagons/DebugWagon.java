package net.favouriteless.trotting_wagons.common.entities.wagons;

import com.kltyton.bonehitboxlib.api.bone.collision.ObbCollisionMode;
import com.kltyton.bonehitboxlib.api.geckolib.entity.GeoBoneHitboxEntity;
import com.kltyton.bonehitboxlib.api.registration.registrar.ObbBoneRegistrar;
import net.favouriteless.trotting_wagons.ServerConfig;
import net.favouriteless.trotting_wagons.common.entities.base.AbstractInventoryWagon;
import net.favouriteless.trotting_wagons.common.entities.base.WagonSeat;
import net.favouriteless.trotting_wagons.common.init.TWItems;
import net.favouriteless.trotting_wagons.common.items.WagonItem;
import net.favouriteless.trotting_wagons.common.menus.DebugMenu;
import net.minecraft.util.Mth;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.ItemStackHandler;

public class DebugWagon extends AbstractInventoryWagon implements GeoBoneHitboxEntity {

    public static final int PUMPKIN_SLOTS = 4;
    /** Cabin posts, driver seat, and floor. Wheels, tongue, and the lantern arm are outside. One pixel of slack. */
    private static final double BODY_HALF_X = 23D / 16D;
    private static final double BODY_FRONT = 32.25D / 16D;
    private static final double BODY_REAR = 40.5D / 16D;
    private static final double BODY_HEIGHT = 4.0D;
    /** Positive rotation swings the upright ladder outward. Negative folded it into the wagon. */
    public static final float LADDER_DROP = (float)Math.toRadians(130.0D);
    private static final int LADDER_TICKS = 40;
    private static final float LADDER_STEP = 1.0F / LADDER_TICKS;

    private static final EntityDataAccessor<Boolean> DATA_LADDER_OPEN = SynchedEntityData.defineId(DebugWagon.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> DATA_LADDER = SynchedEntityData.defineId(DebugWagon.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_PUMPKINS = SynchedEntityData.defineId(DebugWagon.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<ItemStack> DATA_BANNER = SynchedEntityData.defineId(DebugWagon.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Boolean> DATA_LANTERN = SynchedEntityData.defineId(DebugWagon.class, EntityDataSerializers.BOOLEAN);
    /** 0 none, 1 shafts (bone shafts, one animal), 2 tongue (bone Tongue, two animals). */
    private static final EntityDataAccessor<Integer> DATA_HARNESS = SynchedEntityData.defineId(DebugWagon.class, EntityDataSerializers.INT);
    private static final Vec3 LAMP_POS = pivot(-28, 21, 6);

    /** Body mesh only. Wheels, tongue, ladder, seats, and the lantern stay out of the oriented collision. */
    @Override
    public void bonehitboxlib$registerObbBones(ObbBoneRegistrar registrar) {
        registrar.register(GeoBoneHitboxEntity.BONEHITBOXLIB_GEO_SOURCE, "cart")
                .collision(ObbCollisionMode.HARD);
    }

    private final NonNullList<ItemStack> banner = NonNullList.withSize(1, ItemStack.EMPTY);
    private final ItemStackHandler bannerItems = new ItemStackHandler(banner) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.getItem() instanceof BannerItem;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        protected void onContentsChanged(int slot) {
            syncBanner();
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

    private BlockPos lanternLight;
    private float ladderFrom;
    private float ladderTo;
    private boolean ladderVisualReady;

    public static final WagonSeat[] SEATS = new WagonSeat[] {
            new WagonSeat(pivot(0, 27.5D, -28.5D), WagonSeat.Kind.DRIVER),
            new WagonSeat(pivot(-14.5D, 26, -10.25D), WagonSeat.Kind.PLAYER),
            new WagonSeat(pivot(-11, 15.65124D, 9.34415D).add(0, 0.7D, 0), WagonSeat.Kind.ANIMAL),
            new WagonSeat(pivot(8, 16.65124D, -13.65585D).add(0, 0.7D, 0), WagonSeat.Kind.UNIVERSAL)
    };

    public DebugWagon(EntityType<DebugWagon> type, Level level) {
        super(type, level, 1.5D, 1.5D, SEATS, ServerConfig.INSTANCE.debugSpeed.get(),
                ServerConfig.INSTANCE.debugTurnRate.get(), ServerConfig.INSTANCE.debugHealth.get(), 0.3125D, 4.805D, 13, PUMPKIN_SLOTS);
    }

    @Override
    public int hitchSlots() {
        int slots = entityData.get(DATA_HARNESS);
        return slots < 0 ? 0 : Math.min(slots, 2);
    }

    @Override
    protected double singleHarnessFactor() {
        return 0.8D;
    }

    @Override
    protected float diagonalPitch() {
        return -8F;
    }

    @Override
    protected Vec3 hitchOffset(Side side) {
        double z = 76.88461D / 16D;
        if(hitchSlots() < 2)
            return new Vec3(1D / 16D, 0, z);
        return new Vec3(side == Side.LEFT ? 0.6D : -0.6D, 0, z);
    }

    private static Vec3 pivot(double x, double y, double z) {
        return new Vec3(x / 16D, y / 16D, -z / 16D);
    }

    @Override
    protected double tongueLength() {
        return 59.12D / 16D;
    }

    @Override
    protected double tongueHeight() {
        return 8.5D / 16D;
    }

    @Override
    protected double hitchedTongueTarget() {
        return 1.2D;
    }

    @Override
    protected ItemStackHandler createHandler(NonNullList<ItemStack> inventory) {
        return new ItemStackHandler(inventory) {
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return stack.is(Items.PUMPKIN);
            }

            @Override
            public int getSlotLimit(int slot) {
                return 1;
            }

            @Override
            protected void onContentsChanged(int slot) {
                syncPumpkins();
            }
        };
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_LADDER_OPEN, false);
        builder.define(DATA_LADDER, 0.0F);
        builder.define(DATA_PUMPKINS, 0);
        builder.define(DATA_BANNER, ItemStack.EMPTY);
        builder.define(DATA_LANTERN, false);
        builder.define(DATA_HARNESS, 0);
    }

    @Override
    public void tick() {
        super.tick();
        setBoundingBox(makeBoundingBox());
        if(level().isClientSide) {
            tickLadderVisual();
            return;
        }
        tickLadder();
        if(tickCount % 10 == 0)
            updateLanternLight();
    }

    @Override
    protected AABB makeBoundingBox() {
        return orientedBody(1.984D, 0.0D, 4.564D, 2.644D, 1.953D);
    }

    /** The white box stays visible, but walking into the wagon is handled by the yellow body box. */
    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    protected boolean holdsTravel() {
        return ladderProgress() > 0.001F;
    }

    public boolean isLadderOpen() {
        return entityData.get(DATA_LADDER_OPEN);
    }

    public float ladderProgress() {
        return entityData.get(DATA_LADDER);
    }

    public float ladderRender(float partialTick) {
        if(!ladderVisualReady)
            return ladderProgress();
        return Mth.lerp(partialTick, ladderFrom, ladderTo);
    }

    public void toggleLadder(Player player) {
        if(!(player.containerMenu instanceof DebugMenu menu) || menu.getWagon() != this)
            return;
        entityData.set(DATA_LADDER_OPEN, !isLadderOpen());
    }

    private void tickLadder() {
        float next = approach(ladderProgress(), isLadderOpen() ? 1.0F : 0.0F);
        if(next != ladderProgress())
            entityData.set(DATA_LADDER, next);
    }

    private void tickLadderVisual() {
        if(!ladderVisualReady) {
            ladderFrom = ladderProgress();
            ladderTo = ladderFrom;
            ladderVisualReady = true;
        }
        ladderFrom = ladderTo;
        ladderTo = approach(ladderTo, isLadderOpen() ? 1.0F : 0.0F);
    }

    private static float approach(float value, float target) {
        return value + Mth.clamp(target - value, -LADDER_STEP, LADDER_STEP);
    }

    @Override
    public void remove(RemovalReason reason) {
        if(reason.shouldDestroy())
            clearLanternLight(true);
        super.remove(reason);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        syncPumpkins();
        if(nbt.contains("Banner", net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            banner.set(0, ItemStack.EMPTY);
            ContainerHelper.loadAllItems(nbt.getCompound("Banner"), banner, registryAccess());
        }
        syncBanner();
        if(nbt.contains("Lantern", net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            lantern.set(0, ItemStack.EMPTY);
            ContainerHelper.loadAllItems(nbt.getCompound("Lantern"), lantern, registryAccess());
        }
        syncLantern();
        if(nbt.contains("Harness", net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            harness.set(0, ItemStack.EMPTY);
            ContainerHelper.loadAllItems(nbt.getCompound("Harness"), harness, registryAccess());
        }
        syncHarness();
        entityData.set(DATA_LADDER_OPEN, nbt.getBoolean("ladderOpen"));
        entityData.set(DATA_LADDER, Mth.clamp(nbt.getFloat("ladderProgress"), 0.0F, 1.0F));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putBoolean("ladderOpen", isLadderOpen());
        nbt.putFloat("ladderProgress", ladderProgress());
        CompoundTag bannerTag = new CompoundTag();
        ContainerHelper.saveAllItems(bannerTag, banner, registryAccess());
        nbt.put("Banner", bannerTag);
        CompoundTag lanternTag = new CompoundTag();
        ContainerHelper.saveAllItems(lanternTag, lantern, registryAccess());
        nbt.put("Lantern", lanternTag);
        CompoundTag harnessTag = new CompoundTag();
        ContainerHelper.saveAllItems(harnessTag, harness, registryAccess());
        nbt.put("Harness", harnessTag);
    }

    public ItemStackHandler getHarnessItems() {
        return harnessItems;
    }

    private void syncHarness() {
        ItemStack stack = harness.get(0);
        int slots = stack.is(TWItems.TONGUE.get()) ? 2 : stack.is(TWItems.SHAFTS.get()) ? 1 : 0;
        entityData.set(DATA_HARNESS, slots);
    }

    public ItemStackHandler getBannerItems() {
        return bannerItems;
    }

    public ItemStack getBannerStack() {
        return entityData.get(DATA_BANNER);
    }

    private void syncBanner() {
        entityData.set(DATA_BANNER, banner.get(0).copy());
    }

    public ItemStackHandler getLanternItems() {
        return lanternItems;
    }

    public boolean hasLantern() {
        return entityData.get(DATA_LANTERN);
    }

    private void syncLantern() {
        boolean filled = !lantern.get(0).isEmpty();
        entityData.set(DATA_LANTERN, filled);
        if(!filled && !level().isClientSide)
            clearLanternLight();
    }

    private void updateLanternLight() {
        if(!hasLantern()) {
            clearLanternLight(false);
            return;
        }
        BlockPos next = lampBlockPos();
        if(next.equals(lanternLight))
            return;
        // The placed cell is not saved. After a reload the light is still there, but lanternLight is empty.
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

    private BlockPos lampBlockPos() {
        return BlockPos.containing(position().add(rotateY(LAMP_POS, getYRot())));
    }

    private void clearLanternLight() {
        clearLanternLight(false);
    }

    private void clearLanternLight(boolean atLamp) {
        if(level().isClientSide)
            return;
        removeLightBlock(lanternLight);
        if(atLamp)
            removeLightBlock(lampBlockPos());
        lanternLight = null;
    }

    private void removeLightBlock(BlockPos pos) {
        if(pos != null && level().getBlockState(pos).is(Blocks.LIGHT))
            level().setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
    }

    public int getPumpkinCount() {
        return entityData.get(DATA_PUMPKINS);
    }

    private void syncPumpkins() {
        int count = 0;
        for(int i = 0; i < getItems().getSlots(); i++) {
            if(!getItems().getStackInSlot(i).isEmpty())
                count++;
        }
        entityData.set(DATA_PUMPKINS, count);
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new DebugMenu(id, inventory, this);
    }

    @Override
    public ItemStack getPickResult() {
        return WagonItem.setupComponents(new ItemStack(TWItems.DEBUG_WAGON.get()), getColor());
    }

}
