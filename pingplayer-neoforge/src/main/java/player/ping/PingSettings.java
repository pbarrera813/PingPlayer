package player.ping;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumMap;
import java.util.Map;
import java.util.StringJoiner;

public class PingSettings {

    public enum ThresholdTier {
        EXCELLENT("excellent"),
        GOOD("good"),
        FAIR("fair"),
        POOR("poor"),
        TERRIBLE("terrible");

        private final String key;

        ThresholdTier(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }
    }

    public record ThresholdRange(Integer min, Integer max) {
        public boolean matches(int ping) {
            boolean meetsMin = min == null || ping >= min;
            boolean meetsMax = max == null || ping <= max;
            return meetsMin && meetsMax;
        }

        public String describe() {
            if (min != null && max != null) {
                return min + " - " + max + " ms";
            }
            if (min != null) {
                return ">= " + min + " ms";
            }
            if (max != null) {
                return "<= " + max + " ms";
            }
            return "unbounded";
        }
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<ThresholdTier, ThresholdRange> DEFAULT_THRESHOLDS = createDefaultThresholds();
    private static final PingSettings INSTANCE = new PingSettings();

    private Map<ThresholdTier, ThresholdRange> pingThresholds = new EnumMap<>(DEFAULT_THRESHOLDS);
    private boolean showPingOnTab = true;
    private Path configFile;

    private PingSettings() {}

    public static PingSettings getInstance() {
        return INSTANCE;
    }

    public synchronized void load() {
        Path configDir = Paths.get(System.getProperty("user.dir")).resolve("config/player-ping");
        configFile = configDir.resolve("config.json");

        try {
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }

            if (!Files.exists(configFile)) {
                pingThresholds = new EnumMap<>(DEFAULT_THRESHOLDS);
                saveCurrentConfig();
                PlayerPing.LOGGER.info("Created default configuration file.");
                logConfiguration();
                return;
            }

            try (Reader reader = Files.newBufferedReader(configFile)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                pingThresholds = loadThresholds(root.get("pingThresholds"));
                if (root.has("showPingOnTab") && root.get("showPingOnTab").isJsonPrimitive()) {
                    showPingOnTab = root.get("showPingOnTab").getAsBoolean();
                }
            }

            // Re-save to normalize any legacy/partial configuration.
            saveCurrentConfig();
        } catch (Exception e) {
            PlayerPing.LOGGER.error("Error loading configuration! Using defaults.", e);
            pingThresholds = new EnumMap<>(DEFAULT_THRESHOLDS);
            showPingOnTab = true;
        }

        logConfiguration();
    }

    public synchronized void updateThreshold(ThresholdTier tier, Integer min, Integer max) throws IOException {
        if (min == null && max == null) {
            throw new IllegalArgumentException("A threshold must define min, max, or both.");
        }
        if (min != null && min < 0) {
            throw new IllegalArgumentException("Minimum threshold cannot be negative.");
        }
        if (max != null && max < 0) {
            throw new IllegalArgumentException("Maximum threshold cannot be negative.");
        }
        if (min != null && max != null && min > max) {
            throw new IllegalArgumentException("Minimum threshold cannot be greater than maximum threshold.");
        }

        ThresholdRange newRange = new ThresholdRange(min, max);
        for (ThresholdTier otherTier : ThresholdTier.values()) {
            if (otherTier == tier) {
                continue;
            }

            ThresholdRange otherRange = pingThresholds.getOrDefault(otherTier, DEFAULT_THRESHOLDS.get(otherTier));
            if (rangesOverlap(newRange, otherRange)) {
                throw new IllegalArgumentException("Range overlaps with " + otherTier.key() + " (" + otherRange.describe() + ").");
            }
        }

        pingThresholds.put(tier, newRange);
        saveCurrentConfig();
    }

    public synchronized ThresholdTier getThresholdTier(int ping) {
        for (ThresholdTier tier : ThresholdTier.values()) {
            if (pingThresholds.getOrDefault(tier, DEFAULT_THRESHOLDS.get(tier)).matches(ping)) {
                return tier;
            }
        }
        return ThresholdTier.TERRIBLE;
    }

    public synchronized ThresholdRange getThresholdRange(ThresholdTier tier) {
        return pingThresholds.getOrDefault(tier, DEFAULT_THRESHOLDS.get(tier));
    }

    public synchronized boolean getShowPingOnTab() {
        return showPingOnTab;
    }

    private void saveCurrentConfig() throws IOException {
        if (configFile == null) {
            Path configDir = Paths.get(System.getProperty("user.dir")).resolve("config/player-ping");
            configFile = configDir.resolve("config.json");
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }
        }

        JsonObject root = new JsonObject();
        JsonObject thresholdsObject = new JsonObject();

        for (ThresholdTier tier : ThresholdTier.values()) {
            ThresholdRange range = pingThresholds.getOrDefault(tier, DEFAULT_THRESHOLDS.get(tier));
            JsonObject rangeObject = new JsonObject();
            if (range.min() != null) {
                rangeObject.addProperty("min", range.min());
            }
            if (range.max() != null) {
                rangeObject.addProperty("max", range.max());
            }
            thresholdsObject.add(tier.key(), rangeObject);
        }

        root.add("pingThresholds", thresholdsObject);
        root.addProperty("showPingOnTab", showPingOnTab);

        try (Writer writer = Files.newBufferedWriter(configFile)) {
            GSON.toJson(root, writer);
        }
    }

    private Map<ThresholdTier, ThresholdRange> loadThresholds(JsonElement thresholdsElement) {
        if (thresholdsElement == null || !thresholdsElement.isJsonObject()) {
            return new EnumMap<>(DEFAULT_THRESHOLDS);
        }

        JsonObject thresholdsObject = thresholdsElement.getAsJsonObject();

        if (isLegacyThresholdFormat(thresholdsObject)) {
            return loadLegacyThresholds(thresholdsObject);
        }

        EnumMap<ThresholdTier, ThresholdRange> loaded = new EnumMap<>(ThresholdTier.class);
        loaded.put(ThresholdTier.EXCELLENT, parseRange(thresholdsObject, "excellent", DEFAULT_THRESHOLDS.get(ThresholdTier.EXCELLENT)));
        loaded.put(ThresholdTier.GOOD, parseRange(thresholdsObject, "good", DEFAULT_THRESHOLDS.get(ThresholdTier.GOOD)));
        loaded.put(ThresholdTier.FAIR, parseRange(thresholdsObject, "fair", parseRange(thresholdsObject, "medium", DEFAULT_THRESHOLDS.get(ThresholdTier.FAIR))));
        loaded.put(ThresholdTier.POOR, parseRange(thresholdsObject, "poor", parseRange(thresholdsObject, "bad", DEFAULT_THRESHOLDS.get(ThresholdTier.POOR))));
        loaded.put(ThresholdTier.TERRIBLE, parseRange(thresholdsObject, "terrible", DEFAULT_THRESHOLDS.get(ThresholdTier.TERRIBLE)));
        return loaded;
    }

    private Map<ThresholdTier, ThresholdRange> loadLegacyThresholds(JsonObject legacy) {
        int excellent = getPositiveInt(legacy, "excellent", 50);
        int good = getPositiveInt(legacy, "good", 100);
        int fair = getPositiveInt(legacy, "medium", 200);
        int poor = getPositiveInt(legacy, "bad", 300);

        EnumMap<ThresholdTier, ThresholdRange> converted = new EnumMap<>(ThresholdTier.class);
        converted.put(ThresholdTier.EXCELLENT, new ThresholdRange(null, excellent));
        converted.put(ThresholdTier.GOOD, new ThresholdRange(excellent + 1, good));
        converted.put(ThresholdTier.FAIR, new ThresholdRange(good + 1, fair));
        converted.put(ThresholdTier.POOR, new ThresholdRange(fair + 1, poor));
        converted.put(ThresholdTier.TERRIBLE, new ThresholdRange(poor + 1, null));
        return converted;
    }

    private ThresholdRange parseRange(JsonObject thresholdsObject, String key, ThresholdRange fallback) {
        JsonElement element = thresholdsObject.get(key);
        if (element == null) {
            return fallback;
        }

        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
            int value = element.getAsInt();
            return value >= 0 ? new ThresholdRange(null, value) : fallback;
        }

        if (!element.isJsonObject()) {
            return fallback;
        }

        JsonObject range = element.getAsJsonObject();
        Integer min = range.has("min") && range.get("min").isJsonPrimitive() ? range.get("min").getAsInt() : null;
        Integer max = range.has("max") && range.get("max").isJsonPrimitive() ? range.get("max").getAsInt() : null;

        if (min != null && min < 0) {
            min = null;
        }
        if (max != null && max < 0) {
            max = null;
        }

        if (min == null && max == null) {
            return fallback;
        }
        if (min != null && max != null && min > max) {
            return fallback;
        }

        return new ThresholdRange(min, max);
    }

    private static boolean isLegacyThresholdFormat(JsonObject thresholdsObject) {
        return isNumericPrimitive(thresholdsObject, "excellent")
                || isNumericPrimitive(thresholdsObject, "good")
                || isNumericPrimitive(thresholdsObject, "medium")
                || isNumericPrimitive(thresholdsObject, "bad");
    }

    private static boolean isNumericPrimitive(JsonObject object, String key) {
        return object.has(key)
                && object.get(key).isJsonPrimitive()
                && object.get(key).getAsJsonPrimitive().isNumber();
    }

    private static int getPositiveInt(JsonObject object, String key, int fallback) {
        if (!isNumericPrimitive(object, key)) {
            return fallback;
        }

        int value = object.get(key).getAsInt();
        return value >= 0 ? value : fallback;
    }

    private static boolean rangesOverlap(ThresholdRange first, ThresholdRange second) {
        long firstMin = first.min() != null ? first.min() : Long.MIN_VALUE;
        long firstMax = first.max() != null ? first.max() : Long.MAX_VALUE;
        long secondMin = second.min() != null ? second.min() : Long.MIN_VALUE;
        long secondMax = second.max() != null ? second.max() : Long.MAX_VALUE;
        return firstMin <= secondMax && secondMin <= firstMax;
    }

    private void logConfiguration() {
        StringJoiner joiner = new StringJoiner(", ");
        for (ThresholdTier tier : ThresholdTier.values()) {
            ThresholdRange range = pingThresholds.getOrDefault(tier, DEFAULT_THRESHOLDS.get(tier));
            joiner.add(tier.key() + "=" + range.describe());
        }

        PlayerPing.LOGGER.info("Config loaded successfully!");
        PlayerPing.LOGGER.info("Ping thresholds: {}", joiner);
        PlayerPing.LOGGER.info("Showing ping on tab: {}", showPingOnTab);
    }

    private static Map<ThresholdTier, ThresholdRange> createDefaultThresholds() {
        EnumMap<ThresholdTier, ThresholdRange> defaults = new EnumMap<>(ThresholdTier.class);
        defaults.put(ThresholdTier.EXCELLENT, new ThresholdRange(null, 50));
        defaults.put(ThresholdTier.GOOD, new ThresholdRange(51, 100));
        defaults.put(ThresholdTier.FAIR, new ThresholdRange(101, 200));
        defaults.put(ThresholdTier.POOR, new ThresholdRange(201, 300));
        defaults.put(ThresholdTier.TERRIBLE, new ThresholdRange(301, null));
        return defaults;
    }
}
