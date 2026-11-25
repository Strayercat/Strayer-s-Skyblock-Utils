package com.skyblockutils.features.inventory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.skyblockutils.config.ConfigPaths;
import com.skyblockutils.features.hud.layout.HudLocations;
import net.minecraft.network.chat.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public final class ButtonLayout {
    public static final int ROWS = 3;
    public static final int COLUMNS = 4;
    public static final String EXPORT_PREFIX = "SSU-BUTTONS:";
    private static final int MAX_RECENT = 18;
    private static final String BESTIARY_ICON = "texture:ewogICJ0aW1lc3RhbXAiIDogMTY1NzE0NDA2NTAzNywKICAicHJvZmlsZUlkIiA6ICJkZjY2OWIyOGFmNWE0MTNjODFhNjcwOGQ0ZDIyM2FlNSIsCiAgInByb2ZpbGVOYW1lIiA6ICJsYW5nX3FpbmciLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzVmNGI0MGNlZjllMDE3Y2Q0MTEyZDI2YjYyNTU3ZjhjMWQ1YjE4OWRhMmU5OTUzNDIyMmJjOGNlYzdkOTE5NiIKICAgIH0KICB9Cn0=";
    private static final String WIKI_ICON = "texture:ewogICJ0aW1lc3RhbXAiIDogMTYxMzQ4Nzk3NTIzMywKICAicHJvZmlsZUlkIiA6ICJhMjk1ODZmYmU1ZDk0Nzk2OWZjOGQ4ZGE0NzlhNDNlZSIsCiAgInByb2ZpbGVOYW1lIiA6ICJWaWVydGVsdG9hc3RpaWUiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYWFlZjUxNzk5OGI5ZDkzZjYyZDAwNTY3Y2Y2YmM2ODg1MWEzNGJkOTlmMzcwNjk4ZjhiNmQ2MmQ5ZGU0YzNlNCIKICAgIH0KICB9Cn0=";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Gson COMPACT = new GsonBuilder().create();
    private static final Path RECENT_PATH = ConfigPaths.BUTTONS_DIR.resolve("recent.json");

    public static ButtonLayout INSTANCE = defaults();
    private static List<String> recent = new ArrayList<>();

    public ButtonSet defaults = new ButtonSet();
    public Map<String, ButtonSet> locations = new LinkedHashMap<>();

    public static final class ButtonSet {
        public List<Row> rows = new ArrayList<>();

        public Row row(int index) {
            while (rows.size() <= index) rows.add(new Row());
            return rows.get(index);
        }

        public Button button(int row, int column) {
            List<Button> buttons = row(row).buttons;
            while (buttons.size() <= column) buttons.add(new Button());
            return buttons.get(column);
        }

        public void set(int row, int column, Button button) {
            button(row, column);
            row(row).buttons.set(column, button);
        }

        public ButtonSet copy() {
            return GSON.fromJson(GSON.toJson(this), ButtonSet.class).normalize();
        }

        ButtonSet normalize() {
            if (rows == null) rows = new ArrayList<>();
            while (rows.size() > ROWS) rows.removeLast();
            for (int r = 0; r < ROWS; r++) {
                Row row = row(r);
                if (row.label == null) row.label = "";
                if (row.buttons == null) row.buttons = new ArrayList<>();
                while (row.buttons.size() > COLUMNS) row.buttons.removeLast();
                for (int c = 0; c < COLUMNS; c++) button(r, c).normalize();
            }
            return this;
        }
    }

    public static final class Row {
        public String label = "";
        public List<Button> buttons = new ArrayList<>();

        Row() {
        }

        Row(String label, Button... buttons) {
            this.label = label;
            this.buttons.addAll(List.of(buttons));
        }
    }

    public static final class Button {
        public String name = "";
        public String icon = "";
        public String click = "";
        public String shiftName = "";
        public String shift = "";
        public String ctrlName = "";
        public String ctrl = "";

        public Button() {
        }

        Button(String name, String icon, String click) {
            this.name = name;
            this.icon = icon;
            this.click = click;
        }

        Button shift(String name, String action) {
            shiftName = name;
            shift = action;
            return this;
        }

        Button ctrl(String name, String action) {
            ctrlName = name;
            ctrl = action;
            return this;
        }

        void normalize() {
            if (name == null) name = "";
            if (icon == null) icon = "";
            if (click == null) click = "";
            if (shiftName == null) shiftName = "";
            if (shift == null) shift = "";
            if (ctrlName == null) ctrlName = "";
            if (ctrl == null) ctrl = "";
        }

        public boolean isEmpty() {
            return name.isBlank() && icon.isBlank() && ButtonAction.isEmpty(click) && ButtonAction.isEmpty(shift) && ButtonAction.isEmpty(ctrl);
        }

        public Button copy() {
            return GSON.fromJson(GSON.toJson(this), Button.class);
        }

        public ButtonAction actionFor(boolean shiftDown, boolean ctrlDown) {
            if (ctrlDown && !ButtonAction.isEmpty(ctrl)) return ButtonAction.parse(ctrl);
            if (shiftDown && !ButtonAction.isEmpty(shift)) return ButtonAction.parse(shift);
            return ButtonAction.parse(click);
        }

        public List<Component> tooltip() {
            List<Component> lines = new ArrayList<>();
            if (!name.isBlank()) lines.add(ButtonText.parse(name));
            else if (!ButtonAction.isEmpty(click)) lines.add(ButtonText.parse("&7" + ButtonAction.parse(click).describe()));
            if (!ButtonAction.isEmpty(shift)) lines.add(ButtonText.parse("&8Shift: &7" + (shiftName.isBlank() ? ButtonAction.parse(shift).describe() : shiftName)));
            if (!ButtonAction.isEmpty(ctrl)) lines.add(ButtonText.parse("&8Ctrl: &7" + (ctrlName.isBlank() ? ButtonAction.parse(ctrl).describe() : ctrlName)));
            return lines;
        }
    }

    public record Saved(String name, Path path, FileTime modified) {
    }

    private ButtonLayout() {
    }

    public ButtonSet set(String location) {
        if (location == null || location.isEmpty()) return defaults;
        ButtonSet set = locations.get(location);
        return set == null ? defaults : set;
    }

    public boolean hasOwnSet(String location) {
        return location != null && !location.isEmpty() && locations.containsKey(location);
    }

    public void createOwnSet(String location) {
        locations.computeIfAbsent(location, _ -> defaults.copy());
    }

    public void removeOwnSet(String location) {
        locations.remove(location);
    }

    public static ButtonSet defaultSet() {
        ButtonSet set = new ButtonSet();
        set.rows.add(new Row("Stashes",
                new Button("&6Open Material Stash", "item:bundle", "viewstash material"),
                new Button("&6Open Item Stash", "item:item_frame", "viewstash item"),
                new Button("&aPickup All Stashes", "item:hopper", "pickupstash"),
                new Button("&cClear All Stashes", "item:lava_bucket", "clearstash")));
        set.rows.add(new Row("Utility",
                new Button("&aRecipe Book", "item:knowledge_book", "recipebook"),
                new Button("&aBestiary", BESTIARY_ICON, "be").shift("Milestones", "bestiarymilestones"),
                new Button("&eCalendar", "item:clock", "calendar"),
                new Button("&bSkyCrypt", "self", "ssu:skycrypt").shift("Stats", "stats")));
        set.rows.add(new Row("SSU",
                new Button("&{end}Wiki", WIKI_ICON, "ssu:wiki"),
                new Button("&{end}Glowing Players", "sprite:buttons/glowing_player", "ssu:glowingplayers"),
                new Button("&{end}SSU HUD", "sprite:buttons/hud", "ssu:hud"),
                new Button("&{end}SSU Config", "sprite:buttons/config", "ssu:config").shift("Edit Buttons", "ssu:buttons")));
        return set.normalize();
    }

    public static ButtonLayout defaults() {
        ButtonLayout layout = new ButtonLayout();
        layout.defaults = defaultSet();
        return layout;
    }

    public ButtonLayout copy() {
        return fromJson(GSON.toJson(this));
    }

    private ButtonLayout normalize() {
        if (defaults == null) defaults = defaultSet();
        defaults.normalize();
        if (locations == null) locations = new LinkedHashMap<>();
        locations.entrySet().removeIf(entry -> entry.getKey() == null || entry.getKey().isEmpty() || entry.getValue() == null);
        locations.values().forEach(ButtonSet::normalize);
        return this;
    }

    private static ButtonLayout fromJson(String json) {
        ButtonLayout layout = GSON.fromJson(json, ButtonLayout.class);
        if (layout == null) throw new IllegalArgumentException("Empty layout");
        return layout.normalize();
    }

    public static void load() {
        ConfigPaths.ensureDirectories();
        INSTANCE = read(ConfigPaths.BUTTONS);
        if (INSTANCE == null) INSTANCE = defaults();
        loadRecent();
        save();
    }

    public static void save() {
        write(ConfigPaths.BUTTONS, INSTANCE);
        ButtonIcons.clearCache();
    }

    private static ButtonLayout read(Path path) {
        if (!Files.exists(path)) return null;
        try {
            return fromJson(Files.readString(path));
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    private static boolean write(Path path, Object value) {
        ConfigPaths.ensureDirectories();
        try (Writer writer = Files.newBufferedWriter(path)) {
            GSON.toJson(value, writer);
            return true;
        } catch (IOException | RuntimeException e) {
            return false;
        }
    }

    public String export() {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (GZIPOutputStream gzip = new GZIPOutputStream(bytes)) {
                gzip.write(COMPACT.toJson(this).getBytes(StandardCharsets.UTF_8));
            }
            return EXPORT_PREFIX + Base64.getEncoder().encodeToString(bytes.toByteArray());
        } catch (IOException e) {
            return COMPACT.toJson(this);
        }
    }

    public static ButtonLayout importText(String text) {
        if (text == null) throw new IllegalArgumentException("Clipboard is empty");
        String trimmed = text.trim();
        if (trimmed.startsWith(EXPORT_PREFIX)) {
            byte[] data = Base64.getDecoder().decode(trimmed.substring(EXPORT_PREFIX.length()).trim());
            try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(data))) {
                return fromJson(new String(gzip.readAllBytes(), StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new IllegalArgumentException("Corrupted layout string");
            }
        }
        if (trimmed.startsWith("{")) return fromJson(trimmed);
        throw new IllegalArgumentException("Not an SSU button layout");
    }

    public static String fileName(String name) {
        String cleaned = name.trim().replaceAll("[^A-Za-z0-9 _.-]", "_");
        return cleaned.isEmpty() ? "layout" : cleaned;
    }

    public static Path savedPath(String name) {
        return ConfigPaths.SAVED_BUTTONS.resolve(fileName(name) + ".json");
    }

    public boolean saveAs(String name) {
        return write(savedPath(name), this);
    }

    public static List<Saved> listSaved() {
        ConfigPaths.ensureDirectories();
        List<Saved> list = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(ConfigPaths.SAVED_BUTTONS, "*.json")) {
            for (Path path : stream) {
                String file = path.getFileName().toString();
                list.add(new Saved(file.substring(0, file.length() - 5), path, Files.getLastModifiedTime(path)));
            }
        } catch (IOException ignored) {
        }
        list.sort(Comparator.comparing(Saved::modified).reversed());
        return list;
    }

    public static ButtonLayout readSaved(Saved saved) {
        return read(saved.path());
    }

    public static void deleteSaved(Saved saved) {
        try {
            Files.deleteIfExists(saved.path());
        } catch (IOException _) {
        }
    }

    public static boolean renameSaved(Saved saved, String name) {
        Path target = savedPath(name);
        if (Files.exists(target)) return false;
        try {
            Files.move(saved.path(), target);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public String describeLocations() {
        if (locations.isEmpty()) return "Default only";
        List<String> names = new ArrayList<>();
        for (String key : locations.keySet()) names.add(HudLocations.name(key));
        return "Default + " + String.join(", ", names);
    }

    public static List<String> recent() {
        return recent;
    }

    public static void addRecent(String spec) {
        if (spec == null || spec.isBlank()) return;
        recent.remove(spec);
        recent.addFirst(spec);
        while (recent.size() > MAX_RECENT) recent.removeLast();
        write(RECENT_PATH, recent);
    }

    private static void loadRecent() {
        if (!Files.exists(RECENT_PATH)) return;
        try (Reader reader = Files.newBufferedReader(RECENT_PATH)) {
            List<String> list = GSON.fromJson(reader, new TypeToken<List<String>>() {}.getType());
            if (list != null) recent = new ArrayList<>(list.stream().filter(s -> s != null && !s.isBlank()).limit(MAX_RECENT).toList());
        } catch (IOException | RuntimeException ignored) {
        }
    }
}
