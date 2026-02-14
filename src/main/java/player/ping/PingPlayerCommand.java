package player.ping;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public final class PingPlayerCommand {

    private PingPlayerCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("pingplayer")
                .requires(source -> source.hasPermission(2))
                .executes(PingPlayerCommand::executeHelp)
                .then(Commands.literal("reload")
                        .executes(PingPlayerCommand::executeReload))
                .then(Commands.literal("help")
                        .executes(PingPlayerCommand::executeHelp))
        );
    }

    private static int executeReload(CommandContext<CommandSourceStack> context) {
        PingSettings.getInstance().load();
        context.getSource().sendSuccess(() -> Component.literal("Configuration reloaded successfully!")
                .withStyle(style -> style.withColor(ChatFormatting.GREEN)), true);
        return 1;
    }

    private static int executeHelp(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        source.sendSuccess(() -> Component.literal("----- PingPlayer Help -----")
                .withStyle(style -> style.withColor(ChatFormatting.GOLD)), false);
        source.sendSuccess(() -> Component.literal("/ping")
                .withStyle(style -> style.withColor(ChatFormatting.AQUA))
                .append(Component.literal(" - Check your own ping.")
                        .withStyle(style -> style.withColor(ChatFormatting.GOLD))), false);
        source.sendSuccess(() -> Component.literal("/ping <player>")
                .withStyle(style -> style.withColor(ChatFormatting.AQUA))
                .append(Component.literal(" - Check another player's ping.")
                        .withStyle(style -> style.withColor(ChatFormatting.GOLD))), false);
        source.sendSuccess(() -> Component.literal("/ip <player>")
                .withStyle(style -> style.withColor(ChatFormatting.AQUA))
                .append(Component.literal(" - View a player's IP address.")
                        .withStyle(style -> style.withColor(ChatFormatting.GOLD))), false);
        source.sendSuccess(() -> Component.literal("/pingplayer reload")
                .withStyle(style -> style.withColor(ChatFormatting.AQUA))
                .append(Component.literal(" - Reloads the plugin configuration.")
                        .withStyle(style -> style.withColor(ChatFormatting.GOLD))), false);
        source.sendSuccess(() -> Component.literal("/pingplayer help")
                .withStyle(style -> style.withColor(ChatFormatting.AQUA))
                .append(Component.literal(" - Displays this help message.")
                        .withStyle(style -> style.withColor(ChatFormatting.GOLD))), false);
        return 1;
    }
}
