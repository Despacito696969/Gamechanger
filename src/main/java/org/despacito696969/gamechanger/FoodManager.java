package org.despacito696969.gamechanger;

import com.google.gson.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class FoodManager {
    public static Map<ResourceLocation, Optional<FoodMod>> foodMods = new HashMap<>();

    public static JsonArray saveToJson() {
        var result = new JsonArray();
        for (var entry : foodMods.entrySet()) {
            var loc = entry.getKey();
            if (entry.getValue().isEmpty()) {
                var obj = new JsonObject();
                obj.addProperty("id", loc.toString());
                obj.addProperty("type", "remove");
                result.add(obj);
            }
            else {
                var foodMod = entry.getValue().get();
                var obj = new JsonObject();
                obj.addProperty("id", loc.toString());
                obj.addProperty("type", "modify");

                var null_size = obj.size();

                if (foodMod.nutritionOpt != null) {
                    obj.addProperty("nutrition", foodMod.nutritionOpt);
                }
                if (foodMod.saturationModifierOpt != null) {
                    obj.addProperty("saturation", foodMod.saturationModifierOpt);
                }
                if (foodMod.isMeatOpt != null) {
                    obj.addProperty("is_meat", foodMod.isMeatOpt);
                }
                if (foodMod.canAlwaysEatOpt != null) {
                    obj.addProperty("can_always_eat", foodMod.canAlwaysEatOpt);
                }
                if (foodMod.isFastFoodOpt != null) {
                    obj.addProperty("is_fast_food", foodMod.isFastFoodOpt);
                }
                result.add(obj);
            }
        }
        return result;
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
        foodMods.remove(loc);
        foodMods.put(loc, Optional.empty());
    }
}
