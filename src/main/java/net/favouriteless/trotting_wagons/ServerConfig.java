package net.favouriteless.trotting_wagons;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.Builder;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;
import net.neoforged.neoforge.common.ModConfigSpec.DoubleValue;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

public class ServerConfig {

    public static final ServerConfig INSTANCE;
    public static final ModConfigSpec SPEC;

    public final ConfigValue<List<? extends String>> allowedEntities;

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

    public final IntValue debugHealth;
    public final DoubleValue debugSpeed;
    public final DoubleValue debugAcceleration;
    public final DoubleValue debugTurnRate;

    public final IntValue animalHealth;
    public final DoubleValue animalSpeed;
    public final DoubleValue animalAcceleration;
    public final DoubleValue animalTurnRate;

    public final IntValue logHealth;
    public final DoubleValue logSpeedEmpty;
    public final DoubleValue logSpeedPartial;
    public final DoubleValue logSpeedFull;
    public final DoubleValue logAcceleration;
    public final DoubleValue logTurnRate;

    public final IntValue barrelHealth;
    public final DoubleValue barrelSpeedEmpty;
    public final DoubleValue barrelSpeedPartial;
    public final DoubleValue barrelSpeedFull;
    public final DoubleValue barrelAcceleration;
    public final DoubleValue barrelTurnRate;

    public final IntValue passengerHealth;
    public final DoubleValue passengerSpeed;
    public final DoubleValue passengerAcceleration;
    public final DoubleValue passengerTurnRate;

    public final IntValue wareHealth;
    public final DoubleValue wareSpeedEmpty;
    public final DoubleValue wareSpeedPerChest;
    public final DoubleValue wareAcceleration;
    public final DoubleValue wareTurnRate;

    public final IntValue kibitkaHealth;
    public final DoubleValue kibitkaSpeed;
    public final DoubleValue kibitkaAcceleration;
    public final DoubleValue kibitkaTurnRate;

    public final IntValue bigCartHealth;
    public final DoubleValue bigCartSpeed;
    public final DoubleValue bigCartAcceleration;
    public final DoubleValue bigCartTurnRate;

    public ServerConfig(Builder builder) {
        this.allowedEntities = builder.comment("EntityTypes allowed to pull wagons").defineList("allowed_entities", List.of(
                "minecraft:horse"
        ), e -> true);
        builder.push("Conestoga Wagon Options");
        this.conestogaHealth = builder.comment("Maximum health").defineInRange("conestoga_wagon_health", 60, 0, Integer.MAX_VALUE);
        this.conestogaSpeed = builder.comment("Speed coefficient, from 0 to 1. Maximum speed is the slowest hitched animal times this value.").defineInRange("conestoga_wagon_speed_coefficient", 0.8D, 0.0D, 1.0D);
        this.conestogaAcceleration = builder.comment("Unused. Acceleration is the speed coefficient, in blocks per second per second.").defineInRange("conestoga_wagon_acceleration", 2.0D, 0.0D, Double.MAX_VALUE);
        this.conestogaTurnRate = builder.comment("Turn rate").defineInRange("conestoga_wagon_turn_rate", 2.0D, 0.0D, Double.MAX_VALUE);
        builder.pop();
        builder.push("Royal Wagon Options");
        this.royalHealth = builder.comment("Maximum health").defineInRange("royal_wagon_health", 80, 0, Integer.MAX_VALUE);
        this.royalSpeed = builder.comment("Speed coefficient, from 0 to 1.").defineInRange("royal_wagon_speed_coefficient", 1.0D, 0.0D, 1.0D);
        this.royalAcceleration = builder.comment("Unused. Acceleration is the speed coefficient, in blocks per second per second.").defineInRange("royal_wagon_acceleration", 3.0D, 0.0D, Double.MAX_VALUE);
        this.royalTurnRate = builder.comment("Turn rate").defineInRange("royal_wagon_turn_rate", 2.0D, 0.0D, Double.MAX_VALUE);
        builder.pop();
        builder.push("Armored Wagon Options");
        this.armoredHealth = builder.comment("Maximum health").defineInRange("armored_wagon_health", 120, 0, Integer.MAX_VALUE);
        this.armoredSpeed = builder.comment("Speed coefficient, from 0 to 1.").defineInRange("armored_wagon_speed_coefficient", 0.5D, 0.0D, 1.0D);
        this.armoredAcceleration = builder.comment("Unused. Acceleration is the speed coefficient, in blocks per second per second.").defineInRange("armored_wagon_acceleration", 4.0D, 0.0D, Double.MAX_VALUE);
        this.armoredTurnRate = builder.comment("Turn rate").defineInRange("armored_wagon_turn_rate", 2.0D, 0.0D, Double.MAX_VALUE);
        builder.pop();
        builder.push("Debug Wagon Options");
        this.debugHealth = builder.comment("Maximum health").defineInRange("debug_wagon_health", 67, 0, Integer.MAX_VALUE);
        this.debugSpeed = builder.comment("Speed coefficient, from 0 to 1.").defineInRange("debug_wagon_speed_coefficient", 0.9D, 0.0D, 1.0D);
        this.debugAcceleration = builder.comment("Unused. Acceleration is the speed coefficient, in blocks per second per second.").defineInRange("debug_wagon_acceleration", 3.0D, 0.0D, Double.MAX_VALUE);
        this.debugTurnRate = builder.comment("Turn rate").defineInRange("debug_wagon_turn_rate", 2.0D, 0.0D, Double.MAX_VALUE);
        builder.pop();
        builder.push("Animal Wagon Options");
        this.animalHealth = builder.comment("Maximum health").defineInRange("animal_wagon_health", 60, 0, Integer.MAX_VALUE);
        this.animalSpeed = builder.comment("Speed coefficient, from 0 to 1.").defineInRange("animal_wagon_speed_coefficient", 0.9D, 0.0D, 1.0D);
        this.animalAcceleration = builder.comment("Unused. Acceleration is the speed coefficient, in blocks per second per second.").defineInRange("animal_wagon_acceleration", 3.0D, 0.0D, Double.MAX_VALUE);
        this.animalTurnRate = builder.comment("Turn rate").defineInRange("animal_wagon_turn_rate", 2.0D, 0.0D, Double.MAX_VALUE);
        builder.pop();
        builder.push("Log Wagon Options");
        this.logHealth = builder.comment("Maximum health").defineInRange("log_wagon_health", 80, 0, Integer.MAX_VALUE);
        this.logSpeedEmpty = builder.comment("Speed coefficient at 0 items, from 0 to 1. Applied when the cargo changes.").defineInRange("log_wagon_speed_empty", 1.0D, 0.0D, 1.0D);
        this.logSpeedPartial = builder.comment("Speed coefficient from 1 to 1000 items, from 0 to 1. Applied when the cargo changes.").defineInRange("log_wagon_speed_partial", 0.9D, 0.0D, 1.0D);
        this.logSpeedFull = builder.comment("Speed coefficient above 1000 items, from 0 to 1. Applied when the cargo changes.").defineInRange("log_wagon_speed_full", 0.8D, 0.0D, 1.0D);
        this.logAcceleration = builder.comment("Unused. Acceleration is the current speed coefficient, in blocks per second per second.").defineInRange("log_wagon_acceleration", 3.0D, 0.0D, Double.MAX_VALUE);
        this.logTurnRate = builder.comment("Turn rate").defineInRange("log_wagon_turn_rate", 2.0D, 0.0D, Double.MAX_VALUE);
        builder.pop();
        builder.push("Barrel Wagon Options");
        this.barrelHealth = builder.comment("Maximum health").defineInRange("barrel_wagon_health", 80, 0, Integer.MAX_VALUE);
        this.barrelSpeedEmpty = builder.comment("Speed coefficient with no barrels, from 0 to 1.").defineInRange("barrel_wagon_speed_empty", 1.0D, 0.0D, 1.0D);
        this.barrelSpeedPartial = builder.comment("Speed coefficient from 1 to 6 barrels, from 0 to 1.").defineInRange("barrel_wagon_speed_partial", 0.9D, 0.0D, 1.0D);
        this.barrelSpeedFull = builder.comment("Speed coefficient from 7 to 12 barrels, from 0 to 1.").defineInRange("barrel_wagon_speed_full", 0.8D, 0.0D, 1.0D);
        this.barrelAcceleration = builder.comment("Unused. Acceleration is the current speed coefficient, in blocks per second per second.").defineInRange("barrel_wagon_acceleration", 3.0D, 0.0D, Double.MAX_VALUE);
        this.barrelTurnRate = builder.comment("Turn rate").defineInRange("barrel_wagon_turn_rate", 2.0D, 0.0D, Double.MAX_VALUE);
        builder.pop();
        builder.push("Passenger Wagon Options");
        this.passengerHealth = builder.comment("Maximum health").defineInRange("passenger_wagon_health", 100, 0, Integer.MAX_VALUE);
        this.passengerSpeed = builder.comment("Speed coefficient, from 0 to 1.").defineInRange("passenger_wagon_speed_coefficient", 0.9D, 0.0D, 1.0D);
        this.passengerAcceleration = builder.comment("Unused. Acceleration is the speed coefficient, in blocks per second per second.").defineInRange("passenger_wagon_acceleration", 3.0D, 0.0D, Double.MAX_VALUE);
        this.passengerTurnRate = builder.comment("Turn rate").defineInRange("passenger_wagon_turn_rate", 2.0D, 0.0D, Double.MAX_VALUE);
        builder.pop();
        builder.push("Ware Wagon Options");
        this.wareHealth = builder.comment("Maximum health").defineInRange("ware_wagon_health", 100, 0, Integer.MAX_VALUE);
        this.wareSpeedEmpty = builder.comment("Speed coefficient with no chests, from 0 to 1.").defineInRange("ware_wagon_speed_empty", 0.9D, 0.0D, 1.0D);
        this.wareSpeedPerChest = builder.comment("Subtracted once for each placed chest. The result is never below 0.").defineInRange("ware_wagon_speed_per_chest", 0.1D, 0.0D, 1.0D);
        this.wareAcceleration = builder.comment("Unused. Acceleration is the current speed coefficient, in blocks per second per second.").defineInRange("ware_wagon_acceleration", 3.0D, 0.0D, Double.MAX_VALUE);
        this.wareTurnRate = builder.comment("Turn rate").defineInRange("ware_wagon_turn_rate", 2.0D, 0.0D, Double.MAX_VALUE);
        builder.pop();
        builder.push("Kibitka Wagon Options");
        this.kibitkaHealth = builder.comment("Maximum health").defineInRange("kibitka_wagon_health", 60, 0, Integer.MAX_VALUE);
        this.kibitkaSpeed = builder.comment("Speed coefficient, from 0 to 1.").defineInRange("kibitka_wagon_speed_coefficient", 0.9D, 0.0D, 1.0D);
        this.kibitkaAcceleration = builder.comment("Unused. Acceleration is the speed coefficient, in blocks per second per second.").defineInRange("kibitka_wagon_acceleration", 3.0D, 0.0D, Double.MAX_VALUE);
        this.kibitkaTurnRate = builder.comment("Turn rate").defineInRange("kibitka_wagon_turn_rate", 2.0D, 0.0D, Double.MAX_VALUE);
        builder.pop();
        builder.push("Big Cart Options");
        this.bigCartHealth = builder.comment("Maximum health").defineInRange("big_cart_health", 60, 0, Integer.MAX_VALUE);
        this.bigCartSpeed = builder.comment("Speed coefficient, from 0 to 1. Maximum speed is the slowest hitched animal times this value.").defineInRange("big_cart_speed_coefficient", 1.0D, 0.0D, 1.0D);
        this.bigCartAcceleration = builder.comment("Unused. Acceleration is the speed coefficient, in blocks per second per second.").defineInRange("big_cart_acceleration", 2.0D, 0.0D, Double.MAX_VALUE);
        this.bigCartTurnRate = builder.comment("Turn rate").defineInRange("big_cart_turn_rate", 2.0D, 0.0D, Double.MAX_VALUE);
        builder.pop();
    }

    static {
        Pair<ServerConfig, ModConfigSpec> pair = new ModConfigSpec.Builder().configure(ServerConfig::new);
        INSTANCE = pair.getLeft();
        SPEC = pair.getRight();
    }

}
