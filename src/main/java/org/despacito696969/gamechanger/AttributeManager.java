package org.despacito696969.gamechanger;

import com.google.gson.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.IdentityHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.despacito696969.gamechanger.AttributeMod.*;

public class AttributeManager {
    public static IdentityHashMap<Item, AttributeMod> modList = new IdentityHashMap<>();
    public static AttributeMod getOrCreateAttributeManagerEntry(ItemAttributeInfo itemAttributeInfo) {
        var item = itemAttributeInfo.item;
        var entry = modList.get(item);
        if (entry != null) {
            return entry;
        }
        entry = new AttributeMod(itemAttributeInfo);
        modList.put(item, entry);
        return entry;
    }

    public record AttributeModField(String field, Function<AttributeMod, AttributeMod.DoubleMod> getter, BiConsumer<AttributeMod, AttributeMod.DoubleMod> setter) {}

    public static AttributeModField[] fields = new AttributeModField[]{
        new AttributeModField("attack_damage", (attrMod) -> attrMod.attackDamage, (attrMod, val) -> attrMod.attackDamage = val),
        new AttributeModField("attack_speed", (attrMod) -> attrMod.attackSpeed, (attrMod, val) -> attrMod.attackSpeed = val),
        new AttributeModField("speed", (attrMod) -> attrMod.speed, (attrMod, val) -> attrMod.speed = val),
        new AttributeModField("max_health", (attrMod) -> attrMod.maxHealth, (attrMod, val) -> attrMod.maxHealth = val),
        new AttributeModField("armor", (attrMod) -> attrMod.armor, (attrMod, val) -> attrMod.armor = val),
        new AttributeModField("toughness", (attrMod) -> attrMod.toughness, (attrMod, val) -> attrMod.toughness = val),
        new AttributeModField("knockback_resistance", (attrMod) -> attrMod.knockbackResistance, (attrMod, val) -> attrMod.knockbackResistance = val),
    };

    public static JsonArray saveToJson() {
        var result = new JsonArray();
        for (var entry : modList.entrySet()) {
            var obj = new JsonObject();
            var resource = BuiltInRegistries.ITEM.getKey(entry.getKey());
            var id = resource.toString();

            var attributeModd = entry.getValue();

            for (var field : fields) {
                var doubleMod = field.getter().apply(attributeModd);
                if (doubleMod != null) {
                    var subObj = new JsonObject();

                    switch (doubleMod.mode) {
                        case SET -> {
                            subObj.addProperty("type", "set");
                            subObj.addProperty("value", doubleMod.value);
                        }
                        case REMOVE -> {
                            subObj.addProperty("type", "remove");
                        }
                    }
                    obj.add(field.field(), subObj);
                }
            }

            if (obj.size() == 0) {
                continue;
            }
            obj.addProperty("id", id);
            result.add(obj);
        }
        return result;
    }

    private static void logError(String message) {
        Gamechanger.LOGGER.error("Error: attributes: " + message);
    }

    public static void loadFromJson(JsonArray array) {
        for (var elem : array.asList()) {
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
        var attrInfo = ItemAttributeInfo.getAttributeStats(item);

        if (attrInfo == null) {
            logError("item with id " + id + " cannot have attribute modifiers applied: " + obj);
            return;
        }

        AttributeMod newMod = new AttributeMod(attrInfo);

        Consumer<AttributeModField> forEachField = (field) -> {
            var fieldName = field.field();
            if (!obj.has(fieldName)) {
                return;
            }
            var fieldElem = obj.get(fieldName);
            if (!fieldElem.isJsonObject()) {
                logError("field " + fieldName + " has non-object value: " + obj);
                return;
            }
            var fieldObj = fieldElem.getAsJsonObject();
            if (!fieldObj.has("type")) {
                logError("field " + fieldName + " does not have a type subfield: " + obj);
                return;
            }
            var typeElem = fieldObj.get("type");
            if (!(typeElem.isJsonPrimitive() && typeElem.getAsJsonPrimitive().isString())) {
                logError("field " + fieldName + " subfield type has non-string value: " + obj);
                return;
            }

            var type = typeElem.getAsJsonPrimitive().getAsString();
            switch (type) {
                case "set" -> {
                    if (!fieldObj.has("value")) {
                        logError("field " + fieldName + " with type set does not have a subfield value: " + obj);
                        return;
                    }
                    var valueObj = fieldObj.get("value");
                    if (!(valueObj.isJsonPrimitive() && valueObj.getAsJsonPrimitive().isNumber())) {
                        logError("field " + fieldName + " with type set has non-number value subfield: " + obj);
                        return;
                    }
                    var value = valueObj.getAsJsonPrimitive().getAsDouble();
                    field.setter().accept(newMod, new DoubleMod(DoubleMod.DoubleModMode.SET, value));
                }
                case "remove" -> {
                    field.setter().accept(newMod, new DoubleMod(DoubleMod.DoubleModMode.REMOVE, 0));
                }
            }
        };

        for (var field : fields) {
            forEachField.accept(field);
        }

        newMod.updateAttributes(attrInfo);

        AttributeManager.modList.put(item, newMod);
    }
}
