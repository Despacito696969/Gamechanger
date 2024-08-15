package org.despacito696969.gamechanger.mixin;

import net.fabricmc.fabric.api.mininglevel.v1.MiningLevelManager;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.level.block.state.BlockState;
import org.despacito696969.gamechanger.TierManager;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DiggerItem.class)
public class DiggerItemMixin {
    @Inject(method = "isCorrectToolForDrops", at = @At("HEAD"), cancellable = true)
    void isCorrectToolForDrops(BlockState blockState, CallbackInfoReturnable<Boolean> cir) {
        int level = ((DiggerItem)(Object)this).getTier().getLevel();
        cir.setReturnValue(level >= MiningLevelManager.getRequiredMiningLevel(blockState));
    }

    @Redirect(method = "getDestroySpeed", at = @At(value = "FIELD", target = "Lnet/minecraft/world/item/DiggerItem;speed:F", opcode = Opcodes.GETFIELD))
    public float speedRedirect(DiggerItem item) {
        var props = TierManager.tierOverrides.get(item);
        if (props == null) {
            return item.speed;
        }
        if (props.speed == null) {
            return item.speed;
        }
        return props.speed;
    }
}
