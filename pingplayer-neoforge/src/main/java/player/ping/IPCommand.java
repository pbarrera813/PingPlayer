package player.ping;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.net.SocketAddress;

public final class IPCommand {

    private IPCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ip")
                .requires(source -> source.hasPermission(2))
                .executes(IPCommand::executeHelp)
                .then(Commands.literal("help")
                        .executes(IPCommand::executeHelp))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(IPCommand::executeIP))
        );
    }

    private static int executeIP(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        try {
            ServerPlayer target = EntityArgument.getPlayer(context, "player");
            SocketAddress address = target.connection.getRemoteAddress();

            if (address == null) {
                source.sendFailure(Component.literal("Could not retrieve IP address for " + target.getGameProfile().getName()));
                return 0;
            }

            String ipAddress = address.toString();
            // Remove the leading '/' and port from the address (format: /ip:port)
            if (ipAddress.startsWith("/")) {
                ipAddress = ipAddress.substring(1);
            }
            if (ipAddress.contains(":")) {
                ipAddress = ipAddress.substring(0, ipAddress.lastIndexOf(':'));
            }

            String finalIp = ipAddress;
            Component message = Component.literal(target.getGameProfile().getName() + "'s IP address is: ")
                    .withStyle(style -> style.withColor(ChatFormatting.GOLD))
                    .append(Component.literal(finalIp)
                            .withStyle(style -> style.withColor(ChatFormatting.AQUA)));

            source.sendSuccess(() -> message, false);
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("Player not found or not online."));
            return 0;
        }
    }

    private static int executeHelp(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        source.sendSuccess(() -> Component.literal("----- IP Command Help -----")
                .withStyle(style -> style.withColor(ChatFormatting.GOLD)), false);
        source.sendSuccess(() -> Component.literal("/ip <player>")
                .withStyle(style -> style.withColor(ChatFormatting.AQUA))
                .append(Component.literal(" - Displays the IP address of the specified player.")
                        .withStyle(style -> style.withColor(ChatFormatting.GOLD))), false);
        source.sendSuccess(() -> Component.literal("/ip help")
                .withStyle(style -> style.withColor(ChatFormatting.AQUA))
                .append(Component.literal(" - Displays this help message.")
                        .withStyle(style -> style.withColor(ChatFormatting.GOLD))), false);
        return 1;
    }
}
