package net.favouriteless.trotting_wagons.common.init;

import net.favouriteless.trotting_wagons.TrottingWagons;
import net.favouriteless.trotting_wagons.common.items.WagonItem;
import net.favouriteless.trotting_wagons.common.items.HorseWhipItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class TWItems {

    public static final DeferredRegister<Item> REGISTRY = DeferredRegister.create(BuiltInRegistries.ITEM, TrottingWagons.MOD_ID);

    public static final DeferredHolder<Item, WagonItem> CONESTOGA_WAGON = REGISTRY.register("conestoga_wagon", () -> new WagonItem(TWEntityTypes.CONESTOGA_WAGON::get, wagonProps()));
    public static final DeferredHolder<Item, WagonItem> ROYAL_WAGON = REGISTRY.register("royal_wagon", () -> new WagonItem(TWEntityTypes.ROYAL_WAGON::get, wagonProps()));
    public static final DeferredHolder<Item, WagonItem> ARMORED_WAGON = REGISTRY.register("armored_wagon", () -> new WagonItem(TWEntityTypes.ARMORED_WAGON::get, wagonProps()));

    public static final DeferredHolder<Item, HorseWhipItem> HORSE_WHIP = REGISTRY.register("horse_whip", () -> new HorseWhipItem(new Properties().stacksTo(1)));
    public static final DeferredHolder<Item, Item> WHEEL = REGISTRY.register("wheel", () -> new Item(new Properties()));

    private static Properties wagonProps() {
        return new Properties()
                .stacksTo(1)
                .rarity(Rarity.UNCOMMON)
                .component(DataComponents.BASE_COLOR, DyeColor.WHITE);
    }

}
