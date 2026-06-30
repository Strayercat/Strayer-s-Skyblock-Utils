package com.skyblockutils;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.skyblockutils.config.ClothConfigHandler;
import com.skyblockutils.config.ModConfig;
import com.skyblockutils.features.glowingPlayers.GlowingPlayers;
import com.skyblockutils.features.glowingPlayers.GlowingPlayersGui;
import com.skyblockutils.features.NpcFinder;
import com.skyblockutils.features.dungeons.AutoRejoin;
import com.skyblockutils.utils.*;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.stream.Stream;

public class ModCommands {

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
        var command = ClientCommands.literal("ssu")
                .then(ClientCommands.literal("autorejoin")
                        .executes(context -> {
                            if (!AutoRejoin.autoRejoinEnabled) {
                                ModFunctions.displayTextMessageWithHeader("§cPlease specify a floor");
                                return 1;
                            }

                            AutoRejoin.autoRejoinEnabled = false;
                            AutoRejoin.currentFloor = "";
                            ModFunctions.displayTextMessageWithHeader("§cAuto-rejoin disabled");
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
                                        ModFunctions.displayTextMessageWithHeader("§cAuto-rejoin disabled");
                                        ModConfig.save();
                                    } else if (floor.matches("^[mf][1-7]$")) {
                                        AutoRejoin.autoRejoinEnabled = true;
                                        AutoRejoin.currentFloor = floor.toUpperCase();
                                        ModFunctions.displayTextMessageWithHeader("§aAuto-rejoin enabled for " + floor.toUpperCase());
                                        ModConfig.save();
                                    } else {
                                        ModFunctions.displayTextMessageWithHeader("§cInvalid floor! Use m1-m7 or f1-f7, or off to disable");
                                        return 0;
                                    }
                                    return 1;
                                })
                        )
                )
                .then(ClientCommands.literal("config").executes(context -> {
                    ClothConfigHandler.configScreenRequested = true;
                    return 1;
                }))
                .then(ClientCommands.literal("glowingplayers")
                        .then(ClientCommands.literal("add")
                                .then(ClientCommands.argument("username", StringArgumentType.string())
                                        .suggests((ctx, builder) -> {
                                            var connection = Minecraft.getInstance().getConnection();
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
                                            PlayerLookup.getFormattedUsername(name).thenAccept(formattedName -> {
                                                if (formattedName == null) {
                                                    ModFunctions.displayTextMessageWithHeader("§cPlayer " + name + " not found :c");
                                                    return;
                                                }
                                                GlowingPlayers.add(formattedName, 0xFFAA00, false, null);
                                            });
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
                                    ModFunctions.displayTextMessageWithHeader("§aCleared all glowing players.");
                                    return 1;
                                })
                        )
                        .then(ClientCommands.literal("list")
                                .executes(context -> {
                                    List<GlowingPlayers.GlowingPlayer> glowingPlayers = ModConfig.INSTANCE.getGlowingPlayers();
                                    if (glowingPlayers.isEmpty()) {
                                        ModFunctions.displayTextMessageWithHeader("§cYou didn't add any glowing players!");
                                        return 1;
                                    }
                                    StringBuilder players = new StringBuilder();
                                    for (GlowingPlayers.GlowingPlayer glowingPlayer : glowingPlayers) {
                                        players.append(glowingPlayer.username).append(", ");
                                    }
                                    ModFunctions.displayTextMessageWithHeader("§rThese players are glowing: \n" + players);
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
                            ModFunctions.displayTextMessageWithHeader("§rNya! Mreow Mrpp Meow!");
                            return 1;
                        }))
                        .then(ClientCommands.literal("location").executes(context -> {
                            context.getSource().sendFeedback(Component.literal("§6" + SideBarUtils.location));
                            return 1;
                        }))
                        .then(ClientCommands.literal("coordinates")
                                .then(ClientCommands.literal("add").executes(context -> {
                                    MarkCoordinates.addCoordinates();
                                    Minecraft.getInstance().gui.hud.getChat().addClientSystemMessage(Component.literal("Coordinates added"));
                                    return 1;
                                }))
                                .then(ClientCommands.literal("log").executes(context -> {
                                    MarkCoordinates.logCoordinatesList();
                                    return 1;
                                }))
                                .then(ClientCommands.literal("clear").executes(context -> {
                                    MarkCoordinates.clearCoordinates();
                                    Minecraft.getInstance().gui.hud.getChat().addClientSystemMessage(Component.literal("Coordinates cleared"));
                                    return 1;
                                }))
                        )
                        .then(ClientCommands.literal("title")
                                .executes(context -> {
                                    ModFunctions.showTitle(Minecraft.getInstance(), Component.literal("TEST TITLE").withColor(ModStyle.getColor(ModConfig.INSTANCE.colorStyle, ModStyle.ColorType.MAIN)), 20);
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