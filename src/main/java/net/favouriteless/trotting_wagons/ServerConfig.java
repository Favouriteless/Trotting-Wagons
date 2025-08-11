package net.favouriteless.trotting_wagons;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.Builder;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.common.ForgeConfigSpec.DoubleValue;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

public class ServerConfig {

    public static final ServerConfig INSTANCE;
    public static final ForgeConfigSpec SPEC;

    public final ConfigValue<List<? extends String>> allowedEntities;
    public final ConfigValue<String> repairItem;
    public final IntValue repairPerItem;

    public final IntValue conestogaHealth;
    public final DoubleValue conestogaSpeed;
    public final DoubleValue conestogaAcceleration;
    public final DoubleValue conestogaTurnRate;

    public final IntValue royalHealth;
    public final DoubleValue royalSpeed;
    public final DoubleValue royalAcceleration;
    public final DoubleValue royalTurnRate;

    public final IntValue armoredHealth;
    public final DoubleValue armoredSpeed;
    public final DoubleValue armoredAcceleration;
    public final DoubleValue armoredTurnRate;

    public ServerConfig(Builder builder) {
        this.allowedEntities = builder.comment("EntityTypes allowed to pull wagons").defineList("allowed_entities", List.of("minecraft:horse"), e -> true);
        this.repairItem = builder.comment("ID of the item (or tag, if starting with #) used to repair wagons").define("repair_material", "minecraft:iron_ingot");
        this.repairPerItem = builder.comment("The amount of durability regained per repair item.").defineInRange("repair_value", 10, 0, Integer.MAX_VALUE);

        builder.push("Conestoga Wagon Options");
        this.conestogaHealth = builder.comment("Maximum health").defineInRange("conestoga_wagon_health", 60, 0, Integer.MAX_VALUE);
        this.conestogaSpeed = builder.comment("Speed (per level)").defineInRange("conestoga_wagon_speed", 0.1D, 0.0D, Double.MAX_VALUE);
        this.conestogaAcceleration = builder.comment("Acceleration (seconds until maximum speed)").defineInRange("conestoga_wagon_acceleration", 2.0D, 0.0D, Double.MAX_VALUE);
        this.conestogaTurnRate = builder.comment("Turn rate").defineInRange("conestoga_wagon_turn_rate", 2.0D, 0.0D, Double.MAX_VALUE);
        builder.pop();
        builder.push("Royal Wagon Options");
        this.royalHealth = builder.comment("Maximum health").defineInRange("royal_wagon_health", 80, 0, Integer.MAX_VALUE);
        this.royalSpeed = builder.comment("Speed (per level)").defineInRange("royal_wagon_speed", 0.15D, 0.0D, Double.MAX_VALUE);
        this.royalAcceleration = builder.comment("Acceleration (seconds until maximum speed)").defineInRange("royal_wagon_acceleration", 3.0D, 0.0D, Double.MAX_VALUE);
        this.royalTurnRate = builder.comment("Turn rate").defineInRange("royal_wagon_turn_rate", 2.0D, 0.0D, Double.MAX_VALUE);
        builder.pop();
        builder.push("Armored Wagon Options");
        this.armoredHealth = builder.comment("Maximum health").defineInRange("armored_wagon_health", 120, 0, Integer.MAX_VALUE);
        this.armoredSpeed = builder.comment("Speed (per level)").defineInRange("armored_wagon_speed", 0.1D, 0.0D, Double.MAX_VALUE);
        this.armoredAcceleration = builder.comment("Acceleration (seconds until maximum speed)").defineInRange("armored_wagon_acceleration", 4.0D, 0.0D, Double.MAX_VALUE);
        this.armoredTurnRate = builder.comment("Turn rate").defineInRange("armored_wagon_turn_rate", 2.0D, 0.0D, Double.MAX_VALUE);
        builder.pop();
    }

    static {
        Pair<ServerConfig, ForgeConfigSpec> pair = new ForgeConfigSpec.Builder().configure(ServerConfig::new);
        INSTANCE = pair.getLeft();
        SPEC = pair.getRight();
    }

}
