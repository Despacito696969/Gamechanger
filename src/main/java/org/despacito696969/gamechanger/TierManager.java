package org.despacito696969.gamechanger;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.TieredItem;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

public class TierManager {
    // TODO: Change ResourceLocation to Item
    public static IdentityHashMap<TieredItem, TierMod> tierOverrides = new IdentityHashMap<>();
    record DefaultAttack(AttributeModifier attribute, float attackDamageBaseline) {}
    public static TierMod getOrCreateTier(TieredItem item) {
        var props_1 = tierOverrides.get(item);
        if (props_1 != null) {
            return props_1;
        }
        var props_2 = new TierMod(item.getTier());
        tierOverrides.put(item, props_2);
        return props_2;
    }
}
