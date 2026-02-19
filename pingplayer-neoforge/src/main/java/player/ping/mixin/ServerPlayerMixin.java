package player.ping.mixin;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import player.ping.PingSettings;
import player.ping.PingUtils;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {

    @Inject(method = "getTabListDisplayName", at = @At("HEAD"), cancellable = true)
    private void pingplayer$getTabListDisplayName(CallbackInfoReturnable<Component> cir) {
        if (!PingSettings.getInstance().getShowPingOnTab()) {
            return;
        }

        ServerPlayer self = (ServerPlayer) (Object) this;
        int ping = self.connection.latency();
        String playerName = self.getGameProfile().getName();
        ChatFormatting color = PingUtils.getPingColor(ping);

        Component formattedTabName = Component.literal(playerName)
                .append(Component.literal(" [" + ping + " ms]").withStyle(style -> style.withColor(color)));

        cir.setReturnValue(formattedTabName);
    }
}
