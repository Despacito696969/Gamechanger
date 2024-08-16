package org.despacito696969.gamechanger.mixin;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TieredItem;
import org.despacito696969.gamechanger.TierManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TieredItem.class)
public class TieredItemMixin {

    @Inject(method = "Lnet/minecraft/world/item/TieredItem;getTier()Lnet/minecraft/world/item/Tier;", at = @At("HEAD"), cancellable = true)
    private void injected(CallbackInfoReturnable<Tier> cir) {
        var item = (Item)(Object)this;
        var tier_opt = TierManager.tierOverrides.get(item);
        if (tier_opt == null) {
            return;
        }
        cir.setReturnValue(tier_opt);
    }
}
