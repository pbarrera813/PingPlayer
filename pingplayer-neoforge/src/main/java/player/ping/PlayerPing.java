package player.ping;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(PlayerPing.MOD_ID)
public class PlayerPing {

    public static final String MOD_ID = "player_ping";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final String PLACEHOLDER_API_ID = "placeholder_api_neoforge";

    public PlayerPing() {
        LOGGER.info("PingPlayer has been enabled. You can ping players using /ping <playername>");

        PingSettings.getInstance().load();
        NeoForgeCompat.debug("startup", "Debug mode is enabled. Extended diagnostics will be logged.");

        NeoForge.EVENT_BUS.register(this);

        if (ModList.get().isLoaded(PLACEHOLDER_API_ID)) {
            try {
                PlaceholderHook.register();
                NeoForgeCompat.debug("startup", "Registered Placeholder API placeholders.");
            } catch (RuntimeException | LinkageError e) {
                LOGGER.warn("Placeholder API is installed but its placeholders could not be registered: {}", e.toString());
            }
        }
    }

    @SubscribeEvent
    public void onCommandsRegister(RegisterCommandsEvent event) {
        PingCommand.register(event.getDispatcher());
        IPCommand.register(event.getDispatcher());
        PingPlayerCommand.register(event.getDispatcher());
        NeoForgeCompat.debug("startup", "Registered commands: /ping, /ip, /pingplayer");
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        PingSampler.onTick(event.getServer());
        TabUpdateTask.onEndTick(event.getServer());
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        PingSampler.remove(event.getEntity().getUUID());
    }
}
