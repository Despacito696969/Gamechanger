package org.despacito696969.gamechanger.mixin;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import org.despacito696969.gamechanger.AttributeManager;
import org.despacito696969.gamechanger.FoodManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public class ItemMixin {
    @Inject(method = "getFoodProperties", at = @At("HEAD"), cancellable = true)
    void getFoodPropertiesInject(CallbackInfoReturnable<FoodProperties> cir) {
        var loc = BuiltInRegistries.ITEM.getKey(((Item)(Object)this));
        var props = FoodManager.foodMods.get(loc);
        if (props == null) {
            return;
        }
        cir.setReturnValue(props.orElse(null));
    }

    @Inject(method = "isEdible", at = @At("HEAD"), cancellable = true)
    void isEdibleInject(CallbackInfoReturnable<Boolean> cir) {
        var loc = BuiltInRegistries.ITEM.getKey(((Item)(Object)this));
        var props = FoodManager.foodMods.get(loc);
        if (props == null) {
            return;
        }
        cir.setReturnValue(props.isPresent());
    }

    @Overwrite
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        if (slot != EquipmentSlot.MAINHAND) {
            return ImmutableMultimap.of();
        }
        var attrs = AttributeManager.modList.get((Item)(Object)this);
        if (attrs != null) {
            return attrs.replacementAttributes;
        }
        return ImmutableMultimap.of();
    }
}
