package net.favouriteless.trotting_wagons.client.screens;

import net.favouriteless.trotting_wagons.common.menus.ConestogaMenu;
import net.favouriteless.trotting_wagons.common.network.packets.SelectConestogaPagePayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;

/** One of the two chest pages beside the conestoga cargo grid. */
public class ConestogaPageButton extends Button {

    private static final ItemStack CHEST = new ItemStack(Items.CHEST);

    private final int page;
    private boolean selected;

    public ConestogaPageButton(int x, int y, int page, ConestogaMenu menu) {
        super(x, y, 26, 26, Component.empty(), button -> {
            menu.setPage(page);
            PacketDistributor.sendToServer(new SelectConestogaPagePayload(page));
        }, DEFAULT_NARRATION);
        this.page = page;
    }

    public int page() {
        return page;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if(page == 0)
            graphics.renderItem(CHEST, getX() + 5, getY() + 5);
        if(!active)
            graphics.fill(getX() + 2, getY() + 2, getX() + 24, getY() + 24, 0x88000000);
        if(!selected)
            return;
        int x = getX();
        int y = getY();
        graphics.fill(x, y, x + width, y + 1, 0xFFFFFFA0);
        graphics.fill(x, y + height - 1, x + width, y + height, 0xFFFFFFA0);
        graphics.fill(x, y, x + 1, y + height, 0xFFFFFFA0);
        graphics.fill(x + width - 1, y, x + width, y + height, 0xFFFFFFA0);
    }

}
