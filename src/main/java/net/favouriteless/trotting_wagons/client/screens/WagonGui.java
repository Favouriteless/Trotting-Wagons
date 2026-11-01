package net.favouriteless.trotting_wagons.client.screens;

import net.favouriteless.trotting_wagons.common.entities.base.AbstractWagon;
import net.favouriteless.trotting_wagons.common.init.TWItems;
import net.favouriteless.trotting_wagons.common.menus.AbstractWagonMenu;
import net.favouriteless.trotting_wagons.common.menus.AnimalMenu;
import net.favouriteless.trotting_wagons.common.menus.ArmoredMenu;
import net.favouriteless.trotting_wagons.common.menus.BannerSlot;
import net.favouriteless.trotting_wagons.common.menus.BarrelMenu;
import net.favouriteless.trotting_wagons.common.menus.BigCartMenu;
import net.favouriteless.trotting_wagons.common.menus.BarrelSlot;
import net.favouriteless.trotting_wagons.common.menus.ChestSlot;
import net.favouriteless.trotting_wagons.common.menus.ConestogaMenu;
import net.favouriteless.trotting_wagons.common.menus.DebugMenu;
import net.favouriteless.trotting_wagons.common.menus.HarnessSlot;
import net.favouriteless.trotting_wagons.common.menus.KibitkaMenu;
import net.favouriteless.trotting_wagons.common.menus.LogCargoSlot;
import net.favouriteless.trotting_wagons.common.menus.LogMenu;
import net.favouriteless.trotting_wagons.common.menus.PassengerMenu;
import net.favouriteless.trotting_wagons.common.menus.WareMenu;
import net.favouriteless.trotting_wagons.common.menus.RepairSlot;
import net.favouriteless.trotting_wagons.common.menus.RoyalMenu;
import net.favouriteless.trotting_wagons.common.menus.LanternSlot;
import net.favouriteless.trotting_wagons.common.menus.WheelSlot;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.util.Locale;

final class WagonGui {

    private WagonGui() {}

    static void drawFrame(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, 0xFFC6C6C6);
        graphics.fill(x, y, x + 1, y + height, 0xFF000000);
        graphics.fill(x + 1, y, x + 3, y + height, 0xFFFFFFFF);
        graphics.fill(x + width - 3, y, x + width - 1, y + height, 0xFF555555);
        graphics.fill(x + width - 1, y, x + width, y + height, 0xFF000000);
        graphics.fill(x, y, x + width, y + 1, 0xFF000000);
        graphics.fill(x + 3, y + 1, x + width - 3, y + 3, 0xFFFFFFFF);
        graphics.fill(x, y + height - 1, x + width, y + height, 0xFF000000);
        graphics.fill(x + 3, y + height - 3, x + width - 3, y + height - 1, 0xFF555555);
    }

    static void drawSlot(GuiGraphics graphics, int left, int top, Slot slot) {
        int x = left + slot.x - 1;
        int y = top + slot.y - 1;
        graphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFF8B8B8B);
        graphics.fill(x, y, x + 17, y + 1, 0xFF373737);
        graphics.fill(x, y, x + 1, y + 17, 0xFF373737);
        graphics.fill(x, y + 17, x + 18, y + 18, 0xFFFFFFFF);
        graphics.fill(x + 17, y, x + 18, y + 18, 0xFFFFFFFF);
    }

    static void drawWheelCaption(GuiGraphics graphics, Font font, AbstractContainerMenu menu, AbstractWagon wagon, int left, int top) {
        drawWheelCaption(graphics, font, menu, wagon, left, top, 0);
    }

    static void drawWheelCaption(GuiGraphics graphics, Font font, AbstractContainerMenu menu, AbstractWagon wagon, int left, int top, int extraY) {
        int minX = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        for(Slot slot : menu.slots) {
            if(!(slot instanceof WheelSlot))
                continue;
            minX = Math.min(minX, slot.x);
            maxY = Math.max(maxY, slot.y);
        }
        if(minX == Integer.MAX_VALUE || wagon == null)
            return;
        int center = left + minX + 18;
        int y = top + maxY + 19 + extraY;
        Component title = Component.translatable("gui.trotting_wagons.wheels");
        Component state = wagon.wheelSet().label();
        graphics.drawString(font, title, center - font.width(title) / 2, y, 4210752, false);
        graphics.drawString(font, state, center - font.width(state) / 2, y + 10, 4210752, false);
    }

    /** Title row: owner centered, distance on the right. The owner shifts left only to clear the distance. */
    static void drawHeader(GuiGraphics graphics, Font font, AbstractWagon wagon, int y, int imageWidth) {
        drawHeader(graphics, font, wagon, y, imageWidth, 0);
    }

    /** {@code ownerLeft} is the first pixel the owner may use. Zero keeps the owner centered on the window. */
    static void drawHeader(GuiGraphics graphics, Font font, AbstractWagon wagon, int y, int imageWidth, int ownerLeft) {
        if(wagon == null)
            return;
        Component distance = Component.translatable("gui.trotting_wagons.distance", String.format(Locale.ROOT, "%.1f", wagon.getDistance() / 1000.0F));
        int distanceX = imageWidth - 8 - font.width(distance);
        graphics.drawString(font, distance, distanceX, y, 4210752, false);
        String name = wagon.getOwnerName();
        if(name.isEmpty())
            return;
        Component owner = Component.translatable("gui.trotting_wagons.owner", name);
        int width = font.width(owner);
        int limit = distanceX - 6;
        int ownerX = (imageWidth - width) / 2;
        if(ownerLeft > 0)
            ownerX = ownerLeft + Math.max(0, limit - ownerLeft - width) / 2;
        if(ownerX + width > limit)
            ownerX = limit - width;
        if(ownerLeft > 0 && ownerX < ownerLeft)
            ownerX = ownerLeft;
        graphics.drawString(font, owner, ownerX, y, 4210752, false);
    }

    static void drawEmptySlotIcons(GuiGraphics graphics, int left, int top, AbstractContainerMenu menu) {
        for(Slot slot : menu.slots) {
            if(!slot.isActive() || slot.hasItem() || slot.x < 0 || slot.y < 0)
                continue;
            ItemStack icon = emptyIcon(menu, slot);
            if(icon.isEmpty())
                continue;
            GraySlotIcons.draw(graphics, icon, left + slot.x, top + slot.y);
        }
    }

    static ItemStack ghostIcon(AbstractContainerMenu menu, Slot slot) {
        return emptyIcon(menu, slot);
    }

    private static ItemStack emptyIcon(AbstractContainerMenu menu, Slot slot) {
        if(slot instanceof HarnessSlot)
            return new ItemStack(TWItems.SHAFTS.get());
        if(slot instanceof WheelSlot)
            return new ItemStack(TWItems.WHEEL.get());
        if(slot instanceof BannerSlot)
            return new ItemStack(Blocks.WHITE_BANNER);
        if(slot instanceof LanternSlot)
            return new ItemStack(Items.LANTERN);
        if(slot instanceof LogCargoSlot)
            return new ItemStack(Items.OAK_LOG);
        if(slot instanceof BarrelSlot)
            return new ItemStack(Items.BARREL);
        if(slot instanceof ChestSlot)
            return new ItemStack(Items.CHEST);
        if(slot instanceof RepairSlot repair) {
            AbstractWagon wagon = repairWagon(menu);
            if(wagon != null)
                return wagon.repairIcon(repair.getSlotIndex());
        }
        if(menu instanceof DebugMenu && slot instanceof SlotItemHandler && slot.getSlotIndex() < 4)
            return new ItemStack(Items.PUMPKIN);
        return ItemStack.EMPTY;
    }

    private static AbstractWagon repairWagon(AbstractContainerMenu menu) {
        if(menu instanceof DebugMenu debug)
            return debug.getWagon();
        if(menu instanceof AnimalMenu animal)
            return animal.getWagon();
        if(menu instanceof LogMenu log)
            return log.getWagon();
        if(menu instanceof BarrelMenu barrel)
            return barrel.getWagon();
        if(menu instanceof PassengerMenu passenger)
            return passenger.getWagon();
        if(menu instanceof WareMenu ware)
            return ware.getWagon();
        if(menu instanceof KibitkaMenu kibitka)
            return kibitka.getWagon();
        if(menu instanceof AbstractWagonMenu inventoryWagon)
            return inventoryWagon.getWagon();
        if(menu instanceof ArmoredMenu armored)
            return armored.getWagon();
        if(menu instanceof RoyalMenu royal)
            return royal.getWagon();
        if(menu instanceof ConestogaMenu conestoga)
            return conestoga.getWagon();
        if(menu instanceof BigCartMenu bigCart)
            return bigCart.getWagon();
        return null;
    }

    static void drawRepairCaption(GuiGraphics graphics, Font font, AbstractContainerMenu menu, AbstractWagon wagon) {
        if(wagon == null)
            return;
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE;
        for(Slot slot : menu.slots) {
            if(!(slot instanceof RepairSlot) || slot.y < 0)
                continue;
            minX = Math.min(minX, slot.x);
            maxX = Math.max(maxX, slot.x);
            minY = Math.min(minY, slot.y);
        }
        if(minX == Integer.MAX_VALUE)
            return;
        int center = (minX + maxX) / 2 + 8;
        Component durability = Component.translatable("gui.trotting_wagons.durability");
        graphics.drawString(font, durability, center - font.width(durability) / 2, minY - 22, 4210752, false);
        Component condition = wagon.conditionName();
        graphics.drawString(font, condition, center - font.width(condition) / 2, minY - 12, 4210752, false);
    }

}
