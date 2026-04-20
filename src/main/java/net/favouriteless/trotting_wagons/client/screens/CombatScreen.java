package net.favouriteless.trotting_wagons.client.screens;

import net.favouriteless.trotting_wagons.TrottingWagons;
import net.favouriteless.trotting_wagons.common.menus.CombatMenu;
import net.favouriteless.trotting_wagons.common.menus.RoyalMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class CombatScreen extends AbstractWagonScreen<CombatMenu> {

    public CombatScreen(CombatMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, TrottingWagons.id("textures/gui/combat_wagon.png"), 256, 256);
        this.imageWidth = 176;
        this.imageHeight = 132;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 37;
    }

}
