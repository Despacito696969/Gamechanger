package org.despacito696969.gamechanger;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import org.apache.logging.log4j.util.TriConsumer;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class Gamechanger implements ModInitializer {
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
                // .then(literal("set"))

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(literal("gamechanger")
            .requires(source -> source.hasPermission(2))
            .then(literal("tools").then(itemCommandGet).then(itemCommandModify))
            .then(foodCommand)
        ));
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
}
