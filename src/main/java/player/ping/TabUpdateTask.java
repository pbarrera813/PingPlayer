package player.ping;

import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.EnumSet;
import java.util.List;

public final class TabUpdateTask {

    private TabUpdateTask() {}

    public static void onEndTick(MinecraftServer server) {
        if (!PingSettings.getInstance().getShowPingOnTab()) {
            return;
        }

        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        if (players.isEmpty()) {
            return;
        }

        // The mixin on ServerPlayer.getTabListDisplayName() provides the ping-decorated name.
        // This constructor reads getTabListDisplayName() from each player internally.
        ClientboundPlayerInfoUpdatePacket packet = new ClientboundPlayerInfoUpdatePacket(
                EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME),
                players
        );

        server.getPlayerList().broadcastAll(packet);
    }
}
