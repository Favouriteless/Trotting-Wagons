package net.favouriteless.trotting_wagons.client.screens;

import net.favouriteless.trotting_wagons.common.entities.base.AbstractWagon;
import net.favouriteless.trotting_wagons.common.entities.base.WagonSeat;
import net.favouriteless.trotting_wagons.common.network.packets.EjectSeatPayload;
import net.favouriteless.trotting_wagons.common.network.packets.MoveAnimalPayload;
import net.favouriteless.trotting_wagons.common.network.packets.SelectSeatPayload;
import net.favouriteless.trotting_wagons.common.network.packets.ToggleSeatLockPayload;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

public class SeatButton extends AbstractWidget {

    public static final int SIZE = 24;
    private static final long TOOLTIP_DELAY_MS = TooltipDelay.MILLIS;
    private static final ItemStack BARRIER = new ItemStack(Items.BARRIER);

    private static final List<SeatButton> OPEN = new ArrayList<>();
    private static SeatButton dragSource;
    private static boolean dragLeftSource;

    private final AbstractWagon wagon;
    private final int seat;
    private final AbstractWagon.Side draftSide;
    private final boolean drawFrame;
    private long hoverStart = -1L;
    private List<Component> tooltip = List.of();

    public static void bind(List<SeatButton> buttons) {
        OPEN.clear();
        OPEN.addAll(buttons);
        dragSource = null;
        dragLeftSource = false;
    }

    public static boolean press(double mouseX, double mouseY, int button) {
        if(button != GLFW.GLFW_MOUSE_BUTTON_LEFT && button != GLFW.GLFW_MOUSE_BUTTON_RIGHT)
            return false;
        SeatButton hit = at(mouseX, mouseY);
        if(hit == null)
            return false;
        if(hit.draftSide != null) {
            if(hit.occupant().kind() == Kind.ANIMAL) {
                dragSource = hit;
                dragLeftSource = false;
            }
            return true;
        }
        boolean lockable = hit.wagon.getSeat(hit.seat).canLockAnimals();
        if(Screen.hasShiftDown() && lockable) {
            PacketDistributor.sendToServer(new ToggleSeatLockPayload(hit.wagon.getId(), hit.seat));
            return true;
        }
        if(hit.occupant().kind() == Kind.ANIMAL) {
            dragSource = hit;
            dragLeftSource = false;
            return true;
        }
        return false;
    }

    public static void markDrag(double mouseX, double mouseY) {
        if(dragSource != null && !dragSource.contains(mouseX, mouseY))
            dragLeftSource = true;
    }

    public static boolean finishDrag(double mouseX, double mouseY) {
        SeatButton source = dragSource;
        if(source == null)
            return false;
        SeatButton target = at(mouseX, mouseY);
        boolean move = target != null && target != source && target.accepts(source);
        boolean eject = !move && !dragLeftSource && source.contains(mouseX, mouseY);
        int animalId = source.animalId();
        dragSource = null;
        dragLeftSource = false;
        if(move && animalId >= 0) {
            boolean toDraft = target.draftSide != null;
            int index = toDraft ? target.draftSide.ordinal() : target.seat;
            PacketDistributor.sendToServer(new MoveAnimalPayload(source.wagon.getId(), animalId, index, toDraft));
        } else if(eject) {
            boolean fromDraft = source.draftSide != null;
            int index = fromDraft ? source.draftSide.ordinal() : source.seat;
            PacketDistributor.sendToServer(new EjectSeatPayload(source.wagon.getId(), index, fromDraft));
        }
        return true;
    }

    public static void renderCarried(GuiGraphics graphics, int mouseX, int mouseY) {
        if(dragSource == null)
            return;
        int id = dragSource.animalId();
        if(!(dragSource.wagon.level().getEntity(id) instanceof LivingEntity living) || living instanceof Player)
            return;
        int size = 32;
        int x1 = mouseX - size / 2;
        int y1 = mouseY - size / 2;
        InventoryScreen.renderEntityInInventoryFollowsMouse(
                graphics, x1, y1, x1 + size, y1 + size, previewScale(living, 14), 0.15F, mouseX, mouseY, living
        );
    }

    private static SeatButton at(double mouseX, double mouseY) {
        for(SeatButton button : OPEN) {
            if(button.contains(mouseX, mouseY))
                return button;
        }
        return null;
    }

    private boolean contains(double mouseX, double mouseY) {
        return mouseX >= getX() && mouseY >= getY() && mouseX < getX() + width && mouseY < getY() + height;
    }

    public SeatButton(AbstractWagon wagon, int seat, int x, int y) {
        this(wagon, seat, null, x, y, SIZE, true, Component.literal(Integer.toString(seat + 1)));
    }

    public static SeatButton draft(AbstractWagon wagon, AbstractWagon.Side side, int x, int y) {
        return draft(wagon, side, x, y, SIZE, true);
    }

    public static SeatButton placed(AbstractWagon wagon, int seat, int x, int y, int size) {
        return new SeatButton(wagon, seat, null, x, y, size, false, Component.literal(Integer.toString(seat + 1)));
    }

    public static SeatButton placedDraft(AbstractWagon wagon, AbstractWagon.Side side, int x, int y, int size) {
        return draft(wagon, side, x, y, size, false);
    }

    private static SeatButton draft(AbstractWagon wagon, AbstractWagon.Side side, int x, int y, int size, boolean drawFrame) {
        String key = wagon.hitchSlots() == 1 ? "gui.trotting_wagons.draft.middle"
                : side == AbstractWagon.Side.LEFT ? "gui.trotting_wagons.draft.left" : "gui.trotting_wagons.draft.right";
        return new SeatButton(wagon, -1, side, x, y, size, drawFrame, Component.translatable(key));
    }

    private SeatButton(AbstractWagon wagon, int seat, AbstractWagon.Side draftSide, int x, int y, int size, boolean drawFrame, Component message) {
        super(x, y, size, size, message);
        this.wagon = wagon;
        this.seat = seat;
        this.draftSide = draftSide;
        this.drawFrame = drawFrame;
    }

    private int animalId() {
        return draftSide != null ? wagon.getHitchEntityId(draftSide) : wagon.getSeatEntityId(seat);
    }

    public void refresh() {
        Occupant occupant = occupant();
        active = switch(occupant.kind()) {
            case EMPTY_PLAYER, ANIMAL, LOCKABLE -> true;
            case PLAYER, SELF, UNKNOWN -> false;
        };
        tooltip = occupant.tooltip();
    }

    /** The default font draws a newline as a missing-glyph box. Each entry is its own line. */
    public void renderDelayedTooltip(GuiGraphics graphics, Font font, int mouseX, int mouseY) {
        if(!isHovered() || hoverStart < 0 || Util.getMillis() - hoverStart < TOOLTIP_DELAY_MS || tooltip.isEmpty())
            return;
        graphics.renderComponentTooltip(font, tooltip, mouseX, mouseY);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if(isHovered()) {
            if(hoverStart < 0)
                hoverStart = Util.getMillis();
        } else {
            hoverStart = -1L;
        }

        Occupant occupant = occupant();
        boolean locked = draftSide == null && wagon.isSeatLocked(seat)
                && !(occupant.entity() instanceof Player && wagon.getSeat(seat).kind() == WagonSeat.Kind.UNIVERSAL);
        boolean blocked = draftSide == null && wagon.seatBlockHint(seat) != null;
        int color = active ? 0xFF8B8B8B : 0xFF5A5A5A;
        int x = getX();
        int y = getY();
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, color);
        if(drawFrame) {
            graphics.fill(x, y, x + width - 1, y + 1, 0xFF373737);
            graphics.fill(x, y, x + 1, y + height - 1, 0xFF373737);
            graphics.fill(x, y + height - 1, x + width, y + height, 0xFFFFFFFF);
            graphics.fill(x + width - 1, y, x + width, y + height, 0xFFFFFFFF);
        }
        if(dragSource != null && dragSource != this && accepts(dragSource))
            graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0x8800C853);

        boolean carried = dragSource == this;
        if(occupant.entity() instanceof Player player)
            drawPlayer(graphics, player);
        else if(!carried && occupant.entity() instanceof LivingEntity living)
            drawAnimal(graphics, living);
        else if(occupant.entity() == null && occupant.kind() == Kind.UNKNOWN)
            drawFitted(graphics, Component.literal("?"), 18);
        else if(carried || occupant.kind() == Kind.EMPTY_PLAYER || occupant.kind() == Kind.LOCKABLE)
            drawFitted(graphics, draftSide != null ? Component.translatable("gui.trotting_wagons.seat.draft") : typeLabel(wagon.getSeat(seat)), height - 6);

        if(locked || blocked)
            graphics.renderItem(BARRIER, getX() + (width - 16) / 2, getY() + (height - 16) / 2);
        graphics.drawString(Minecraft.getInstance().font, getMessage(), getX() + 2, getY() + 2, 0xFFFFFF, true);
    }

    private List<Component> emptyAnimalTooltip() {
        Component hint = wagon.seatBlockHint(seat);
        if(hint == null)
            return List.of(lockTooltip());
        if(wagon.isSeatLocked(seat))
            return List.of(hint, lockTooltip());
        return List.of(hint);
    }

    @Override
    protected boolean isValidClickButton(int button) {
        return button == GLFW.GLFW_MOUSE_BUTTON_LEFT || button == GLFW.GLFW_MOUSE_BUTTON_RIGHT;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if(!active || !visible || !isMouseOver(mouseX, mouseY) || !isValidClickButton(button))
            return false;

        boolean lockable = draftSide == null && wagon.getSeat(seat).canLockAnimals();
        if(Screen.hasShiftDown() && lockable) {
            PacketDistributor.sendToServer(new ToggleSeatLockPayload(wagon.getId(), seat));
            return true;
        }
        if(draftSide == null && wagon.seatBlockHint(seat) != null)
            return true;

        Kind kind = occupant().kind();
        if(kind == Kind.ANIMAL)
            return false;
        else if(kind == Kind.EMPTY_PLAYER)
            PacketDistributor.sendToServer(new SelectSeatPayload(wagon.getId(), seat));
        else
            return false;
        return true;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }

    private Occupant occupant() {
        if(draftSide != null)
            return draftOccupant();
        int id = wagon.getSeatEntityId(seat);
        WagonSeat definition = wagon.getSeat(seat);
        if(id < 0) {
            if(definition.kind() == WagonSeat.Kind.UNIVERSAL)
                return new Occupant(Kind.EMPTY_PLAYER, null, List.of(Component.translatable("gui.trotting_wagons.tooltip.move"), lockTooltip()));
            if(definition.kind() == WagonSeat.Kind.ANIMAL || definition.kind() == WagonSeat.Kind.ANIMALBIG)
                return new Occupant(Kind.LOCKABLE, null, emptyAnimalTooltip());
            return new Occupant(Kind.EMPTY_PLAYER, null, List.of(Component.translatable("gui.trotting_wagons.tooltip.move")));
        }

        if(!(wagon.level().getEntity(id) instanceof LivingEntity living))
            return new Occupant(Kind.UNKNOWN, null, List.of(Component.translatable("gui.trotting_wagons.tooltip.unknown")));

        if(living instanceof Player player) {
            if(player == Minecraft.getInstance().player)
                return new Occupant(Kind.SELF, player, List.of(Component.translatable("gui.trotting_wagons.tooltip.here")));
            return new Occupant(Kind.PLAYER, player, List.of(Component.translatable("gui.trotting_wagons.tooltip.occupied", player.getName())));
        }

        List<Component> tip = definition.canLockAnimals()
                ? List.of(Component.translatable("gui.trotting_wagons.tooltip.eject"), lockTooltip())
                : List.of(Component.translatable("gui.trotting_wagons.tooltip.eject"));
        return new Occupant(Kind.ANIMAL, living, tip);
    }

    private Occupant draftOccupant() {
        int id = wagon.getHitchEntityId(draftSide);
        if(id < 0)
            return new Occupant(Kind.LOCKABLE, null, List.of(Component.translatable("gui.trotting_wagons.seat.draft")));
        if(!(wagon.level().getEntity(id) instanceof LivingEntity living) || living instanceof Player)
            return new Occupant(Kind.UNKNOWN, null, List.of(Component.translatable("gui.trotting_wagons.tooltip.unknown")));
        return new Occupant(Kind.ANIMAL, living, List.of(Component.translatable("gui.trotting_wagons.tooltip.unhitch")));
    }

    private boolean accepts(SeatButton source) {
        if(source == null || source.wagon != wagon)
            return false;
        if(!(wagon.level().getEntity(source.animalId()) instanceof LivingEntity animal) || animal instanceof Player)
            return false;
        if(draftSide != null)
            return wagon.canPlaceDraft(animal, draftSide);
        return wagon.canPlaceAnimal(seat, animal);
    }

    private Component lockTooltip() {
        String key = wagon.isSeatLocked(seat) ? "gui.trotting_wagons.tooltip.unlock" : "gui.trotting_wagons.tooltip.lock";
        return Component.translatable(key);
    }

    private static Component typeLabel(WagonSeat seat) {
        return switch(seat.kind()) {
            case DRIVER -> Component.translatable("gui.trotting_wagons.seat.driver");
            case PLAYER -> Component.translatable("gui.trotting_wagons.seat.player");
            case UNIVERSAL -> Component.translatable("gui.trotting_wagons.seat.universal");
            case ANIMAL -> Component.translatable("gui.trotting_wagons.seat.animal_small");
            case ANIMALBIG -> Component.translatable("gui.trotting_wagons.seat.animal_large");
            case DRAFT -> Component.translatable("gui.trotting_wagons.seat.draft");
        };
    }

    private void drawPlayer(GuiGraphics graphics, Player player) {
        if(player instanceof AbstractClientPlayer clientPlayer) {
            PlayerFaceRenderer.draw(graphics, clientPlayer.getSkin().texture(), getX() + 6, getY() + 8, 14);
            return;
        }
        drawFitted(graphics, player.getName(), 12);
    }

    private void drawAnimal(GuiGraphics graphics, LivingEntity living) {
        int x1 = getX() + 1;
        int y1 = getY() + 1;
        int x2 = getX() + width - 1;
        int y2 = getY() + height - 1;
        graphics.enableScissor(x1, y1, x2, y2);
        if(draftSide != null) {
            // Body yaw is 180° + angle * 20. −1.5 turns the hitch animal 30° the other way.
            InventoryScreen.renderEntityInInventoryFollowsAngle(graphics, x1, y1, x2, y2, previewScale(living, 8), 0.12F, -1.5F, 0.0F, living);
        } else {
            InventoryScreen.renderEntityInInventoryFollowsMouse(
                    graphics, x1, y1, x2, y2, previewScale(living, 10), 0.35F, getX() + width / 2F, getY() + height / 2F, living);
        }
        graphics.disableScissor();
    }

    /** A camel is 2.375 blocks tall against a horse's 1.6, so the same slot scale fills the button. */
    private static int previewScale(LivingEntity living, int base) {
        if(living instanceof Camel)
            return Math.max(1, Math.round(base * 1.6F / 2.375F));
        return base;
    }

    private void drawFitted(GuiGraphics graphics, Component text, int y) {
        var pose = graphics.pose();
        pose.pushPose();
        pose.translate(getX() + width / 2F, getY() + y, 0);
        pose.scale(0.5F, 0.5F, 1F);
        graphics.drawCenteredString(Minecraft.getInstance().font, text, 0, 0, 0xFFFFFF);
        pose.popPose();
    }

    private enum Kind {
        EMPTY_PLAYER,
        LOCKABLE,
        ANIMAL,
        PLAYER,
        SELF,
        UNKNOWN
    }

    private record Occupant(Kind kind, LivingEntity entity, List<Component> tooltip) {}

}
