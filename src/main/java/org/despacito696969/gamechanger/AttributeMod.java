package org.despacito696969.gamechanger;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import org.apache.logging.log4j.util.TriConsumer;

import java.util.ArrayList;
import java.util.UUID;
import java.util.function.BiConsumer;

public class AttributeMod {
    public static class DoubleMod {
        public DoubleModMode mode;
        public double value;
        public enum DoubleModMode {
            REMOVE,
            SET,
        }

        public DoubleMod(DoubleModMode mode, double value) {
            this.mode = mode;
            this.value = value;
        }
    }

    public DoubleMod attackDamage;
    public DoubleMod attackSpeed;
    public DoubleMod armor;
    public DoubleMod toughness;
    public DoubleMod speed;
    public DoubleMod knockbackResistance;
    public DoubleMod maxHealth;
    public Multimap<Attribute, AttributeModifier> replacementAttributes;

    private static class AttributeEntry {
        public Attribute attribute;
        public AttributeModifier attributeModifier;
        public AttributeEntry(Attribute attribute, AttributeModifier attributeModifier) {
            this.attribute = attribute;
            this.attributeModifier = attributeModifier;
        }
    }

    public AttributeMod(ItemAttributeInfo itemAttributeInfo) {
        updateAttributes(itemAttributeInfo);
    }

    public void updateAttributes(
        ItemAttributeInfo itemAttributeInfo
    ) {
        var uuid = itemAttributeInfo.uuid;
        var attributeName = itemAttributeInfo.name;
        var attributes = itemAttributeInfo.defaultAttributes;
        ArrayList<AttributeEntry> entries = new ArrayList<>();
        for (var entry : attributes.entries()) {
            entries.add(new AttributeEntry(entry.getKey(), entry.getValue()));
        }

        TriConsumer<DoubleMod, Attribute, Double> applyAttribute = (doubleMod, attribute, defaultValue) -> {
            if (doubleMod == null) {
                return;
            }
            switch (doubleMod.mode) {
                case REMOVE -> {
                    for (int index = 0; index < entries.size(); ++index) {
                        var entry = entries.get(index);
                        if (entry.attribute == attribute) {
                            entries.remove(index);
                            break;
                        }
                    }
                }
                case SET -> {
                    boolean foundAttribute = false;
                    for (int index = 0; index < entries.size(); ++index) {
                        var entry = entries.get(index);
                        if (entry.attribute == attribute) {
                            var oldModifier = entry.attributeModifier;
                            entry.attributeModifier = new AttributeModifier(
                                uuid,
                                oldModifier.getName(),
                                doubleMod.value - defaultValue,
                                oldModifier.getOperation()
                            );
                            foundAttribute = true;
                            break;
                        }
                    }
                    if (!foundAttribute) {
                        entries.add(
                            new AttributeEntry(
                                attribute,
                                new AttributeModifier(
                                    uuid,
                                    attributeName,
                                    doubleMod.value,
                                    AttributeModifier.Operation.ADDITION
                                )
                            )
                        );
                    }
                }
            }
        };

        applyAttribute.accept(attackDamage, Attributes.ATTACK_DAMAGE, 1.0);
        applyAttribute.accept(attackSpeed, Attributes.ATTACK_SPEED, 4.0);
        applyAttribute.accept(armor, Attributes.ARMOR, 0.0);
        applyAttribute.accept(toughness, Attributes.ARMOR_TOUGHNESS, 0.0);
        applyAttribute.accept(speed, Attributes.MOVEMENT_SPEED, 0.0);
        applyAttribute.accept(knockbackResistance, Attributes.KNOCKBACK_RESISTANCE, 0.0);
        applyAttribute.accept(maxHealth, Attributes.MAX_HEALTH,0.0);
        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();

        BiConsumer<Attribute, UUID> replace_attribute = (attribute, newUuid) -> {
            for (int index = 0; index < entries.size(); ++index) {
                var entry = entries.get(index);
                if (entry.attribute == attribute) {
                    entries.set(
                        index,
                        new AttributeEntry(
                            entry.attribute,
                            new AttributeModifier(
                                newUuid,
                                entry.attributeModifier.getName(),
                                entry.attributeModifier.getAmount(),
                                entry.attributeModifier.getOperation()
                            )
                        )
                    );
                }
            }
        };

        replace_attribute.accept(Attributes.ATTACK_DAMAGE, Item.BASE_ATTACK_DAMAGE_UUID);
        replace_attribute.accept(Attributes.ATTACK_SPEED, Item.BASE_ATTACK_SPEED_UUID);

        for (var entry : entries) {
            builder.put(entry.attribute, entry.attributeModifier);
        }
        this.replacementAttributes = builder.build();
    }
}
