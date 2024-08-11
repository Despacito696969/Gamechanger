package org.despacito696969.gamechanger;

import net.minecraft.world.food.FoodProperties;
import org.apache.commons.compress.utils.Lists;

import java.util.ArrayList;

public class FoodMod extends FoodProperties {
    public FoodProperties props;

    public Integer nutritionOpt;
    public Float saturationModifierOpt;
    public Boolean isMeatOpt;
    public Boolean canAlwaysEatOpt;
    public Boolean isFastFoodOpt;

    public FoodMod(FoodProperties props) {
        super(
            props == null ? 0 : props.getNutrition(),
            props == null ? 0.0f : props.getSaturationModifier(),
            props == null ? false : props.isMeat(),
            props == null ? false : props.canAlwaysEat(),
            props == null ? false : props.isFastFood(),
            props == null ? Lists.newArrayList() : props.getEffects()
        );
        this.props = props;
    }

    @Override
    public int getNutrition() {
        return nutritionOpt != null ?
            nutritionOpt : super.getNutrition();
    }

    @Override
    public float getSaturationModifier() {
        return saturationModifierOpt != null ?
            saturationModifierOpt : super.getSaturationModifier();
    }

    @Override
    public boolean isMeat() {
        return isMeatOpt != null ?
            isMeatOpt : super.isMeat();
    }

    @Override
    public boolean canAlwaysEat() {
        return canAlwaysEatOpt != null ?
                canAlwaysEatOpt : super.canAlwaysEat();
    }

    @Override
    public boolean isFastFood() {
        return isFastFoodOpt != null ?
            isFastFoodOpt : super.isFastFood();
    }
}
