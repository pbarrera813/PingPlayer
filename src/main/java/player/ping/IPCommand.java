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
                .requires(source -> FabricCompat.hasPermission(source, 0))
                .executes(IPCommand::executeSelfIP)
                .then(Commands.literal("help")
                        .executes(IPCommand::executeHelp))
                .then(Commands.argument("player", EntityArgument.player())
                        .requires(FabricCompat::hasAdminPermission)
                        .executes(IPCommand::executeTargetIP))
        );
    }

    private static int executeSelfIP(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        FabricCompat.debug("command/ip", "Received /ip from '%s'.", source.getTextName());

        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("Console must use /ip <player>."));
            FabricCompat.debug("command/ip", "Rejected /ip for non-player source '%s'.", source.getTextName());
            return 0;
        }

        SocketAddress address = player.connection.getRemoteAddress();
        if (address == null) {
            source.sendFailure(Component.literal("Could not retrieve your IP address."));
            FabricCompat.debug("command/ip", "Remote address was null for self lookup by '%s'.", source.getTextName());
            return 0;
        }

        String ipAddress = normalizeIpAddress(address);
        Component message = Component.literal("Your public IP is: ")
                .withStyle(style -> style.withColor(ChatFormatting.BLUE))
                .append(Component.literal(ipAddress)
                        .withStyle(style -> style.withColor(ChatFormatting.AQUA)));

        source.sendSuccess(() -> message, false);
        FabricCompat.debug("command/ip", "Resolved self IP for '%s'.", source.getTextName());
        return 1;
    }

    private static int executeTargetIP(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        FabricCompat.debug("command/ip", "Received /ip <player> from '%s'.", source.getTextName());
        if (!FabricCompat.hasAdminPermission(source)) {
            source.sendFailure(Component.literal("Only server admins can use /ip <player>."));
            FabricCompat.debug("command/ip", "Denied /ip <player> for '%s' due to missing admin permission.", source.getTextName());
            return 0;
        }

        try {
            ServerPlayer target = EntityArgument.getPlayer(context, "player");
            SocketAddress address = target.connection.getRemoteAddress();

            if (address == null) {
                source.sendFailure(Component.literal("Could not retrieve IP address for " + FabricCompat.getProfileName(target)));
                FabricCompat.debug("command/ip", "Remote address was null for target '%s'.", FabricCompat.getProfileName(target));
                return 0;
            }

            String finalIp = normalizeIpAddress(address);
            Component message = Component.literal(FabricCompat.getProfileName(target) + "'s public IP is: ")
                    .withStyle(style -> style.withColor(ChatFormatting.BLUE))
                    .append(Component.literal(finalIp)
                            .withStyle(style -> style.withColor(ChatFormatting.AQUA)));

            source.sendSuccess(() -> message, false);
            FabricCompat.debug("command/ip", "Resolved IP for target '%s' via requester '%s'.", FabricCompat.getProfileName(target), source.getTextName());
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("Player not found or not online."));
            FabricCompat.debug("command/ip", "Failed /ip for '%s': %s", source.getTextName(), e.getClass().getSimpleName());
            return 0;
        }
    }

    private static int executeHelp(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        FabricCompat.debug("command/ip", "Displayed /ip help for '%s'.", source.getTextName());
        source.sendSuccess(() -> Component.literal("IP Commands")
                .withStyle(style -> style.withColor(ChatFormatting.DARK_AQUA)), false);
        source.sendSuccess(() -> Component.literal("  /ip")
                .withStyle(style -> style.withColor(ChatFormatting.AQUA).withBold(true))
                .append(Component.literal("  Show your own public IP.")
                        .withStyle(style -> style.withColor(ChatFormatting.GRAY).withBold(false))), false);
        source.sendSuccess(() -> Component.literal("  /ip <player>")
                .withStyle(style -> style.withColor(ChatFormatting.AQUA).withBold(true))
                .append(Component.literal("  Show another player's public IP (admins only).")
                        .withStyle(style -> style.withColor(ChatFormatting.GRAY).withBold(false))), false);
        source.sendSuccess(() -> Component.literal("  /ip help")
                .withStyle(style -> style.withColor(ChatFormatting.AQUA).withBold(true))
                .append(Component.literal("  Show this help message.")
                        .withStyle(style -> style.withColor(ChatFormatting.GRAY).withBold(false))), false);
        return 1;
    }

    private static String normalizeIpAddress(SocketAddress address) {
        String ipAddress = address.toString();
        // Remove leading '/' and trailing ':port' if present.
        if (ipAddress.startsWith("/")) {
            ipAddress = ipAddress.substring(1);
        }

        if (ipAddress.startsWith("[") && ipAddress.contains("]:")) {
            int bracketEnd = ipAddress.indexOf("]:");
            if (bracketEnd > 1) {
                return ipAddress.substring(1, bracketEnd);
            }
        }

        int lastColon = ipAddress.lastIndexOf(':');
        if (lastColon > 0 && ipAddress.indexOf(':') == lastColon) {
            ipAddress = ipAddress.substring(0, lastColon);
        }
        return ipAddress;
    }
}
