package org.despacito696969.gamechanger;

import com.google.common.collect.Multimap;
import com.google.gson.*;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.mininglevel.v1.MiningLevelManager;
import net.fabricmc.fabric.api.networking.v1.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class Gamechanger implements ModInitializer {
    public static final String MOD_ID = "gamechanger";
    public static final String CONFIG_FILE_NAME = "gamechanger.json";

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public record ManagerConfigFields(String field, Consumer<JsonArray> loader, Supplier<JsonArray> saver) {}

    public static ResourceLocation CONFIG_SYNC_PACKET_ID = new ResourceLocation(MOD_ID, "config_sync");

    public static ManagerConfigFields[] managers = new ManagerConfigFields[] {
        new ManagerConfigFields("food", FoodManager::loadFromJson, FoodManager::saveToJson),
        new ManagerConfigFields("blocks", BlockPropertiesManager::loadFromJson, BlockPropertiesManager::saveToJson),
        new ManagerConfigFields("attributes", AttributeManager::loadFromJson, AttributeManager::saveToJson),
        new ManagerConfigFields("tools", TierManager::loadFromJson, TierManager::saveToJson),
    };

    @Override
    public void onInitialize() {
        registerGamechangerCommand();
        registerServerLifecycleEvents();
        registerPackets();
    }

    public void registerGamechangerCommand() {
        // This language is an embodiment of C418 - 13
        record AttrFieldPair(
            String name,
            BiConsumer<AttributeMod, AttributeMod.DoubleMod> setter
        ) {}

        ArrayList<AttrFieldPair> attrFields = new ArrayList<>();
        attrFields.add(new AttrFieldPair("attack_damage", (v, w) -> v.attackDamage = w));
        attrFields.add(new AttrFieldPair("attack_speed", (v, w) -> v.attackSpeed = w));
        attrFields.add(new AttrFieldPair("speed", (v, w) -> v.speed = w));
        attrFields.add(new AttrFieldPair("max_health", (v, w) -> v.maxHealth = w));
        attrFields.add(new AttrFieldPair("armor", (v, w) -> v.armor = w));
        attrFields.add(new AttrFieldPair("toughness", (v, w) -> v.toughness = w));
        attrFields.add(new AttrFieldPair("knockback_resistance", (v, w) -> v.knockbackResistance = w));

        var listCommand = literal("list")
            .then(literal("unedited_foods")
                .executes(ctx -> {
                    BuiltInRegistries.ITEM.forEach((item) -> {
                        var resource = BuiltInRegistries.ITEM.getKey(item);

                        if (item.foodProperties != null && !FoodManager.foodMods.containsKey(item)) {
                            ctx.getSource().sendSuccess(() -> Component.literal(resource.toString()), true);
                        }
                    });
                    return 1;
                })
            );

        var attributeSet = literal("set");

        for (var field : attrFields) {
            attributeSet = attributeSet.then(literal(field.name)
                .then(argument("value", FloatArgumentType.floatArg())
                    .executes(ctx -> {
                        var item = getItem(ctx);
                        if (item == null) {
                            return 0;
                        }
                        var stats = ItemAttributeInfo.getAttributeStats(item);
                        if (stats == null) {
                            return 0;
                        }
                        var attributeMod = AttributeManager.getOrCreateAttributeManagerEntry(stats);
                        var value = (double)FloatArgumentType.getFloat(ctx, "value");
                        field.setter.accept(
                            attributeMod,
                            new AttributeMod.DoubleMod(AttributeMod.DoubleMod.DoubleModMode.SET, value)
                        );
                        attributeMod.updateAttributes(stats);
                        updateConfig(ctx);
                        return 1;
                    })
                )
            );
        }

        var attributeRemove = literal("remove");

        for (var field : attrFields) {
            attributeRemove = attributeRemove.then(literal(field.name)
                .executes(ctx -> {
                    var item = getItem(ctx);
                    if (item == null) {
                        return 0;
                    }
                    var stats = ItemAttributeInfo.getAttributeStats(item);
                    if (stats == null) {
                        return 0;
                    }
                    var attributeMod = AttributeManager.getOrCreateAttributeManagerEntry(stats);
                    field.setter.accept(
                        attributeMod,
                        new AttributeMod.DoubleMod(AttributeMod.DoubleMod.DoubleModMode.REMOVE, 0)
                    );
                    attributeMod.updateAttributes(stats);
                    updateConfig(ctx);
                    return 1;
                })
            );
        }

        var attributeClear = literal("clear");

        for (var field : attrFields) {
            attributeClear = attributeClear.then(literal(field.name)
                .executes(ctx -> {
                    var item = getItem(ctx);
                    if (item == null) {
                        return 0;
                    }
                    var stats = ItemAttributeInfo.getAttributeStats(item);
                    if (stats == null) {
                        return 0;
                    }
                    var attributeMod = AttributeManager.getOrCreateAttributeManagerEntry(stats);
                    field.setter.accept(
                        attributeMod,
                        null
                    );
                    attributeMod.updateAttributes(stats);
                    updateConfig(ctx);
                    return 1;
                })
            );
        }

        var attributeCommand = literal("attributes")
            .then(literal("get")
                .executes(ctx -> {
                    var item = getItem(ctx);
                    if (item == null) {
                        return 0;
                    }

                    Consumer<Multimap<Attribute, AttributeModifier>> printAttributes = (attribute_mods) -> {
                        var resource = BuiltInRegistries.ITEM.getKey(item);
                        var result = Component.literal(resource.toString());
                        for (var entry : attribute_mods.entries()) {
                            result.append("\n");
                            result.append(Component.literal(entry.getKey().getDescriptionId()));
                            result.append("\n");
                            var modifier = entry.getValue();
                            result.append(Component.literal("Name: " + modifier.getName()));
                            result.append("\n");
                            result.append(Component.literal("Operation: " + modifier.getOperation().toValue()));
                            result.append("\n");
                            result.append(Component.literal("Amount: " + modifier.getAmount()));
                            result.append("\n");
                            result.append(Component.literal("Id: " + modifier.getId()));
                            result.append("\n");
                        }
                        ctx.getSource().sendSuccess(() -> result, true);
                    };

                    var stats = ItemAttributeInfo.getAttributeStats(item);
                    if (stats == null) {
                        return 0;
                    }

                    printAttributes.accept(stats.currentAttributes);
                    return 1;
                })
            )
            .then(literal("modify")
                .then(attributeSet)
                .then(attributeRemove)
                .then(attributeClear)
            );

        var itemCommand = literal("tools")
            .then(literal("get")
                .executes(
                    context -> {
                        var item = getItem(context);
                        if (item == null) {
                            return 0;
                        }

                        var resource = BuiltInRegistries.ITEM.getKey(item);
                        var result = Component.literal(resource.toString());
                        result.append("\n");
                        if (item instanceof TieredItem tieredItem) {
                            record Pair(String name, String value) {}
                            var tier = tieredItem.getTier();
                            var list = new Pair[]{
                                new Pair("Uses", Integer.toString(tier.getUses())),
                                new Pair("Speed", Float.toString(tier.getSpeed())),
                                new Pair("Attack Damage Bonus", Float.toString(tier.getAttackDamageBonus())),
                                new Pair("Level", Integer.toString(tier.getLevel())),
                                new Pair("Enchantment Value", Integer.toString(tier.getEnchantmentValue())),
                            };

                            for (var e : list) {
                                result.append(Component.literal(e.name() + ": " + e.value() + "\n"));
                            }
                        }
                        else {
                            result.append(Component.literal("Not a tiered item!"));
                        }

                        context.getSource().sendSuccess(() -> result, true);
                        return 1;
                    }
                )
            )
            .then(literal("set")
                .then(literal("level")
                    .then(tierSetInteger((tier, value) -> tier.level = value))
                )
                .then(literal("uses")
                    .then(tierSetInteger((tier, value) -> tier.uses = value))
                )
                .then(literal("enchantment_value")
                    .then(tierSetInteger((tier, value) -> tier.enchantmentValue = value))
                )
                .then(literal("speed")
                    .then(tierSetFloat((tier, value) -> tier.speed = value))
                )
                .then(literal("attack_damage")
                    .then(tierSetFloat((tier, value) -> tier.attackDamageBonus = value - 1))
                )
            )
            .then(literal("clear")
                .then(literal("level")
                    .executes(ctx -> tierSetNone(ctx, (tier) -> tier.level = null))
                )
                .then(literal("uses")
                    .executes(ctx -> tierSetNone(ctx, (tier) -> tier.uses = null))
                )
                .then(literal("enchantment_value")
                    .executes(ctx -> tierSetNone(ctx, (tier) -> tier.enchantmentValue = null))
                )
                .then(literal("speed")
                    .executes(ctx -> tierSetNone(ctx, (tier) -> tier.speed = null))
                )
                .then(literal("attack_damage")
                    .executes(ctx -> tierSetNone(ctx, (tier) -> tier.attackDamageBonus = null))
                )
            );

        var foodCommand = literal("food")
            .then(literal("set")
                .then(literal("nutrition").then(argument("value", IntegerArgumentType.integer())
                    .executes(ctx -> {
                        var item = getItem(ctx);
                        if (item == null) {
                            return 0;
                        }
                        var props = FoodManager.getOrCreateFoodProperties(item);
                        props.nutritionOpt = IntegerArgumentType.getInteger(ctx, "value");
                        updateConfig(ctx);
                        return 1;
                    })
                ))
                .then(literal("saturation").then(argument("value", FloatArgumentType.floatArg())
                    .executes(ctx -> {
                        var item = getItem(ctx);
                        if (item == null) {
                            return 0;
                        }
                        var props = FoodManager.getOrCreateFoodProperties(item);
                        props.saturationModifierOpt = FloatArgumentType.getFloat(ctx, "value");
                        updateConfig(ctx);
                        return 1;
                    })
                ))
                .then(literal("is_meat").then(argument("value", BoolArgumentType.bool())
                    .executes(ctx -> {
                        var item = getItem(ctx);
                        if (item == null) {
                            return 0;
                        }
                        var props = FoodManager.getOrCreateFoodProperties(item);
                        props.isMeatOpt = BoolArgumentType.getBool(ctx, "value");
                        updateConfig(ctx);
                        return 1;
                    })
                ))
                .then(literal("can_always_eat").then(argument("value", BoolArgumentType.bool())
                    .executes(ctx -> {
                        var item = getItem(ctx);
                        if (item == null) {
                            return 0;
                        }
                        var props = FoodManager.getOrCreateFoodProperties(item);
                        props.canAlwaysEatOpt = BoolArgumentType.getBool(ctx, "value");
                        updateConfig(ctx);
                        return 1;
                    })
                ))
                .then(literal("is_fast_food").then(argument("value", BoolArgumentType.bool())
                    .executes(ctx -> {
                        var item = getItem(ctx);
                        if (item == null) {
                            return 0;
                        }
                        var props = FoodManager.getOrCreateFoodProperties(item);
                        props.isFastFoodOpt = BoolArgumentType.getBool(ctx, "value");
                        updateConfig(ctx);
                        return 1;
                    })
                ))
            )
            .then(literal("inedible").executes(ctx -> {
                var item = getItem(ctx);
                if (item == null) {
                    return 0;
                }
                FoodManager.removeFoodProperties(item);
                updateConfig(ctx);
                return 1;
            }))
            .then(literal("reset").executes(
                ctx -> applyToItem(ctx, FoodManager::clearMods)
            ));
        var blockCommand = literal("blocks")
            .then(literal("get")
                .executes(ctx ->
                    executeForBlock(ctx, (block) -> {
                        var hardness = block.defaultDestroyTime();
                        var mod = BlockPropertiesManager.propMods.get(block);
                        if (mod != null) {
                            if (mod.destroyTime != null) {
                                hardness = mod.destroyTime;
                            }
                        }
                        final var explosionResistance = block.getExplosionResistance();
                        final var finalHardness = hardness;
                        final var miningLevel = MiningLevelManager.getRequiredMiningLevel(block.defaultBlockState());
                        ctx.getSource().sendSuccess(
                            () -> Component.literal(
                                BuiltInRegistries.BLOCK.getKey(block) + "\n"
                                    + "hardness: " + finalHardness + "\n"
                                    + "explosionResistance: " + explosionResistance + "\n"
                                    + "miningLevel: " + miningLevel
                            ), false
                        );
                    })
                )
            )
            .then(literal("set")
                .then(literal("hardness")
                    .then(argument("value", FloatArgumentType.floatArg())
                        .executes(
                            ctx -> executeForBlock(ctx, (block) -> {
                                var props = BlockPropertiesManager.getOrCreateProperties(block);
                                props.destroyTime = FloatArgumentType.getFloat(ctx, "value");
                                updateConfig(ctx);
                            })
                        )
                    )
                )
                .then(literal("explosion_resistance")
                    .then(argument("value", FloatArgumentType.floatArg())
                        .executes(
                            ctx -> executeForBlock(ctx, (block) -> {
                                var props = BlockPropertiesManager.getOrCreateProperties(block);
                                props.explosionResistance = FloatArgumentType.getFloat(ctx, "value");
                                updateConfig(ctx);
                            })
                        )
                    )
                )
                .then(literal("mining_level")
                    .then(argument("value", IntegerArgumentType.integer())
                        .executes(
                            ctx -> executeForBlock(ctx, (block) -> {
                                var props = BlockPropertiesManager.getOrCreateProperties(block);
                                props.miningLevel = IntegerArgumentType.getInteger(ctx, "value");
                                updateConfig(ctx);
                            })
                        )
                    )
                )
            )
            .then(literal("clear")
                .then(literal("hardness")
                    .executes(ctx -> executeForBlock(ctx, (block) -> {
                        var props = BlockPropertiesManager.propMods.get(block);
                        if (props == null) {
                            return;
                        }
                        props.destroyTime = null;
                        updateConfig(ctx);
                    }))
                )
                .then(literal("explosion_resistance")
                    .executes(ctx -> executeForBlock(ctx, (block) -> {
                        var props = BlockPropertiesManager.propMods.get(block);
                        if (props == null) {
                            return;
                        }
                        props.explosionResistance = null;
                        updateConfig(ctx);
                    }))
                )
                .then(literal("mining_level")
                    .executes(ctx -> executeForBlock(ctx, (block) -> {
                        var props = BlockPropertiesManager.propMods.get(block);
                        if (props == null) {
                            return;
                        }
                        props.miningLevel = null;
                        updateConfig(ctx);
                    }))
                )
            );

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(literal("gamechanger")
            .requires(source -> source.hasPermission(2))
            .then(itemCommand)
            .then(blockCommand)
            .then(foodCommand)
            .then(attributeCommand)
            .then(listCommand)
        ));
    }

    public void registerServerLifecycleEvents() {
        ServerLifecycleEvents.SERVER_STARTING.register((server) -> {
            Path configPath = FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE_NAME);

            resetManagers();

            try (var reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
                var element = JsonParser.parseReader(reader);
                loadFromJson(element);
            }
            catch (IOException exception) {
                LOGGER.error("Error while reading config: " + exception);
            }
        });

        ServerLifecycleEvents.SERVER_STOPPING.register((server) -> {
            var toSave = saveToJson();

            Path configPath = FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE_NAME);
            try (var writer = Files.newBufferedWriter(configPath, StandardCharsets.UTF_8)) {
                writer.write(toSave.toString());
            }
            catch (IOException exception) {
                LOGGER.error("Error while writing config: " + exception);
            }
        });
    }

    public void registerPackets() {
        ServerPlayConnectionEvents.JOIN.register(
            (handler, sender, server) -> {
                var str = saveToJson().toString();
                FriendlyByteBuf packet = PacketByteBufs.create();
                packet.writeUtf(str);
                sender.sendPacket(
                    CONFIG_SYNC_PACKET_ID,
                    packet
                );
            }
        );
    }

    public static void updateConfig(CommandContext<CommandSourceStack> ctx) {
        var players = PlayerLookup.all(ctx.getSource().getServer());
        for (var player : players) {
            var str = saveToJson().toString();
            FriendlyByteBuf packet = PacketByteBufs.create();
            packet.writeUtf(str);
            ServerPlayNetworking.send(player, CONFIG_SYNC_PACKET_ID, packet);
        }
    }

    public static JsonObject saveToJson() {
        var toSave = new JsonObject();
        for (var field : managers) {
            toSave.add(field.field(), field.saver.get());
        }
        return toSave;
    }

    public static void resetManagers() {
        FoodManager.foodMods = new IdentityHashMap<>();
        BlockPropertiesManager.propMods = new IdentityHashMap<>();
        AttributeManager.modList = new IdentityHashMap<>();
        TierManager.tierOverrides = new IdentityHashMap<>();
    }

    public static void loadFromJson(JsonElement element) {
        if (!element.isJsonObject()) {
            LOGGER.warn("Config does not contain a json object");
            return;
        }
        var mainObject = element.getAsJsonObject();

        for (var field : managers) {
            var fieldName = field.field();
            if (mainObject.has(fieldName)) {
                var subObj = mainObject.get(fieldName);
                if (subObj.isJsonArray()) {
                    var arr = subObj.getAsJsonArray();
                    field.loader.accept(arr);
                }
                else {
                    LOGGER.warn("Config: " + fieldName + " field should contain an array");
                }
            }
            else {
                LOGGER.warn("Config: no " + fieldName + " field");
            }
        }
    }

    @Nullable
    public static Item getItem(CommandContext<CommandSourceStack> ctx) {
        var entity = ctx.getSource().getEntity();
        if (entity == null) {
            return null;
        }
        if (!(entity instanceof LivingEntity livingEntity)) {
            return null;
        }
        var item = livingEntity.getMainHandItem().getItem();
        if (item == Items.AIR) {
            return null;
        }
        return item;
    }

    public static int applyToItem(CommandContext<CommandSourceStack> ctx, Consumer<Item> itemConsumer) {
        var item = getItem(ctx);
        if (item == Items.AIR) {
            return 0;
        }
        itemConsumer.accept(item);
        return 1;
    }

    public static int executeForBlock(CommandContext<CommandSourceStack> ctx, Consumer<Block> blockConsumer) {
        if (!(ctx.getSource().getEntity() instanceof Player player)) {
            return 0;
        }
        var pos = player.blockPosition().below();
        var block = player.level().getBlockState(pos).getBlock();
        if (block == Blocks.AIR) {
            return 0;
        }

        blockConsumer.accept(block);
        return 1;
    }

    public static RequiredArgumentBuilder<CommandSourceStack, Integer> tierSetInteger(
        BiConsumer<TierMod, Integer> consumer
    ) {
        return argument("value", IntegerArgumentType.integer()).executes(ctx -> {
            var item = getItem(ctx);
            if (!(item instanceof TieredItem tieredItem)) {
                return 0;
            }
            var tier = TierManager.getOrCreateTier(tieredItem);
            var value = IntegerArgumentType.getInteger(ctx, "value");
            consumer.accept(tier, value);
            updateConfig(ctx);
            return 1;
        });
    }

    public static RequiredArgumentBuilder<CommandSourceStack, Float> tierSetFloat(
        BiConsumer<TierMod, Float> consumer
    ) {
        return argument("value", FloatArgumentType.floatArg()).executes(ctx -> {
            var item = getItem(ctx);
            if (!(item instanceof TieredItem tieredItem)) {
                var loc = BuiltInRegistries.ITEM.getKey(item);
                ctx.getSource().sendFailure(Component.literal(loc + " is not tiered item!"));
                return 0;
            }
            var tier = TierManager.getOrCreateTier(tieredItem);
            var value = FloatArgumentType.getFloat(ctx, "value");
            consumer.accept(tier, value);
            updateConfig(ctx);
            return 1;
        });
    }

    public static int tierSetNone(
        CommandContext<CommandSourceStack> ctx,
        Consumer<TierMod> consumer
    ) {
        var item = getItem(ctx);
        if (!(item instanceof TieredItem tieredItem)) {
            var loc = BuiltInRegistries.ITEM.getKey(item);
            ctx.getSource().sendFailure(Component.literal(loc + " is not tiered item!"));
            return 0;
        }
        var tier = TierManager.getOrCreateTier(tieredItem);
        consumer.accept(tier);
        updateConfig(ctx);
        return 1;
    }
}
