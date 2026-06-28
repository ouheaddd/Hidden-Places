package com.overyourhead.hidden_places.common.item;

import com.overyourhead.hidden_places.client.renderer.ParchedSkeletonRemainsItemRenderer;
import com.overyourhead.hidden_places.common.entity.ParchedSkeletonRemainsEntity;
import com.overyourhead.hidden_places.core.registry.HPEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ParchedSkeletonRemainsItem extends Item implements GeoItem {
    private static final String TAG_SELECTED_VARIANT = "SelectedVariant";
    private static final int AUTO_VARIANT = 0;
    private static final int[] CYCLE = new int[] {AUTO_VARIANT, 1, 2, 3, 4, 5};

    private final AnimatableInstanceCache animatableInstanceCache = GeckoLibUtil.createInstanceCache(this);

    public ParchedSkeletonRemainsItem(Properties properties) {
        super(properties.stacksTo(16));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();

        if (player != null && player.isShiftKeyDown()) {
            return cycleVariant(context.getLevel(), player, stack);
        }

        Direction face = context.getClickedFace();
        if (face != Direction.UP && face != Direction.DOWN) {
            if (player != null && !context.getLevel().isClientSide) {
                player.displayClientMessage(Component.translatable("message.hidden_places.parched_skeleton_remains.floor_or_ceiling"), true);
            }
            return InteractionResult.FAIL;
        }

        Level level = context.getLevel();
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        BlockPos clickedPos = context.getClickedPos();
        boolean ceiling = face == Direction.DOWN;
        if (!ceiling && getSelectedVariant(stack) == ParchedSkeletonRemainsEntity.CEILING_VARIANT) {
            if (player != null) {
                player.displayClientMessage(Component.translatable("message.hidden_places.parched_skeleton_remains.ceiling_only"), true);
            }
            return InteractionResult.FAIL;
        }

        BlockPos supportPos = ceiling ? clickedPos : clickedPos;
        Direction supportFace = ceiling ? Direction.DOWN : Direction.UP;
        BlockState supportState = level.getBlockState(supportPos);

        if (!supportState.isFaceSturdy(level, supportPos, supportFace)) {
            if (player != null) {
                player.displayClientMessage(Component.translatable("message.hidden_places.parched_skeleton_remains.bad_support"), true);
            }
            return InteractionResult.FAIL;
        }

        ParchedSkeletonRemainsEntity remains = HPEntities.PARCHED_SKELETON_REMAINS.get().create(level);
        if (remains == null) {
            return InteractionResult.FAIL;
        }

        Vec3 click = context.getClickLocation();
        double x = clickedPos.getX() + 0.5D;
        double z = clickedPos.getZ() + 0.5D;
        // Floor variants sit on top of the clicked block.
        // Ceiling variant #3 is spawned lower so the model hangs under the block
        // instead of being pushed up into/above the support block.
        double y = ceiling ? clickedPos.getY() - 2.0D : clickedPos.getY() + 1.0D;

        remains.setPos(x, y, z);
        float placedYaw = player != null ? getSkeletonPlacedYaw(player) : 0.0F;
        remains.setPlacedYaw(placedYaw);
        remains.setCeilingMounted(ceiling);
        remains.setVariant(ceiling ? ParchedSkeletonRemainsEntity.CEILING_VARIANT : resolveFloorVariant(level, stack));
        level.addFreshEntity(remains);

        level.playSound(null, click.x, click.y, click.z, SoundEvents.BONE_BLOCK_PLACE, SoundSource.BLOCKS, 0.75F, 0.9F + level.random.nextFloat() * 0.2F);

        if (player != null && !player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        return InteractionResult.CONSUME;
    }

    private static float getSkeletonPlacedYaw(Player player) {
        float yaw = Mth.wrapDegrees(player.getYRot());
        Direction direction = Direction.fromYRot(yaw);

        // The skeleton model has a 180-degree offset only on the north/south axis.
        // East/west already faces correctly, so only flip Z-axis placements.
        if (direction.getAxis() == Direction.Axis.Z) {
            yaw = Mth.wrapDegrees(yaw + 180.0F);
        }

        return yaw;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            InteractionResult result = cycleVariant(level, player, stack);
            return new InteractionResultHolder<>(result, stack);
        }
        return InteractionResultHolder.pass(stack);
    }

    private static InteractionResult cycleVariant(Level level, Player player, ItemStack stack) {
        if (!level.isClientSide) {
            int current = getSelectedVariant(stack);
            int next = CYCLE[0];
            for (int i = 0; i < CYCLE.length; i++) {
                if (CYCLE[i] == current) {
                    next = CYCLE[(i + 1) % CYCLE.length];
                    break;
                }
            }

            setSelectedVariant(stack, next);
            player.displayClientMessage(Component.translatable("message.hidden_places.parched_skeleton_remains.selected", variantName(next)), true);
            level.playSound(null, player.blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.45F, 1.1F);
        }
        return InteractionResult.SUCCESS;
    }

    private static int resolveFloorVariant(Level level, ItemStack stack) {
        int selected = getSelectedVariant(stack);
        if (selected == AUTO_VARIANT) {
            int[] variants = ParchedSkeletonRemainsEntity.FLOOR_VARIANTS;
            return variants[level.random.nextInt(variants.length)];
        }
        return selected;
    }

    private static int getSelectedVariant(ItemStack stack) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        return tag.contains(TAG_SELECTED_VARIANT) ? tag.getInt(TAG_SELECTED_VARIANT) : AUTO_VARIANT;
    }

    private static void setSelectedVariant(ItemStack stack, int variant) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putInt(TAG_SELECTED_VARIANT, variant);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private static Component variantName(int variant) {
        return variant == AUTO_VARIANT
                ? Component.translatable("message.hidden_places.parched_skeleton_remains.variant.auto")
                : Component.literal("#" + variant);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag tooltipFlag) {
        tooltip.add(Component.translatable("tooltip.hidden_places.parched_skeleton_remains.usage").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.hidden_places.parched_skeleton_remains.selected", variantName(getSelectedVariant(stack))).withStyle(ChatFormatting.DARK_GRAY));
    }


    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private ParchedSkeletonRemainsItemRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                if (this.renderer == null) {
                    this.renderer = new ParchedSkeletonRemainsItemRenderer();
                }

                return this.renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Static item. No animation for now.
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.animatableInstanceCache;
    }

}
