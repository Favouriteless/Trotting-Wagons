package net.favouriteless.trotting_wagons.client.screens;

import net.favouriteless.trotting_wagons.TrottingWagons;
import net.favouriteless.trotting_wagons.common.entities.BaseWagon;
import net.favouriteless.trotting_wagons.common.menus.BaseBuildMenu;
import net.favouriteless.trotting_wagons.common.network.packets.ClearBaseBuildPayload;
import net.favouriteless.trotting_wagons.common.network.packets.SelectBaseBuildPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

public class BaseBuildScreen extends net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<BaseBuildMenu> {

    private static final ResourceLocation FRAME = TrottingWagons.id("textures/gui/icons/big_slot.png");

    public BaseBuildScreen(BaseBuildMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = BaseBuildMenu.WIDTH;
        imageHeight = BaseBuildMenu.HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        for(int i = 0; i < BaseWagon.CHOICES.length; i++) {
            String id = BaseWagon.CHOICES[i];
            int x = leftPos + 8 + (i % 5) * 32;
            int y = topPos + 8 + (i / 5) * 36;
            addRenderableWidget(new ChoiceButton(x, y, id, menu.getWagon()));
        }
        int clearX = leftPos + (imageWidth - 26) / 2;
        int clearY = topPos + 78;
        addRenderableWidget(new ClearButton(clearX, clearY, menu.getWagon()));
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xC0101010);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {}

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    private static final class ChoiceButton extends AbstractWidget {

        private final String id;
        private final BaseWagon wagon;

        private ChoiceButton(int x, int y, String id, BaseWagon wagon) {
            super(x, y, 26, 26, Component.translatable("item.trotting_wagons." + id));
            this.id = id;
            this.wagon = wagon;
            setTooltip(Tooltip.create(getMessage()));
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            boolean locked = wagon != null && wagon.planStarted() && !id.equals(wagon.choice());
            active = !locked;
            setTooltip(Tooltip.create(locked ? Component.translatable("gui.trotting_wagons.base_build.locked") : getMessage()));
            graphics.blit(FRAME, getX(), getY(), 0, 0, 26, 26, 26, 26);
            if(locked)
                graphics.fill(getX(), getY(), getX() + 26, getY() + 26, 0x88000000);
            if(wagon != null && id.equals(wagon.choice())) {
                int x = getX();
                int y = getY();
                graphics.fill(x, y, x + 26, y + 1, 0xFFFFFF55);
                graphics.fill(x, y + 25, x + 26, y + 26, 0xFFFFFF55);
                graphics.fill(x, y, x + 1, y + 26, 0xFFFFFF55);
                graphics.fill(x + 25, y, x + 26, y + 26, 0xFFFFFF55);
            }
            graphics.pose().pushPose();
            graphics.pose().scale(0.45F, 0.45F, 1.0F);
            int textX = (int)((getX() + 2) / 0.45F);
            int textY = (int)((getY() + 9) / 0.45F);
            graphics.drawString(net.minecraft.client.Minecraft.getInstance().font, shortName(), textX, textY, 0xFFFFFF, false);
            graphics.pose().popPose();
        }

        private String shortName() {
            String text = getMessage().getString();
            return text.length() > 8 ? text.substring(0, 8) : text;
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            if(wagon == null || (wagon.planStarted() && !id.equals(wagon.choice())))
                return;
            PacketDistributor.sendToServer(new SelectBaseBuildPayload(wagon.getId(), id));
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }

    }

    private static final class ClearButton extends AbstractWidget {

        private static final ItemStack ICON = new ItemStack(Items.BARRIER);
        private final BaseWagon wagon;

        private ClearButton(int x, int y, BaseWagon wagon) {
            super(x, y, 26, 26, Component.translatable("gui.trotting_wagons.base_build.clear"));
            this.wagon = wagon;
            setTooltip(Tooltip.create(getMessage()));
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            graphics.blit(FRAME, getX(), getY(), 0, 0, 26, 26, 26, 26);
            graphics.renderItem(ICON, getX() + 5, getY() + 5);
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            if(wagon == null)
                return;
            PacketDistributor.sendToServer(new ClearBaseBuildPayload(wagon.getId()));
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }

    }

}
