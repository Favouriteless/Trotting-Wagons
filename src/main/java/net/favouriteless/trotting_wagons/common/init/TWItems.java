package net.favouriteless.trotting_wagons.common.init;

import net.favouriteless.trotting_wagons.TrottingWagons;
import net.favouriteless.trotting_wagons.common.items.WagonItem;
import net.favouriteless.trotting_wagons.common.items.HorseWhipItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class TWItems {

    public static final DeferredRegister<Item> REGISTRY = DeferredRegister.create(ForgeRegistries.ITEMS, TrottingWagons.MOD_ID);

    public static final RegistryObject<WagonItem> CONESTOGA_WAGON = REGISTRY.register("conestoga_wagon", () -> new WagonItem(TWEntityTypes.CONESTOGA_WAGON::get, wagonProps()));
    public static final RegistryObject<WagonItem> ROYAL_WAGON = REGISTRY.register("royal_wagon", () -> new WagonItem(TWEntityTypes.ROYAL_WAGON::get, wagonProps()));
    public static final RegistryObject<WagonItem> ARMORED_WAGON = REGISTRY.register("armored_wagon", () -> new WagonItem(TWEntityTypes.ARMORED_WAGON::get, wagonProps()));

    public static final RegistryObject<HorseWhipItem> HORSE_WHIP = REGISTRY.register("horse_whip", () -> new HorseWhipItem(new Properties().stacksTo(1)));
    public static final RegistryObject<Item> WHEEL = REGISTRY.register("wheel", () -> new Item(new Properties()));

    private static Properties wagonProps() {
        return new Properties().stacksTo(1).rarity(Rarity.UNCOMMON);
    }

}
