package org.despacito696969.gamechanger;

import com.google.gson.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.despacito696969.gamechanger.Gamechanger.LOGGER;

public class FoodManager {

    // TODO: Change ResourceLocation to Item
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

    public static void loadFromJson(JsonArray foods) {
        for (var e : foods.asList()) {
            if (!e.isJsonObject()) {
                LOGGER.warn("Config: food contains not an object: " + e);
                continue;
            }
            var object = e.getAsJsonObject();
            if (!object.has("id")) {
                LOGGER.warn("Config: food doesn't contain id: " + e);
                continue;
            }
            var jsonId = object.get("id");
            if (!(jsonId.isJsonPrimitive() && jsonId.getAsJsonPrimitive().isString())) {
                LOGGER.warn("Config: food doesn't contain string in id field: " + e);
                continue;
            }
            var id = jsonId.getAsJsonPrimitive().getAsString();

            if (!object.has("type")) {
                LOGGER.warn("Config: food doesn't contain type: " + e);
                continue;
            }
            var jsonType = object.get("type");
            if (!(jsonType.isJsonPrimitive() && jsonType.getAsJsonPrimitive().isString())) {
                LOGGER.warn("Config: food doesn't contain string in type field");
                continue;
            }
            var type = jsonType.getAsJsonPrimitive().getAsString();

            var loc = new ResourceLocation(id);
            var item = BuiltInRegistries.ITEM.get(loc);
            if (item == Items.AIR) {
                LOGGER.warn("Config: food has id of not existing item: " + id);
                continue;
            }

            if (type.equals("remove")) {
                FoodManager.foodMods.put(loc, Optional.empty());
            }
            else if (type.equals("modify")) {
                var props = FoodManager.getOrCreateFoodProperties(item);
                if (object.has("nutrition")) {
                    var nutritionObj = object.get("nutrition");
                    if (nutritionObj.isJsonPrimitive() && nutritionObj.getAsJsonPrimitive().isNumber()) {
                        var number = nutritionObj.getAsJsonPrimitive().getAsNumber();
                        props.nutritionOpt = number.intValue();
                    }
                    else {
                        LOGGER.warn("Config: food: nutrition doesn't contain a Number: " + e);
                    }
                }
                if (object.has("saturation")) {
                    var saturationObj = object.get("saturation");
                    if (saturationObj.isJsonPrimitive() && saturationObj.getAsJsonPrimitive().isNumber()) {
                        var number = saturationObj.getAsJsonPrimitive().getAsNumber();
                        props.saturationModifierOpt = number.floatValue();
                    }
                    else {
                        LOGGER.warn("Config: food: saturation doesn't contain a Number: " + e);
                    }
                }
                if (object.has("is_meat")) {
                    var is_meatObj = object.get("is_meat");
                    if (is_meatObj.isJsonPrimitive() && is_meatObj.getAsJsonPrimitive().isBoolean()) {
                        props.isMeatOpt = is_meatObj.getAsJsonPrimitive().getAsBoolean();
                    }
                    else {
                        LOGGER.warn("Config: food: is_meat doesn't contain a Boolean: " + e);
                    }
                }
                if (object.has("can_always_eat")) {
                    var can_always_eatObj = object.get("can_always_eat");
                    if (can_always_eatObj.isJsonPrimitive() && can_always_eatObj.getAsJsonPrimitive().isBoolean()) {
                        props.canAlwaysEatOpt = can_always_eatObj.getAsJsonPrimitive().getAsBoolean();
                    }
                    else {
                        LOGGER.warn("Config: food: can_always_eat doesn't contain a Boolean: " + e);
                    }
                }
                if (object.has("is_fast_food")) {
                    var is_fast_foodObj = object.get("is_fast_food");
                    if (is_fast_foodObj.isJsonPrimitive() && is_fast_foodObj.getAsJsonPrimitive().isBoolean()) {
                        props.isFastFoodOpt = is_fast_foodObj.getAsJsonPrimitive().getAsBoolean();
                    }
                    else {
                        LOGGER.warn("Config: food: is_fast_food doesn't contain a Boolean: " + e);
                    }
                }
            }
            else {
                LOGGER.warn("Config: food has unsupported type: " + e);
            }
        }
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
        var mod = new FoodMod(item.foodProperties);
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
