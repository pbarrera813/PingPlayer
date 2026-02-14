package player.ping;

import net.minecraft.ChatFormatting;

import java.util.List;

public final class PingUtils {

    private PingUtils() {}

    public record PingQuality(ChatFormatting color, String quality) {}

    public static PingQuality getPingQuality(int ping) {
        List<Integer> thresholds = PingSettings.getInstance().getPingThresholds();
        if (ping <= thresholds.get(0)) return new PingQuality(ChatFormatting.GREEN, "excellent");
        if (ping <= thresholds.get(1)) return new PingQuality(ChatFormatting.YELLOW, "good");
        if (ping <= thresholds.get(2)) return new PingQuality(ChatFormatting.GOLD, "ok");
        if (ping <= thresholds.get(3)) return new PingQuality(ChatFormatting.RED, "bad");
        return new PingQuality(ChatFormatting.DARK_RED, "terrible");
    }

    public static ChatFormatting getPingColor(int ping) {
        return getPingQuality(ping).color();
    }
}
