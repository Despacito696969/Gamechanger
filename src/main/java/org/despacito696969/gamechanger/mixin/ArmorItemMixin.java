package org.despacito696969.gamechanger.mixin;

import com.google.common.collect.Multimap;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ArmorItem;
import org.despacito696969.gamechanger.AttributeManager;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ArmorItem.class)
public class ArmorItemMixin {

    @Redirect(method = "getDefaultAttributeModifiers", at = @At(value = "FIELD", target = "Lnet/minecraft/world/item/ArmorItem;defaultModifiers:Lcom/google/common/collect/Multimap;", opcode = Opcodes.GETFIELD))
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiersRedirect(ArmorItem armorItem) {
        var entry = AttributeManager.modList.get(armorItem);
        if (entry == null) {
            return armorItem.defaultModifiers;
        }
        return entry.replacementAttributes;
    }
}
