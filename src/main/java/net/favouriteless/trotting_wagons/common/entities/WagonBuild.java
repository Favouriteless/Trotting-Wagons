package net.favouriteless.trotting_wagons.common.entities;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Planks and iron spent while a chassis is still being built. */
public final class WagonBuild {

    public static final int PLANKS = 16;
    public static final int IRON = 4;

    private final List<ItemStack> stacks = new ArrayList<>();

    public int planks() {
        return count(stack -> stack.is(ItemTags.PLANKS));
    }

    public int iron() {
        return count(stack -> stack.is(Items.IRON_INGOT));
    }

    public boolean ready() {
        return planks() >= PLANKS && iron() >= IRON;
    }

    public int roomFor(ItemStack hand) {
        if(hand.is(ItemTags.PLANKS))
            return Math.max(0, PLANKS - planks());
        if(hand.is(Items.IRON_INGOT))
            return Math.max(0, IRON - iron());
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

    public void save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for(ItemStack stack : stacks) {
            if(stack.isEmpty())
                continue;
            Tag saved = stack.save(registries, new CompoundTag());
            list.add(saved);
        }
        tag.put("Build", list);
    }

    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        stacks.clear();
        if(!tag.contains("Build", Tag.TAG_LIST))
            return;
        for(Tag element : tag.getList("Build", Tag.TAG_COMPOUND)) {
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

    private int count(java.util.function.Predicate<ItemStack> test) {
        int count = 0;
        for(ItemStack stack : stacks)
            if(test.test(stack))
                count += stack.getCount();
        return count;
    }

}
