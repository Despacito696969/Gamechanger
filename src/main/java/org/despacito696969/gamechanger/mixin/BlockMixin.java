package org.despacito696969.gamechanger.mixin;

import net.minecraft.world.level.block.Block;
import org.despacito696969.gamechanger.BlockPropertiesManager;
import org.despacito696969.gamechanger.BlockPropertiesMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Block.class)
public class BlockMixin {
    @Inject(method = "getExplosionResistance", at = @At("HEAD"), cancellable = true)
    void getExplosionResistanceInject(CallbackInfoReturnable<Float> cir) {
        var block = (Block)(Object)this;
        var props = BlockPropertiesManager.propMods.get(block);
        if (props == null) {
            return;
        }
        if (props.explosionResistance == null) {
            return;
        }
        cir.setReturnValue(props.explosionResistance);
    }
}
