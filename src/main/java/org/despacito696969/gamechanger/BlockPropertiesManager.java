package org.despacito696969.gamechanger;

import com.google.gson.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.IdentityHashMap;

import static org.despacito696969.gamechanger.Gamechanger.LOGGER;

public class BlockPropertiesManager {
    public static IdentityHashMap<Block, BlockPropertiesMod> propMods = new IdentityHashMap<>();

    public static BlockPropertiesMod getOrCreateProperties(Block block) {
        var props_1 = propMods.get(block);
        if (props_1 != null) {
            return props_1;
        }

        var props_2 = new BlockPropertiesMod();
        BlockPropertiesManager.propMods.put(block, props_2);
        return props_2;
    }

    public static JsonElement saveToJson() {
        var result = new JsonArray();
        for (var entry : propMods.entrySet()) {
            var obj = new JsonObject();
            var props = entry.getValue();
            if (props.miningLevel != null) {
                obj.addProperty("mining_level", props.miningLevel);
            }
            if (props.destroyTime != null) {
                obj.addProperty("hardness", props.destroyTime);
            }
            if (props.explosionResistance != null) {
                obj.addProperty("explosion_resistance", props.explosionResistance);
            }
            if (obj.size() == 0) {
                continue;
            }
            var id = BuiltInRegistries.BLOCK.getKey(entry.getKey()).toString();
            obj.addProperty("id", id);
            result.add(obj);
        }
        return result;
    }

    public static void loadFromJson(JsonArray blocks) {
        for (var e : blocks) {
            if (!e.isJsonObject()) {
                LOGGER.warn("Config: block contains not an object: " + e);
                continue;
            }
            var blockObj = e.getAsJsonObject();
            if (!blockObj.has("id")) {
                LOGGER.warn("Config: block doesn't contain id: " + e);
                continue;
            }
            var jsonId = blockObj.get("id");
            if (!(jsonId.isJsonPrimitive() && jsonId.getAsJsonPrimitive().isString())) {
                LOGGER.warn("Config: block doesn't contain string in id field: " + e);
                continue;
            }
            var id = jsonId.getAsJsonPrimitive().getAsString();

            var loc = new ResourceLocation(id);
            var block = BuiltInRegistries.BLOCK.get(loc);
            if (block == Blocks.AIR) {
                LOGGER.warn("Config: block has id of not existing block: " + id);
                continue;
            }

            var props = getOrCreateProperties(block);

            if (blockObj.has("mining_level")) {
                var obj = blockObj.get("mining_level");
                if (obj.isJsonPrimitive() && obj.getAsJsonPrimitive().isNumber()) {
                    var value = obj.getAsJsonPrimitive().getAsNumber();
                    props.miningLevel = value.intValue();
                }
                else {
                    LOGGER.warn("Config: blocks: mining_level doesn't contain a Number: " + e);
                }
            }

            if (blockObj.has("hardness")) {
                var obj = blockObj.get("hardness");
                if (obj.isJsonPrimitive() && obj.getAsJsonPrimitive().isNumber()) {
                    var value = obj.getAsJsonPrimitive().getAsNumber();
                    props.destroyTime = value.floatValue();
                }
                else {
                    LOGGER.warn("Config: blocks: hardness doesn't contain a Number: " + e);
                }
            }

            if (blockObj.has("explosion_resistance")) {
                var obj = blockObj.get("explosion_resistance");
                if (obj.isJsonPrimitive() && obj.getAsJsonPrimitive().isNumber()) {
                    var value = obj.getAsJsonPrimitive().getAsNumber();
                    props.explosionResistance = value.floatValue();
                }
                else {
                    LOGGER.warn("Config: blocks: explosion_resistance doesn't contain a Number: " + e);
                }
            }
        }
    }
}
