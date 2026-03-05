package com.skyblockutils;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.skyblockutils.config.ClothConfigHandler;
import com.skyblockutils.config.ModConfig;
import com.skyblockutils.features.GlowingPlayers;
import com.skyblockutils.features.dungeons.DungeonPartyCommands;
import com.skyblockutils.utils.OnScreenNotification;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.text.Text;

import java.util.List;

public class ModCommands {
    @SuppressWarnings("unused")
    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        var command = ClientCommandManager.literal("ssu")
                .then(ClientCommandManager.literal("autorejoin")
                        .then(ClientCommandManager.literal("on").executes(context -> {
                            if (DungeonPartyCommands.autoRejoinEnabled) {
                                context.getSource().sendFeedback(Text.literal("§6[SSU] §cAuto-rejoin is already on"));
                            } else {
                                DungeonPartyCommands.resetDungeonPartyCommands();
                                DungeonPartyCommands.autoRejoinEnabled = true;
                                context.getSource().sendFeedback(Text.literal("§6[SSU] §aAuto-rejoin enabled"));
                                ModConfig.save();
                            }
                            return 1;
                        }))
                        .then(ClientCommandManager.literal("off").executes(context -> {
                            if (DungeonPartyCommands.autoRejoinEnabled) {
                                DungeonPartyCommands.autoRejoinEnabled = false;
                                DungeonPartyCommands.currentFloorJoinCommand = "";
                                DungeonPartyCommands.currentFloor = "";
                                context.getSource().sendFeedback(Text.literal("§6[SSU] §cAuto-rejoin disabled"));
                                ModConfig.save();
                            } else {
                                context.getSource().sendFeedback(Text.literal("§6[SSU] §cAuto-rejoin is already off"));
                            }
                            return 1;
                        })))
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
                                                handler.getPlayerList().forEach(entry -> builder.suggest(entry.getProfile().name()));
                                            }
                                            return builder.buildFuture();
                                        })
                                        .executes(context -> {
                                            String name = StringArgumentType.getString(context, "username");
                                            GlowingPlayers.add(name, 0xFFAA00);
                                            return 1;
                                        })
                                        .then(ClientCommandManager.argument("color", StringArgumentType.string())
                                                .suggests((ctx, builder) -> {
                                                    GlowingPlayers.MINECRAFT_COLORS.keySet().forEach(builder::suggest);
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
                                            ModConfig.INSTANCE.getGlowingPlayers().forEach(gp -> builder.suggest(gp.getUsername()));
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
                                    context.getSource().sendFeedback(Text.literal("§6[SSU] §aCleared all glowing players."));
                                    return 1;
                                })
                        )
                        .then(ClientCommandManager.literal("list")
                                .executes(context -> {
                                    List<GlowingPlayers.GlowingPlayer> glowingPlayers = ModConfig.INSTANCE.getGlowingPlayers();
                                    StringBuilder players = new StringBuilder();
                                    if (glowingPlayers.isEmpty()) {
                                        context.getSource().sendFeedback(Text.literal("§6[SSU] §cYou didn't add any glowing players!"));
                                        return 1;
                                    }
                                    for (GlowingPlayers.GlowingPlayer glowingPlayer : glowingPlayers) {
                                        players.append(glowingPlayer.username).append(", ");
                                    }
                                    context.getSource().sendFeedback(Text.literal("§6[SSU] §rThese players are glowing: \n" + players));
                                    return 1;
                                })
                        )
                )
                .then(ClientCommandManager.literal("dev")
                        .then(ClientCommandManager.literal("testNotification").executes(context -> {
                            OnScreenNotification.renderNotification("Test", "THIS NOTIFICATION IS A TEST\nYou ran /ssu testNotification", 100);
                            return 1;
                        }))
                        .then(ClientCommandManager.literal("teehee").executes(context -> {
                            context.getSource().sendFeedback(Text.literal("§6[SSU] §rNya! Mreow Mrpp Meow!"));
                            return 1;
                        }))
                        .then(ClientCommandManager.literal("location").executes(context -> {
                            context.getSource().sendFeedback(Text.literal("§6" + StrayersSkyblockUtilsClient.location));
                            return 1;
                        }))
                );

        dispatcher.register(command);
        dispatcher.register(ClientCommandManager.literal("strayerskyblockutils").redirect(command.build()));
    }
}