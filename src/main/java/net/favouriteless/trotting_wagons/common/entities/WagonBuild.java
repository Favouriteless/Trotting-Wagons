package net.favouriteless.trotting_wagons.common.entities;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import net.favouriteless.trotting_wagons.common.init.TWItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Items spent on one build stage. The first matching need accepts a held stack. */
public final class WagonBuild {

    public static final int PLANKS = 16;
    public static final int IRON = 4;
    public static final int ANIMAL_STICKS = 6;
    public static final int ANIMAL_PLANKS = 32;
    public static final int ANIMAL_IRON = 12;
    public static final int ANIMAL_HAY = 4;
    public static final int ANIMAL_WOOL = 1;

    private final Need[] needs;
    private final List<ItemStack> stacks = new ArrayList<>();

    public WagonBuild() {
        this(need("planks", stack -> stack.is(ItemTags.PLANKS), PLANKS), need("iron", stack -> stack.is(Items.IRON_INGOT), IRON));
    }

    public WagonBuild(Need... needs) {
        this.needs = needs;
    }

    public static WagonBuild none() {
        return new WagonBuild(new Need[0]);
    }

    public static WagonBuild forChoice(String id) {
        if(id == null)
            return none();
        return switch(id) {
            case "animal_wagon" -> animal();
            case "armored_wagon" -> armored();
            case "barrel_wagon" -> barrel();
            case "big_cart" -> bigCart();
            case "conestoga_wagon" -> conestoga();
            case "kibitka_wagon" -> kibitka();
            case "log_wagon" -> log();
            case "passenger_wagon" -> passenger();
            case "royal_wagon" -> royal();
            case "ware_wagon" -> ware();
            default -> none();
        };
    }

    public static WagonBuild animal() {
        return new WagonBuild(
                need("sticks", stack -> stack.is(TWItems.LONG_STICK.get()), ANIMAL_STICKS),
                need("planks", stack -> stack.is(ItemTags.PLANKS), ANIMAL_PLANKS),
                need("iron", stack -> stack.is(Items.IRON_INGOT), ANIMAL_IRON),
                need("hay", stack -> stack.is(Items.HAY_BLOCK), ANIMAL_HAY),
                need("wool", stack -> stack.is(ItemTags.WOOL), ANIMAL_WOOL));
    }

    public static WagonBuild armored() {
        return new WagonBuild(
                need("planks", stack -> stack.is(ItemTags.PLANKS), 64),
                need("sticks", stack -> stack.is(TWItems.LONG_STICK.get()), 12),
                need("iron_block", stack -> stack.is(Items.IRON_BLOCK), 2),
                need("iron", stack -> stack.is(Items.IRON_INGOT), 32),
                need("string", stack -> stack.is(Items.STRING), 8));
    }

    public static WagonBuild barrel() {
        return new WagonBuild(
                need("sticks", stack -> stack.is(TWItems.LONG_STICK.get()), 4),
                need("planks", stack -> stack.is(ItemTags.PLANKS), 24),
                need("iron", stack -> stack.is(Items.IRON_INGOT), 12),
                need("wool", stack -> stack.is(ItemTags.WOOL), 1),
                need("string", stack -> stack.is(Items.STRING), 8));
    }

    public static WagonBuild bigCart() {
        return new WagonBuild(
                need("planks", stack -> stack.is(ItemTags.PLANKS), 16),
                need("string", stack -> stack.is(Items.STRING), 16),
                need("wool", stack -> stack.is(ItemTags.WOOL), 1),
                need("iron", stack -> stack.is(Items.IRON_INGOT), 8));
    }

    public static WagonBuild conestoga() {
        return new WagonBuild(
                need("chest", stack -> stack.is(Items.CHEST), 2),
                need("wool", stack -> stack.is(ItemTags.WOOL), 16),
                need("sticks", stack -> stack.is(TWItems.LONG_STICK.get()), 18),
                need("lead", stack -> stack.is(Items.LEAD), 1),
                need("planks", stack -> stack.is(ItemTags.PLANKS), 32),
                need("iron", stack -> stack.is(Items.IRON_INGOT), 8));
    }

    public static WagonBuild kibitka() {
        return new WagonBuild(
                need("red_dye", stack -> stack.is(Items.RED_DYE), 2),
                need("yellow_dye", stack -> stack.is(Items.YELLOW_DYE), 2),
                need("blue_dye", stack -> stack.is(Items.BLUE_DYE), 2),
                need("bed", stack -> stack.is(ItemTags.BEDS), 1),
                need("chest", stack -> stack.is(Items.CHEST), 1),
                need("log", stack -> stack.is(ItemTags.LOGS), 4),
                need("sticks", stack -> stack.is(TWItems.LONG_STICK.get()), 12),
                need("planks", stack -> stack.is(ItemTags.PLANKS), 40),
                need("iron", stack -> stack.is(Items.IRON_INGOT), 8),
                need("wool", stack -> stack.is(ItemTags.WOOL), 4),
                need("brick", stack -> stack.is(Items.BRICK), 40));
    }

    public static WagonBuild log() {
        return new WagonBuild(
                need("iron", stack -> stack.is(Items.IRON_INGOT), 24),
                need("sticks", stack -> stack.is(TWItems.LONG_STICK.get()), 16),
                need("planks", stack -> stack.is(ItemTags.PLANKS), 24),
                need("wool", stack -> stack.is(ItemTags.WOOL), 1));
    }

    public static WagonBuild passenger() {
        return new WagonBuild(
                need("wool", stack -> stack.is(ItemTags.WOOL), 1),
                need("planks", stack -> stack.is(ItemTags.PLANKS), 64),
                need("string", stack -> stack.is(Items.STRING), 8),
                need("iron", stack -> stack.is(Items.IRON_INGOT), 32));
    }

    public static WagonBuild royal() {
        return new WagonBuild(
                need("wool", stack -> stack.is(ItemTags.WOOL), 16),
                need("chest", stack -> stack.is(Items.CHEST), 1),
                need("string", stack -> stack.is(Items.STRING), 8),
                need("planks", stack -> stack.is(ItemTags.PLANKS), 32),
                need("iron", stack -> stack.is(Items.IRON_INGOT), 12));
    }

    public static WagonBuild ware() {
        return new WagonBuild(
                need("iron", stack -> stack.is(Items.IRON_INGOT), 32),
                need("planks", stack -> stack.is(ItemTags.PLANKS), 48),
                need("wool", stack -> stack.is(ItemTags.WOOL), 1),
                need("string", stack -> stack.is(Items.STRING), 16));
    }

    private static Need need(String name, Predicate<ItemStack> test, int required) {
        return new Need(name, test, required);
    }

    public int size() {
        return needs.length;
    }

    public int required(int index) {
        if(index < 0 || index >= needs.length)
            return 0;
        return needs[index].required;
    }

    public String needName(int index) {
        if(index < 0 || index >= needs.length)
            return "";
        return needs[index].name;
    }

    public int countAt(int index) {
        return count(needs[index].test);
    }

    public int planks() {
        return count(stack -> stack.is(ItemTags.PLANKS));
    }

    public int iron() {
        return count(stack -> stack.is(Items.IRON_INGOT));
    }

    public boolean ready() {
        for(int i = 0; i < needs.length; i++)
            if(countAt(i) < needs[i].required)
                return false;
        return true;
    }

    public int roomFor(ItemStack hand) {
        for(Need need : needs) {
            if(!need.test.test(hand))
                continue;
            return Math.max(0, need.required - count(need.test));
        }
        return 0;
    }

    /** Moves up to {@code limit} matching items, and never more than this stage still needs. */
    public boolean offer(ItemStack hand, boolean creative, int limit) {
        int room = roomFor(hand);
        if(room <= 0 || hand.isEmpty() || limit <= 0)
            return false;
        int take = Math.min(room, Math.min(hand.getCount(), limit));
        add(hand.copyWithCount(take));
        if(!creative)
            hand.shrink(take);
        return true;
    }

    public void copyFrom(WagonBuild other) {
        stacks.clear();
        for(ItemStack stack : other.stacks)
            stacks.add(stack.copy());
    }

    public void drop(Entity entity) {
        for(ItemStack stack : stacks)
            if(!stack.isEmpty())
                entity.spawnAtLocation(stack.copy());
    }

    /** Drops up to {@code amount} stored items of this kind. The remainder uses {@code fallback}. */
    public void dropUpTo(Entity entity, Predicate<ItemStack> test, int amount, Item fallback) {
        if(amount <= 0)
            return;
        int left = amount;
        for(ItemStack stack : stacks) {
            if(left <= 0)
                break;
            if(stack.isEmpty() || !test.test(stack))
                continue;
            int take = Math.min(left, stack.getCount());
            entity.spawnAtLocation(stack.copyWithCount(take));
            left -= take;
        }
        if(left > 0)
            entity.spawnAtLocation(new ItemStack(fallback, left));
    }

    public void save(CompoundTag tag, HolderLookup.Provider registries) {
        save(tag, "Build", registries);
    }

    public void save(CompoundTag tag, String key, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for(ItemStack stack : stacks) {
            if(stack.isEmpty())
                continue;
            Tag saved = stack.save(registries, new CompoundTag());
            list.add(saved);
        }
        tag.put(key, list);
    }

    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        load(tag, "Build", registries);
    }

    public void load(CompoundTag tag, String key, HolderLookup.Provider registries) {
        stacks.clear();
        if(!tag.contains(key, Tag.TAG_LIST))
            return;
        for(Tag element : tag.getList(key, Tag.TAG_COMPOUND)) {
            ItemStack stack = ItemStack.parse(registries, element).orElse(ItemStack.EMPTY);
            if(!stack.isEmpty())
                stacks.add(stack);
        }
    }

    private void add(ItemStack incoming) {
        for(ItemStack stored : stacks) {
            if(!ItemStack.isSameItemSameComponents(stored, incoming))
                continue;
            int room = stored.getMaxStackSize() - stored.getCount();
            if(room <= 0)
                continue;
            int move = Math.min(room, incoming.getCount());
            stored.grow(move);
            incoming.shrink(move);
            if(incoming.isEmpty())
                return;
        }
        if(!incoming.isEmpty())
            stacks.add(incoming);
    }

    private int count(Predicate<ItemStack> test) {
        int count = 0;
        for(ItemStack stack : stacks)
            if(test.test(stack))
                count += stack.getCount();
        return count;
    }

    public record Need(String name, Predicate<ItemStack> test, int required) {}

}
