package org.despacito696969.gamechanger.mixin;

import net.fabricmc.fabric.api.mininglevel.v1.MiningLevelManager;
import net.minecraft.world.level.block.state.BlockState;
import org.despacito696969.gamechanger.BlockPropertiesManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MiningLevelManager.class)
public class MiningLevelManagerMixin {
    @Inject(method = "getRequiredMiningLevel", at = @At("HEAD"), cancellable = true, remap = false)
    private static void getRequiredMiningLevelInject(BlockState state, CallbackInfoReturnable<Integer> cir) {
        var props = BlockPropertiesManager.propMods.get(state.getBlock());
        if (props == null) {
            return;
        }
        if (props.miningLevel == null) {
            return;
        }
        cir.setReturnValue(props.miningLevel);
    }

}
