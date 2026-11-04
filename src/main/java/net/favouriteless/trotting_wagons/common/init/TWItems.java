package net.favouriteless.trotting_wagons.common.init;

import net.favouriteless.trotting_wagons.TrottingWagons;
import net.favouriteless.trotting_wagons.common.items.ChassisItem;
import net.favouriteless.trotting_wagons.common.items.HorseWhipItem;
import net.favouriteless.trotting_wagons.common.items.WagonItem;
import net.favouriteless.trotting_wagons.common.items.WheelItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class TWItems {

    public static final DeferredRegister<Item> REGISTRY = DeferredRegister.create(BuiltInRegistries.ITEM, TrottingWagons.MOD_ID);

    public static final DeferredHolder<Item, WagonItem> CONESTOGA_WAGON = REGISTRY.register("conestoga_wagon", () -> new WagonItem(TWEntityTypes.CONESTOGA_WAGON::get, wagonProps()));
    public static final DeferredHolder<Item, WagonItem> ROYAL_WAGON = REGISTRY.register("royal_wagon", () -> new WagonItem(TWEntityTypes.ROYAL_WAGON::get, wagonProps()));
    public static final DeferredHolder<Item, WagonItem> ARMORED_WAGON = REGISTRY.register("armored_wagon", () -> new WagonItem(TWEntityTypes.ARMORED_WAGON::get, wagonProps()));
    public static final DeferredHolder<Item, WagonItem> DEBUG_WAGON = REGISTRY.register("debug_wagon", () -> new WagonItem(TWEntityTypes.DEBUG_WAGON::get, wagonProps()));
    public static final DeferredHolder<Item, WagonItem> ANIMAL_WAGON = REGISTRY.register("animal_wagon", () -> new WagonItem(TWEntityTypes.ANIMAL_WAGON::get, wagonProps()));
    public static final DeferredHolder<Item, WagonItem> LOG_WAGON = REGISTRY.register("log_wagon", () -> new WagonItem(TWEntityTypes.LOG_WAGON::get, wagonProps()));
    public static final DeferredHolder<Item, WagonItem> BARREL_WAGON = REGISTRY.register("barrel_wagon", () -> new WagonItem(TWEntityTypes.BARREL_WAGON::get, wagonProps()));
    public static final DeferredHolder<Item, WagonItem> PASSENGER_WAGON = REGISTRY.register("passenger_wagon", () -> new WagonItem(TWEntityTypes.PASSENGER_WAGON::get, wagonProps()));
    public static final DeferredHolder<Item, WagonItem> WARE_WAGON = REGISTRY.register("ware_wagon", () -> new WagonItem(TWEntityTypes.WARE_WAGON::get, wagonProps()));
    public static final DeferredHolder<Item, WagonItem> KIBITKA_WAGON = REGISTRY.register("kibitka_wagon", () -> new WagonItem(TWEntityTypes.KIBITKA_WAGON::get, wagonProps()));
    public static final DeferredHolder<Item, WagonItem> BIG_CART_WAGON = REGISTRY.register("big_cart", () -> new WagonItem(TWEntityTypes.BIG_CART_WAGON::get, wagonProps()));

    public static final DeferredHolder<Item, HorseWhipItem> HORSE_WHIP = REGISTRY.register("horse_whip", () -> new HorseWhipItem(new Properties().stacksTo(1)));
    /** Two animals. The model bone is Tongue. */
    public static final DeferredHolder<Item, Item> TONGUE = REGISTRY.register("tongue", () -> new Item(new Properties().stacksTo(1)));
    /** One animal. The model bone is shafts. */
    public static final DeferredHolder<Item, Item> SHAFTS = REGISTRY.register("shafts", () -> new Item(new Properties().stacksTo(1)));
    public static final DeferredHolder<Item, WheelItem> WHEEL = REGISTRY.register("wheel", () -> new WheelItem(new Properties().durability(100)));
    public static final DeferredHolder<Item, WheelItem> WIDE_WHEEL = REGISTRY.register("wide_wheel", () -> new WheelItem(new Properties().durability(100)));
    public static final DeferredHolder<Item, WheelItem> REINFORCED_WHEEL = REGISTRY.register("reinforced_wheel", () -> new WheelItem(new Properties().durability(300)));
    public static final DeferredHolder<Item, Item> LONG_STICK = REGISTRY.register("long_stick", () -> new Item(new Properties()));
    public static final DeferredHolder<Item, ChassisItem> CHASSIS = REGISTRY.register("chassis", () -> new ChassisItem(new Properties()));

    public static boolean isHarness(ItemStack stack) {
        return stack.is(TONGUE.get()) || stack.is(SHAFTS.get());
    }

    private static Properties wagonProps() {
        return new Properties()
                .stacksTo(1)
                .rarity(Rarity.UNCOMMON)
                .component(DataComponents.BASE_COLOR, DyeColor.WHITE);
    }

}
