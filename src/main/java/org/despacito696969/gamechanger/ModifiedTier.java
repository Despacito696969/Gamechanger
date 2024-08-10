package org.despacito696969.gamechanger;

import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.function.Function;

public class ModifiedTier implements Tier {
    public enum Modifier {
        NONE,
        SET,
        ADD,
        MULTIPLY,
    }

    public static class ModifiedStatFloat {
        public Modifier modifier = Modifier.NONE;
        public float value;
        public Function<Tier, Float> getter;

        public ModifiedStatFloat(Function<Tier, Float> getter) {
            this(getter, Modifier.NONE, 0);
        }

        public void setModifier(Modifier modifier, float value) {
            this.modifier = modifier;
            this.value = value;
        }

        public void clearModifier() {
            this.modifier = Modifier.NONE;
        }

        public ModifiedStatFloat(Function<Tier, Float> getter, Modifier modifier, float value) {
            this.getter = getter;
        }

        public float getValue(Tier tier) {
            return switch (modifier) {
                case NONE -> getter.apply(tier);
                case SET -> value;
                case ADD -> getter.apply(tier) + value;
                case MULTIPLY -> getter.apply(tier) * value;
            };
        }
    }

    public static class ModifiedStatInt {
        public Modifier modifier = Modifier.NONE;
        public int value;
        public Function<Tier, Integer> getter;

        public ModifiedStatInt(Function<Tier, Integer> getter) {
            this(getter, Modifier.NONE, 0);
        }

        public void setModifier(Modifier modifier, int value) {
            this.modifier = modifier;
            this.value = value;
        }

        public void clearModifier() {
            this.modifier = Modifier.NONE;
        }

        public ModifiedStatInt(Function<Tier, Integer> getter, Modifier modifier, int value) {
            this.getter = getter;
        }

        public int getValue(Tier tier) {
            return switch (modifier) {
                case NONE -> getter.apply(tier);
                case SET -> value;
                case ADD -> getter.apply(tier) + value;
                case MULTIPLY -> getter.apply(tier) * value;
            };
        }
    }
    public final Tier tier;

    public ModifiedStatInt levelFn;
    public ModifiedStatInt enchantmentValueFn;
    public ModifiedStatInt usesFn;
    public ModifiedStatFloat speedFn;

    public ModifiedStatFloat attackDamageBonusFn;
    public ModifiedTier(Tier tier) {
        this.tier = tier;
        levelFn = new ModifiedStatInt(Tier::getLevel);
        enchantmentValueFn = new ModifiedStatInt(Tier::getEnchantmentValue);
        usesFn = new ModifiedStatInt(Tier::getUses);
        speedFn = new ModifiedStatFloat(Tier::getSpeed);
        attackDamageBonusFn = new ModifiedStatFloat(Tier::getAttackDamageBonus);
    }

    @Override
    public int getUses() {
        return usesFn.getValue(tier);
    }

    @Override
    public float getSpeed() {
        return speedFn.getValue(tier);
    }

    @Override
    public float getAttackDamageBonus() {
        return attackDamageBonusFn.getValue(tier);
    }

    @Override
    public int getLevel() {
        return levelFn.getValue(tier);
    }

    @Override
    public int getEnchantmentValue() {
        return enchantmentValueFn.getValue(tier);
    }

    @Override
    public Ingredient getRepairIngredient() {
        return tier.getRepairIngredient();
    }
}
