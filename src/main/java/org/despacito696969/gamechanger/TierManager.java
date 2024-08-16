package org.despacito696969.gamechanger;

import com.google.gson.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.TieredItem;

import java.util.IdentityHashMap;
import java.util.function.Consumer;

public class TierManager {
    // TODO: Change ResourceLocation to Item
    public static IdentityHashMap<TieredItem, TierMod> tierOverrides = new IdentityHashMap<>();
    record DefaultAttack(AttributeModifier attribute, float attackDamageBaseline) {}
    public static TierMod getOrCreateTier(TieredItem item) {
        var props_1 = tierOverrides.get(item);
        if (props_1 != null) {
            return props_1;
        }
        var props_2 = new TierMod(item.getTier());
        tierOverrides.put(item, props_2);
        return props_2;
    }

    public static JsonArray saveToJson() {
        var result = new JsonArray();
        for (var entry : tierOverrides.entrySet()) {
            var id = BuiltInRegistries.ITEM.getKey(entry.getKey()).toString();
            var obj = new JsonObject();
            obj.addProperty("id", id);
            var tierMod = entry.getValue();
            if (tierMod.attackDamageBonus != null) {
                obj.addProperty("attack_damage_bonus", tierMod.attackDamageBonus);
            }

            if (tierMod.enchantmentValue != null) {
                obj.addProperty("enchantment_value", tierMod.enchantmentValue);
            }

            if (tierMod.level != null) {
                obj.addProperty("level", tierMod.level);
            }

            if (tierMod.speed != null) {
                obj.addProperty("speed", tierMod.speed);
            }

            if (tierMod.uses != null) {
                obj.addProperty("uses", tierMod.uses);
            }
            result.add(obj);
        }
        return result;
    }

    private static void logError(String message) {
        Gamechanger.LOGGER.error("Error: attributes: " + message);
    }


    public static void loadFromJson(JsonArray array) {
        for (var elem : array) {
            if (!elem.isJsonObject()) {
                logError("array element is not an object: " + elem);
                continue;
            }
            var obj = elem.getAsJsonObject();
            singleLoadFromJson(obj);
        }
    }

    public static void singleLoadFromJson(JsonObject obj) {
        if (!obj.has("id")) {
            logError("doesn't contain id: " + obj);
            return;
        }
        var jsonId = obj.get("id");
        if (!(jsonId.isJsonPrimitive() && jsonId.getAsJsonPrimitive().isString())) {
            logError("doesn't contain string in id field: " + obj);
            return;
        }
        var id = jsonId.getAsJsonPrimitive().getAsString();

        var item = BuiltInRegistries.ITEM.get(new ResourceLocation(id));
        if (!(item instanceof TieredItem tieredItem)) {
            logError("item with id " + id + " is not TieredItem: " + obj);
            return;
        }

        var tierMod = TierManager.getOrCreateTier(tieredItem);

        record TierModField(String name, Consumer<JsonPrimitive> assignPrimitive) {}

        var tierModFields = new TierModField[] {
            new TierModField("attack_damage_bonus", (p) -> tierMod.attackDamageBonus = p.getAsFloat()),
            new TierModField("level", (p) -> tierMod.level = p.getAsInt()),
            new TierModField("speed", (p) -> tierMod.speed = p.getAsFloat()),
            new TierModField("uses", (p) -> tierMod.uses = p.getAsInt()),
            new TierModField("enchantment_value", (p) -> tierMod.enchantmentValue = p.getAsInt()),
        };

        Consumer<TierModField> forEachField = (field) -> {
            var fieldName = field.name();
            if (!(obj.has(fieldName))) {
                return;
            }

            var fieldElem = obj.get(fieldName);
            if (!(fieldElem.isJsonPrimitive() && fieldElem.getAsJsonPrimitive().isNumber())) {
                logError("field " + fieldName + " has non-number value: " + obj);
                return;
            }

            field.assignPrimitive().accept(fieldElem.getAsJsonPrimitive());
        };

        for (var field : tierModFields) {
            forEachField.accept(field);
        }
    }
}
