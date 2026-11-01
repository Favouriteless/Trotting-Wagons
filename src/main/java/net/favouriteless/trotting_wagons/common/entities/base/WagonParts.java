package net.favouriteless.trotting_wagons.common.entities.base;

import net.favouriteless.trotting_wagons.TrottingWagons;
import net.favouriteless.trotting_wagons.common.init.TWItems;
import net.favouriteless.trotting_wagons.common.entities.wagons.AnimalWagon;
import net.favouriteless.trotting_wagons.common.entities.wagons.BarrelWagon;
import net.favouriteless.trotting_wagons.common.entities.wagons.ArmoredWagon;
import net.favouriteless.trotting_wagons.common.entities.wagons.ConestogaWagon;
import net.favouriteless.trotting_wagons.common.entities.wagons.DebugWagon;
import net.favouriteless.trotting_wagons.common.entities.wagons.KibitkaWagon;
import net.favouriteless.trotting_wagons.common.entities.wagons.LogWagon;
import net.favouriteless.trotting_wagons.common.entities.wagons.PassengerWagon;
import net.favouriteless.trotting_wagons.common.entities.wagons.RoyalWagon;
import net.favouriteless.trotting_wagons.common.entities.wagons.WareWagon;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class WagonParts {

    public enum Kind {
        WHEEL, PUMPKIN, BANNER, LANTERN, HARNESS
    }

    public record Part(Kind kind, int index, List<AABB> local) {}

    private static final double WHEEL_X = 0.22D;
    private static final double WHEEL_R = 0.75D;
    private static final double WHEEL_WIDE_OUTER = 0.25D;

    private static final List<Part> CONESTOGA = conestogaParts();
    private static final List<Part> ROYAL = royalParts();
    private static final List<Part> ARMORED = armoredParts();
    private static final List<Part> DEBUG = debugParts();
    private static final List<Part> ANIMAL = animalParts();
    private static final List<Part> LOG = logParts();
    private static final List<Part> BARREL = barrelParts();
    private static final List<Part> PASSENGER = passengerParts();
    private static final List<Part> WARE = wareParts();
    private static final List<Part> KIBITKA = kibitkaParts();

    private WagonParts() {}

    public static List<Part> of(AbstractWagon wagon) {
        if(wagon instanceof DebugWagon)
            return DEBUG;
        if(wagon instanceof ConestogaWagon)
            return CONESTOGA;
        if(wagon instanceof RoyalWagon)
            return ROYAL;
        if(wagon instanceof ArmoredWagon)
            return ARMORED;
        if(wagon instanceof AnimalWagon)
            return ANIMAL;
        if(wagon instanceof LogWagon)
            return LOG;
        if(wagon instanceof BarrelWagon)
            return BARREL;
        if(wagon instanceof PassengerWagon)
            return PASSENGER;
        if(wagon instanceof WareWagon)
            return WARE;
        if(wagon instanceof KibitkaWagon)
            return KIBITKA;
        return List.of();
    }

    public static Part pick(AbstractWagon wagon, Player player) {
        double reach = player.entityInteractionRange();
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getViewVector(1.0F).scale(reach));
        Part best = null;
        double bestDistance = Double.MAX_VALUE;
        Vec3 bestHit = null;
        for(Part part : of(wagon)) {
            if(!present(wagon, part))
                continue;
            for(AABB local : clickBoxes(wagon, part)) {
                Optional<Vec3> hit = clipPart(wagon, local, eye, end);
                if(hit.isEmpty())
                    continue;
                double distance = hit.get().distanceToSqr(eye);
                if(distance < bestDistance) {
                    bestDistance = distance;
                    best = part;
                    bestHit = hit.get();
                }
            }
        }
        if(bestHit != null && blockIsCloser(player, eye, bestHit, reach))
            return null;
        return best;
    }

    public static boolean present(AbstractWagon wagon, Part part) {
        return switch(part.kind()) {
            case WHEEL -> (wagon.getWheelMask() & (1 << part.index())) != 0;
            case PUMPKIN -> wagon instanceof DebugWagon debug && debug.getPumpkinCount() > part.index();
            case BANNER -> wagon instanceof DebugWagon debug && !debug.getBannerStack().isEmpty();
            case HARNESS -> harnessOf(wagon) != null && !harnessOf(wagon).getStackInSlot(0).isEmpty();
            case LANTERN -> wagon instanceof DebugWagon debug ? debug.hasLantern()
                    : wagon instanceof PassengerWagon passenger ? passenger.hasLantern(part.index())
                    : wagon instanceof WareWagon ware ? ware.hasLantern()
                    : wagon instanceof KibitkaWagon kibitka && kibitka.hasLantern();
        };
    }

    public static boolean use(AbstractWagon wagon, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if(!canInsert(wagon, stack))
            return false;
        if(!wagon.level().isClientSide)
            insert(wagon, player, stack, aimPoint(wagon, player));
        return true;
    }

    /** Shift-right-click. Removes the part under the cursor. */
    public static boolean take(AbstractWagon wagon, Player player) {
        Part part = pick(wagon, player);
        if(part == null)
            return false;
        if(!wagon.level().isClientSide)
            remove(wagon, player, part);
        return true;
    }

    public static Vec3 worldCenter(AbstractWagon wagon, Part part) {
        return wagon.toWorld(part.local().getFirst().getCenter());
    }

    private static Optional<Vec3> clipPart(AbstractWagon wagon, AABB local, Vec3 eye, Vec3 end) {
        Optional<Vec3> hit = local.clip(wagon.fromWorld(eye), wagon.fromWorld(end));
        return hit.map(wagon::toWorld);
    }

    public static List<AABB> clickBoxes(AbstractWagon wagon, Part part) {
        if(part.kind() != Kind.WHEEL || wagon.getWheelKind(part.index()) != AbstractWagon.WheelKind.WIDE)
            return part.local();
        boolean outward = part.index() % 2 == 0; // 0 and 2 are the +X wheels; the wide rim sticks out on that side.
        List<AABB> boxes = new ArrayList<>();
        for(AABB local : part.local())
            boxes.add(outward ? local.expandTowards(WHEEL_WIDE_OUTER, 0, 0) : local.expandTowards(-WHEEL_WIDE_OUTER, 0, 0));
        return boxes;
    }

    public static AABB worldBox(AbstractWagon wagon, AABB local) {
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;
        for(int i = 0; i < 8; i++) {
            Vec3 corner = wagon.toWorld(new Vec3(
                    (i & 1) == 0 ? local.minX : local.maxX,
                    (i & 2) == 0 ? local.minY : local.maxY,
                    (i & 4) == 0 ? local.minZ : local.maxZ));
            minX = Math.min(minX, corner.x);
            minY = Math.min(minY, corner.y);
            minZ = Math.min(minZ, corner.z);
            maxX = Math.max(maxX, corner.x);
            maxY = Math.max(maxY, corner.y);
            maxZ = Math.max(maxZ, corner.z);
        }
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private static boolean canInsert(AbstractWagon wagon, ItemStack stack) {
        if(stack.isEmpty())
            return false;
        if(stack.is(TrottingWagons.WAGON_WHEELS)) {
            for(int i = 0; i < AbstractWagon.WHEEL_COUNT; i++) {
                if(wagon.getWheels().getStackInSlot(i).isEmpty())
                    return true;
            }
        }
        if(stack.is(Items.LANTERN)) {
            if(wagon instanceof DebugWagon debug && !debug.hasLantern())
                return true;
            if(wagon instanceof PassengerWagon passenger && passenger.firstEmptyLantern() >= 0)
                return true;
            if(wagon instanceof WareWagon ware && !ware.hasLantern())
                return true;
            if(wagon instanceof KibitkaWagon kibitka && !kibitka.hasLantern())
                return true;
        }
        ItemStackHandler harness = harnessOf(wagon);
        if(harness != null && TWItems.isHarness(stack) && harness.getStackInSlot(0).isEmpty() && !wagon.hasDraftAnimal())
            return true;
        if(!(wagon instanceof DebugWagon debug))
            return false;
        if(stack.is(Items.PUMPKIN)) {
            for(int i = 0; i < DebugWagon.PUMPKIN_SLOTS; i++) {
                if(debug.getItems().getStackInSlot(i).isEmpty())
                    return true;
            }
        }
        return stack.getItem() instanceof BannerItem && debug.getBannerStack().isEmpty();
    }

    private static Vec3 aimPoint(AbstractWagon wagon, Player player) {
        double reach = player.entityInteractionRange();
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getViewVector(1.0F).scale(reach));
        Vec3 closest = null;
        double closestDistance = Double.MAX_VALUE;
        for(Part part : of(wagon)) {
            for(AABB local : clickBoxes(wagon, part)) {
                Optional<Vec3> hit = clipPart(wagon, local, eye, end);
                if(hit.isEmpty())
                    continue;
                double distance = hit.get().distanceToSqr(eye);
                if(distance < closestDistance) {
                    closestDistance = distance;
                    closest = hit.get();
                }
            }
        }
        if(closest != null)
            return closest;
        Optional<Vec3> body = wagon.getBoundingBox().clip(eye, end);
        return body.orElse(eye);
    }

    private static int nearestEmptyWheel(AbstractWagon wagon, Vec3 aim) {
        int slot = -1;
        double best = Double.MAX_VALUE;
        for(Part part : of(wagon)) {
            if(part.kind() != Kind.WHEEL || !wagon.getWheels().getStackInSlot(part.index()).isEmpty())
                continue;
            double distance = worldCenter(wagon, part).distanceToSqr(aim);
            if(distance < best) {
                best = distance;
                slot = part.index();
            }
        }
        return slot;
    }

    private static int nearestEmptyLantern(PassengerWagon wagon, Vec3 aim) {
        int slot = -1;
        double best = Double.MAX_VALUE;
        for(Part part : of(wagon)) {
            if(part.kind() != Kind.LANTERN || wagon.hasLantern(part.index()))
                continue;
            double distance = worldCenter(wagon, part).distanceToSqr(aim);
            if(distance < best) {
                best = distance;
                slot = part.index();
            }
        }
        return slot;
    }

    private static void insert(AbstractWagon wagon, Player player, ItemStack stack, Vec3 aim) {
        if(stack.is(TrottingWagons.WAGON_WHEELS)) {
            int slot = nearestEmptyWheel(wagon, aim);
            if(slot < 0)
                return;
            ItemStack one = player.getAbilities().instabuild ? stack.copyWithCount(1) : stack.split(1);
            wagon.getWheels().setStackInSlot(slot, one);
            return;
        }
        if(stack.is(Items.LANTERN)) {
            if(wagon instanceof DebugWagon debug && !debug.hasLantern()) {
                debug.getLanternItems().setStackInSlot(0, one(player, stack));
                return;
            }
            if(wagon instanceof PassengerWagon passenger) {
                int slot = nearestEmptyLantern(passenger, aim);
                if(slot >= 0)
                    passenger.getLanternItems().setStackInSlot(slot, one(player, stack));
                return;
            }
            if(wagon instanceof WareWagon ware && !ware.hasLantern()) {
                ware.getLanternItems().setStackInSlot(0, one(player, stack));
                return;
            }
            if(wagon instanceof KibitkaWagon kibitka && !kibitka.hasLantern()) {
                kibitka.getLanternItems().setStackInSlot(0, one(player, stack));
                return;
            }
        }
        ItemStackHandler harness = harnessOf(wagon);
        if(harness != null && TWItems.isHarness(stack) && harness.getStackInSlot(0).isEmpty() && !wagon.hasDraftAnimal()) {
            harness.setStackInSlot(0, one(player, stack));
            return;
        }
        if(!(wagon instanceof DebugWagon debug))
            return;
        ItemStack one = one(player, stack);
        if(one.is(Items.PUMPKIN)) {
            for(int i = 0; i < DebugWagon.PUMPKIN_SLOTS; i++) {
                if(debug.getItems().getStackInSlot(i).isEmpty()) {
                    debug.getItems().setStackInSlot(i, one);
                    return;
                }
            }
        }
        if(one.getItem() instanceof BannerItem && debug.getBannerStack().isEmpty())
            debug.getBannerItems().setStackInSlot(0, one);
    }

    private static ItemStack one(Player player, ItemStack stack) {
        return player.getAbilities().instabuild ? stack.copyWithCount(1) : stack.split(1);
    }

    private static void remove(AbstractWagon wagon, Player player, Part part) {
        ItemStack stack = switch(part.kind()) {
            case WHEEL -> takeWheel(wagon, part.index());
            case PUMPKIN -> wagon instanceof DebugWagon debug ? takeLast(debug.getItems(), DebugWagon.PUMPKIN_SLOTS) : ItemStack.EMPTY;
            case BANNER -> wagon instanceof DebugWagon debug ? takeSlot(debug.getBannerItems(), 0) : ItemStack.EMPTY;
            case HARNESS -> {
                ItemStackHandler gear = harnessOf(wagon);
                if(gear != null && !wagon.hasDraftAnimal())
                    yield takeSlot(gear, 0);
                yield ItemStack.EMPTY;
            }
            case LANTERN -> {
                if(wagon instanceof DebugWagon debug)
                    yield takeSlot(debug.getLanternItems(), 0);
                if(wagon instanceof PassengerWagon passenger)
                    yield takeSlot(passenger.getLanternItems(), part.index());
                if(wagon instanceof WareWagon ware)
                    yield takeSlot(ware.getLanternItems(), 0);
                if(wagon instanceof KibitkaWagon kibitka)
                    yield takeSlot(kibitka.getLanternItems(), 0);
                yield ItemStack.EMPTY;
            }
        };
        if(!stack.isEmpty())
            give(player, stack);
    }

    private static ItemStackHandler harnessOf(AbstractWagon wagon) {
        return wagon.getHarnessItems();
    }

    private static ItemStack takeWheel(AbstractWagon wagon, int index) {
        ItemStack stack = wagon.getWheels().getStackInSlot(index);
        if(stack.isEmpty())
            return ItemStack.EMPTY;
        wagon.getWheels().setStackInSlot(index, ItemStack.EMPTY);
        return stack;
    }

    private static ItemStack takeLast(net.neoforged.neoforge.items.ItemStackHandler handler, int limit) {
        for(int i = limit - 1; i >= 0; i--) {
            ItemStack stack = takeSlot(handler, i);
            if(!stack.isEmpty())
                return stack;
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack takeSlot(net.neoforged.neoforge.items.ItemStackHandler handler, int index) {
        ItemStack stack = handler.getStackInSlot(index);
        if(stack.isEmpty())
            return ItemStack.EMPTY;
        handler.setStackInSlot(index, ItemStack.EMPTY);
        return stack;
    }

    private static void give(Player player, ItemStack stack) {
        if(!player.getInventory().add(stack))
            player.drop(stack, false);
    }

    private static boolean blockIsCloser(Player player, Vec3 eye, Vec3 hit, double reach) {
        HitResult block = player.pick(reach, 1.0F, false);
        return block.getType() == HitResult.Type.BLOCK && block.getLocation().distanceToSqr(eye) < hit.distanceToSqr(eye);
    }

    private static Part wheel(int index, double x, double y, double z) {
        Vec3 center = local(x, y, z);
        return new Part(Kind.WHEEL, index, List.of(new AABB(
                center.x - WHEEL_X, center.y - WHEEL_R, center.z - WHEEL_R,
                center.x + WHEEL_X, center.y + WHEEL_R, center.z + WHEEL_R)));
    }

    private static List<Part> conestogaParts() {
        List<Part> parts = new ArrayList<>();
        parts.add(wheel(0, 16, 7.15385D, -16));
        parts.add(wheel(1, -16, 7.15385D, -16));
        parts.add(wheel(2, 16, 8.5D, 17));
        parts.add(wheel(3, -16, 8.5D, 17));
        parts.add(new Part(Kind.HARNESS, 0, List.of(plainBox(-16, 7.5D, -85, 32, 8, 57))));
        return List.copyOf(parts);
    }

    private static List<Part> royalParts() {
        List<Part> parts = new ArrayList<>();
        parts.add(wheel(0, 17.75D, 10.5D, -21));
        parts.add(wheel(1, -17.75D, 10.5D, -21));
        parts.add(wheel(2, 17.75D, 10.5D, 21));
        parts.add(wheel(3, -17.75D, 10.5D, 21));
        parts.add(new Part(Kind.HARNESS, 0, List.of(plainBox(-16, 7.5D, -85, 32, 8, 57))));
        return List.copyOf(parts);
    }

    private static List<Part> armoredParts() {
        List<Part> parts = new ArrayList<>();
        parts.add(wheel(0, 16, 8.5D, -17));
        parts.add(wheel(1, -16, 8.5D, -17));
        parts.add(wheel(2, 16, 8.5D, 17));
        parts.add(wheel(3, -16, 8.5D, 17));
        parts.add(new Part(Kind.HARNESS, 0, List.of(plainBox(-16, 7.5D, -85, 32, 8, 57))));
        return List.copyOf(parts);
    }

    private static List<Part> logParts() {
        List<Part> parts = new ArrayList<>();
        parts.add(wheel(0, 19.25D, 9.5D, -17));
        parts.add(wheel(1, -18.75D, 9.5D, -17));
        parts.add(wheel(2, 19.75D, 9.5D, 21));
        parts.add(wheel(3, -19.75D, 9.5D, 21));
        parts.add(new Part(Kind.HARNESS, 0, List.of(plainBox(-16, 7.5D, -85, 32, 8, 57))));
        return List.copyOf(parts);
    }

    private static List<Part> animalParts() {
        List<Part> parts = new ArrayList<>();
        parts.add(wheel(0, 19.25D, 9.5D, -17));
        parts.add(wheel(1, -18.75D, 9.5D, -17));
        parts.add(wheel(2, 19.75D, 9.5D, 29));
        parts.add(wheel(3, -19.75D, 9.5D, 29));
        parts.add(new Part(Kind.HARNESS, 0, List.of(plainBox(-16, 7.5D, -85, 32, 8, 57))));
        return List.copyOf(parts);
    }

    private static List<Part> debugParts() {
        List<Part> parts = new ArrayList<>();
        parts.add(wheel(0, 24.25D, 9.5D, -21));
        parts.add(wheel(1, -23.75D, 9.5D, -21));
        parts.add(wheel(2, 24.75D, 9.5D, 27));
        parts.add(wheel(3, -24.75D, 9.5D, 27));
        parts.add(pumpkin(0, -6.03528D, 14.65124D, 24.48045D, 1.96472D, 22.65124D, 32.48045D, -152.5F));
        parts.add(pumpkin(1, 10.96472D, 14.65124D, 20.48045D, 18.96472D, 22.65124D, 28.48045D, 107.5F));
        parts.add(pumpkin(2, 11.96472D, 30.65124D, 20.48045D, 19.96472D, 38.65124D, 28.48045D, 7.5F));
        parts.add(pumpkin(3, -6.43505D, 30.65124D, 23.17074D, 1.56495D, 38.65124D, 31.17074D, -85F));
        parts.add(new Part(Kind.BANNER, 0, List.of(
                turnedBox(7.5D, 33, 24.8D, 20, 40, 1, 21.5D, 29, 25.8D, -90),
                turnedBox(16.5D, 29, 25.8D, 2, 42, 2, 21.5D, 29, 25.8D, -90),
                turnedBox(7.5D, 71, 25.8D, 20, 2, 2, 21.5D, 29, 25.8D, -90)
        )));
        parts.add(new Part(Kind.LANTERN, 0, List.of(plainBox(-31, 17.1D, 2.9D, 6, 13, 6.4D))));
        parts.add(new Part(Kind.HARNESS, 0, List.of(plainBox(-16, 7.5D, -82, 32, 8, 54))));
        return List.copyOf(parts);
    }

    private static List<Part> passengerParts() {
        List<Part> parts = new ArrayList<>();
        parts.add(wheel(0, 19.25D, 9.5D, -17));
        parts.add(wheel(1, -18.75D, 9.5D, -17));
        parts.add(wheel(2, 19.75D, 9.5D, 29));
        parts.add(wheel(3, -19.75D, 9.5D, 29));
        parts.add(new Part(Kind.LANTERN, 0, List.of(plainBox(-3, 36.1D, -33, 6, 9.25D, 6))));
        parts.add(new Part(Kind.LANTERN, 1, List.of(plainBox(-3, 34.35D, 47, 6, 11, 6))));
        parts.add(new Part(Kind.HARNESS, 0, List.of(plainBox(-16, 7.5D, -85, 32, 8, 57))));
        return List.copyOf(parts);
    }

    private static List<Part> wareParts() {
        List<Part> parts = new ArrayList<>();
        parts.add(wheel(0, 19.25D, 9.5D, -17));
        parts.add(wheel(1, -18.75D, 9.5D, -17));
        parts.add(wheel(2, 19.75D, 9.5D, 21));
        parts.add(wheel(3, -19.75D, 9.5D, 21));
        parts.add(new Part(Kind.LANTERN, 0, List.of(plainBox(11.53D, 38.7D, -25.68D, 6, 9, 6))));
        parts.add(new Part(Kind.HARNESS, 0, List.of(plainBox(-16, 7.5D, -85, 32, 8, 57))));
        return List.copyOf(parts);
    }

    private static List<Part> kibitkaParts() {
        List<Part> parts = new ArrayList<>();
        parts.add(wheel(0, 19.25D, 9.5D, -17));
        parts.add(wheel(1, -18.75D, 9.5D, -17));
        parts.add(wheel(2, 19.75D, 9.5D, 17));
        parts.add(wheel(3, -19.75D, 9.5D, 17));
        parts.add(new Part(Kind.LANTERN, 0, List.of(plainBox(-3, 36.1D, -39, 6, 7, 6))));
        parts.add(new Part(Kind.HARNESS, 0, List.of(plainBox(-16, 7.5D, -85, 32, 8, 57))));
        return List.copyOf(parts);
    }

    private static List<Part> barrelParts() {
        List<Part> parts = new ArrayList<>();
        parts.add(wheel(0, 19.25D, 9.5D, -17));
        parts.add(wheel(1, -18.75D, 9.5D, -17));
        parts.add(wheel(2, 19.75D, 9.5D, 17));
        parts.add(wheel(3, -19.75D, 9.5D, 17));
        parts.add(new Part(Kind.HARNESS, 0, List.of(plainBox(-16, 7.5D, -85, 32, 8, 57))));
        return List.copyOf(parts);
    }

    private static Part pumpkin(int index, double x, double y, double z, double px, double py, double pz, float yaw) {
        return new Part(Kind.PUMPKIN, index, List.of(pumpkinBox(x, y, z, 16, px, py, pz, yaw)));
    }

    private static AABB pumpkinBox(double x, double y, double z, double size, double px, double py, double pz, float yaw) {
        AABB spun = turnedBox(x, y, z, size, size, size, px, py, pz, yaw);
        return yawBox(spun, 9, 16.65124D, -10.65585D, -15);
    }

    private static AABB turnedBox(double x, double y, double z, double sx, double sy, double sz, double px, double py, double pz, float yaw) {
        return yawBox(plainBox(x, y, z, sx, sy, sz), px, py, pz, yaw);
    }

    private static AABB yawBox(AABB box, double px, double py, double pz, float yaw) {
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;
        for(int i = 0; i < 8; i++) {
            double cx = (i & 1) == 0 ? box.minX : box.maxX;
            double cy = (i & 2) == 0 ? box.minY : box.maxY;
            double cz = (i & 4) == 0 ? box.minZ : box.maxZ;
            Vec3 model = yawModel(cx * 16, cy * 16, -cz * 16, px, py, pz, yaw);
            Vec3 point = local(model.x, model.y, model.z);
            minX = Math.min(minX, point.x);
            minY = Math.min(minY, point.y);
            minZ = Math.min(minZ, point.z);
            maxX = Math.max(maxX, point.x);
            maxY = Math.max(maxY, point.y);
            maxZ = Math.max(maxZ, point.z);
        }
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private static AABB plainBox(double x, double y, double z, double sx, double sy, double sz) {
        Vec3 a = local(x, y, z);
        Vec3 b = local(x + sx, y + sy, z + sz);
        return new AABB(Math.min(a.x, b.x), Math.min(a.y, b.y), Math.min(a.z, b.z), Math.max(a.x, b.x), Math.max(a.y, b.y), Math.max(a.z, b.z));
    }

    private static Vec3 yawModel(double x, double y, double z, double px, double py, double pz, float degrees) {
        double angle = Math.toRadians(degrees);
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double ox = x - px;
        double oz = z - pz;
        return new Vec3(px + ox * cos + oz * sin, y, pz - ox * sin + oz * cos);
    }

    private static Vec3 local(double x, double y, double z) {
        return new Vec3(x / 16.0D, y / 16.0D, -z / 16.0D);
    }

}
