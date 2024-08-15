package org.despacito696969.gamechanger;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;

public class AttributeMod {

    public class DoubleMod {
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

    private static class AttributeEntry {
        public Attribute attribute;
        public AttributeModifier attributeModifier;
        public AttributeEntry(Attribute attribute, AttributeModifier attributeModifier) {
            this.attribute = attribute;
            this.attributeModifier = attributeModifier;
        }
    }

    public Multimap<Attribute, AttributeModifier> getNewAttributes(
        UUID uuid,
        String attributeName,
        Multimap<Attribute, AttributeModifier> attributes
    ) {
        ArrayList<AttributeEntry> entries = new ArrayList<>();
        for (var entry : attributes.entries()) {
            entries.add(new AttributeEntry(entry.getKey(), entry.getValue()));
        }

        BiConsumer<DoubleMod, Attribute> applyAttribute = (doubleMod, attribute) -> {
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
                                doubleMod.value,
                                oldModifier.getOperation()
                            );
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

        applyAttribute.accept(attackDamage, Attributes.ATTACK_DAMAGE);
        applyAttribute.accept(attackSpeed, Attributes.ATTACK_SPEED);
        applyAttribute.accept(armor, Attributes.ARMOR);
        applyAttribute.accept(toughness, Attributes.ARMOR_TOUGHNESS);
        applyAttribute.accept(speed, Attributes.MOVEMENT_SPEED);
        applyAttribute.accept(knockbackResistance, Attributes.KNOCKBACK_RESISTANCE);
        applyAttribute.accept(maxHealth, Attributes.MAX_HEALTH);
        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        for (var entry : entries) {
            builder.put(entry.attribute, entry.attributeModifier);
        }
        return builder.build();
    }
}
