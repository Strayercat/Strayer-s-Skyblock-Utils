package com.skyblockutils.features.party;

import com.skyblockutils.utils.ChatListScanner;
import com.skyblockutils.utils.SSUIndicator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static com.skyblockutils.utils.Scheduler.scheduler;

public class PartyListParser extends ChatListScanner {
    private static final Pattern MANUAL_COMMAND = Pattern.compile("^((p|party) (list|l)|pl)\\b.*");
    private static final Pattern PARTY_LINE = Pattern.compile("^Party (Members \\(\\d+\\)|Leader:|Moderators:|Members:).*");
    private static final Pattern RANK = Pattern.compile("\\[[^]]+] ");

    public static final PartyListParser INSTANCE = new PartyListParser();

    private ClientLevel lastLevel;

    private PartyListParser() {
        super("Party", "party list", MANUAL_COMMAND);
    }

    @Override
    public void tickCapture() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != lastLevel) {
            lastLevel = level;
            if (level != null) requested = false;
        }
        super.tickCapture();
    }

    @Override
    protected void onTimeout() {
        requested = false;
    }

    public void requestListDelayed() {
        scheduler.schedule(() -> Minecraft.getInstance().execute(this::requestList), 500, TimeUnit.MILLISECONDS);
    }

    @Override
    protected boolean isStartLine(String text) {
        return PARTY_LINE.matcher(text).matches() || isNotInParty(text);
    }

    private static boolean isNotInParty(String text) {
        return text.startsWith("You are not currently in a party") || text.startsWith("You are not in a party");
    }

    @Override
    protected String summary(List<String> lines) {
        if (!PartyInfo.isInParty) return "not in a party";
        return "leader " + PartyInfo.leader + ", " + PartyInfo.members.size() + " member(s): " + String.join(", ", PartyInfo.members);
    }

    @Override
    protected void parse(List<String> lines) {
        PartyInfo.members.clear();

        if (lines.stream().noneMatch(line -> PARTY_LINE.matcher(line).matches())) {
            clear();
            return;
        }

        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("Party Leader:")) PartyInfo.leader = names(line.replaceFirst("Party Leader: ", "")).stream().findFirst().orElse(null);
            if (line.startsWith("Party Moderators:") || line.startsWith("Party Members:")) {
                PartyInfo.members.addAll(names(line.replaceFirst("Party (Members|Moderators): ", "")));
            }
        }

        PartyInfo.isInParty = !PartyInfo.members.isEmpty();
        if (!PartyInfo.isInParty) PartyCommands.allInvEnabled = false;

        List<String> watched = new ArrayList<>(PartyInfo.members);
        if (PartyInfo.leader != null) watched.add(PartyInfo.leader);
        SSUIndicator.setPartyMembers(watched);
    }

    private static List<String> names(String text) {
        List<String> names = new ArrayList<>();
        for (String name : RANK.matcher(text).replaceAll("").replace("●", "").split("\\s+")) {
            if (!name.isBlank()) names.add(name.trim());
        }
        return names;
    }

    public void clear() {
        PartyInfo.isInParty = false;
        PartyInfo.leader = null;
        PartyInfo.members.clear();
        PartyCommands.allInvEnabled = false;
        SSUIndicator.setPartyMembers(List.of());
    }

    @Override
    public void reset() {
        super.reset();
        clear();
    }
}
