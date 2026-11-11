package player.ping;

import net.minecraft.network.protocol.common.ClientboundPingPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PingSampler {

    private static final int SAMPLE_INTERVAL_TICKS = 20;
    private static final int WINDOW = 30;
    private static final int RECENT = 5;
    private static final long LOSS_AFTER_NANOS = 5_000_000_000L;
    private static final long FRESH_NANOS = 10_000_000_000L;
    private static final int LOST = -1;

    // Tagged id range so pongs sent for other mods or vanilla are ignored.
    private static final int ID_TAG = 0x50500000;
    private static final int ID_TAG_MASK = 0xFFFF0000;

    public record Stats(int avgMs, int jitterMs, int lossPercent) {}

    // Methods take the clock as a parameter and are synchronized: pongs arrive on the network thread.
    static final class Window {
        private final long[] outcomes = new long[WINDOW];
        private final Map<Integer, Long> pending = new HashMap<>();
        private int count;
        private int next;
        private int seq;
        private long lastReceivedAt;

        synchronized int send(long now) {
            for (Iterator<Map.Entry<Integer, Long>> it = pending.entrySet().iterator(); it.hasNext(); ) {
                if (now - it.next().getValue() > LOSS_AFTER_NANOS) {
                    it.remove();
                    add(LOST);
                }
            }
            int id = ID_TAG | (seq++ & 0xFFFF);
            pending.put(id, now);
            return id;
        }

        synchronized void receive(int id, long now) {
            Long sentAt = pending.remove(id);
            if (sentAt != null) {
                add(now - sentAt);
                lastReceivedAt = now;
            }
        }

        private void add(long outcome) {
            outcomes[next] = outcome;
            next = (next + 1) % WINDOW;
            count = Math.min(count + 1, WINDOW);
        }

        synchronized Integer recentAvgMs(long now) {
            if (count == 0 || now - lastReceivedAt > FRESH_NANOS) {
                return null;
            }
            long sum = 0;
            int taken = 0;
            for (int i = 1; i <= count && taken < RECENT; i++) {
                long value = outcomes[(next - i + WINDOW) % WINDOW];
                if (value != LOST) {
                    sum += value;
                    taken++;
                }
            }
            return taken == 0 ? null : toMs(sum / (double) taken);
        }

        synchronized Stats stats() {
            long sum = 0;
            long jitterSum = 0;
            int received = 0;
            int lost = 0;
            long previous = LOST;
            for (int i = count; i >= 1; i--) {
                long value = outcomes[(next - i + WINDOW) % WINDOW];
                if (value == LOST) {
                    lost++;
                    continue;
                }
                sum += value;
                if (previous != LOST) {
                    jitterSum += Math.abs(value - previous);
                }
                previous = value;
                received++;
            }
            if (received == 0) {
                return null;
            }
            double jitter = received > 1 ? jitterSum / (double) (received - 1) : 0;
            return new Stats(toMs(sum / (double) received), toMs(jitter), Math.round(100f * lost / count));
        }
    }

    private static final Map<UUID, Window> WINDOWS = new ConcurrentHashMap<>();

    private PingSampler() {}

    private static int toMs(double nanos) {
        return (int) Math.round(nanos / 1_000_000.0);
    }

    public static void onTick(MinecraftServer server) {
        if (!PingSettings.getInstance().getAccuratePing() || server.getTickCount() % SAMPLE_INTERVAL_TICKS != 0) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            Window window = WINDOWS.computeIfAbsent(player.getUUID(), id -> new Window());
            player.connection.send(new ClientboundPingPacket(window.send(System.nanoTime())));
        }
    }

    public static void onPong(UUID player, int id, long now) {
        if ((id & ID_TAG_MASK) != ID_TAG) {
            return;
        }
        Window window = WINDOWS.get(player);
        if (window != null) {
            window.receive(id, now);
        }
    }

    public static void remove(UUID player) {
        WINDOWS.remove(player);
    }

    public static Integer currentMs(UUID player) {
        Window window = WINDOWS.get(player);
        return window == null ? null : window.recentAvgMs(System.nanoTime());
    }

    public static Stats stats(UUID player) {
        Window window = WINDOWS.get(player);
        return window == null ? null : window.stats();
    }
}
