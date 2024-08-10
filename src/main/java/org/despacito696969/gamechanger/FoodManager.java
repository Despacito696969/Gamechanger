package org.despacito696969.gamechanger;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class FoodManager {
    public static Map<ResourceLocation, Optional<FoodMod>> foodMods = new HashMap<>();

    static {
        var loc = BuiltInRegistries.ITEM.getKey(Items.NETHERITE_INGOT);
        var mod = new FoodMod(Items.NETHERITE_INGOT.getFoodProperties());
        mod.nutritionOpt = 16;
        mod.saturationModifierOpt = 0.75f;
        foodMods.put(loc, Optional.of(mod));
    }

    public static FoodMod getOrCreateFoodProperties(Item item) {
        var loc = BuiltInRegistries.ITEM.getKey(item);
        var foodMod = foodMods.get(loc);
        if (foodMod != null) {
            if (foodMod.isPresent()) {
                return foodMod.get();
            }
            foodMods.remove(loc);
        }
        var mod = new FoodMod(item.getFoodProperties());
        foodMods.put(loc, Optional.of(mod));
        return mod;
    }

    public static void clearMods(Item item) {
        var loc = BuiltInRegistries.ITEM.getKey(item);
        foodMods.remove(loc);
    }

    public static void removeFoodProperties(Item item) {
        var loc = BuiltInRegistries.ITEM.getKey(item);
        foodMods.replace(loc, Optional.empty());
    }
}
