package player.ping;

import eu.pb4.placeholders.api.PlaceholderResult;
import eu.pb4.placeholders.api.Placeholders;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class PlaceholderHook {

    private PlaceholderHook() {}

    public static void register() {
        Placeholders.register(ResourceLocation.fromNamespaceAndPath("pingplayer", "ping"), (ctx, arg) -> {
            if (!ctx.hasPlayer()) {
                return PlaceholderResult.invalid("No player!");
            }
            return PlaceholderResult.value(ping(ctx.player()));
        });
        Placeholders.register(ResourceLocation.fromNamespaceAndPath("pingplayer", "quality"), (ctx, arg) -> {
            if (!ctx.hasPlayer()) {
                return PlaceholderResult.invalid("No player!");
            }
            return PlaceholderResult.value(quality(ctx.player()));
        });
    }

    private static Component ping(ServerPlayer player) {
        int ping = PingUtils.currentPing(player);
        return Component.literal(ping + " ms").withStyle(PingUtils.getPingColor(ping));
    }

    private static Component quality(ServerPlayer player) {
        PingUtils.PingQuality quality = PingUtils.getPingQuality(PingUtils.currentPing(player));
        return Component.literal(quality.quality()).withStyle(quality.color());
    }
}
