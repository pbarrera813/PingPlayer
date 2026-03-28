package player.ping;

import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.EnumSet;
import java.util.List;

public final class TabUpdateTask {

    private static final long DEBUG_TAB_THROTTLE_MS = 5000L;

    private TabUpdateTask() {}

    public static void onEndTick(MinecraftServer server) {
        if (!PingSettings.getInstance().getShowPingOnTab()) {
            FabricCompat.debugThrottled("tab/tick", "tab-disabled", DEBUG_TAB_THROTTLE_MS, "Skipped tab update tick because showPingOnTab=false.");
            return;
        }

        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        if (players.isEmpty()) {
            FabricCompat.debugThrottled("tab/tick", "tab-empty", DEBUG_TAB_THROTTLE_MS, "Skipped tab update tick because there are no online players.");
            return;
        }

        // The mixin on ServerPlayer.getTabListDisplayName() provides the ping-decorated name.
        // This constructor reads getTabListDisplayName() from each player internally.
        ClientboundPlayerInfoUpdatePacket packet = new ClientboundPlayerInfoUpdatePacket(
                EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME),
                players
        );

        server.getPlayerList().broadcastAll(packet);
        FabricCompat.debugThrottled("tab/tick", "tab-broadcast", DEBUG_TAB_THROTTLE_MS,
                "Broadcasted tab display-name refresh packet for %d online players.", players.size());
    }
}
