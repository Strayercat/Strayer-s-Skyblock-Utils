package com.skyblockutils;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.skyblockutils.config.ClothConfigHandler;
import com.skyblockutils.config.ModConfig;
import com.skyblockutils.features.DailyReminders;
import com.skyblockutils.features.NpcFinder;
import com.skyblockutils.features.dungeons.AutoRejoin;
import com.skyblockutils.features.glowingPlayers.GlowingPlayers;
import com.skyblockutils.features.glowingPlayers.GlowingPlayersGui;
import com.skyblockutils.features.guild.GuildListParser;
import com.skyblockutils.features.hud.BestiaryHud;
import com.skyblockutils.features.hud.layout.HudEditScreen;
import com.skyblockutils.features.inventory.ButtonEditScreen;
import com.skyblockutils.features.party.PartyInviteNotifications;
import com.skyblockutils.features.party.PartyListParser;
import com.skyblockutils.features.slayer.CarryTracker;
import com.skyblockutils.features.slayer.SlayerTracker;
import com.skyblockutils.features.browser.InGameBrowser;
import com.skyblockutils.features.wiki.WikiLookup;
import com.skyblockutils.hypixel.HypixelLocation;
import com.skyblockutils.utils.*;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ModCommands {
    private static final List<String> FLOORS = List.of("off", "m1", "m2", "m3", "m4", "m5", "m6", "m7", "f1", "f2", "f3", "f4", "f5", "f6", "f7");
    private static final String USERNAME = "^[A-Za-z0-9_]{1,16}$";
    private static final String SKYCRYPT_URL = "https://sky.shiiyu.moe/stats/";

    private ModCommands() {
    }

    @SuppressWarnings("unused")
    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandBuildContext registryAccess) {
        LiteralCommandNode<FabricClientCommandSource> ssu = dispatcher.register(ClientCommands.literal("ssu")
                .then(autoRejoin())
                .then(carry())
                .then(slayer())
                .then(ClientCommands.literal("hud").executes(run(() -> ScreenOpener.openNextTick(HudEditScreen::new))))
                .then(ClientCommands.literal("config").executes(run(() -> ScreenOpener.openNextTick(ClothConfigHandler::createConfigScreen))))
                .then(ClientCommands.literal("buttons").executes(run(() -> ScreenOpener.openNextTick(ButtonEditScreen::new))))
                .then(glowingPlayers())
                .then(dev())
                .then(npcFinder())
                .then(wiki())
                .then(skycrypt())
                .then(ClientCommands.literal("partyinvites").executes(run(ModCommands::showPartyInvites)))
                .then(disableReminder()));

        dispatcher.register(ClientCommands.literal("strayerskyblockutils").redirect(ssu));
        dispatcher.register(ClientCommands.literal("snpc")
                .then(ClientCommands.argument("id", StringArgumentType.string())
                        .executes(run(context -> NpcFinder.handleCallback(StringArgumentType.getString(context, "id"))))));
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> autoRejoin() {
        return ClientCommands.literal("autorejoin")
                .executes(run(() -> {
                    if (!AutoRejoin.isEnabled()) {
                        ModFunctions.sendSystemMessage("§cPlease specify a floor", false);
                        return;
                    }
                    disableAutoRejoin();
                }))
                .then(ClientCommands.argument("floor", StringArgumentType.string())
                        .suggests(suggest(() -> FLOORS))
                        .executes(context -> {
                            String floor = StringArgumentType.getString(context, "floor");
                            if (floor.equals("off")) {
                                disableAutoRejoin();
                                return 1;
                            }
                            if (!floor.matches("^[mf][1-7]$")) {
                                ModFunctions.sendSystemMessage("§cInvalid floor! Use m1-m7 or f1-f7, or off to disable", false);
                                return 0;
                            }
                            AutoRejoin.enable(floor.toUpperCase(Locale.ROOT));
                            ModFunctions.sendSystemMessage("§aAuto-rejoin enabled for " + AutoRejoin.floor(), false);
                            ModConfig.save();
                            return 1;
                        }));
    }

    private static void disableAutoRejoin() {
        AutoRejoin.disable();
        ModFunctions.sendSystemMessage("§cAuto-rejoin disabled", false);
        ModConfig.save();
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> carry() {
        return ClientCommands.literal("carry")
                .then(quietLiteral(ClientCommands.literal("adjust")
                        .then(ClientCommands.argument("username", StringArgumentType.word())
                                .suggests(suggest(CarryTracker::activeTargets))
                                .then(carryAdjust("add", 1))
                                .then(carryAdjust("remove", -1)))))
                .then(quietLiteral(ClientCommands.literal("stop")
                        .executes(run(() -> CarryTracker.stop(null)))
                        .then(ClientCommands.argument("username", StringArgumentType.word())
                                .suggests(suggest(CarryTracker::activeTargets))
                                .executes(run(context -> CarryTracker.stop(StringArgumentType.getString(context, "username")))))))
                .then(ClientCommands.argument("username", StringArgumentType.word())
                        .suggests(ModCommands::carryTargetSuggestions)
                        .then(ClientCommands.argument("amount", IntegerArgumentType.integer(1))
                                .then(ClientCommands.argument("price", StringArgumentType.greedyString())
                                        .executes(run(context -> CarryTracker.start(
                                                StringArgumentType.getString(context, "username"),
                                                IntegerArgumentType.getInteger(context, "amount"),
                                                StringArgumentType.getString(context, "price")))))));
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> carryAdjust(String name, int sign) {
        return ClientCommands.literal(name)
                .executes(run(context -> CarryTracker.adjust(StringArgumentType.getString(context, "username"), sign)))
                .then(ClientCommands.argument("count", IntegerArgumentType.integer(1))
                        .executes(run(context -> CarryTracker.adjust(
                                StringArgumentType.getString(context, "username"),
                                sign * IntegerArgumentType.getInteger(context, "count")))));
    }

    private static CompletableFuture<Suggestions> carryTargetSuggestions(CommandContext<FabricClientCommandSource> context, SuggestionsBuilder builder) {
        String lower = builder.getRemaining().toLowerCase(Locale.ROOT);
        Set<String> texts = new LinkedHashSet<>();
        for (String literal : List.of("adjust", "stop")) {
            if (literal.startsWith(lower)) texts.add(literal);
        }
        List<String> players = onlinePlayers().stream().filter(name -> name.matches(USERNAME)).toList();
        texts.addAll(FuzzySearch.search(builder.getRemaining(), players));
        return orderedSuggestions(builder, texts);
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> slayer() {
        return ClientCommands.literal("slayer")
                .then(ClientCommands.literal("reset").executes(run(() -> {
                    SlayerTracker.reset();
                    ModFunctions.sendSystemMessage("§aSlayer tracker session reset", false);
                })));
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> glowingPlayers() {
        RequiredArgumentBuilder<FabricClientCommandSource, String> addUsername = ClientCommands.argument("username", StringArgumentType.string())
                .suggests(suggest(ModCommands::onlinePlayers))
                .executes(run(context -> GlowingPlayers.add(StringArgumentType.getString(context, "username"), 0xFFAA00, false, null)))
                .then(ClientCommands.argument("color", StringArgumentType.string())
                        .suggests(suggest(() -> GlowingPlayers.MINECRAFT_COLORS.keySet()))
                        .executes(run(context -> {
                            String colorName = StringArgumentType.getString(context, "color").toUpperCase(Locale.ROOT);
                            int color = GlowingPlayers.MINECRAFT_COLORS.getOrDefault(colorName, 0xFFAA00);
                            GlowingPlayers.add(StringArgumentType.getString(context, "username"), color, false, null);
                        })));

        return ClientCommands.literal("glowingplayers")
                .then(ClientCommands.literal("add").then(addUsername))
                .then(ClientCommands.literal("remove")
                        .then(ClientCommands.argument("username", StringArgumentType.string())
                                .suggests(suggest(() -> ModConfig.INSTANCE.getGlowingPlayers().stream().map(player -> player.username).toList()))
                                .executes(run(context -> GlowingPlayers.remove(StringArgumentType.getString(context, "username"), false)))))
                .then(ClientCommands.literal("clear").executes(run(() -> {
                    GlowingPlayers.clearAll();
                    ModFunctions.sendSystemMessage("§aCleared all glowing players.", false);
                })))
                .then(ClientCommands.literal("list").executes(run(ModCommands::listGlowingPlayers)))
                .then(ClientCommands.literal("gui").executes(run(() -> ScreenOpener.openNextTick(GlowingPlayersGui::createConfigScreen))));
    }

    private static void listGlowingPlayers() {
        List<GlowingPlayers.GlowingPlayer> players = ModConfig.INSTANCE.getGlowingPlayers();
        if (players.isEmpty()) {
            ModFunctions.sendSystemMessage("§cYou didn't add any glowing players!", false);
            return;
        }
        String names = String.join(", ", players.stream().map(player -> player.username).toList());
        ModFunctions.sendSystemMessage("§rThese players are glowing: \n" + names, false);
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> dev() {
        Minecraft client = Minecraft.getInstance();
        return ClientCommands.literal("dev")
                .then(ClientCommands.literal("partyscan").executes(run(PartyListParser.INSTANCE::devScan)))
                .then(ClientCommands.literal("guildscan").executes(run(GuildListParser.INSTANCE::devScan)))
                .then(ClientCommands.literal("bestiary").executes(run(BestiaryHud::devCopy)))
                .then(ClientCommands.literal("testnotification").executes(run(() -> OnScreenNotification.builder()
                        .title("Test")
                        .subtitle("THIS NOTIFICATION IS A TEST\nYou ran /ssu dev testNotification")
                        .tickTime(100)
                        .withSound(true)
                        .send())))
                .then(ClientCommands.literal("teehee").executes(run(() -> ModFunctions.sendSystemMessage("§rNya! Mreow Mrpp Meow!", false))))
                .then(ClientCommands.literal("location").executes(feedback(() -> SideBarUtils.location)))
                .then(ClientCommands.literal("hypixellocation").executes(feedback(HypixelLocation::describe)))
                .then(ClientCommands.literal("generallocation").executes(feedback(SkyblockAreas::current)))
                .then(ClientCommands.literal("coordinates")
                        .then(ClientCommands.literal("add").executes(run(() -> {
                            MarkCoordinates.addCoordinates();
                            client.gui.hud.getChat().addClientSystemMessage(Component.literal("Coordinates added"));
                        })))
                        .then(ClientCommands.literal("log").executes(run(MarkCoordinates::logCoordinatesList)))
                        .then(ClientCommands.literal("clear").executes(run(() -> {
                            MarkCoordinates.clearCoordinates();
                            client.gui.hud.getChat().addClientSystemMessage(Component.literal("Coordinates cleared"));
                        }))))
                .then(ClientCommands.literal("title").executes(run(() ->
                        ModFunctions.showTitle(client, Component.literal("TEST TITLE").withColor(ModStyle.main()), 20, true))));
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> wiki() {
        return ClientCommands.literal("wiki").executes(run(WikiLookup::openHomeFromCommand));
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> skycrypt() {
        return ClientCommands.literal("skycrypt")
                .executes(run(() -> openSkyCrypt(Minecraft.getInstance().getUser().getName())))
                .then(ClientCommands.argument("username", StringArgumentType.word())
                        .suggests(suggest(ModCommands::onlinePlayers))
                        .executes(context -> {
                            String username = StringArgumentType.getString(context, "username");
                            if (!username.matches(USERNAME)) {
                                ModFunctions.sendSystemMessage("§cInvalid username: " + username, false);
                                return 0;
                            }
                            openSkyCrypt(username);
                            return 1;
                        }));
    }

    public static void openSkyCrypt(String username) {
        InGameBrowser.openFromCommand(SKYCRYPT_URL + username);
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> npcFinder() {
        return ClientCommands.literal("npcfinder")
                .then(ClientCommands.argument("npc", StringArgumentType.greedyString())
                        .suggests(suggest(() -> NpcFinder.NPCS.stream().map(NpcFinder.Npc::name).distinct().toList()))
                        .executes(run(context -> NpcFinder.handleCommand(StringArgumentType.getString(context, "npc")))));
    }

    private static void showPartyInvites() {
        List<String> invites = PartyInviteNotifications.previousInvites;
        if (invites.isEmpty()) {
            ModFunctions.sendSystemMessage("§cNobody has invited you to their party this session.", false);
            return;
        }
        ModFunctions.sendSystemMessage("Here are the last 10 received party invites (newest to oldest):", false);
        List<String> lastTen = new ArrayList<>(invites.subList(Math.max(0, invites.size() - 10), invites.size()).stream().map(String::trim).toList());
        Collections.reverse(lastTen);
        Minecraft.getInstance().gui.hud.getChat().addClientSystemMessage(Component.literal(String.join(", ", lastTen).trim()));
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> disableReminder() {
        return ClientCommands.literal("disableReminder")
                .then(ClientCommands.argument("type", StringArgumentType.string())
                        .suggests(suggest(() -> Arrays.stream(DailyReminders.ReminderType.values()).map(Enum::name).toList()))
                        .executes(context -> {
                            String typeArg = StringArgumentType.getString(context, "type").toUpperCase(Locale.ROOT);
                            try {
                                DailyReminders.disable(DailyReminders.ReminderType.valueOf(typeArg));
                                ModFunctions.sendSystemMessage("§cDisabled reminder: " + typeArg, false);
                                return 1;
                            } catch (IllegalArgumentException e) {
                                ModFunctions.sendSystemMessage("§cUnknown reminder type: " + typeArg, false);
                                return 0;
                            }
                        }));
    }

    private static Command<FabricClientCommandSource> run(Runnable action) {
        return _ -> {
            action.run();
            return 1;
        };
    }

    private static Command<FabricClientCommandSource> run(Consumer<CommandContext<FabricClientCommandSource>> action) {
        return context -> {
            action.accept(context);
            return 1;
        };
    }

    private static Command<FabricClientCommandSource> feedback(Supplier<String> text) {
        return context -> {
            context.getSource().sendFeedback(Component.literal("§6" + text.get()));
            return 1;
        };
    }

    private static SuggestionProvider<FabricClientCommandSource> suggest(Supplier<? extends Collection<String>> candidates) {
        return (_, builder) -> {
            FuzzySearch.search(builder.getRemaining(), candidates.get()).forEach(builder::suggest);
            return builder.buildFuture();
        };
    }

    private static List<String> onlinePlayers() {
        var connection = Minecraft.getInstance().getConnection();
        if (connection == null) return List.of();
        return connection.getOnlinePlayers().stream().map(entry -> entry.getProfile().name()).toList();
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
}
