package org.despacito696969.gamechanger;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TieredItem;

import java.util.HashMap;
import java.util.Map;

public class TierManager {
    public static Map<ResourceLocation, ModifiedTier> tierOverrides = new HashMap<>();

    record DefaultAttack(AttributeModifier attribute, float attackDamageBaseline) {}
    public static Map<ResourceLocation, DefaultAttack> defaultAttackDamage = new HashMap<>();
}
