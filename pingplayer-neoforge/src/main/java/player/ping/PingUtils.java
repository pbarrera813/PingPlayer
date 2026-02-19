package player.ping;

import net.minecraft.ChatFormatting;

public final class PingUtils {

    private PingUtils() {}

    public record PingQuality(ChatFormatting color, String quality) {}

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
