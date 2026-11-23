package com.skyblockutils.features.guild;

import com.skyblockutils.utils.ChatListScanner;
import com.skyblockutils.utils.SSUIndicator;
import com.skyblockutils.utils.TextUtils;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GuildListParser extends ChatListScanner {
    private static final long RETRY_MS = 30_000;
    private static final long CHANGE_DELAY_MS = 1_000;
    private static final Pattern MEMBERSHIP_CHANGE = Pattern.compile("^(?:\\[[^]]+] )*\\w{3,16} (?:joined the guild!|left the guild!|was kicked from the guild.*|disbanded the guild\\.)$");
    private static final Pattern SELF_REMOVED = Pattern.compile("^(?:You left the guild.*|You were kicked from the guild.*|(?:\\[[^]]+] )*\\w{3,16} disbanded the guild\\.)$");
    private static final Pattern MANUAL_COMMAND = Pattern.compile("^(g|guild) (list|online|members|l|o)\\b.*");
    private static final Pattern MEMBER = Pattern.compile("^(?:\\[[^]]+] )*(\\w{3,16})$");
    private static final String NOT_IN_GUILD = "You must be in a guild";
    private static final List<String> IGNORED_PREFIXES = List.of("Guild Name:", "--", "Total Members:", "Online Members:", "Offline Members:");

    public static final GuildListParser INSTANCE = new GuildListParser();

    public final List<String> members = new ArrayList<>();
    private long rescanAt = 0;

    private GuildListParser() {
        super("Guild", "guild online", MANUAL_COMMAND);
    }

    @Override
    protected boolean refreshDue(long now) {
        return rescanAt > 0 && now >= rescanAt;
    }

    @Override
    protected void onTimeout() {
        rescanAt = System.currentTimeMillis() + RETRY_MS;
    }

    @Override
    protected void beforeRequest() {
        rescanAt = 0;
    }

    public void observeMessage(Component message) {
        for (String line : TextUtils.strip(message).split("\n")) {
            String text = line.trim();
            if (SELF_REMOVED.matcher(text).matches()) {
                members.clear();
                SSUIndicator.setGuildMembers(members);
                rescanAt = 0;
                return;
            }
            if (MEMBERSHIP_CHANGE.matcher(text).matches()) {
                rescanAt = System.currentTimeMillis() + CHANGE_DELAY_MS;
                return;
            }
        }
    }

    @Override
    protected boolean isStartLine(String text) {
        return text.startsWith("Guild Name:") || text.startsWith(NOT_IN_GUILD);
    }

    @Override
    protected void parse(List<String> lines) {
        members.clear();
        if (!notInGuild(lines)) {
            for (String line : lines) {
                String text = line.trim();
                if (text.isEmpty() || IGNORED_PREFIXES.stream().anyMatch(text::startsWith)) continue;
                for (String part : text.split("●")) {
                    Matcher m = MEMBER.matcher(part.trim());
                    if (m.matches()) members.add(m.group(1));
                }
            }
        }
        SSUIndicator.setGuildMembers(members);
    }

    @Override
    protected String summary(List<String> lines) {
        if (members.isEmpty() && notInGuild(lines)) return "not in a guild";
        return members.size() + " online member(s): " + String.join(", ", members);
    }

    private static boolean notInGuild(List<String> lines) {
        return lines.stream().anyMatch(line -> line.startsWith(NOT_IN_GUILD));
    }
}
