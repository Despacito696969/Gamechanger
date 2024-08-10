package org.despacito696969.gamechanger;

import net.minecraft.resources.ResourceLocation;
public record AttributeMod(float value, AttributeMod.Type type) {
    public enum Type {
        BASE_ATTACK_DAMAGE,
        BASE_ATTACK_SPEED,
    }
}
