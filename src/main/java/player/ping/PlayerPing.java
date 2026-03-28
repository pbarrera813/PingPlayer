package player.ping;

import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlayerPing implements DedicatedServerModInitializer {

    public static final String MOD_ID = "player-ping";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeServer() {
        LOGGER.info("PingPlayer has been enabled. You can ping players using /ping <playername>");

        // Load configuration
        PingSettings.getInstance().load();
        FabricCompat.debug("startup", "Debug mode is enabled. Extended diagnostics will be logged.");

        // Register commands
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            PingCommand.register(dispatcher);
            IPCommand.register(dispatcher);
            PingPlayerCommand.register(dispatcher);
            FabricCompat.debug("startup", "Registered commands: /ping, /ip, /pingplayer");
        });

        // Register tab update task on server tick
        ServerTickEvents.END_SERVER_TICK.register(TabUpdateTask::onEndTick);
        FabricCompat.debug("startup", "Registered tab-list update tick event.");
    }
}
