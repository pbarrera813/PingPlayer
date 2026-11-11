package player.ping;

import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;

public final class PingUtils {

    private PingUtils() {}

    public record PingQuality(ChatFormatting color, String quality) {}

    public static int currentPing(ServerPlayer player) {
        Integer sampled = PingSampler.currentMs(player.getUUID());
        return sampled != null ? sampled : player.connection.latency();
    }

    public static String statsSuffix(ServerPlayer player) {
        PingSampler.Stats stats = PingSampler.stats(player.getUUID());
        return stats == null ? "" : " (jitter " + stats.jitterMs() + " ms, loss " + stats.lossPercent() + "%)";
    }

    public static PingQuality getPingQuality(int ping) {
        PingSettings.ThresholdTier tier = PingSettings.getInstance().getThresholdTier(ping);
        return new PingQuality(getThresholdColor(tier), getThresholdLabel(tier));
    }

    public static ChatFormatting getPingColor(int ping) {
        return getPingQuality(ping).color();
    }

    public static ChatFormatting getThresholdColor(PingSettings.ThresholdTier tier) {
        return switch (tier) {
            case EXCELLENT -> ChatFormatting.GREEN;
            case GOOD -> ChatFormatting.YELLOW;
            case FAIR -> ChatFormatting.GOLD;
            case POOR -> ChatFormatting.RED;
            case TERRIBLE -> ChatFormatting.DARK_RED;
        };
    }

    public static String getThresholdLabel(PingSettings.ThresholdTier tier) {
        return tier.key();
    }
}
