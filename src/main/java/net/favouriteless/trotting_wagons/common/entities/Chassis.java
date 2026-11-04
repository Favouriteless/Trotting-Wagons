package net.favouriteless.trotting_wagons.common.entities;

import net.favouriteless.trotting_wagons.common.init.TWEntityTypes;
import net.favouriteless.trotting_wagons.common.init.TWItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager.ControllerRegistrar;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * The placed wagon frame. The vanilla box is only there so an axe or a build click can find it.
 * It does not stop the player.
 */
public class Chassis extends Entity implements GeoEntity {

    /** Chassis cubes, model pixels converted with (-x / 16, y / 16, -z / 16). */
    private static final double MIN_X = -1.5D;
    private static final double MAX_X = 1.5D;
    private static final double MIN_Y = -1.25D / 16.0D;
    private static final double MAX_Y = 5.75D / 16.0D;
    private static final double MIN_Z = -29.0D / 16.0D;
    private static final double MAX_Z = 22.0D / 16.0D;

    private static final EntityDataAccessor<Integer> DATA_PLANKS = SynchedEntityData.defineId(Chassis.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_IRON = SynchedEntityData.defineId(Chassis.class, EntityDataSerializers.INT);

    private final AnimatableInstanceCache animCache = GeckoLibUtil.createInstanceCache(this);
    private final WagonBuild build = new WagonBuild();

    public Chassis(EntityType<? extends Chassis> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    public int plankCount() {
        return entityData.get(DATA_PLANKS);
    }

    public int ironCount() {
        return entityData.get(DATA_IRON);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_PLANKS, 0);
        builder.define(DATA_IRON, 0);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        build.load(compound, registryAccess());
        syncCounts();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        build.save(compound, registryAccess());
    }

    @Override
    public void tick() {
        super.tick();
        setBoundingBox(makeBoundingBox());
    }

    @Override
    protected AABB makeBoundingBox() {
        return turnedBox(MIN_X, MAX_X, MIN_Y, MAX_Y, MIN_Z, MAX_Z);
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        int room = roomFor(held);
        if(room <= 0)
            return InteractionResult.PASS;
        if(level().isClientSide)
            return InteractionResult.SUCCESS;
        int limit = player.isShiftKeyDown() ? Integer.MAX_VALUE : 1;
        if(!build.offer(held, player.getAbilities().instabuild, limit))
            return InteractionResult.PASS;
        syncCounts();
        if(build.ready())
            finish();
        else
            playSound(held.is(Items.IRON_INGOT) ? SoundEvents.ITEM_PICKUP : SoundEvents.WOOD_PLACE, 0.8F, 1.0F);
        return InteractionResult.CONSUME;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if(isInvulnerableTo(source))
            return false;
        if(!(source.getEntity() instanceof Player player) || !player.getMainHandItem().is(ItemTags.AXES))
            return false;
        if(level().isClientSide)
            return true;
        if(!player.getAbilities().instabuild && level().getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
            spawnAtLocation(TWItems.CHASSIS.get());
            build.drop(this);
        }
        playSound(SoundEvents.WOOD_BREAK, 1.0F, 1.0F);
        discard();
        return true;
    }

    @Override
    public void registerControllers(ControllerRegistrar controllers) {}

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animCache;
    }

    private int roomFor(ItemStack held) {
        if(held.is(ItemTags.PLANKS))
            return Math.max(0, WagonBuild.PLANKS - shown(build.planks(), plankCount()));
        if(held.is(Items.IRON_INGOT))
            return Math.max(0, WagonBuild.IRON - shown(build.iron(), ironCount()));
        return 0;
    }

    private int shown(int stored, int synced) {
        return level().isClientSide ? synced : stored;
    }

    private void syncCounts() {
        entityData.set(DATA_PLANKS, build.planks());
        entityData.set(DATA_IRON, build.iron());
    }

    private void finish() {
        BaseWagon wagon = TWEntityTypes.BASE_WAGON.get().create(level());
        if(wagon == null)
            return;
        wagon.moveTo(getX(), getY(), getZ(), getYRot(), getXRot());
        wagon.yRotO = getYRot();
        wagon.xRotO = getXRot();
        wagon.takeBuild(build);
        level().addFreshEntity(wagon);
        playSound(SoundEvents.WOOD_PLACE, 1.0F, 1.2F);
        discard();
    }

    private AABB turnedBox(double minX, double maxX, double minY, double maxY, double minZ, double maxZ) {
        float yaw = getYRot();
        double left = Double.POSITIVE_INFINITY;
        double bottom = Double.POSITIVE_INFINITY;
        double front = Double.POSITIVE_INFINITY;
        double right = Double.NEGATIVE_INFINITY;
        double top = Double.NEGATIVE_INFINITY;
        double back = Double.NEGATIVE_INFINITY;
        double[] xs = {minX, maxX};
        double[] ys = {minY, maxY};
        double[] zs = {minZ, maxZ};
        for(double x : xs) {
            for(double y : ys) {
                for(double z : zs) {
                    Vec3 turned = rotateY(x, y, z, yaw);
                    left = Math.min(left, turned.x);
                    bottom = Math.min(bottom, turned.y);
                    front = Math.min(front, turned.z);
                    right = Math.max(right, turned.x);
                    top = Math.max(top, turned.y);
                    back = Math.max(back, turned.z);
                }
            }
        }
        return new AABB(getX() + left, getY() + bottom, getZ() + front, getX() + right, getY() + top, getZ() + back);
    }

    private static Vec3 rotateY(double x, double y, double z, float angle) {
        float a = angle * Mth.DEG_TO_RAD;
        float sin = Mth.sin(a);
        float cos = Mth.cos(a);
        return new Vec3(x * cos - z * sin, y, z * cos + x * sin);
    }

}
