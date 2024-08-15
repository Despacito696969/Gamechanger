package org.despacito696969.gamechanger;

import com.google.common.collect.Multimap;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;

import java.util.IdentityHashMap;

public class AttributeManager {
    public static class AttributeManagerEntry {
        public AttributeMod mod = new AttributeMod();
        @Nullable
        public Multimap<Attribute, AttributeModifier> replacementAttributes = null;
    }
    public static IdentityHashMap<Item, AttributeManagerEntry> modList = new IdentityHashMap<>();

    /*public static AttributeManagerEntry getOrCreateAttributeEntry(Item item) {
        var entry = modList.get(item);
        if (entry != null) {
            return entry;
        }
        entry = new AttributeManagerEntry();
        modList.put(item, entry);
        return entry;
    }*/
}
