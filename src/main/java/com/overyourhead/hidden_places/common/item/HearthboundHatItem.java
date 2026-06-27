package com.overyourhead.hidden_places.common.item;

import com.overyourhead.hidden_places.client.renderer.HearthboundHatRenderer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.function.Consumer;

public class HearthboundHatItem extends ArmorItem implements GeoItem {
    public static final int HOLD_TICKS = 20 * 3;
    public static final int BLOCKS_PER_LEVEL = 1000;
    public static final int MAX_COST_LEVELS = 15;
    public static final int CROSS_DIMENSION_COST_LEVELS = MAX_COST_LEVELS;
    public static final int COOLDOWN_TICKS = 20 * 60;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public HearthboundHatItem(Holder<ArmorMaterial> material, Properties properties) {
        super(material, Type.HELMET, properties);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // No animations for now.
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    @SuppressWarnings("removal")
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private HearthboundHatRenderer renderer;

            @Override
            public HumanoidModel<?> getHumanoidArmorModel(
                    LivingEntity livingEntity,
                    ItemStack itemStack,
                    EquipmentSlot equipmentSlot,
                    HumanoidModel<?> original
            ) {
                if (this.renderer == null) {
                    this.renderer = new HearthboundHatRenderer();
                }

                this.renderer.prepForRender(livingEntity, itemStack, equipmentSlot, original);
                return this.renderer;
            }
        });
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        tooltip.add(Component.translatable("tooltip.hidden_places.hearthbound_hat.usage").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.hidden_places.hearthbound_hat.cost").withStyle(ChatFormatting.DARK_GRAY));
    }
}
