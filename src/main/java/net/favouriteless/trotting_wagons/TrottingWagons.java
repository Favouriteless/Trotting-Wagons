package net.favouriteless.trotting_wagons;

import com.mojang.logging.LogUtils;
import net.favouriteless.trotting_wagons.common.init.TWEntityTypes;
import net.favouriteless.trotting_wagons.common.init.TWItems;
import net.favouriteless.trotting_wagons.common.init.TWMenuTypes;
import net.favouriteless.trotting_wagons.common.init.TWSoundEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig.Type;
import org.slf4j.Logger;

import java.util.Map;

@Mod(TrottingWagons.MOD_ID)
public class TrottingWagons {

    public static final String MOD_ID = "trotting_wagons";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final TagKey<EntityType<?>> CANNOT_MOUNT_WAGON = TagKey.create(Registries.ENTITY_TYPE, id("cannot_mount_wagon"));

    public static final Map<Item, DyeColor> DYE_ITEMS = Map.ofEntries(
        Map.entry(Items.WHITE_WOOL, DyeColor.WHITE),
        Map.entry(Items.ORANGE_WOOL, DyeColor.ORANGE),
        Map.entry(Items.MAGENTA_WOOL, DyeColor.MAGENTA),
        Map.entry(Items.LIGHT_BLUE_WOOL, DyeColor.LIGHT_BLUE),
        Map.entry(Items.YELLOW_WOOL, DyeColor.YELLOW),
        Map.entry(Items.LIME_WOOL, DyeColor.LIME),
        Map.entry(Items.PINK_WOOL, DyeColor.PINK),
        Map.entry(Items.GRAY_WOOL, DyeColor.GRAY),
        Map.entry(Items.LIGHT_GRAY_WOOL, DyeColor.LIGHT_GRAY),
        Map.entry(Items.CYAN_WOOL, DyeColor.CYAN),
        Map.entry(Items.PURPLE_WOOL, DyeColor.PURPLE),
        Map.entry(Items.BLUE_WOOL, DyeColor.BLUE),
        Map.entry(Items.BROWN_WOOL, DyeColor.BROWN),
        Map.entry(Items.GREEN_WOOL, DyeColor.GREEN),
        Map.entry(Items.RED_WOOL, DyeColor.RED),
        Map.entry(Items.BLACK_WOOL, DyeColor.BLACK)
    );


    public TrottingWagons(IEventBus bus, ModContainer container) {
        TWEntityTypes.REGISTRY.register(bus);
        TWItems.REGISTRY.register(bus);
        TWMenuTypes.REGISTRY.register(bus);
        TWSoundEvents.REGISTRY.register(bus);

        container.registerConfig(Type.SERVER, ServerConfig.SPEC);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

}
