package player.ping;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class PingCommand {

    private PingCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ping")
                .executes(PingCommand::executeSelf)
                .then(Commands.literal("help")
                        .executes(PingCommand::executeHelp))
                .then(Commands.literal("list")
                        .requires(NeoForgeCompat::hasAdminPermission)
                        .executes(PingCommand::executeList))
                .then(Commands.argument("player", EntityArgument.player())
                        .requires(NeoForgeCompat::hasAdminPermission)
                        .executes(PingCommand::executeOther))
        );
    }

    private static int executeSelf(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();

        if (player == null) {
            source.sendFailure(Component.literal("You must be a player to use this command without arguments!"));
            NeoForgeCompat.debug("command/ping", "Rejected /ping self from non-player source.");
            return 0;
        }

        sendSelfPingMessage(source, player);
        return 1;
    }

    private static int executeOther(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        NeoForgeCompat.debug("command/ping", "Received /ping <player> from '%s'.", source.getTextName());
        if (!NeoForgeCompat.hasAdminPermission(source)) {
            source.sendFailure(Component.literal("Only server admins can check another player's ping."));
            NeoForgeCompat.debug("command/ping", "Denied /ping <player> for '%s' due to missing admin permission.", source.getTextName());
            return 0;
        }

        try {
            ServerPlayer target = EntityArgument.getPlayer(context, "player");
            sendPingMessage(source, target);
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("Player not found or offline. Please enter a valid username!"));
            NeoForgeCompat.debug("command/ping", "Failed /ping <player> for '%s': %s", source.getTextName(), e.getClass().getSimpleName());
            return 0;
        }
    }

    private static int executeList(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        List<ServerPlayer> players = new ArrayList<>(source.getServer().getPlayerList().getPlayers());
        NeoForgeCompat.debug("command/ping", "Listing ping for %d players for '%s'.", players.size(), source.getTextName());
        if (players.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No players online."), false);
            return 1;
        }

        players.sort(Comparator.<ServerPlayer>comparingInt(PingUtils::currentPing).reversed());
        source.sendSuccess(() -> Component.literal("Player ping (" + players.size() + " online)")
                .withStyle(style -> style.withColor(net.minecraft.ChatFormatting.DARK_AQUA)), false);
        for (ServerPlayer player : players) {
            int ping = PingUtils.currentPing(player);
            PingUtils.PingQuality quality = PingUtils.getPingQuality(ping);
            source.sendSuccess(() -> Component.literal("  " + NeoForgeCompat.getProfileName(player) + "  ")
                    .withStyle(style -> style.withColor(net.minecraft.ChatFormatting.AQUA))
                    .append(Component.literal(ping + " ms " + quality.quality())
                            .withStyle(style -> style.withColor(quality.color())))
                    .append(Component.literal(PingUtils.statsSuffix(player))
                            .withStyle(style -> style.withColor(net.minecraft.ChatFormatting.GRAY))), false);
        }
        return players.size();
    }

    private static int executeHelp(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        NeoForgeCompat.debug("command/ping", "Displayed /ping help for '%s'.", source.getTextName());
        source.sendSuccess(() -> Component.literal("Ping Commands")
                .withStyle(style -> style.withColor(net.minecraft.ChatFormatting.DARK_AQUA)), false);
        source.sendSuccess(() -> Component.literal("  /ping")
                .withStyle(style -> style.withColor(net.minecraft.ChatFormatting.AQUA).withBold(true))
                .append(Component.literal("  Check your own ping.")
                        .withStyle(style -> style.withColor(net.minecraft.ChatFormatting.GRAY).withBold(false))), false);
        source.sendSuccess(() -> Component.literal("  /ping <player>")
                .withStyle(style -> style.withColor(net.minecraft.ChatFormatting.AQUA).withBold(true))
                .append(Component.literal("  Check another player's ping (admins only).")
                        .withStyle(style -> style.withColor(net.minecraft.ChatFormatting.GRAY).withBold(false))), false);
        source.sendSuccess(() -> Component.literal("  /ping list")
                .withStyle(style -> style.withColor(net.minecraft.ChatFormatting.AQUA).withBold(true))
                .append(Component.literal("  List every online player's ping (admins only).")
                        .withStyle(style -> style.withColor(net.minecraft.ChatFormatting.GRAY).withBold(false))), false);
        source.sendSuccess(() -> Component.literal("  /ping help")
                .withStyle(style -> style.withColor(net.minecraft.ChatFormatting.AQUA).withBold(true))
                .append(Component.literal("  Show this help message.")
                        .withStyle(style -> style.withColor(net.minecraft.ChatFormatting.GRAY).withBold(false))), false);
        return 1;
    }

    private static void sendSelfPingMessage(CommandSourceStack source, ServerPlayer player) {
        int ping = PingUtils.currentPing(player);
        PingUtils.PingQuality quality = PingUtils.getPingQuality(ping);
        NeoForgeCompat.debug("command/ping", "Computed self ping for '%s': ping=%dms quality=%s", NeoForgeCompat.getProfileName(player), ping, quality.quality());
        Component message = Component.literal("Your ping is ")
                .withStyle(style -> style.withColor(net.minecraft.ChatFormatting.GREEN))
                .append(Component.literal(ping + " ms")
                        .withStyle(style -> style.withColor(quality.color())))
                .append(Component.literal(PingUtils.statsSuffix(player))
                        .withStyle(style -> style.withColor(net.minecraft.ChatFormatting.GRAY)));

        source.sendSuccess(() -> message, false);
    }

    private static void sendPingMessage(CommandSourceStack source, ServerPlayer target) {
        int ping = PingUtils.currentPing(target);
        PingUtils.PingQuality quality = PingUtils.getPingQuality(ping);
        NeoForgeCompat.debug("command/ping", "Computed other ping: requester='%s' target='%s' ping=%dms quality=%s",
                source.getTextName(), NeoForgeCompat.getProfileName(target), ping, quality.quality());

        Component message = Component.literal(NeoForgeCompat.getProfileName(target) + "'s latency is ")
                .withStyle(style -> style.withColor(net.minecraft.ChatFormatting.GREEN))
                .append(Component.literal(ping + " ms, which is " + quality.quality() + "!")
                        .withStyle(style -> style.withColor(quality.color())))
                .append(Component.literal(PingUtils.statsSuffix(target))
                        .withStyle(style -> style.withColor(net.minecraft.ChatFormatting.GRAY)));

        source.sendSuccess(() -> message, false);
        PlayerPing.LOGGER.info("{}: {} ({}ms)", NeoForgeCompat.getProfileName(target), quality.quality(), ping);
    }
}
