package com.skyblockutils;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.skyblockutils.config.ClothConfigHandler;
import com.skyblockutils.config.ModConfig;
import com.skyblockutils.features.DailyReminders;
import com.skyblockutils.features.glowingPlayers.GlowingPlayers;
import com.skyblockutils.features.glowingPlayers.GlowingPlayersGui;
import com.skyblockutils.features.NpcFinder;
import com.skyblockutils.features.dungeons.AutoRejoin;
import com.skyblockutils.features.party.PartyInviteNotifications;
import com.skyblockutils.features.slayer.CarryTracker;
import com.skyblockutils.features.slayer.SlayerTracker;
import com.skyblockutils.features.guild.GuildListParser;
import com.skyblockutils.features.party.PartyListParser;
import com.skyblockutils.hypixel.HypixelLocation;
import com.skyblockutils.features.hud.BestiaryHud;
import com.skyblockutils.features.hud.LogHud;
import com.skyblockutils.features.hud.layout.HudEditScreen;
import com.skyblockutils.utils.*;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public class ModCommands {

    private static final class OrderedSuggestion extends Suggestion {
        private final int order;

        OrderedSuggestion(StringRange range, String text, int order) {
            super(range, text);
            this.order = order;
        }

        @Override
        public int compareTo(Suggestion other) {
            return other instanceof OrderedSuggestion o ? Integer.compare(order, o.order) : super.compareTo(other);
        }

        @Override
        public int compareToIgnoreCase(Suggestion other) {
            return other instanceof OrderedSuggestion o ? Integer.compare(order, o.order) : super.compareToIgnoreCase(other);
        }

        @Override
        public Suggestion expand(String command, StringRange range) {
            if (range.equals(getRange())) return this;
            return new OrderedSuggestion(range, super.expand(command, range).getText(), order);
        }
    }

    private static CompletableFuture<Suggestions> orderedSuggestions(SuggestionsBuilder builder, Set<String> texts) {
        if (texts.isEmpty()) return Suggestions.empty();
        StringRange range = StringRange.between(builder.getStart(), builder.getInput().length());
        List<Suggestion> list = new ArrayList<>();
        int i = 0;
        for (String text : texts) list.add(new OrderedSuggestion(range, text, i++));
        return CompletableFuture.completedFuture(new Suggestions(range, list));
    }

    private static <S> LiteralCommandNode<S> quietLiteral(LiteralArgumentBuilder<S> builder) {
        LiteralCommandNode<S> built = builder.build();
        LiteralCommandNode<S> quiet = new LiteralCommandNode<>(built.getLiteral(), built.getCommand(), built.getRequirement(),
                built.getRedirect(), built.getRedirectModifier(), built.isFork()) {
            @Override
            public CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder suggestions) {
                return Suggestions.empty();
            }
        };
        for (CommandNode<S> child : built.getChildren()) quiet.addChild(child);
        return quiet;
    }

    private static Stream<String> fuzzyMatch(Stream<String> candidates, String remaining) {
        List<String> all = candidates.toList();
        String lower = remaining.toLowerCase();

        List<String> prefixMatches = all.stream()
                .filter(s -> s.toLowerCase().startsWith(lower))
                .toList();

        List<String> containsOnlyMatches = all.stream()
                .filter(s -> !s.toLowerCase().startsWith(lower) && s.toLowerCase().contains(lower))
                .toList();

        return Stream.concat(prefixMatches.stream(), containsOnlyMatches.stream());
    }

    @SuppressWarnings("unused")
    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandBuildContext registryAccess) {
        Minecraft client = Minecraft.getInstance();

        var command = ClientCommands.literal("ssu")
                .then(ClientCommands.literal("autorejoin")
                        .executes(context -> {
                            if (!AutoRejoin.autoRejoinEnabled) {
                                ModFunctions.sendSystemMessage("§cPlease specify a floor", false);
                                return 1;
                            }

                            AutoRejoin.autoRejoinEnabled = false;
                            AutoRejoin.currentFloor = "";
                            ModFunctions.sendSystemMessage("§cAuto-rejoin disabled", false);
                            ModConfig.save();
                            return 1;
                        })
                        .then(ClientCommands.argument("floor", StringArgumentType.string())
                                .suggests((ctx, builder) -> {
                                    String remaining = builder.getRemaining().toLowerCase();
                                    fuzzyMatch(
                                            Stream.of("off", "m1", "m2", "m3", "m4", "m5", "m6", "m7", "f1", "f2", "f3", "f4", "f5", "f6", "f7"),
                                            remaining
                                    ).forEach(builder::suggest);
                                    return builder.buildFuture();
                                })
                                .executes(context -> {
                                    String floor = StringArgumentType.getString(context, "floor");
                                    if (floor.equals("off")) {
                                        AutoRejoin.autoRejoinEnabled = false;
                                        AutoRejoin.currentFloor = "";
                                        ModFunctions.sendSystemMessage("§cAuto-rejoin disabled", false);
                                        ModConfig.save();
                                    } else if (floor.matches("^[mf][1-7]$")) {
                                        AutoRejoin.autoRejoinEnabled = true;
                                        AutoRejoin.currentFloor = floor.toUpperCase();
                                        ModFunctions.sendSystemMessage("§aAuto-rejoin enabled for " + floor.toUpperCase(), false);
                                        ModConfig.save();
                                    } else {
                                        ModFunctions.sendSystemMessage("§cInvalid floor! Use m1-m7 or f1-f7, or off to disable", false);
                                        return 0;
                                    }
                                    return 1;
                                })
                        )
                )
                .then(ClientCommands.literal("carry")
                        .then(quietLiteral(ClientCommands.literal("adjust")
                                .then(ClientCommands.argument("username", StringArgumentType.word())
                                        .suggests((ctx, builder) -> {
                                            fuzzyMatch(CarryTracker.activeTargets().stream(), builder.getRemaining()).forEach(builder::suggest);
                                            return builder.buildFuture();
                                        })
                                        .then(ClientCommands.literal("add")
                                                .executes(context -> {
                                                    CarryTracker.adjust(StringArgumentType.getString(context, "username"), 1);
                                                    return 1;
                                                })
                                                .then(ClientCommands.argument("count", IntegerArgumentType.integer(1))
                                                        .executes(context -> {
                                                            CarryTracker.adjust(StringArgumentType.getString(context, "username"), IntegerArgumentType.getInteger(context, "count"));
                                                            return 1;
                                                        })
                                                )
                                        )
                                        .then(ClientCommands.literal("remove")
                                                .executes(context -> {
                                                    CarryTracker.adjust(StringArgumentType.getString(context, "username"), -1);
                                                    return 1;
                                                })
                                                .then(ClientCommands.argument("count", IntegerArgumentType.integer(1))
                                                        .executes(context -> {
                                                            CarryTracker.adjust(StringArgumentType.getString(context, "username"), -IntegerArgumentType.getInteger(context, "count"));
                                                            return 1;
                                                        })
                                                )
                                        )
                                )
                        ))
                        .then(quietLiteral(ClientCommands.literal("stop")
                                .executes(context -> {
                                    CarryTracker.stop(null);
                                    return 1;
                                })
                                .then(ClientCommands.argument("username", StringArgumentType.word())
                                        .suggests((ctx, builder) -> {
                                            fuzzyMatch(CarryTracker.activeTargets().stream(), builder.getRemaining()).forEach(builder::suggest);
                                            return builder.buildFuture();
                                        })
                                        .executes(context -> {
                                            CarryTracker.stop(StringArgumentType.getString(context, "username"));
                                            return 1;
                                        })
                                )
                        ))
                        .then(ClientCommands.argument("username", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    String lower = builder.getRemaining().toLowerCase(Locale.ROOT);
                                    Set<String> texts = new LinkedHashSet<>();
                                    for (String literal : List.of("adjust", "stop")) {
                                        if (literal.startsWith(lower)) texts.add(literal);
                                    }
                                    var connection = client.getConnection();
                                    if (connection != null) {
                                        fuzzyMatch(
                                                connection.getOnlinePlayers().stream()
                                                        .map(entry -> entry.getProfile().name())
                                                        .filter(name -> name.matches("^[A-Za-z0-9_]{1,16}$")),
                                                builder.getRemaining()
                                        ).forEach(texts::add);
                                    }
                                    return orderedSuggestions(builder, texts);
                                })
                                .then(ClientCommands.argument("amount", IntegerArgumentType.integer(1))
                                        .then(ClientCommands.argument("price", StringArgumentType.greedyString())
                                                .executes(context -> {
                                                    CarryTracker.start(
                                                            StringArgumentType.getString(context, "username"),
                                                            IntegerArgumentType.getInteger(context, "amount"),
                                                            StringArgumentType.getString(context, "price")
                                                    );
                                                    return 1;
                                                })
                                        )
                                )
                        )
                )
                .then(ClientCommands.literal("slayer")
                        .then(ClientCommands.literal("reset").executes(context -> {
                            SlayerTracker.reset();
                            ModFunctions.sendSystemMessage("§aSlayer tracker session reset", false);
                            return 1;
                        }))
                )
                .then(ClientCommands.literal("hud").executes(context -> {
                    HudEditScreen.requested = true;
                    return 1;
                }))
                .then(ClientCommands.literal("config").executes(context -> {
                    ClothConfigHandler.configScreenRequested = true;
                    return 1;
                }))
                .then(ClientCommands.literal("glowingplayers")
                        .then(ClientCommands.literal("add")
                                .then(ClientCommands.argument("username", StringArgumentType.string())
                                        .suggests((ctx, builder) -> {
                                            var connection = client.getConnection();
                                            if (connection != null) {
                                                String remaining = builder.getRemaining();
                                                fuzzyMatch(
                                                        connection.getOnlinePlayers().stream()
                                                                .map(entry -> entry.getProfile().name()),
                                                        remaining
                                                ).forEach(builder::suggest);
                                            }
                                            return builder.buildFuture();
                                        })
                                        .executes(context -> {
                                            String name = StringArgumentType.getString(context, "username");
                                            GlowingPlayers.add(name, 0xFFAA00, false, null);
                                            return 1;
                                        })
                                        .then(ClientCommands.argument("color", StringArgumentType.string())
                                                .suggests((ctx, builder) -> {
                                                    String remaining = builder.getRemaining();
                                                    fuzzyMatch(
                                                            GlowingPlayers.MINECRAFT_COLORS.keySet().stream(),
                                                            remaining
                                                    ).forEach(builder::suggest);
                                                    return builder.buildFuture();
                                                })
                                                .executes(context -> {
                                                    String name = StringArgumentType.getString(context, "username");
                                                    String colorName = StringArgumentType.getString(context, "color").toUpperCase();
                                                    int hex = GlowingPlayers.MINECRAFT_COLORS.getOrDefault(colorName, 0xFFAA00);
                                                    GlowingPlayers.add(name, hex, false, null);
                                                    return 1;
                                                })
                                        )
                                )
                        )
                        .then(ClientCommands.literal("remove")
                                .then(ClientCommands.argument("username", StringArgumentType.string())
                                        .suggests((ctx, builder) -> {
                                            String remaining = builder.getRemaining();
                                            fuzzyMatch(
                                                    ModConfig.INSTANCE.getGlowingPlayers().stream()
                                                            .map(gp -> gp.username),
                                                    remaining
                                            ).forEach(builder::suggest);
                                            return builder.buildFuture();
                                        })
                                        .executes(context -> {
                                            String name = StringArgumentType.getString(context, "username");
                                            GlowingPlayers.remove(name, false);
                                            return 1;
                                        })
                                )
                        )
                        .then(ClientCommands.literal("clear")
                                .executes(context -> {
                                    GlowingPlayers.clearAll();
                                    ModFunctions.sendSystemMessage("§aCleared all glowing players.", false);
                                    return 1;
                                })
                        )
                        .then(ClientCommands.literal("list")
                                .executes(context -> {
                                    List<GlowingPlayers.GlowingPlayer> glowingPlayers = ModConfig.INSTANCE.getGlowingPlayers();
                                    if (glowingPlayers.isEmpty()) {
                                        ModFunctions.sendSystemMessage("§cYou didn't add any glowing players!", false);
                                        return 1;
                                    }
                                    StringBuilder players = new StringBuilder();
                                    for (GlowingPlayers.GlowingPlayer glowingPlayer : glowingPlayers) {
                                        players.append(glowingPlayer.username).append(", ");
                                    }
                                    ModFunctions.sendSystemMessage("§rThese players are glowing: \n" + players, false);
                                    return 1;
                                })
                        )
                        .then(ClientCommands.literal("gui")
                                .executes(context -> {
                                    GlowingPlayersGui.configScreenRequested = true;
                                    return 1;
                                })
                        )
                )
                .then(ClientCommands.literal("dev")
                        .then(ClientCommands.literal("partyscan").executes(context -> {
                            PartyListParser.devScan();
                            return 1;
                        }))
                        .then(ClientCommands.literal("guildscan").executes(context -> {
                            GuildListParser.devScan();
                            return 1;
                        }))
                        .then(ClientCommands.literal("bestiary").executes(context -> {
                            BestiaryHud.devCopy();
                            return 1;
                        }))
                        .then(ClientCommands.literal("loghud")
                                .executes(context -> {
                                    LogHud.toggle();
                                    return 1;
                                })
                                .then(ClientCommands.literal("copy").executes(context -> {
                                    LogHud.copy();
                                    return 1;
                                }))
                        )
                        .then(ClientCommands.literal("damagelog").executes(context -> {
                            DamageLogger.toggle();
                            return 1;
                        }))
                        .then(ClientCommands.literal("testnotification").executes(context -> {
                            OnScreenNotification.builder()
                                    .title("Test")
                                    .subtitle("THIS NOTIFICATION IS A TEST\nYou ran /ssu dev testNotification")
                                    .tickTime(100)
                                    .withSound(true)
                                    .send();
                            return 1;
                        }))
                        .then(ClientCommands.literal("teehee").executes(context -> {
                            ModFunctions.sendSystemMessage("§rNya! Mreow Mrpp Meow!", false);
                            return 1;
                        }))
                        .then(ClientCommands.literal("location").executes(context -> {
                            context.getSource().sendFeedback(Component.literal("§6" + SideBarUtils.location));
                            return 1;
                        }))
                        .then(ClientCommands.literal("hypixellocation").executes(context -> {
                            context.getSource().sendFeedback(Component.literal("§6" + HypixelLocation.describe()));
                            return 1;
                        }))
                        .then(ClientCommands.literal("generallocation").executes(context -> {
                            context.getSource().sendFeedback(Component.literal("§6" + ModFunctions.mapLocationToGeneralArea(SideBarUtils.location)));
                            return 1;
                        }))
                        .then(ClientCommands.literal("coordinates")
                                .then(ClientCommands.literal("add").executes(context -> {
                                    MarkCoordinates.addCoordinates();
                                    client.gui.hud.getChat().addClientSystemMessage(Component.literal("Coordinates added"));
                                    return 1;
                                }))
                                .then(ClientCommands.literal("log").executes(context -> {
                                    MarkCoordinates.logCoordinatesList();
                                    return 1;
                                }))
                                .then(ClientCommands.literal("clear").executes(context -> {
                                    MarkCoordinates.clearCoordinates();
                                    client.gui.hud.getChat().addClientSystemMessage(Component.literal("Coordinates cleared"));
                                    return 1;
                                }))
                        )
                        .then(ClientCommands.literal("title")
                                .executes(context -> {
                                    ModFunctions.showTitle(client, Component.literal("TEST TITLE").withColor(ModStyle.getColor(ModConfig.INSTANCE.colorStyle, ModStyle.ColorType.MAIN)), 20, true);
                                    return 1;
                                })
                        )
                ).then(ClientCommands.literal("npcfinder")
                        .then(ClientCommands.argument("npc", StringArgumentType.greedyString())
                                .suggests((ctx, builder) -> {
                                    String remaining = builder.getRemaining();
                                    fuzzyMatch(
                                            NpcFinder.allSkyblockNpcs.values().stream()
                                                    .map(NpcFinder.Npc::name)
                                                    .distinct(),
                                            remaining
                                    ).forEach(builder::suggest);
                                    return builder.buildFuture();
                                })
                                .executes(context -> {
                                    String name = StringArgumentType.getString(context, "npc");
                                    NpcFinder.handleCommand(name);
                                    return 1;
                                })
                        )
                ).then(ClientCommands.literal("partyinvites")
                        .executes(ctx -> {
                            List<String> previousInvites = PartyInviteNotifications.previousInvites;
                            if (!previousInvites.isEmpty()) {
                                ModFunctions.sendSystemMessage("Here are the last 10 received party invites (newest to oldest):", false);
                                List<String> lastTenInvites = new ArrayList<>(
                                        previousInvites.subList(Math.max(0, previousInvites.size() - 10), previousInvites.size())
                                                .stream()
                                                .map(String::trim)
                                                .toList()
                                );
                                Collections.reverse(lastTenInvites);
                                client.gui.hud.getChat().addClientSystemMessage(Component.literal(String.join(", ", lastTenInvites).trim()));
                            } else {
                                ModFunctions.sendSystemMessage("§cNobody has invited you to their party this session.", false);
                            }
                            return 1;
                        })
                )
                .then(ClientCommands.literal("disableReminder")
                        .then(ClientCommands.argument("type", StringArgumentType.string())
                                .suggests((ctx, builder) -> {
                                    String remaining = builder.getRemaining();
                                    fuzzyMatch(
                                            Arrays.stream(DailyReminders.ReminderType.values())
                                                    .map(Enum::name),
                                            remaining
                                    ).forEach(builder::suggest);
                                    return builder.buildFuture();
                                })
                                .executes(context -> {
                                    String typeArg = StringArgumentType.getString(context, "type").toUpperCase();
                                    try {
                                        DailyReminders.ReminderType type = DailyReminders.ReminderType.valueOf(typeArg);
                                        DailyReminders.disable(type);
                                        ModFunctions.sendSystemMessage("§cDisabled reminder: " + typeArg, false);
                                    } catch (IllegalArgumentException e) {
                                        ModFunctions.sendSystemMessage("§cUnknown reminder type: " + typeArg, false);
                                        return 0;
                                    }
                                    return 1;
                                })
                        )
                );

        var npcFinderCommand = ClientCommands.literal("snpc")
                .then(ClientCommands.argument("id", StringArgumentType.string())
                        .executes(context -> {
                            NpcFinder.handleCallback(StringArgumentType.getString(context, "id"));
                            return 1;
                        })
                );

        dispatcher.register(command);
        dispatcher.register(ClientCommands.literal("strayerskyblockutils").redirect(command.build()));
        dispatcher.register(npcFinderCommand);
    }
}