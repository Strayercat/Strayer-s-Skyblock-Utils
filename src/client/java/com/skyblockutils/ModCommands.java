package com.skyblockutils;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.skyblockutils.config.ClothConfigHandler;
import com.skyblockutils.config.ModConfig;
import com.skyblockutils.features.GlowingPlayers;
import com.skyblockutils.features.NpcFinder;
import com.skyblockutils.features.dungeons.AutoRejoin;
import com.skyblockutils.utils.MarkCoordinates;
import com.skyblockutils.utils.OnScreenNotification;
import com.skyblockutils.utils.PlayerLookup;
import com.skyblockutils.utils.SideBarUtils;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.text.Text;

import java.util.List;
import java.util.stream.Stream;

public class ModCommands {
    @SuppressWarnings("unused")
    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        var command = ClientCommandManager.literal("ssu")
                .then(ClientCommandManager.literal("autorejoin")
                        .then(ClientCommandManager.argument("floor", StringArgumentType.string())
                                .suggests((ctx, builder) -> {
                                    String remaining = builder.getRemaining().toLowerCase();
                                    Stream.of("off", "m1", "m2", "m3", "m4", "m5", "m6", "m7", "f1", "f2", "f3", "f4", "f5", "f6", "f7")
                                            .filter(f -> f.startsWith(remaining))
                                            .forEach(builder::suggest);
                                    return builder.buildFuture();
                                })
                                .executes(context -> {
                                    String floor = StringArgumentType.getString(context, "floor");
                                    if (floor.equals("off")) {
                                        AutoRejoin.autoRejoinEnabled = false;
                                        AutoRejoin.currentFloor = "";
                                        ModFunctions.displayMessageWithHeader("§cAuto-rejoin disabled");
                                        ModConfig.save();
                                    } else if (floor.matches("^[mf][1-7]$")) {
                                        AutoRejoin.autoRejoinEnabled = true;
                                        AutoRejoin.currentFloor = floor.toUpperCase();
                                        ModFunctions.displayMessageWithHeader("§aAuto-rejoin enabled for " + floor.toUpperCase());
                                        ModConfig.save();
                                    } else {
                                        ModFunctions.displayMessageWithHeader("§cInvalid floor! Use m1-m7 or f1-f7, or off to disable");
                                        return 0;
                                    }
                                    return 1;
                                })
                        )
                )
                .then(ClientCommandManager.literal("config").executes(context -> {
                    ClothConfigHandler.configScreenRequested = true;
                    return 1;
                }))
                .then(ClientCommandManager.literal("glowingPlayers")
                        .then(ClientCommandManager.literal("add")
                                .then(ClientCommandManager.argument("username", StringArgumentType.string())
                                        .suggests((ctx, builder) -> {
                                            var handler = MinecraftClient.getInstance().getNetworkHandler();
                                            if (handler != null) {
                                                String remaining = builder.getRemaining().toLowerCase();
                                                handler.getPlayerList().stream()
                                                        .map(entry -> entry.getProfile().name())
                                                        .filter(name -> name.toLowerCase().startsWith(remaining))
                                                        .forEach(builder::suggest);
                                            }
                                            return builder.buildFuture();
                                        })
                                        .executes(context -> {
                                            String name = StringArgumentType.getString(context, "username");
                                            PlayerLookup.getFormattedUsername(name).thenAccept(formattedName -> GlowingPlayers.add(formattedName, 0xFFAA00));
                                            return 1;
                                        })
                                        .then(ClientCommandManager.argument("color", StringArgumentType.string())
                                                .suggests((ctx, builder) -> {
                                                    String remaining = builder.getRemaining().toLowerCase();
                                                    GlowingPlayers.MINECRAFT_COLORS.keySet().stream()
                                                            .filter(color -> color.toLowerCase().startsWith(remaining))
                                                            .forEach(builder::suggest);
                                                    return builder.buildFuture();
                                                })
                                                .executes(context -> {
                                                    String name = StringArgumentType.getString(context, "username");
                                                    String colorName = StringArgumentType.getString(context, "color").toUpperCase();
                                                    int hex = GlowingPlayers.MINECRAFT_COLORS.getOrDefault(colorName, 0xFFAA00);

                                                    GlowingPlayers.add(name, hex);
                                                    return 1;
                                                })
                                        )
                                )
                        )
                        .then(ClientCommandManager.literal("remove")
                                .then(ClientCommandManager.argument("username", StringArgumentType.string())
                                        .suggests((ctx, builder) -> {
                                            String remaining = builder.getRemaining().toLowerCase();
                                            ModConfig.INSTANCE.getGlowingPlayers().stream()
                                                    .map(gp -> gp.username)
                                                    .filter(name -> name.toLowerCase().startsWith(remaining))
                                                    .forEach(builder::suggest);
                                            return builder.buildFuture();
                                        })
                                        .executes(context -> {
                                            String name = StringArgumentType.getString(context, "username");
                                            GlowingPlayers.remove(name);
                                            return 1;
                                        })
                                )
                        )
                        .then(ClientCommandManager.literal("clear")
                                .executes(context -> {
                                    GlowingPlayers.clearAll();
                                    ModFunctions.displayMessageWithHeader("§aCleared all glowing players.");
                                    return 1;
                                })
                        )
                        .then(ClientCommandManager.literal("list")
                                .executes(context -> {
                                    List<GlowingPlayers.GlowingPlayer> glowingPlayers = ModConfig.INSTANCE.getGlowingPlayers();
                                    StringBuilder players = new StringBuilder();
                                    if (glowingPlayers.isEmpty()) {
                                        ModFunctions.displayMessageWithHeader("§cYou didn't add any glowing players!");
                                        return 1;
                                    }
                                    for (GlowingPlayers.GlowingPlayer glowingPlayer : glowingPlayers) {
                                        players.append(glowingPlayer.username).append(", ");
                                    }
                                    ModFunctions.displayMessageWithHeader("§rThese players are glowing: \n" + players);
                                    return 1;
                                })
                        )
                )
                .then(ClientCommandManager.literal("dev")
                        .then(ClientCommandManager.literal("testNotification").executes(context -> {
                            OnScreenNotification.renderNotification("Test", "THIS NOTIFICATION IS A TEST\nYou ran /ssu dev testNotification", 100);
                            return 1;
                        }))
                        .then(ClientCommandManager.literal("teehee").executes(context -> {
                            ModFunctions.displayMessageWithHeader("§rNya! Mreow Mrpp Meow!");
                            return 1;
                        }))
                        .then(ClientCommandManager.literal("location").executes(context -> {
                            context.getSource().sendFeedback(Text.literal("§6" + SideBarUtils.location));
                            return 1;
                        }))
                        .then(ClientCommandManager.literal("coordinates")
                                .then(ClientCommandManager.literal("add").executes(context -> {
                                    MarkCoordinates.addCoordinates();
                                    MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.literal("Coordinates added"));
                                    return 1;
                                }))
                                .then(ClientCommandManager.literal("log").executes(context -> {
                                    MarkCoordinates.logCoordinatesList();
                                    return 1;
                                }))
                                .then(ClientCommandManager.literal("clear").executes(context -> {
                                    MarkCoordinates.clearCoordinates();
                                    MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.literal("Coordinates cleared"));
                                    return 1;
                                })))
                ).then(ClientCommandManager.literal("npcfinder")
                        .then(ClientCommandManager.argument("npc", StringArgumentType.greedyString())
                                .suggests((ctx, builder) -> {
                                    String remaining = builder.getRemaining().toLowerCase();
                                    NpcFinder.allSkyblockNpcs.values().stream()
                                            .map(NpcFinder.Npc::name)
                                            .distinct()
                                            .filter(name -> name.toLowerCase().startsWith(remaining))
                                            .forEach(builder::suggest);
                                    return builder.buildFuture();
                                })
                                .executes(context -> {
                                    String name = StringArgumentType.getString(context, "npc");
                                    NpcFinder.handleCommand(name);
                                    return 1;
                                })
                        )
                );

        var npcFinderCommand = ClientCommandManager.literal("snpc")
                .then(ClientCommandManager.argument("id", StringArgumentType.string())
                        .executes(context -> {
                            NpcFinder.handleCallback(StringArgumentType.getString(context, "id"));
                            return 1;
                        })
                );

        dispatcher.register(command);
        dispatcher.register(ClientCommandManager.literal("strayerskyblockutils").redirect(command.build()));
        dispatcher.register(npcFinderCommand);
    }
}