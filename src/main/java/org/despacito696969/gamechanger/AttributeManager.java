package org.despacito696969.gamechanger;

import com.google.common.collect.Multimap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class AttributeManager {
    public record DefaultStats(
        float attackDamageBaseline,
        Multimap<Attribute, AttributeModifier> attributes
    ) {}
    public static Map<ResourceLocation, ArrayList<AttributeMod>> modList = new HashMap<>();
    public static Map<ResourceLocation, DefaultStats> defaultAttributes = new HashMap<>();

    void restoreItem(ResourceLocation loc) {
        var defaults = defaultAttributes.get(loc);
        if (defaults == null) {
            return;
        }
        Item item = BuiltInRegistries.ITEM.get(loc);
        if (item instanceof DiggerItem diggerItem) {
            diggerItem.attackDamageBaseline = defaults.attackDamageBaseline;
            diggerItem.defaultModifiers = defaults.attributes;
        }
    }
}
