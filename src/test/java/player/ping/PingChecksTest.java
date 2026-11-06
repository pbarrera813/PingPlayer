package player.ping;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PingChecksTest {

    private static final long MS = 1_000_000L;

    @Test
    void defaultTierBoundaries() {
        PingSettings settings = PingSettings.getInstance();
        assertEquals(PingSettings.ThresholdTier.EXCELLENT, settings.getThresholdTier(50));
        assertEquals(PingSettings.ThresholdTier.GOOD, settings.getThresholdTier(51));
        assertEquals(PingSettings.ThresholdTier.GOOD, settings.getThresholdTier(100));
        assertEquals(PingSettings.ThresholdTier.FAIR, settings.getThresholdTier(101));
        assertEquals(PingSettings.ThresholdTier.FAIR, settings.getThresholdTier(200));
        assertEquals(PingSettings.ThresholdTier.POOR, settings.getThresholdTier(201));
        assertEquals(PingSettings.ThresholdTier.POOR, settings.getThresholdTier(300));
        assertEquals(PingSettings.ThresholdTier.TERRIBLE, settings.getThresholdTier(301));
    }

    @Test
    void overlappingThresholdIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> PingSettings.getInstance().updateThreshold(PingSettings.ThresholdTier.GOOD, 40, 100));
    }

    @Test
    void windowAverageJitterAndLoss() {
        PingSampler.Window window = new PingSampler.Window();
        long now = 0;
        for (int rtt : new int[]{10, 20, 10}) {
            int id = window.send(now);
            window.receive(id, now + rtt * MS);
            now += 1000 * MS;
        }

        PingSampler.Stats stats = window.stats();
        assertEquals(13, stats.avgMs());
        assertEquals(10, stats.jitterMs());
        assertEquals(0, stats.lossPercent());

        window.send(now);
        window.send(now + 6000 * MS);
        assertEquals(25, window.stats().lossPercent());

        assertEquals(13, window.recentAvgMs(now + 6000 * MS));
        assertNull(window.recentAvgMs(now + 60_000 * MS));
    }
}
