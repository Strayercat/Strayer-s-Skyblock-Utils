package com.skyblockutils.features.mining;

import com.skyblockutils.ModFunctions;
import com.skyblockutils.config.ModConfig;
import com.skyblockutils.utils.OnScreenNotification;
import com.skyblockutils.utils.SideBarUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEventListener;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PowderChestNotifications implements SoundEventListener {
    private enum ScanContext {
        BASIC_SCAN, NEW_SCAN
    }

    private record TargetedChest(BlockPos pos, long tick) {
    }

    private static final List<TargetedChest> RECENT_CHEST_TARGETS = new ArrayList<>();
    private static final int TARGET_HISTORY_TICKS = 5;

    private static final List<BlockPos> KNOWN_CHEST_LOCATIONS = new ArrayList<>();
    private static final List<BlockPos> LOCKPICK_CHEST_LOCATIONS = new ArrayList<>();

    private static int EXPECTING_LOCKPICK_CHEST = 0;
    private static int EXPECTING_LOOT_CHEST = 0;

    private static boolean reading = false;
    private static final List<Component> buffer = new ArrayList<>();
    private static Component openingBorder;
    private static Component closingBorder;

    private static final int NEW_SCAN_DELAY_TICKS = 3;
    private static int pendingNewScanTicks = 0;

    public static void handleMessage(Component message) {
        if (!ModConfig.INSTANCE.powderChestNotification || !ModFunctions.mapLocationToGeneralArea(SideBarUtils.location).equals("Crystal Hollows"))
            return;

        if (message.getString().equals("You uncovered a treasure chest!")) {
            pendingNewScanTicks = NEW_SCAN_DELAY_TICKS;
        }
    }

    public static InteractionResult handleChestclick(BlockHitResult hitResult) {
        if (!ModConfig.INSTANCE.powderChestNotification
                || !ModFunctions.mapLocationToGeneralArea(SideBarUtils.location).equals("Crystal Hollows"))
            return InteractionResult.PASS;

        BlockPos pos = hitResult.getBlockPos().immutable();

        if (LOCKPICK_CHEST_LOCATIONS.contains(pos)) {
            EXPECTING_LOCKPICK_CHEST++;
            LOCKPICK_CHEST_LOCATIONS.remove(pos);
            KNOWN_CHEST_LOCATIONS.remove(pos);
        } else if (KNOWN_CHEST_LOCATIONS.contains(pos) && !SideBarUtils.location.equals("Mines of Divan")) {
            EXPECTING_LOOT_CHEST++;
            KNOWN_CHEST_LOCATIONS.remove(pos);
        }

        RECENT_CHEST_TARGETS.clear();
        return InteractionResult.PASS;
    }

    public static void tick() {
        if (!ModConfig.INSTANCE.powderChestNotification || !ModFunctions.mapLocationToGeneralArea(SideBarUtils.location).equals("Crystal Hollows"))
            return;

        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return;

        recordTargetedChest(client);

        if (pendingNewScanTicks > 0) {
            pendingNewScanTicks--;
            if (pendingNewScanTicks == 0) {
                scanForChests(client, ScanContext.NEW_SCAN);
            }
            return;
        }

        ItemStack stack = client.player.getMainHandItem();
        String mainHandItemName = stack.getItem().getName(stack).getString().toLowerCase();

        if (mainHandItemName.contains("drill") || mainHandItemName.contains("pickaxe"))
            scanForChests(client, ScanContext.BASIC_SCAN);
    }

    private static void recordTargetedChest(Minecraft client) {
        if (client.level == null) return;

        BlockPos pos = getTargetedChestPos(client);
        long now = client.level.getGameTime();

        if (pos != null) {
            RECENT_CHEST_TARGETS.addFirst(new TargetedChest(pos, now));
        }
        RECENT_CHEST_TARGETS.removeIf(t -> now - t.tick > TARGET_HISTORY_TICKS);
    }

    private static void scanForChests(Minecraft client, ScanContext context) {
        if (client.player == null || client.level == null) return;

        int SCAN_RADIUS = 10;
        BlockPos playerPos = client.player.blockPosition();

        pruneStaleChests(client, playerPos, SCAN_RADIUS);

        if (Objects.requireNonNull(context) == ScanContext.BASIC_SCAN) {
            BlockPos.betweenClosedStream(
                            playerPos.offset(-SCAN_RADIUS, -SCAN_RADIUS, -SCAN_RADIUS),
                            playerPos.offset(SCAN_RADIUS, SCAN_RADIUS, SCAN_RADIUS))
                    .filter(p -> client.level.getBlockState(p).is(Blocks.CHEST))
                    .forEach(p -> {
                        BlockPos immutable = p.immutable();
                        if (!KNOWN_CHEST_LOCATIONS.contains(immutable) && !LOCKPICK_CHEST_LOCATIONS.contains(immutable)) {
                            KNOWN_CHEST_LOCATIONS.add(immutable);
                        }
                    });
        } else {
            List<BlockPos> currentChests = new ArrayList<>();

            BlockPos.betweenClosedStream(
                            playerPos.offset(-SCAN_RADIUS, -SCAN_RADIUS, -SCAN_RADIUS),
                            playerPos.offset(SCAN_RADIUS, SCAN_RADIUS, SCAN_RADIUS))
                    .filter(p -> client.level.getBlockState(p).is(Blocks.CHEST))
                    .forEach(p -> {
                        BlockPos immutable = p.immutable();
                        if (!KNOWN_CHEST_LOCATIONS.contains(immutable) && !LOCKPICK_CHEST_LOCATIONS.contains(immutable)) {
                            currentChests.add(immutable);
                        }
                    });

            LOCKPICK_CHEST_LOCATIONS.addAll(currentChests);
        }
    }

    private static void pruneStaleChests(Minecraft client, BlockPos playerPos, int radius) {
        if (client.level == null) return;

        KNOWN_CHEST_LOCATIONS.removeIf(p -> isWithinRadius(p, playerPos, radius) && !client.level.getBlockState(p).is(Blocks.CHEST));
        LOCKPICK_CHEST_LOCATIONS.removeIf(p -> isWithinRadius(p, playerPos, radius) && !client.level.getBlockState(p).is(Blocks.CHEST));
    }

    private static boolean isWithinRadius(BlockPos pos, BlockPos center, int radius) {
        return Math.abs(pos.getX() - center.getX()) <= radius
                && Math.abs(pos.getY() - center.getY()) <= radius
                && Math.abs(pos.getZ() - center.getZ()) <= radius;
    }

    public static void resetKnownChests() {
        KNOWN_CHEST_LOCATIONS.clear();
        LOCKPICK_CHEST_LOCATIONS.clear();
        RECENT_CHEST_TARGETS.clear();
        pendingNewScanTicks = 0;
        EXPECTING_LOOT_CHEST = 0;
        EXPECTING_LOCKPICK_CHEST = 0;
    }

    @Override
    public void onPlaySound(@NotNull SoundInstance sound, @NotNull WeighedSoundEvents soundEvent, float range) {
        if (!ModConfig.INSTANCE.powderChestNotification || !ModFunctions.mapLocationToGeneralArea(SideBarUtils.location).equals("Crystal Hollows"))
            return;
        String soundId = sound.getIdentifier().toString();

        if (!soundId.equals("minecraft:block.chest.open")) return;

        Minecraft client = Minecraft.getInstance();
        BlockPos targetedChest = getTargetedChestPos(client);

        if (targetedChest == null && !RECENT_CHEST_TARGETS.isEmpty()) {
            targetedChest = RECENT_CHEST_TARGETS.getFirst().pos;
        }

        if (targetedChest == null) {
            return;
        }

        if (LOCKPICK_CHEST_LOCATIONS.contains(targetedChest)) {
            EXPECTING_LOCKPICK_CHEST++;
            LOCKPICK_CHEST_LOCATIONS.remove(targetedChest);
            KNOWN_CHEST_LOCATIONS.remove(targetedChest);
        } else if (KNOWN_CHEST_LOCATIONS.contains(targetedChest) && !SideBarUtils.location.equals("Mines of Divan")) {
            EXPECTING_LOOT_CHEST++;
            KNOWN_CHEST_LOCATIONS.remove(targetedChest);
        } else {
            KNOWN_CHEST_LOCATIONS.remove(targetedChest);
        }

        RECENT_CHEST_TARGETS.clear();
    }

    private static BlockPos getTargetedChestPos(Minecraft client) {
        if (client.level == null) return null;
        if (client.hitResult instanceof BlockHitResult blockHit
                && blockHit.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = blockHit.getBlockPos();
            if (client.level.getBlockState(pos).is(Blocks.CHEST)) {
                return pos.immutable();
            }
        }
        return null;
    }

    public static boolean parseChestReward(Component message) {
        if (!ModConfig.INSTANCE.powderChestNotification || !ModFunctions.mapLocationToGeneralArea(SideBarUtils.location).equals("Crystal Hollows"))
            return true;

        boolean expectingAny = EXPECTING_LOCKPICK_CHEST > 0 || EXPECTING_LOOT_CHEST > 0;

        if (!reading && !expectingAny) return true;

        boolean isBorder = message.getString().trim().startsWith("▬▬▬▬");

        if (isBorder) {
            if (!reading) {
                if (!expectingAny) return true;

                reading = true;
                buffer.clear();
                openingBorder = message;
            } else {
                reading = false;
                closingBorder = message;

                boolean matchedLockpick = EXPECTING_LOCKPICK_CHEST > 0 && isBufferValid("CHEST LOCKPICKED");
                boolean matchedLoot = EXPECTING_LOOT_CHEST > 0 && isBufferValid("LOOT CHEST COLLECTED");

                if (matchedLockpick || matchedLoot) {

                    List<Component> subtitle = buffer.stream()
                            .filter(c -> {
                                String componentString = c.getString().trim();
                                return !componentString.equals("CHEST LOCKPICKED")
                                        && !componentString.equals("LOOT CHEST COLLECTED")
                                        && !componentString.equals("REWARDS")
                                        && !componentString.isEmpty();
                            })
                            .map(c -> OnScreenNotification.removeText(c, " Gemstone")).toList();

                    OnScreenNotification.builder()
                            .title(matchedLockpick ? "CHEST LOCKPICKED" : "LOOT CHEST")
                            .subtitle(subtitle)
                            .tickTime(ModConfig.INSTANCE.powderChestNotificationTime)
                            .send();
                    if (matchedLockpick) EXPECTING_LOCKPICK_CHEST--;
                    if (matchedLoot) EXPECTING_LOOT_CHEST--;

                    buffer.clear();
                } else {
                    replaySwallowedMessages();
                    buffer.clear();
                }
            }
            return false;
        }

        if (reading) {
            buffer.add(message);
            return false;
        }

        return true;
    }

    private static boolean isBufferValid(String expectedHeader) {
        return buffer.stream().anyMatch(c -> c.getString().trim().contains(expectedHeader));
    }

    private static void replaySwallowedMessages() {
        ChatComponent chatHud = Minecraft.getInstance().gui.hud.getChat();

        chatHud.addClientSystemMessage(openingBorder);
        for (Component message : buffer) chatHud.addClientSystemMessage(message);
        chatHud.addClientSystemMessage(closingBorder);
    }
}