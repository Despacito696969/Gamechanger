package org.despacito696969.gamechanger;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SwordItem;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
public class ItemAttributeInfo {
    public static UUID temp_uuid = UUID.fromString("B8D292EA-B181-4C67-980F-2468E7067DD3");
    public UUID uuid;
    public String name;
    public Multimap<Attribute, AttributeModifier> defaultAttributes;
    public Multimap<Attribute, AttributeModifier> currentAttributes;
    public Item item;
    private ItemAttributeInfo(
        UUID uuid,
        String name,
        Multimap<Attribute, AttributeModifier> defaultAttributes,
        Item item
    ) {
        this.uuid = uuid;
        this.name = name;
        this.defaultAttributes = defaultAttributes;

        this.currentAttributes = defaultAttributes;
        var currentAttributes = AttributeManager.modList.get(item);
        if (currentAttributes != null) {
            this.currentAttributes = currentAttributes.replacementAttributes;
        }

        this.item = item;
    }
    @Nullable
    public static ItemAttributeInfo getAttributeStats(Item item) {
        if (item instanceof ArmorItem armorItem) {
            var attributes = armorItem.defaultModifiers;
            var name = "Armor modifier";
            UUID uuid = ArmorItem.ARMOR_MODIFIER_UUID_PER_TYPE.get(armorItem.getType());
            return new ItemAttributeInfo(uuid, name, attributes, item);
        }
        else if (item instanceof DiggerItem diggerItem) {
            var attributes = diggerItem.defaultModifiers;
            var name = "Tool modifier";
            UUID uuid = temp_uuid;
            return new ItemAttributeInfo(uuid, name, attributes, item);
        }
        else if (item instanceof SwordItem swordItem) {
            var attributes = swordItem.defaultModifiers;
            var name = "Weapon modifier";
            UUID uuid = temp_uuid;
            return new ItemAttributeInfo(uuid, name, attributes, item);
        }
        else {
            ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
            var attributes = builder.build();
            var name = "Item modifier";
            UUID uuid = temp_uuid;
            return new ItemAttributeInfo(uuid, name, attributes, item);
        }
    }
}
