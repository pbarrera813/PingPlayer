package player.ping;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public final class PingPlayerCommand {

    private PingPlayerCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(buildCommandTree("pingplayer"));
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> buildCommandTree(String name) {
        return Commands.literal(name)
                .requires(FabricCompat::hasAdminPermission)
                .executes(PingPlayerCommand::executeHelp)
                .then(Commands.literal("help")
                        .executes(PingPlayerCommand::executeHelp))
                .then(Commands.literal("threshold")
                        .then(buildThresholdArgument(PingSettings.ThresholdTier.EXCELLENT))
                        .then(buildThresholdArgument(PingSettings.ThresholdTier.GOOD))
                        .then(buildThresholdArgument(PingSettings.ThresholdTier.FAIR))
                        .then(buildThresholdArgument(PingSettings.ThresholdTier.POOR))
                        .then(buildThresholdArgument(PingSettings.ThresholdTier.TERRIBLE)))
                .then(Commands.literal("debug")
                        .executes(PingPlayerCommand::executeDebugToggle))
                .then(Commands.literal("dump")
                        .executes(PingPlayerCommand::executeDump));
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> buildThresholdArgument(PingSettings.ThresholdTier tier) {
        return Commands.literal(tier.key())
                .executes(context -> executeShowThreshold(context, tier))
                .then(Commands.literal("equal-or-more")
                        .then(Commands.argument("value", IntegerArgumentType.integer(0))
                                .suggests((context, builder) -> suggestComparisonValue(tier, "equal-or-more", builder))
                                .executes(context -> executeComparisonUpdate(context, tier, "equal-or-more"))))
                .then(Commands.literal("equal-or-less")
                        .then(Commands.argument("value", IntegerArgumentType.integer(0))
                                .suggests((context, builder) -> suggestComparisonValue(tier, "equal-or-less", builder))
                                .executes(context -> executeComparisonUpdate(context, tier, "equal-or-less"))))
                .then(Commands.literal("equal")
                        .then(Commands.argument("value", IntegerArgumentType.integer(0))
                                .suggests((context, builder) -> suggestComparisonValue(tier, "equal", builder))
                                .executes(context -> executeComparisonUpdate(context, tier, "equal"))))
                .then(Commands.argument("min", IntegerArgumentType.integer(0))
                        .suggests((context, builder) -> suggestMinValues(tier, builder))
                        .then(Commands.argument("max", IntegerArgumentType.integer(0))
                                .suggests((context, builder) -> suggestMaxValues(context, tier, builder))
                                .executes(context -> executeRangeUpdate(context, tier))));
    }

    private static int executeShowThreshold(CommandContext<CommandSourceStack> context, PingSettings.ThresholdTier tier) {
        if (!ensureAdminPermission(context.getSource())) {
            return 0;
        }
        FabricCompat.debug("command/pingplayer", "Threshold query by '%s' for tier '%s'.", context.getSource().getTextName(), tier.key());

        PingSettings.ThresholdRange range = PingSettings.getInstance().getThresholdRange(tier);
        ChatFormatting color = PingUtils.getThresholdColor(tier);
        Component response = Component.literal("Current threshold for ")
                .append(Component.literal(PingUtils.getThresholdLabel(tier)).withStyle(style -> style.withColor(color)))
                .append(Component.literal(" is "))
                .append(Component.literal(range.describe()).withStyle(style -> style.withColor(color)))
                .append(Component.literal("."));

        context.getSource().sendSuccess(() -> response, false);
        return 1;
    }

    private static int executeComparisonUpdate(CommandContext<CommandSourceStack> context, PingSettings.ThresholdTier tier, String mode) {
        if (!ensureAdminPermission(context.getSource())) {
            return 0;
        }
        FabricCompat.debug("command/pingplayer", "Threshold comparison update requested by '%s': tier='%s', mode='%s'.",
                context.getSource().getTextName(), tier.key(), mode);

        int value = IntegerArgumentType.getInteger(context, "value");

        Integer min;
        Integer max;
        String phrase;

        switch (mode) {
            case "equal-or-more" -> {
                min = value;
                max = null;
                phrase = "equal or above to";
            }
            case "equal-or-less" -> {
                min = null;
                max = value;
                phrase = "equal or less to";
            }
            case "equal" -> {
                min = value;
                max = value;
                phrase = "equal to";
            }
            default -> {
                context.getSource().sendFailure(Component.literal("Invalid mode. Use: equal-or-more, equal-or-less, or equal."));
                FabricCompat.debug("command/pingplayer", "Rejected threshold update due to invalid mode '%s'.", mode);
                return 0;
            }
        }

        try {
            PingSettings.getInstance().updateThreshold(tier, min, max);
        } catch (IllegalArgumentException | IOException e) {
            context.getSource().sendFailure(Component.literal("Failed to update threshold: " + e.getMessage()));
            FabricCompat.debug("command/pingplayer", "Threshold update failed for '%s': %s", context.getSource().getTextName(), e.getMessage());
            return 0;
        }

        ChatFormatting color = PingUtils.getThresholdColor(tier);
        Component response = Component.literal("The threshold for ")
                .append(Component.literal(PingUtils.getThresholdLabel(tier)).withStyle(style -> style.withColor(color)))
                .append(Component.literal(" ping has been set to " + phrase + " "))
                .append(Component.literal(String.valueOf(value)).withStyle(style -> style.withColor(color)))
                .append(Component.literal(" ms."));

        context.getSource().sendSuccess(() -> response, true);
        FabricCompat.debug("command/pingplayer", "Threshold comparison update applied by '%s': tier='%s', mode='%s', value=%d",
                context.getSource().getTextName(), tier.key(), mode, value);
        return 1;
    }

    private static int executeRangeUpdate(CommandContext<CommandSourceStack> context, PingSettings.ThresholdTier tier) {
        if (!ensureAdminPermission(context.getSource())) {
            return 0;
        }
        FabricCompat.debug("command/pingplayer", "Threshold range update requested by '%s' for tier '%s'.",
                context.getSource().getTextName(), tier.key());

        int min = IntegerArgumentType.getInteger(context, "min");
        int max = IntegerArgumentType.getInteger(context, "max");

        if (min > max) {
            context.getSource().sendFailure(Component.literal("Invalid range: minimum value cannot be greater than maximum value. Example: /pingplayer threshold fair 101 250"));
            FabricCompat.debug("command/pingplayer", "Rejected range update for tier '%s': min=%d max=%d", tier.key(), min, max);
            return 0;
        }

        try {
            PingSettings.getInstance().updateThreshold(tier, min, max);
        } catch (IllegalArgumentException | IOException e) {
            context.getSource().sendFailure(Component.literal("Failed to update threshold: " + e.getMessage()));
            FabricCompat.debug("command/pingplayer", "Range update failed for '%s': %s", context.getSource().getTextName(), e.getMessage());
            return 0;
        }

        ChatFormatting color = PingUtils.getThresholdColor(tier);
        Component response = Component.literal("The threshold for ")
                .append(Component.literal(PingUtils.getThresholdLabel(tier)).withStyle(style -> style.withColor(color)))
                .append(Component.literal(" connection has been set to "))
                .append(Component.literal(String.valueOf(min)).withStyle(style -> style.withColor(color)))
                .append(Component.literal(" - "))
                .append(Component.literal(String.valueOf(max)).withStyle(style -> style.withColor(color)))
                .append(Component.literal(" ms."));

        context.getSource().sendSuccess(() -> response, true);
        FabricCompat.debug("command/pingplayer", "Threshold range update applied by '%s': tier='%s', min=%d, max=%d",
                context.getSource().getTextName(), tier.key(), min, max);
        return 1;
    }

    private static int executeHelp(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (!ensureAdminPermission(source)) {
            return 0;
        }
        FabricCompat.debug("command/pingplayer", "Displayed /pingplayer help for '%s'.", source.getTextName());

        source.sendSuccess(() -> Component.literal("===== PingPlayer Commands =====")
                .withStyle(style -> style.withColor(ChatFormatting.DARK_AQUA)), false);
        source.sendSuccess(() -> Component.literal("Core")
                .withStyle(style -> style.withColor(ChatFormatting.BLUE)), false);
        source.sendSuccess(() -> Component.literal("  /ping")
                .withStyle(style -> style.withColor(ChatFormatting.AQUA).withBold(true))
                .append(Component.literal("  Check your own ping.")
                        .withStyle(style -> style.withColor(ChatFormatting.GRAY).withBold(false))), false);
        source.sendSuccess(() -> Component.literal("  /ip")
                .withStyle(style -> style.withColor(ChatFormatting.AQUA).withBold(true))
                .append(Component.literal("  View your own public IP.")
                        .withStyle(style -> style.withColor(ChatFormatting.GRAY).withBold(false))), false);
        source.sendSuccess(() -> Component.literal("Admin")
                .withStyle(style -> style.withColor(ChatFormatting.BLUE)), false);
        source.sendSuccess(() -> Component.literal("  /ping <player>")
                .withStyle(style -> style.withColor(ChatFormatting.AQUA).withBold(true))
                .append(Component.literal("  Check another player's ping.")
                        .withStyle(style -> style.withColor(ChatFormatting.GRAY).withBold(false))), false);
        source.sendSuccess(() -> Component.literal("  /ip <player>")
                .withStyle(style -> style.withColor(ChatFormatting.AQUA).withBold(true))
                .append(Component.literal("  View another player's public IP.")
                        .withStyle(style -> style.withColor(ChatFormatting.GRAY).withBold(false))), false);
        source.sendSuccess(() -> Component.literal("  /pingplayer threshold <tier> <equal-or-more|equal-or-less|equal> <value>")
                .withStyle(style -> style.withColor(ChatFormatting.AQUA).withBold(true))
                .append(Component.literal("  Set threshold with comparison mode.")
                        .withStyle(style -> style.withColor(ChatFormatting.GRAY).withBold(false))), false);
        source.sendSuccess(() -> Component.literal("  /pingplayer threshold <tier> <min> <max>")
                .withStyle(style -> style.withColor(ChatFormatting.AQUA).withBold(true))
                .append(Component.literal("  Set threshold using a range.")
                        .withStyle(style -> style.withColor(ChatFormatting.GRAY).withBold(false))), false);
        source.sendSuccess(() -> Component.literal("  /pingplayer debug")
                .withStyle(style -> style.withColor(ChatFormatting.AQUA).withBold(true))
                .append(Component.literal("  Toggle full debug mode.")
                        .withStyle(style -> style.withColor(ChatFormatting.GRAY).withBold(false))), false);
        source.sendSuccess(() -> Component.literal("  /pingplayer dump")
                .withStyle(style -> style.withColor(ChatFormatting.AQUA).withBold(true))
                .append(Component.literal("  Generate a full diagnostic dump file.")
                        .withStyle(style -> style.withColor(ChatFormatting.GRAY).withBold(false))), false);
        source.sendSuccess(() -> Component.literal("Tips")
                .withStyle(style -> style.withColor(ChatFormatting.BLUE)), false);
        source.sendSuccess(() -> Component.literal("  Example: /pingplayer threshold terrible equal-or-more 500")
                .withStyle(style -> style.withColor(ChatFormatting.DARK_GRAY)), false);
        source.sendSuccess(() -> Component.literal("  Threshold changes apply immediately.")
                .withStyle(style -> style.withColor(ChatFormatting.GREEN)), false);
        return 1;
    }

    private static boolean ensureAdminPermission(CommandSourceStack source) {
        if (FabricCompat.hasAdminPermission(source)) {
            return true;
        }

        source.sendFailure(Component.literal("Only server admins can use /pingplayer."));
        FabricCompat.debug("command/pingplayer", "Denied /pingplayer access for '%s'.", source.getTextName());
        return false;
    }

    private static int executeDebugToggle(CommandContext<CommandSourceStack> context) {
        if (!ensureAdminPermission(context.getSource())) {
            return 0;
        }
        FabricCompat.debug("command/pingplayer", "Debug toggle requested by '%s'.", context.getSource().getTextName());

        boolean current = PingSettings.getInstance().getDebugPermissionChecks();
        return setDebugMode(context.getSource(), !current);
    }

    private static int setDebugMode(CommandSourceStack source, boolean enabled) {
        try {
            PingSettings.getInstance().setDebugPermissionChecks(enabled);
        } catch (IOException e) {
            source.sendFailure(Component.literal("Failed to update debug mode: " + e.getMessage()));
            return 0;
        }

        ChatFormatting statusColor = enabled ? ChatFormatting.GREEN : ChatFormatting.RED;
        source.sendSuccess(() -> Component.literal("PingPlayer debug mode is now ")
                .append(Component.literal(enabled ? "ENABLED" : "DISABLED")
                        .withStyle(style -> style.withColor(statusColor)))
                .append(Component.literal(".")), true);
        FabricCompat.debug("command/pingplayer", "Debug mode changed by '%s' -> %s", source.getTextName(), enabled ? "ENABLED" : "DISABLED");
        return 1;
    }

    private static int executeDump(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (!ensureAdminPermission(source)) {
            return 0;
        }

        FabricCompat.debug("command/pingplayer", "Diagnostic dump requested by '%s'.", source.getTextName());
        try {
            Path dumpPath = PingDumpReport.generate(source);
            source.sendSuccess(() -> Component.literal("PingPlayer diagnostic dump generated: ")
                    .withStyle(style -> style.withColor(ChatFormatting.GREEN))
                    .append(Component.literal(dumpPath.toString())
                            .withStyle(style -> style.withColor(ChatFormatting.AQUA))), false);
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("Failed to generate diagnostic dump: " + e.getMessage()));
            FabricCompat.debug("command/pingplayer", "Diagnostic dump failed for '%s': %s", source.getTextName(), e.getMessage());
            return 0;
        }
    }

    private static CompletableFuture<Suggestions> suggestComparisonValue(PingSettings.ThresholdTier tier, String mode, SuggestionsBuilder builder) {
        PingSettings.ThresholdRange range = PingSettings.getInstance().getThresholdRange(tier);
        int min = range.min() != null ? range.min() : 0;
        int max = range.max() != null ? range.max() : Math.max(min + 100, 500);

        if ("equal-or-more".equals(mode)) {
            builder.suggest(String.valueOf(min));
            builder.suggest(String.valueOf(Math.max(min, max - 50)));
        } else if ("equal-or-less".equals(mode)) {
            builder.suggest(String.valueOf(max));
            builder.suggest(String.valueOf(Math.max(0, min + 50)));
        } else {
            builder.suggest(String.valueOf(min));
            builder.suggest(String.valueOf(max));
        }

        return builder.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestMinValues(PingSettings.ThresholdTier tier, SuggestionsBuilder builder) {
        PingSettings.ThresholdRange range = PingSettings.getInstance().getThresholdRange(tier);
        int min = range.min() != null ? range.min() : 0;
        int max = range.max() != null ? range.max() : Math.max(min + 100, 500);

        builder.suggest(String.valueOf(min));
        builder.suggest(String.valueOf(Math.max(0, max - 50)));
        return builder.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestMaxValues(CommandContext<CommandSourceStack> context, PingSettings.ThresholdTier tier, SuggestionsBuilder builder) {
        int min = IntegerArgumentType.getInteger(context, "min");
        PingSettings.ThresholdRange range = PingSettings.getInstance().getThresholdRange(tier);
        int currentMax = range.max() != null ? range.max() : Math.max(min + 100, 500);

        builder.suggest(String.valueOf(Math.max(min, currentMax)));
        builder.suggest(String.valueOf(min + 100));
        return builder.buildFuture();
    }
}
