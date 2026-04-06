package player.ping;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public final class PingDumpReport {

    private static final DateTimeFormatter FILE_DATE_FORMAT = DateTimeFormatter.ofPattern("MMddyyyy");
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String LOADER_TAG = "neoforge";

    private PingDumpReport() {}

    public static Path generate(CommandSourceStack source) throws IOException {
        Path configDir = PingSettings.getInstance().getConfigDirectory();
        Path dumpDir = configDir.resolve("crash_reports");
        Files.createDirectories(dumpDir);

        String datePart = LocalDate.now().format(FILE_DATE_FORMAT);
        String baseFileName = "pingplayer_crash_" + datePart + "_" + LOADER_TAG;
        Path dumpFile = resolveUniqueDumpPath(dumpDir, baseFileName);

        try (BufferedWriter writer = Files.newBufferedWriter(dumpFile, StandardCharsets.UTF_8)) {
            writeHeader(writer, source);
            writeServerSnapshot(writer, source);
            writeConfigSnapshot(writer);
            writePlayerSnapshot(writer, source);
            writeRecentEvents(writer);
            writeThreadDump(writer);
            writeLatestLog(writer);
        }

        NeoForgeCompat.debug("dump", "Generated diagnostic dump at %s by '%s'.", dumpFile, source.getTextName());
        return dumpFile;
    }

    private static Path resolveUniqueDumpPath(Path dumpDir, String baseFileName) {
        Path firstCandidate = dumpDir.resolve(baseFileName + ".txt");
        if (!Files.exists(firstCandidate)) {
            return firstCandidate;
        }

        for (int i = 2; i < 1000; i++) {
            Path candidate = dumpDir.resolve(baseFileName + "_" + i + ".txt");
            if (!Files.exists(candidate)) {
                return candidate;
            }
        }

        return dumpDir.resolve(baseFileName + "_" + System.currentTimeMillis() + ".txt");
    }

    private static void writeHeader(BufferedWriter writer, CommandSourceStack source) throws IOException {
        writeSectionTitle(writer, "PINGPLAYER DIAGNOSTIC DUMP");
        writer.write("Generated at: " + DATE_TIME_FORMAT.format(LocalDateTime.now()));
        writer.newLine();
        writer.write("Requested by: " + source.getTextName());
        writer.newLine();
        writer.write("Loader: " + LOADER_TAG);
        writer.newLine();
        writer.write("Mod version: " + getModVersion());
        writer.newLine();
        writer.newLine();
    }

    private static void writeServerSnapshot(BufferedWriter writer, CommandSourceStack source) throws IOException {
        writeSectionTitle(writer, "SERVER SNAPSHOT");

        Object server = source.getServer();
        writer.write("Server class: " + server.getClass().getName());
        writer.newLine();
        writer.write("Server version: " + reflectString(server, "getServerVersion", "unknown"));
        writer.newLine();
        writer.write("Server mod name: " + reflectString(server, "getServerModName", "unknown"));
        writer.newLine();
        writer.write("Is dedicated server: " + reflectBoolean(server, "isDedicatedServer", false));
        writer.newLine();
        writer.write("Online player count: " + source.getServer().getPlayerList().getPlayers().size());
        writer.newLine();
        writer.write("Java version: " + System.getProperty("java.version", "unknown"));
        writer.newLine();
        writer.write("OS: " + System.getProperty("os.name", "unknown") + " " + System.getProperty("os.version", "unknown"));
        writer.newLine();
        writer.newLine();
    }

    private static void writeConfigSnapshot(BufferedWriter writer) throws IOException {
        writeSectionTitle(writer, "PINGPLAYER CONFIG");

        PingSettings settings = PingSettings.getInstance();
        writer.write("Config directory: " + settings.getConfigDirectory().toAbsolutePath());
        writer.newLine();
        writer.write("showPingOnTab: " + settings.getShowPingOnTab());
        writer.newLine();
        writer.write("debugPermissionChecks: " + settings.getDebugPermissionChecks());
        writer.newLine();
        writer.write("minLuckPermsWeight: " + settings.getMinLuckPermsWeight());
        writer.newLine();
        writer.write("Thresholds:");
        writer.newLine();
        for (PingSettings.ThresholdTier tier : PingSettings.ThresholdTier.values()) {
            PingSettings.ThresholdRange range = settings.getThresholdRange(tier);
            writer.write(" - " + tier.key() + ": " + range.describe());
            writer.newLine();
        }
        writer.newLine();
    }

    private static void writePlayerSnapshot(BufferedWriter writer, CommandSourceStack source) throws IOException {
        writeSectionTitle(writer, "ONLINE PLAYERS");

        List<ServerPlayer> players = source.getServer().getPlayerList().getPlayers();
        if (players.isEmpty()) {
            writer.write("No online players.");
            writer.newLine();
            writer.newLine();
            return;
        }

        Set<String> opNames = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        opNames.addAll(Arrays.asList(source.getServer().getPlayerList().getOpNames()));

        for (ServerPlayer player : players) {
            String name = NeoForgeCompat.getProfileName(player);
            int ping = player.connection.latency();
            PingUtils.PingQuality quality = PingUtils.getPingQuality(ping);
            boolean isOp = opNames.contains(name);
            writer.write(String.format(Locale.ROOT,
                    "- %s | uuid=%s | ping=%dms | quality=%s | op=%s",
                    name,
                    player.getUUID(),
                    ping,
                    quality.quality(),
                    isOp));
            writer.newLine();
        }
        writer.newLine();
    }

    private static void writeRecentEvents(BufferedWriter writer) throws IOException {
        writeSectionTitle(writer, "RECENT PINGPLAYER EVENTS");

        List<String> events = NeoForgeCompat.getRecentEventsSnapshot();
        if (events.isEmpty()) {
            writer.write("No recorded events yet.");
            writer.newLine();
            writer.newLine();
            return;
        }

        for (String event : events) {
            writer.write(event);
            writer.newLine();
        }
        writer.newLine();
    }

    private static void writeThreadDump(BufferedWriter writer) throws IOException {
        writeSectionTitle(writer, "JVM THREAD DUMP");

        Map<Thread, StackTraceElement[]> threadDump = Thread.getAllStackTraces();
        for (Map.Entry<Thread, StackTraceElement[]> entry : threadDump.entrySet()) {
            Thread thread = entry.getKey();
            writer.write(String.format(Locale.ROOT,
                    "Thread \"%s\" (id=%d, state=%s, daemon=%s)",
                    thread.getName(),
                    thread.threadId(),
                    thread.getState(),
                    thread.isDaemon()));
            writer.newLine();

            StackTraceElement[] stack = entry.getValue();
            if (stack.length == 0) {
                writer.write("  <no stack trace>");
                writer.newLine();
            } else {
                for (StackTraceElement element : stack) {
                    writer.write("  at " + element);
                    writer.newLine();
                }
            }
            writer.newLine();
        }
    }

    private static void writeLatestLog(BufferedWriter writer) throws IOException {
        writeSectionTitle(writer, "SERVER latest.log");

        Path latestLog = Path.of("logs", "latest.log").toAbsolutePath().normalize();
        writer.write("Log path: " + latestLog);
        writer.newLine();
        if (!Files.exists(latestLog)) {
            writer.write("latest.log not found.");
            writer.newLine();
            writer.newLine();
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(latestLog, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                writer.write(line);
                writer.newLine();
            }
        } catch (Exception e) {
            writer.write("Failed to read latest.log: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            writer.newLine();
        }
        writer.newLine();
    }

    private static String getModVersion() {
        try {
            Class<?> modListClass = Class.forName("net.neoforged.fml.ModList");
            Object modList = modListClass.getMethod("get").invoke(null);
            Object optional = modListClass.getMethod("getModContainerById", String.class).invoke(modList, PlayerPing.MOD_ID);
            if (optional instanceof java.util.Optional<?> containerOpt) {
                Object container = containerOpt.orElse(null);
                if (container == null) {
                    return "unknown";
                }
                Object modInfo = container.getClass().getMethod("getModInfo").invoke(container);
                Object version = modInfo.getClass().getMethod("getVersion").invoke(modInfo);
                return version != null ? String.valueOf(version) : "unknown";
            }
        } catch (Exception ignored) {
            // Fallback to unknown on any API differences.
        }
        return "unknown";
    }

    private static void writeSectionTitle(BufferedWriter writer, String title) throws IOException {
        writer.write("========== " + title + " ==========");
        writer.newLine();
    }

    private static String reflectString(Object target, String methodName, String fallback) {
        try {
            Object value = target.getClass().getMethod(methodName).invoke(target);
            return value != null ? String.valueOf(value) : fallback;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static boolean reflectBoolean(Object target, String methodName, boolean fallback) {
        try {
            Object value = target.getClass().getMethod(methodName).invoke(target);
            return value instanceof Boolean b ? b : fallback;
        } catch (Exception ignored) {
            return fallback;
        }
    }
}
