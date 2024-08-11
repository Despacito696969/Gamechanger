package org.despacito696969.gamechanger;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.mininglevel.v1.MiningLevelManager;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.apache.logging.log4j.util.TriConsumer;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class Gamechanger implements ModInitializer {
    public static final String MOD_ID = "gamechanger";
    public static final String CONFIG_FILE_NAME = "gamechanger.json";

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    /**
     * Runs the mod initializer.
     */
    @Override
    public void onInitialize() {
        var itemCommandGet = literal("get")
            .executes(
                context -> {
                    var entity = context.getSource().getEntity();
                    if (entity == null) {
                        return 0;
                    }
                    if (entity instanceof LivingEntity livingEntity) {
                        var item = livingEntity.getMainHandItem().getItem();
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
                    else {
                        return 0;
                    }
                }
            );

            var itemCommandModify =
                literal("modify")
                .then(applyModifierInt(literal("level"), (ctx, mod, value) ->
                    applyToItemTier(ctx, (item, tier) -> tier.levelFn.setModifier(mod, value))
                ))
                .then(applyModifierInt(literal("enchantment_value"), (ctx, mod, value) ->
                    applyToItemTier(ctx, (item, tier) -> tier.levelFn.setModifier(mod, value))
                ))
                .then(applyModifierFloat(literal("speed"), (ctx, mod, value) ->
                    applyToItemTier(ctx, (item, tier) -> {
                        tier.speedFn.setModifier(mod, value);
                        if (item instanceof DiggerItem diggerItem) {
                            diggerItem.speed = tier.getSpeed();
                        }
                    })
                ))
                .then(applyModifierFloat(literal("attackDamage"), (ctx, mod, value) ->
                    applyToItemTier(ctx, (item, tier) -> {
                        if (item instanceof DiggerItem diggerItem) {
                            var loc = BuiltInRegistries.ITEM.getKey(item);
                            if (mod == ModifiedTier.Modifier.NONE) {
                                var modifier = TierManager.defaultAttackDamage.get(loc);
                                if (modifier == null) {
                                    return;
                                }
                                diggerItem.defaultModifiers = replaceAttackDamage(diggerItem.defaultModifiers, loc, modifier.attribute(), 0);
                                diggerItem.attackDamageBaseline = modifier.attackDamageBaseline();

                                TierManager.defaultAttackDamage.remove(loc);
                            }
                            else if (mod == ModifiedTier.Modifier.SET) {
                                diggerItem.defaultModifiers = replaceAttackDamage(
                                    diggerItem.defaultModifiers,
                                    loc,
                                    new AttributeModifier(
                                        Item.BASE_ATTACK_DAMAGE_UUID,
                                        "Tool modifier",
                                        (double)value - 1.0f,
                                        AttributeModifier.Operation.ADDITION
                                    ),
                                    diggerItem.attackDamageBaseline
                                );
                                diggerItem.attackDamageBaseline = value;
                            }
                            else {
                                ctx.getSource().sendFailure(Component.literal(mod.name() + " is not supported"));
                            }
                        }
                    })
                ))
                .then(applyModifierInt(literal("uses"), (ctx, mod, value) ->
                        applyToItemTier(ctx, (item, tier) -> tier.usesFn.setModifier(mod, value))
                ));
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
                            return 1;
                        })
                    ))
                )
                .then(literal("unedible").executes(ctx -> {
                    var item = getItem(ctx);
                    if (item == null) {
                        return 0;
                    }
                    FoodManager.removeFoodProperties(item);
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
                    }))
                )
                .then(literal("explosion_resistance")
                    .executes(ctx -> executeForBlock(ctx, (block) -> {
                        var props = BlockPropertiesManager.propMods.get(block);
                        if (props == null) {
                            return;
                        }
                        props.explosionResistance = null;
                    }))
                )
                .then(literal("mining_level")
                    .executes(ctx -> executeForBlock(ctx, (block) -> {
                        var props = BlockPropertiesManager.propMods.get(block);
                        if (props == null) {
                            return;
                        }
                        props.miningLevel = null;
                    }))
                )
            );


        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(literal("gamechanger")
            .requires(source -> source.hasPermission(2))
            .then(literal("tools").then(itemCommandGet).then(itemCommandModify))
            .then(blockCommand)
            .then(foodCommand)
        ));

        ServerLifecycleEvents.SERVER_STARTING.register((server) -> {
            Path configPath = FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE_NAME);

            FoodManager.foodMods = new HashMap<>();
            BlockPropertiesManager.propMods = new IdentityHashMap<>();

            try (var reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
                var element = JsonParser.parseReader(reader);
                if (!element.isJsonObject()) {
                    LOGGER.warn("Config does not contain a json object");
                    return;
                }
                var mainObject = element.getAsJsonObject();
                if (mainObject.has("food")) {
                    var foodObject = mainObject.get("food");
                    if (foodObject.isJsonArray()) {
                        var foods = foodObject.getAsJsonArray();
                        FoodManager.loadFromJson(foods);
                    }
                    else {
                        LOGGER.warn("Config: food field should contain an array");
                    }
                }
                else {
                    LOGGER.warn("Config: no food field");
                }

                if (mainObject.has("blocks")) {
                    var blocksObject = mainObject.get("blocks");
                    if (blocksObject.isJsonArray()) {
                        var blocks = blocksObject.getAsJsonArray();
                        BlockPropertiesManager.loadFromJson(blocks);
                    }
                    else {
                        LOGGER.warn("Config: blocks field should contain an array");
                    }
                }
                else {
                    LOGGER.warn("Config: no blocks field");
                }
            }
            catch (IOException exception) {
                LOGGER.error("Error while reading config: " + exception);
            }
        });

        ServerLifecycleEvents.SERVER_STOPPING.register((server) -> {
            var toSave = new JsonObject();
            toSave.add("food", FoodManager.saveToJson());
            toSave.add("blocks", BlockPropertiesManager.saveToJson());

            Path configPath = FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE_NAME);
            try (var writer = Files.newBufferedWriter(configPath, StandardCharsets.UTF_8)) {
                writer.write(toSave.toString());
            }
            catch (IOException exception) {
                LOGGER.error("Error while writing config: " + exception);
            }
        });
    }
    // "Tool modifier"
    public static ImmutableMultimap<Attribute, AttributeModifier> replaceAttackDamage(
        Multimap<Attribute, AttributeModifier> attributes,
        ResourceLocation loc,
        AttributeModifier mod,
        float attackDamageBaseline
    ) {
        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        for (var entry : attributes.entries()) {
            if (entry.getValue().getId().equals(Item.BASE_ATTACK_DAMAGE_UUID)) {
                TierManager.defaultAttackDamage.putIfAbsent(
                    loc,
                    new TierManager.DefaultAttack(entry.getValue(), attackDamageBaseline)
                );
                builder.put(
                    Attributes.ATTACK_DAMAGE, mod
                );
            }
            else {
                builder.put(entry.getKey(), entry.getValue());
            }
        }
        return builder.build();
    }

    public static LiteralArgumentBuilder<CommandSourceStack> applyModifierInt(LiteralArgumentBuilder<CommandSourceStack> val, TriConsumer<CommandContext<CommandSourceStack>, ModifiedTier.Modifier, Integer> consumer) {
        var type = IntegerArgumentType.integer();
        return val
            .then(literal("none").executes(context -> {
                consumer.accept(context, ModifiedTier.Modifier.NONE, 0);
                return 1;
            }))
            .then(literal("set").then(argument("value", type)
                .executes(
                    context -> {
                        final int value = IntegerArgumentType.getInteger(context, "value");
                        consumer.accept(context, ModifiedTier.Modifier.SET, value);
                        return 1;
                    }
                )
            ))
            .then(literal("add").then(argument("value", type)
                .executes(
                    context -> {
                        final int value = IntegerArgumentType.getInteger(context, "value");
                        consumer.accept(context, ModifiedTier.Modifier.ADD, value);
                        return 1;
                    }
                )
            ))
            .then(literal("multiply").then(argument("value", type)
                .executes(
                    context -> {
                        final int value = IntegerArgumentType.getInteger(context, "value");
                        consumer.accept(context, ModifiedTier.Modifier.MULTIPLY, value);
                        return 1;
                    }
                )
            ));
    }

    public static LiteralArgumentBuilder<CommandSourceStack> applyModifierFloat(LiteralArgumentBuilder<CommandSourceStack> val, TriConsumer<CommandContext<CommandSourceStack>, ModifiedTier.Modifier, Float> consumer) {
        var type = FloatArgumentType.floatArg();
        return val
            .then(literal("none").executes(context -> {
                consumer.accept(context, ModifiedTier.Modifier.NONE, 0.0f);
                return 1;
            }))
            .then(literal("set").then(argument("value", type)
                .executes(
                    context -> {
                        final float value = FloatArgumentType.getFloat(context, "value");
                        consumer.accept(context, ModifiedTier.Modifier.SET, value);
                        return 1;
                    }
                )
            ))
            .then(literal("add").then(argument("value", type)
                .executes(
                    context -> {
                        final float value = FloatArgumentType.getFloat(context, "value");
                        consumer.accept(context, ModifiedTier.Modifier.ADD, value);
                        return 1;
                    }
                )
            ))
            .then(literal("multiply").then(argument("value", type)
                .executes(
                    context -> {
                        final float value = FloatArgumentType.getFloat(context, "value");
                        consumer.accept(context, ModifiedTier.Modifier.MULTIPLY, value);
                        return 1;
                    }
                )
            ));
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

    public static int applyToItemTier(CommandContext<CommandSourceStack> ctx, BiConsumer<TieredItem, ModifiedTier> tieredItemConsumer) {
        var item = getItem(ctx);
        if (!(item instanceof TieredItem tieredItem)) {
            return 0;
        }
        var resource = BuiltInRegistries.ITEM.getKey(item);
        var tier = TierManager.tierOverrides.get(resource);
        if (tier == null) {
            tier = new ModifiedTier(tieredItem.getTier());
            TierManager.tierOverrides.put(resource, tier);
        }
        tieredItemConsumer.accept(tieredItem, tier);
        return 1;
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
}
