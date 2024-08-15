package org.despacito696969.gamechanger;

import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;

public class TierMod implements Tier {
    public final Tier tier;

    public Integer level = null;
    public Integer enchantmentValue = null;
    public Integer uses = null;
    public Float speed = null;

    public Float attackDamageBonus = null;
    public TierMod(Tier tier) {
        this.tier = tier;
    }

    @Override
    public int getUses() {
        return uses != null ? uses : tier.getUses();
    }

    @Override
    public float getSpeed() {
        return speed != null ? speed : tier.getSpeed();
    }

    @Override
    public float getAttackDamageBonus() {
        return attackDamageBonus != null ? speed : tier.getAttackDamageBonus();
    }

    @Override
    public int getLevel() {
        return level != null ? level : tier.getLevel();
    }

    @Override
    public int getEnchantmentValue() {
        return enchantmentValue != null ? enchantmentValue : tier.getEnchantmentValue();
    }

    @Override
    public Ingredient getRepairIngredient() {
        return tier.getRepairIngredient();
    }
}
