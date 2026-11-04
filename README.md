# Trotting Wagons Extended

Horse-drawn wagons for Minecraft 1.21.1. Version **0.9.2** is a public test build: it is meant to be played, and it is not a finished 1.0.

This repository continues [Favouriteless/Trotting-Wagons](https://github.com/Favouriteless/Trotting-Wagons). The mod id is still `trotting_wagons`, so old worlds keep their wagons. The name in the game is Trotting Wagons Extended.

The current test jar is on the [0.9.2 release](https://github.com/JourneyNSettle/Trotting-Wagons-Extended/releases/tag/v0.9.2).

## Requirements

| | |
| --- | --- |
| Minecraft | 1.21.1 |
| NeoForge | 21.1.248 or newer in the 21.1 line |
| BoneHitboxLib | 0.3.0 for NeoForge 1.21.1. The debug wagon uses it for the body collision |
| GeckoLib | 4.8.3 for NeoForge 1.21.1 |
| Sides | Server and every client |

[Block Runner](https://www.curseforge.com/minecraft/mc-mods/block-runner) is optional. When it is installed, a wheel's speed follows the block under that wheel, and the Block Runner modifier is taken off the draft animal before the wagon speed is calculated.

## Install

Put `trotting-wagons-extended-0.9.2.jar`, GeckoLib, and BoneHitboxLib in the `mods` folder. The server and every player need the same copy.

## Wagon base

Craft a long stick from sticks and string, then a chassis from long sticks, sticks, string, and a slimeball. Place the chassis and right-click it with planks and iron ingots. One click adds one item. Sneak adds as many as the stage still needs, up to 16 planks and 4 iron ingots. When both are full, the chassis becomes a wagon base. The chassis does not stop the player. The finished base does, through a BoneHitboxLib hitbox. Break either with an axe to get the chassis item and the materials back.

New wagons are in the **Trotting Wagons Extended** creative tab. The Conestoga, the royal wagon, the armored wagon, the horse whip, and all three wheel types also have crafting recipes. The other wagons do not, yet.

## Driving

Sit in the driver seat. Right-click with the horse whip raises the gear by one, up to 4. Left-click with the whip in the main hand lowers it by one. Gear 0 is stopped. The wagon does not drop from 4 back to 0 on its own. A bar above the hotbar shows speed only to the driver.

Reverse is shift-right-click with the whip. Reverse uses the same gear at a quarter of the forward target.

The wagon moves only while a player is driving, every hitch point has an animal, and all four wheels are fitted. Steering still works with a partial team. A harnessed wagon that is not moving turns in place at half rate.

Top speed is the slowest living draft animal, times the wagon, times the wheels, times the harness. One wide wheel anywhere on the wagon multiplies speed by 0.8. Four wide wheels stay at 0.8. Reinforced wheels do not change speed. The numbers are in [docs/common.md](docs/common.md).

## Hitching

The hitch key is **R**. While you are riding an allowed animal, it sends that animal to a free hitch point within 1.5 blocks. The default allowed animal is `minecraft:horse`. Babies are refused. Change the list in `config/trotting_wagons-server.toml` under `allowed_entities`.

Every wagon has a harness slot.

| In the game | Item id | Animals | Speed factor |
| --- | --- | --- | --- |
| Shafts | `tongue` | 1, centered | Depends on the wagon, from 1.0 down to 0.5 |
| Tongue | `shafts` | 2, left and right | 1.0 on every wagon |

The English names follow the icons. Shafts are the single pole. The tongue is the pair.

Right-click a free wagon with the harness item to fit it. Shift-right-click the pole to take it off. It cannot be removed or swapped while an animal is hitched. An empty harness releases any animals that were still recorded on that wagon.

A hitched animal stands straight, does not graze, and is pulled back if it drifts more than half a block sideways. If it ends up more than 2 blocks from its hitch point, in any direction, it is released where it stands. An animal in an unloaded chunk stays hitched.

## Seats and the menu

Right-click a wagon to sit in the nearest free seat. Opening your inventory while seated opens the wagon instead. The menu closes when you leave the wagon.

From the menu you can move passengers and animals between seats, hitch and unhitch draft animals, change wheels, repair the wagon, and use that wagon's own cargo. Villagers and wandering traders count as passengers. They can use player and universal seats. They cannot drive.

The owner is the player who placed the wagon. Only the owner can break it, and only with an axe, while sneaking.

## Wagons

| Wagon | What it is for |
| --- | --- |
| Conestoga | A covered wagon with two cargo pages of 42 slots. The second page, a large animal, and the two universal seats cannot all be used at once. |
| Royal wagon | A carriage with 40 cargo slots and five seats: the driver and four passengers, two to a bench. |
| Armored wagon | A slow wagon with the driver and six passenger seats. |
| Passenger wagon | Seats for riders. Passengers step off by the rope ladder. The driver steps off beside the driver seat. |
| Ware wagon | A freight wagon. Chests placed on it show on the model and slow it down. |
| Log wagon | Holds logs, planks, and sticks. The pile on the model grows with the load, and so does the slowdown. |
| Barrel wagon | Twelve barrel slots. Filled barrels show on the model and slow the wagon. |
| Animal wagon | Seats for animals, including a large one. |
| Kibitka | A covered cart with its own camp layout. |
| Big cart | One open bed: 42 cargo slots, or one large animal, or two universal seats along the wagon. It has a lantern. The ladder is only decoration, and the wagon cannot be dyed. |
| Debug wagon | The workbench wagon used to try seats, wheels, a lantern, and the harness. |

Most of these can be dyed, and the dye is not used up. Dye does not change the big cart, or the wagons that ship with a single texture.

## Wheels

A new wagon starts with four regular wheels. Wheels wear down as the wagon rolls. At low durability the wagon sags. A missing wheel stops it.

| Wheel | Effect |
| --- | --- |
| Regular | Normal speed. |
| Wide | 0.8 speed, once for the whole wagon, if at least one is fitted. |
| Reinforced | Same speed as a regular wheel, and it lasts longer. |

Fit a wheel by right-clicking the wheel position. Shift-right-click removes it.

## Config

Server settings are in `config/trotting_wagons-server.toml`. Each wagon has health, a speed coefficient, and a turn rate. The `*_acceleration` keys are not used. Acceleration follows the current speed coefficient.

The log wagon, the barrel wagon, and the ware wagon pick a new coefficient when the load changes, and again when the world loads. Other wagons keep the value they were given when they were created.

## Building

JDK 21. From the repository root:

```
./gradlew build
```

On Windows, use `gradlew.bat`. The jar is written to `build/libs`.

The notes under [docs/](docs/) describe the current behavior in Russian, one file per wagon plus [docs/common.md](docs/common.md) for the shared rules.

## License

All rights reserved. See the authors listed in `gradle.properties`.
