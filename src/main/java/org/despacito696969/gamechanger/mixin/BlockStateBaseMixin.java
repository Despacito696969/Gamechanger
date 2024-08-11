package org.despacito696969.gamechanger.mixin;

import net.minecraft.world.level.block.state.BlockBehaviour;
import org.despacito696969.gamechanger.BlockPropertiesManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.BlockStateBase.class)
public class BlockStateBaseMixin {
    @Inject(method = "getDestroySpeed", at = @At("HEAD"), cancellable = true)
    void defaultDestroyTimeInject(CallbackInfoReturnable<Float> cir) {
        var blockState = (BlockBehaviour.BlockStateBase)(Object)this;
        var mod = BlockPropertiesManager.propMods.get(blockState.getBlock());
        if (mod == null) {
            return;
        }
        if (mod.destroyTime == null) {
            return;
        }
        cir.setReturnValue(mod.destroyTime);
    }
}
