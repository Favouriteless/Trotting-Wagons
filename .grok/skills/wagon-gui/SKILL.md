---
name: wagon-gui
description: >
  Lay out and wire Trotting Wagons wagon screens. Use when the user adds or
  edits a wagon GUI, interface, menu, screen, harness slot, draft slots,
  repair slots, wheel slots, or passenger seats, or drops a wagon UI PNG.
  Use when they say интерфейс, GUI, слот упряжки, дышло, оглобли, or /wagon-gui.
---

# Wagon GUI

The user paints the PNG by hand. Copy it into `assets/trotting_wagons/textures/gui/`. Do not resize it, do not redraw wells, and do not bake text into it. Measure wells before placing slots. A vanilla slot coordinate is the top-left of the 16×16 interior. A big control is 26×26: its button origin is one pixel above and left of a 24×24 interior.

## Size and order

The image is at most 320×250. Top to bottom, unless the user marks a wagon otherwise:

1. One text row, left to right: wagon name, owner, distance in kilometres. Draw these in code.
2. That wagon's own slots and buttons.
3. The durability bar, drawn in code.
4. On the right, the repair header («Починка») lines up with the top edge of the passenger seats. The open repair slots sit centered under that header: one slot in the middle, two in a row, three as a row plus one centered below, four as a square.
5. Under the repair slots, when the wagon has custom item slots: the two-line Custom Items header, then those slots.
6. Passenger seats, centered. At least the driver. Further seats are passengers, animals, large animals, or universal.
7. On the left, 0, 1, or 2 draft-animal slots. These are big slots.
8. On the left, under the draft slots, one line: «Нет упряжки», «Оглобли», or «Дышло», from the harness slot.
9. The harness slot. It accepts only the tongue item or the shafts item.
10. Two lines, «Колёса:» and the wheel-set name.
11. Wheel slots: 4, or 2 on a small wagon.

`textures/gui/icons/big_slot.png` is the 26×26 frame for a player, an animal, or a draft animal. Blit it only where the PNG did not already paint that frame. Small wells in the PNG are the 18×18 item slots. Repair slots that are not painted are drawn with `WagonGui.drawSlot`, and only while they are open.

## Harness

Two items, no recipe unless the user asks:

| Item id | Name | Bone shown | Draft slots |
| --- | --- | --- | --- |
| `tongue` | Оглобли / Shafts | `TongueSingle` | 1, centered |
| `shafts` | Дышло / Tongue | `Tongue` | 2, left and right |

An empty slot hides both bones and leaves 0 draft slots. Its ghost icon is the дышло item (`shafts`, English Tongue). The harness cannot be taken out or swapped while any animal is hitched. Right-click the wagon with the item to install it. Shift-right-click the part to take it back. The harness still stays while an animal is hitched. Both bones still take the same `tongueAngle`.

Wire this slot only on a wagon whose GUI already paints it. An empty slot on an old screen would clear that wagon's existing hitch.

## Painted exceptions

Log wagon, `log_wagon.png`, is 320×190 and that is the one-row size. Name, owner, and kilometres sit to the right of the driver, as on the ware wagon. Cargo is 15 columns from (25, 14) and is not painted: draw each visible well. Extra rows insert at y=54 and push the painted lower half down. The durability bar stays 4px under the last cargo row and 8px above the driver. Harness (26, 98), wheels (17, 146), repair header on the driver top, and the player inventory stay in that painted half.

Armored wagon, `armored_wagon.png`, is 320×174. The wells match the passenger wagon without lanterns. The title row is on top. Seven player seats start at (69, 38). There is no custom-item column.

Royal carriage, `royal_carriage_ui.png` → `royal_wagon.png`, is 320×250. Cargo is 10×4 from (71, 21), forty slots. Health is (13, 96). Five seats start at (95, 114), step 26: driver, the bench nearer the driver, then the far bench. Harness is (26, 158). Wheels are (17, 206). Player inventory is (80, 166). Repair header is the top of the seats. Inside the cabin each bench holds two players at x ±7/16, z +0.7 and −0.7.

Conestoga, `conestoga_ui.png` → `conestoga_wagon.png`, is 320×236. Cargo is 14×3 from (18, 22), forty-two slots, and a second page of forty-two behind the lower button at (277, 50). The upper button at (277, 20) is the default page and draws a chest icon. Health is (13, 82). Seats from (108, 100): driver, universal, large animal, universal. Harness is (26, 144). Wheels are (17, 192). Player inventory is (80, 152). Repair header is the top of the seats. The hold, the large animal, and the two universal seats exclude one another.

Do not add a wagon lock button. The user has not designed that control yet and will ask for it later.
