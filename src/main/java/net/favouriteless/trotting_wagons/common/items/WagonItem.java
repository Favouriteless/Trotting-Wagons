package net.favouriteless.trotting_wagons.common.items;

import net.favouriteless.trotting_wagons.client.render.WagonItemRenderer;
import net.favouriteless.trotting_wagons.common.entities.base.AbstractWagon;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class WagonItem extends Item {

    private final Supplier<EntityType<? extends AbstractWagon>> entityType;

    public WagonItem(Supplier<EntityType<? extends AbstractWagon>> entityType, Properties properties) {
        super(properties);
        this.entityType = entityType;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();

        if(context.getClickedFace() != Direction.UP)
            return InteractionResult.PASS;

        if(level.isClientSide)
            return InteractionResult.SUCCESS;

        ItemStack stack = context.getItemInHand();

        AbstractWagon wagon = entityType.get().create(level);
        wagon.setPos(Vec3.atBottomCenterOf(context.getClickedPos().above()).add(0, 0.05D, 0));
        wagon.setYRot(context.getPlayer().getYHeadRot());
        wagon.owner = context.getPlayer().getUUID();

        CompoundTag nbt = stack.getTag();
        if(nbt != null && nbt.contains("color"))
            wagon.setColor(DyeColor.byId(nbt.getInt("color")));

        level.addFreshEntity(wagon);

        context.getItemInHand().shrink(1);
        return InteractionResult.CONSUME;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {

            private WagonItemRenderer renderer = null;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if(renderer == null)
                    renderer = new WagonItemRenderer(entityType.get());
                return renderer;
            }

        });
    }

    public static ItemStack setupNbt(ItemStack stack, DyeColor color) {
        if(color != null) {
            CompoundTag nbt = stack.getOrCreateTag();
            nbt.putInt("color", color.getId());
            stack.setTag(nbt);
        }
        return stack;
    }

}
