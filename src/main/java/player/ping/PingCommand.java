package player.ping;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class PingCommand {

    private PingCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ping")
                .requires(source -> source.hasPermission(0))
                .executes(PingCommand::executeSelf)
                .then(Commands.literal("help")
                        .executes(PingCommand::executeHelp))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(PingCommand::executeOther))
        );
    }

    private static int executeSelf(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();

        if (player == null) {
            source.sendFailure(Component.literal("You must be a player to use this command without arguments!"));
            return 0;
        }

        sendPingMessage(source, player);
        return 1;
    }

    private static int executeOther(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        try {
            ServerPlayer target = EntityArgument.getPlayer(context, "player");
            sendPingMessage(source, target);
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("Player not found or offline. Please enter a valid username!"));
            return 0;
        }
    }

    private static int executeHelp(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        source.sendSuccess(() -> Component.literal("Usage:")
                .withStyle(style -> style.withColor(net.minecraft.ChatFormatting.AQUA)), false);
        source.sendSuccess(() -> Component.literal("/ping - Check your own ping")
                .withStyle(style -> style.withColor(net.minecraft.ChatFormatting.GREEN)), false);
        source.sendSuccess(() -> Component.literal("/ping <player> - Check another player's ping")
                .withStyle(style -> style.withColor(net.minecraft.ChatFormatting.YELLOW)), false);
        return 1;
    }

    private static void sendPingMessage(CommandSourceStack source, ServerPlayer target) {
        int ping = target.connection.latency();
        PingUtils.PingQuality quality = PingUtils.getPingQuality(ping);

        Component message = Component.literal(target.getGameProfile().getName() + "'s latency is ")
                .withStyle(style -> style.withColor(net.minecraft.ChatFormatting.GREEN))
                .append(Component.literal(ping + " ms, which is " + quality.quality() + "!")
                        .withStyle(style -> style.withColor(quality.color())));

        source.sendSuccess(() -> message, false);
        PlayerPing.LOGGER.info("{}: {} ({}ms)", target.getGameProfile().getName(), quality.quality(), ping);
    }
}
