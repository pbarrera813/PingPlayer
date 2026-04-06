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
                .requires(source -> NeoForgeCompat.hasPermission(source, 0))
                .executes(PingCommand::executeSelf)
                .then(Commands.literal("help")
                        .executes(PingCommand::executeHelp))
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
        source.sendSuccess(() -> Component.literal("  /ping help")
                .withStyle(style -> style.withColor(net.minecraft.ChatFormatting.AQUA).withBold(true))
                .append(Component.literal("  Show this help message.")
                        .withStyle(style -> style.withColor(net.minecraft.ChatFormatting.GRAY).withBold(false))), false);
        return 1;
    }

    private static void sendSelfPingMessage(CommandSourceStack source, ServerPlayer player) {
        int ping = player.connection.latency();
        PingUtils.PingQuality quality = PingUtils.getPingQuality(ping);
        NeoForgeCompat.debug("command/ping", "Computed self ping for '%s': ping=%dms quality=%s", NeoForgeCompat.getProfileName(player), ping, quality.quality());
        Component message = Component.literal("Your ping is ")
                .withStyle(style -> style.withColor(net.minecraft.ChatFormatting.GREEN))
                .append(Component.literal(ping + " ms")
                        .withStyle(style -> style.withColor(quality.color())));

        source.sendSuccess(() -> message, false);
    }

    private static void sendPingMessage(CommandSourceStack source, ServerPlayer target) {
        int ping = target.connection.latency();
        PingUtils.PingQuality quality = PingUtils.getPingQuality(ping);
        NeoForgeCompat.debug("command/ping", "Computed other ping: requester='%s' target='%s' ping=%dms quality=%s",
                source.getTextName(), NeoForgeCompat.getProfileName(target), ping, quality.quality());

        Component message = Component.literal(NeoForgeCompat.getProfileName(target) + "'s latency is ")
                .withStyle(style -> style.withColor(net.minecraft.ChatFormatting.GREEN))
                .append(Component.literal(ping + " ms, which is " + quality.quality() + "!")
                        .withStyle(style -> style.withColor(quality.color())));

        source.sendSuccess(() -> message, false);
        PlayerPing.LOGGER.info("{}: {} ({}ms)", NeoForgeCompat.getProfileName(target), quality.quality(), ping);
    }
}
