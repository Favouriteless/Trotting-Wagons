package net.favouriteless.trotting_wagons.common.entities;

import com.kltyton.bonehitboxlib.api.bone.collision.ObbCollisionMode;
import com.kltyton.bonehitboxlib.api.geckolib.entity.GeoBoneHitboxEntity;
import com.kltyton.bonehitboxlib.api.registration.registrar.ObbBoneRegistrar;
import net.favouriteless.trotting_wagons.common.entities.base.AbstractWagon;
import net.favouriteless.trotting_wagons.common.init.TWEntityTypes;
import net.favouriteless.trotting_wagons.common.init.TWItems;
import net.favouriteless.trotting_wagons.common.menus.BaseBuildMenu;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
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
 * The finished wagon base. Walking stops on the oriented hitbox bone.
 * The vanilla box only lets an axe find it.
 */
public class BaseWagon extends Entity implements GeoEntity, GeoBoneHitboxEntity, MenuProvider {

    public static final String ANIMAL = "animal_wagon";
    public static final String[] CHOICES = {
            "armored_wagon", ANIMAL, "log_wagon", "barrel_wagon", "passenger_wagon",
            "ware_wagon", "kibitka_wagon", "conestoga_wagon", "royal_wagon", "big_cart"
    };

    private static final EntityDataAccessor<String> DATA_CHOICE = SynchedEntityData.defineId(BaseWagon.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_PLAN = SynchedEntityData.defineId(BaseWagon.class, EntityDataSerializers.STRING);

    /** Hitbox bone, model pixels converted with (-x / 16, y / 16, -z / 16). */
    /** Body mesh, wheels included. The hitch area in front is not part of the box. */
    private static final double MIN_X = -1.5D;
    private static final double MAX_X = 1.5D;
    private static final double MIN_Y = 0.0D;
    private static final double MAX_Y = 1.369D;
    private static final double MIN_Z = -2.469D;
    private static final double MAX_Z = 1.781D;

    private final AnimatableInstanceCache animCache = GeckoLibUtil.createInstanceCache(this);
    private final WagonBuild build = new WagonBuild();
    private WagonBuild plan = WagonBuild.none();

    public BaseWagon(EntityType<? extends BaseWagon> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    public void takeBuild(WagonBuild source) {
        build.copyFrom(source);
    }

    /** The body shell. The enclosing vanilla box stays non-solid so the player meets these cubes. */
    @Override
    public void bonehitboxlib$registerObbBones(ObbBoneRegistrar registrar) {
        registrar.register(GeoBoneHitboxEntity.BONEHITBOXLIB_GEO_SOURCE, "hitbox")
                .collision(ObbCollisionMode.HARD);
    }

    public String choice() {
        return entityData.get(DATA_CHOICE);
    }

    public boolean showsPlan() {
        return planSize() > 0;
    }

    public int planSize() {
        return WagonBuild.forChoice(choice()).size();
    }

    public String planName(int index) {
        return WagonBuild.forChoice(choice()).needName(index);
    }

    public int planCount(int index) {
        if(!level().isClientSide)
            return plan.countAt(index);
        String raw = entityData.get(DATA_PLAN);
        if(raw.isEmpty())
            return 0;
        String[] parts = raw.split(",");
        if(index < 0 || index >= parts.length)
            return 0;
        try {
            return Integer.parseInt(parts[index]);
        } catch(NumberFormatException ignored) {
            return 0;
        }
    }

    public int planRequired(int index) {
        return WagonBuild.forChoice(choice()).required(index);
    }

    /** Something has already been paid toward the selected wagon. */
    public boolean planStarted() {
        for(int i = 0; i < planSize(); i++)
            if(planCount(i) > 0)
                return true;
        return false;
    }

    /** Once any material is in, the choice stays until it is cleared or the base is broken. */
    public void choose(String id, Player player) {
        if(planStarted())
            return;
        applyChoice(id, player == null || !player.getAbilities().instabuild);
    }

    /** Drops the deposited materials and leaves no wagon selected. */
    public void clearSelection(Player player) {
        if(level() != null && !level().isClientSide && (player == null || !player.getAbilities().instabuild))
            plan.drop(this);
        plan = WagonBuild.none();
        entityData.set(DATA_CHOICE, "");
        syncPlan();
    }

    private void applyChoice(String id, boolean dropHeld) {
        String next = allowed(id) ? id : "";
        if(next.equals(choice()))
            return;
        if(dropHeld && !level().isClientSide)
            plan.drop(this);
        plan = WagonBuild.forChoice(next);
        entityData.set(DATA_CHOICE, next);
        syncPlan();
    }

    public static boolean allowed(String id) {
        for(String choice : CHOICES)
            if(choice.equals(id))
                return true;
        return false;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("gui.trotting_wagons.base_build");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new BaseBuildMenu(id, inventory, this);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if(level().isClientSide)
            return InteractionResult.SUCCESS;
        if(showsPlan()) {
            ItemStack held = player.getItemInHand(hand);
            int limit = player.isShiftKeyDown() ? Integer.MAX_VALUE : 1;
            boolean metal = held.is(Items.IRON_INGOT) || held.is(Items.IRON_BLOCK);
            if(plan.offer(held, player.getAbilities().instabuild, limit)) {
                syncPlan();
                if(plan.ready())
                    finishSelected();
                else
                    playSound(metal ? SoundEvents.ITEM_PICKUP : SoundEvents.WOOD_PLACE, 0.8F, 1.0F);
                return InteractionResult.CONSUME;
            }
        }
        player.openMenu(this, buf -> buf.writeInt(getId()));
        return InteractionResult.CONSUME;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_CHOICE, "");
        builder.define(DATA_PLAN, "");
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        build.load(compound, registryAccess());
        applyChoice(compound.getString("Choice"), false);
        plan.load(compound, "Plan", registryAccess());
        syncPlan();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        build.save(compound, registryAccess());
        plan.save(compound, "Plan", registryAccess());
        compound.putString("Choice", choice());
    }

    @Override
    public void tick() {
        super.tick();
        setBoundingBox(makeBoundingBox());
    }

    @Override
    protected AABB makeBoundingBox() {
        float yaw = getYRot();
        double left = Double.POSITIVE_INFINITY;
        double bottom = Double.POSITIVE_INFINITY;
        double front = Double.POSITIVE_INFINITY;
        double right = Double.NEGATIVE_INFINITY;
        double top = Double.NEGATIVE_INFINITY;
        double back = Double.NEGATIVE_INFINITY;
        double[] xs = {MIN_X, MAX_X};
        double[] ys = {MIN_Y, MAX_Y};
        double[] zs = {MIN_Z, MAX_Z};
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

    private void syncPlan() {
        StringBuilder joined = new StringBuilder();
        for(int i = 0; i < plan.size(); i++) {
            if(i > 0)
                joined.append(',');
            joined.append(plan.countAt(i));
        }
        entityData.set(DATA_PLAN, joined.toString());
    }

    private void finishSelected() {
        EntityType<? extends AbstractWagon> type = typeFor(choice());
        if(type == null)
            return;
        AbstractWagon wagon = type.create(level());
        if(wagon == null)
            return;
        wagon.moveTo(getX(), getY(), getZ(), getYRot(), getXRot());
        wagon.yRotO = getYRot();
        wagon.xRotO = getXRot();
        wagon.takePlan(plan);
        level().addFreshEntity(wagon);
        playSound(SoundEvents.WOOD_PLACE, 1.0F, 1.2F);
        discard();
    }

    private static EntityType<? extends AbstractWagon> typeFor(String id) {
        return switch(id) {
            case "armored_wagon" -> TWEntityTypes.ARMORED_WAGON.get();
            case ANIMAL -> TWEntityTypes.ANIMAL_WAGON.get();
            case "log_wagon" -> TWEntityTypes.LOG_WAGON.get();
            case "barrel_wagon" -> TWEntityTypes.BARREL_WAGON.get();
            case "passenger_wagon" -> TWEntityTypes.PASSENGER_WAGON.get();
            case "ware_wagon" -> TWEntityTypes.WARE_WAGON.get();
            case "kibitka_wagon" -> TWEntityTypes.KIBITKA_WAGON.get();
            case "conestoga_wagon" -> TWEntityTypes.CONESTOGA_WAGON.get();
            case "royal_wagon" -> TWEntityTypes.ROYAL_WAGON.get();
            case "big_cart" -> TWEntityTypes.BIG_CART_WAGON.get();
            default -> null;
        };
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
            plan.drop(this);
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

    private static Vec3 rotateY(double x, double y, double z, float angle) {
        float a = angle * Mth.DEG_TO_RAD;
        float sin = Mth.sin(a);
        float cos = Mth.cos(a);
        return new Vec3(x * cos - z * sin, y, z * cos + x * sin);
    }

}
