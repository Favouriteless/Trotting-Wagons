package net.favouriteless.trotting_wagons.client.screens;

import net.favouriteless.trotting_wagons.TrottingWagons;
import net.favouriteless.trotting_wagons.common.menus.ConestogaMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class ConestogaScreen extends AbstractWagonScreen<ConestogaMenu> {

    public ConestogaScreen(ConestogaMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, TrottingWagons.id("textures/gui/conestoga_wagon.png"), 512, 512);
        this.imageWidth = 320;
        this.imageHeight = 222;
        this.inventoryLabelX = 80;
        this.inventoryLabelY = 127;
    }

}
