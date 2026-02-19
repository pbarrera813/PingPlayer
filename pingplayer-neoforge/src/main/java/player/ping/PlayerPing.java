package player.ping;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(PlayerPing.MOD_ID)
public class PlayerPing {

    public static final String MOD_ID = "player_ping";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public PlayerPing() {
        LOGGER.info("PingPlayer has been enabled. You can ping players using /ping <playername>");

        // Load configuration
        PingSettings.getInstance().load();

        // Register this instance for NeoForge event bus events
        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onCommandsRegister(RegisterCommandsEvent event) {
        PingCommand.register(event.getDispatcher());
        IPCommand.register(event.getDispatcher());
        PingPlayerCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        TabUpdateTask.onEndTick(event.getServer());
    }
}
