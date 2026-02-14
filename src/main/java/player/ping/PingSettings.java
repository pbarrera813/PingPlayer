package player.ping;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class PingSettings {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final List<Integer> DEFAULT_THRESHOLDS = Arrays.asList(50, 100, 200, 300);
    private static final PingSettings INSTANCE = new PingSettings();

    private List<Integer> pingThresholds = DEFAULT_THRESHOLDS;
    private boolean showPingOnTab = true;

    private PingSettings() {}

    public static PingSettings getInstance() {
        return INSTANCE;
    }

    public void load() {
        Path configDir = FabricLoader.getInstance().getConfigDir().resolve("player-ping");
        Path configFile = configDir.resolve("config.json");

        try {
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }

            if (!Files.exists(configFile)) {
                save(configFile);
                PlayerPing.LOGGER.info("Created default configuration file.");
                return;
            }

            try (Reader reader = Files.newBufferedReader(configFile)) {
                ConfigData data = GSON.fromJson(reader, ConfigData.class);
                if (data != null) {
                    if (data.pingThresholds != null) {
                        loadPingThresholds(data.pingThresholds);
                    }
                    showPingOnTab = data.showPingOnTab;
                }
            }

            // Re-save to sync any missing/corrected values
            save(configFile);

        } catch (IOException e) {
            PlayerPing.LOGGER.error("Error loading configuration!", e);
            pingThresholds = DEFAULT_THRESHOLDS;
            showPingOnTab = true;
        }

        logConfiguration();
    }

    private void loadPingThresholds(PingThresholds thresholds) {
        try {
            List<Integer> values = Arrays.asList(
                    thresholds.excellent > 0 ? thresholds.excellent : DEFAULT_THRESHOLDS.get(0),
                    thresholds.good > 0 ? thresholds.good : DEFAULT_THRESHOLDS.get(1),
                    thresholds.medium > 0 ? thresholds.medium : DEFAULT_THRESHOLDS.get(2),
                    thresholds.bad > 0 ? thresholds.bad : DEFAULT_THRESHOLDS.get(3)
            );
            pingThresholds = values.stream().sorted().collect(Collectors.toList());
        } catch (Exception e) {
            PlayerPing.LOGGER.warn("Error loading ping thresholds! Using default values.");
            pingThresholds = DEFAULT_THRESHOLDS;
        }
    }

    private void save(Path configFile) throws IOException {
        ConfigData data = new ConfigData();
        data.pingThresholds = new PingThresholds();
        data.pingThresholds.excellent = pingThresholds.get(0);
        data.pingThresholds.good = pingThresholds.get(1);
        data.pingThresholds.medium = pingThresholds.get(2);
        data.pingThresholds.bad = pingThresholds.get(3);
        data.showPingOnTab = showPingOnTab;

        try (Writer writer = Files.newBufferedWriter(configFile)) {
            GSON.toJson(data, writer);
        }
    }

    private void logConfiguration() {
        PlayerPing.LOGGER.info("Config loaded successfully!");
        PlayerPing.LOGGER.info("Latency thresholds: {}", pingThresholds.stream()
                .map(String::valueOf).collect(Collectors.joining(", ")));
        PlayerPing.LOGGER.info("Showing ping on tab: {}", showPingOnTab);
    }

    public List<Integer> getPingThresholds() {
        return Collections.unmodifiableList(pingThresholds);
    }

    public boolean getShowPingOnTab() {
        return showPingOnTab;
    }

    private static class ConfigData {
        PingThresholds pingThresholds = new PingThresholds();
        boolean showPingOnTab = true;
    }

    private static class PingThresholds {
        int excellent = 50;
        int good = 100;
        int medium = 200;
        int bad = 300;
    }
}
