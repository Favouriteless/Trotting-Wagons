package net.favouriteless.trotting_wagons.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.favouriteless.trotting_wagons.TrottingWagons;
import net.favouriteless.trotting_wagons.common.entities.Chassis;
import net.favouriteless.trotting_wagons.common.entities.WagonBuild;
import net.favouriteless.trotting_wagons.common.entities.base.AbstractWagon;
import net.favouriteless.trotting_wagons.common.items.HorseWhipItem;
import net.favouriteless.trotting_wagons.common.network.packets.HitchAnimalPayload;
import net.favouriteless.trotting_wagons.common.network.packets.OpenWagonMenuPayload;
import net.favouriteless.trotting_wagons.common.network.packets.WhipLowerGearPayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = TrottingWagons.MOD_ID, value = Dist.CLIENT)
public class TWClientEvents {

    public static final KeyMapping HITCH_KEY = new KeyMapping(
            "key.trotting_wagons.hitch",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            "key.categories.trotting_wagons"
    );

    @SubscribeEvent
    public static void onRenderHarnessed(RenderLivingEvent.Pre<?, ?> event) {
        if(!showHarnessed(event.getEntity()) || !(event.getEntity() instanceof Camel camel))
            return;
        camel.idleAnimationState.stop();
        camel.sitAnimationState.stop();
        camel.sitPoseAnimationState.stop();
        camel.sitUpAnimationState.stop();
        camel.dashAnimationState.stop();
    }

    private static boolean showHarnessed(LivingEntity entity) {
        return entity instanceof Mob mob && (mob instanceof AbstractHorse || mob instanceof Camel)
                && mob.isNoAi() && AbstractWagon.isHarnessed(mob);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if(minecraft.player == null || minecraft.screen != null)
            return;

        while(HITCH_KEY.consumeClick()) {
            if(minecraft.player.getVehicle() instanceof Mob)
                PacketDistributor.sendToServer(new HitchAnimalPayload());
        }
    }

    @SubscribeEvent
    public static void onAttackKey(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft minecraft = Minecraft.getInstance();
        if(!event.isAttack() || minecraft.player == null)
            return;
        if(!(minecraft.player.getMainHandItem().getItem() instanceof HorseWhipItem))
            return;
        if(!(minecraft.player.getVehicle() instanceof AbstractWagon wagon) || wagon.getControllingPassenger() != minecraft.player)
            return;
        event.setCanceled(true);
        event.setSwingHand(true);
        if(minecraft.player.getAttackStrengthScale(0.0F) < 0.9F)
            return;
        minecraft.player.resetAttackStrengthTicker();
        PacketDistributor.sendToServer(new WhipLowerGearPayload());
    }

    @SubscribeEvent
    public static void onRenderHud(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if(minecraft.player != null && minecraft.screen == null && minecraft.crosshairPickEntity instanceof Chassis chassis)
            drawBuildHint(event.getGuiGraphics(), minecraft, chassis);
        if(minecraft.player == null || !(minecraft.player.getVehicle() instanceof AbstractWagon wagon))
            return;
        if(wagon.getControllingPassenger() != minecraft.player)
            return;
        GuiGraphics graphics = event.getGuiGraphics();
        int width = 182;
        int filled = (int)(wagon.speedFraction() * 183.0F);
        int x = (graphics.guiWidth() - width) / 2;
        int y = graphics.guiHeight() - 32 + 3;
        RenderSystem.enableBlend();
        graphics.blitSprite(ResourceLocation.withDefaultNamespace("hud/jump_bar_background"), x, y, width, 5);
        if(filled > 0)
            graphics.blitSprite(ResourceLocation.withDefaultNamespace("hud/jump_bar_progress"), width, 5, 0, 0, x, y, filled, 5);
        RenderSystem.disableBlend();
    }

    private static void drawBuildHint(GuiGraphics graphics, Minecraft minecraft, Chassis chassis) {
        Component planks = Component.translatable("gui.trotting_wagons.build.planks", chassis.plankCount(), WagonBuild.PLANKS);
        Component iron = Component.translatable("gui.trotting_wagons.build.iron", chassis.ironCount(), WagonBuild.IRON);
        int x = graphics.guiWidth() / 2;
        int y = graphics.guiHeight() / 2 + 16;
        graphics.drawString(minecraft.font, planks, x - minecraft.font.width(planks) / 2, y, 0xFFFFFF, true);
        graphics.drawString(minecraft.font, iron, x - minecraft.font.width(iron) / 2, y + 10, 0xFFFFFF, true);
    }

    @SubscribeEvent
    public static void onScreenOpen(ScreenEvent.Opening event) {
        Minecraft minecraft = Minecraft.getInstance();
        if(!(event.getScreen() instanceof InventoryScreen) || minecraft.player == null)
            return;
        if(minecraft.player.getVehicle() instanceof AbstractWagon wagon && wagon.hasFixedSeats()) {
            event.setCanceled(true);
            PacketDistributor.sendToServer(new OpenWagonMenuPayload());
        }
    }

}
