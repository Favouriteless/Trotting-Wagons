package net.favouriteless.trotting_wagons;

import com.eliotlash.mclib.math.Variable;
import com.mojang.logging.LogUtils;
import net.favouriteless.trotting_wagons.common.init.*;
import net.favouriteless.trotting_wagons.common.items.WagonItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.*;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig.Type;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import software.bernie.geckolib.core.molang.MolangParser;

import java.util.HashMap;
import java.util.Map;

@Mod(TrottingWagons.MOD_ID)
public class TrottingWagons {

    public static final String MOD_ID = "trotting_wagons";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final Map<Item, DyeColor> DYE_ITEMS = new HashMap<>();
    public static final TagKey<EntityType<?>> CANNOT_MOUNT_WAGON = TagKey.create(Registries.ENTITY_TYPE, id("cannot_mount_wagon"));

    static {
        DYE_ITEMS.put(Items.WHITE_WOOL, DyeColor.WHITE);
        DYE_ITEMS.put(Items.ORANGE_WOOL, DyeColor.ORANGE);
        DYE_ITEMS.put(Items.MAGENTA_WOOL, DyeColor.MAGENTA);
        DYE_ITEMS.put(Items.LIGHT_BLUE_WOOL, DyeColor.LIGHT_BLUE);
        DYE_ITEMS.put(Items.YELLOW_WOOL, DyeColor.YELLOW);
        DYE_ITEMS.put(Items.LIME_WOOL, DyeColor.LIME);
        DYE_ITEMS.put(Items.PINK_WOOL, DyeColor.PINK);
        DYE_ITEMS.put(Items.GRAY_WOOL, DyeColor.GRAY);
        DYE_ITEMS.put(Items.LIGHT_GRAY_WOOL, DyeColor.LIGHT_GRAY);
        DYE_ITEMS.put(Items.CYAN_WOOL, DyeColor.CYAN);
        DYE_ITEMS.put(Items.PURPLE_WOOL, DyeColor.PURPLE);
        DYE_ITEMS.put(Items.BLUE_WOOL, DyeColor.BLUE);
        DYE_ITEMS.put(Items.BROWN_WOOL, DyeColor.BROWN);
        DYE_ITEMS.put(Items.GREEN_WOOL, DyeColor.GREEN);
        DYE_ITEMS.put(Items.RED_WOOL, DyeColor.RED);
        DYE_ITEMS.put(Items.BLACK_WOOL, DyeColor.BLACK);
    }

    public TrottingWagons() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        bus.addListener(TrottingWagons::onCreativeTabContents);

        TWEntityTypes.REGISTRY.register(bus);
        TWItems.REGISTRY.register(bus);
        TWMenuTypes.REGISTRY.register(bus);
        TWSoundEvents.REGISTRY.register(bus);
        TWPackets.register();

        MolangParser.INSTANCE.register(new Variable("query.wagon_speed", 0));
        ModLoadingContext.get().registerConfig(Type.SERVER, ServerConfig.SPEC);
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }

    public static void onCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if(!event.getTabKey().equals(CreativeModeTabs.REDSTONE_BLOCKS))
            return;

        event.accept(TWItems.ARMORED_WAGON.get());
        event.accept(WagonItem.setupNbt(new ItemStack(TWItems.CONESTOGA_WAGON.get()), DyeColor.WHITE));
        event.accept(WagonItem.setupNbt(new ItemStack(TWItems.ROYAL_WAGON.get()), DyeColor.WHITE));
        event.accept(TWItems.HORSE_WHIP.get());
        event.accept(TWItems.WHEEL.get());
    }

}
