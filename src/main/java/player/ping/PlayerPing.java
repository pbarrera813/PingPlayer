package player.ping;

import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlayerPing implements DedicatedServerModInitializer {

    public static final String MOD_ID = "player-ping";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeServer() {
        LOGGER.info("PingPlayer has been enabled. You can ping players using /ping <playername>");

        PingSettings.getInstance().load();
        FabricCompat.debug("startup", "Debug mode is enabled. Extended diagnostics will be logged.");

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            PingCommand.register(dispatcher);
            IPCommand.register(dispatcher);
            PingPlayerCommand.register(dispatcher);
            FabricCompat.debug("startup", "Registered commands: /ping, /ip, /pingplayer");
        });

        ServerTickEvents.END_SERVER_TICK.register(PingSampler::onTick);
        ServerTickEvents.END_SERVER_TICK.register(TabUpdateTask::onEndTick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> PingSampler.remove(handler.player.getUUID()));
        FabricCompat.debug("startup", "Registered ping sampler, tab-list update and disconnect events.");

        if (FabricLoader.getInstance().isModLoaded("placeholder-api")) {
            try {
                PlaceholderHook.register();
                FabricCompat.debug("startup", "Registered Placeholder API placeholders.");
            } catch (RuntimeException | LinkageError e) {
                LOGGER.warn("Placeholder API is installed but its placeholders could not be registered: {}", e.toString());
            }
        }
    }
}
