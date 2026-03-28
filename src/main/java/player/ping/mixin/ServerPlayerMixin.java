package player.ping.mixin;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import player.ping.FabricCompat;
import player.ping.PingSettings;
import player.ping.PingUtils;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {

    private static final long DEBUG_TAB_ENTRY_THROTTLE_MS = 3000L;

    @Inject(method = "getTabListDisplayName", at = @At("HEAD"), cancellable = true)
    private void pingplayer$getTabListDisplayName(CallbackInfoReturnable<Component> cir) {
        if (!PingSettings.getInstance().getShowPingOnTab()) {
            return;
        }

        ServerPlayer self = (ServerPlayer) (Object) this;
        int ping = self.connection.latency();
        String playerName = FabricCompat.getProfileName(self);
        ChatFormatting color = PingUtils.getPingColor(ping);

        Component formattedTabName = Component.literal(playerName)
                .append(Component.literal(" [" + ping + " ms]").withStyle(style -> style.withColor(color)));

        FabricCompat.debugThrottled("tab/mixin", "tab-entry-" + playerName.toLowerCase(), DEBUG_TAB_ENTRY_THROTTLE_MS,
                "Generated tab display for '%s': ping=%dms color=%s", playerName, ping, color.name());
        cir.setReturnValue(formattedTabName);
    }
}
