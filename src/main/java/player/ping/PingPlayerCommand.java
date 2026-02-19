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
import java.util.concurrent.CompletableFuture;

public final class PingPlayerCommand {

    private PingPlayerCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("pingplayer")
                .requires(source -> source.hasPermission(2))
                .executes(PingPlayerCommand::executeHelp)
                .then(Commands.literal("help")
                        .executes(PingPlayerCommand::executeHelp))
                .then(Commands.literal("threshold")
                        .then(buildThresholdArgument(PingSettings.ThresholdTier.EXCELLENT))
                        .then(buildThresholdArgument(PingSettings.ThresholdTier.GOOD))
                        .then(buildThresholdArgument(PingSettings.ThresholdTier.FAIR))
                        .then(buildThresholdArgument(PingSettings.ThresholdTier.POOR))
                        .then(buildThresholdArgument(PingSettings.ThresholdTier.TERRIBLE)))
        );
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
                return 0;
            }
        }

        try {
            PingSettings.getInstance().updateThreshold(tier, min, max);
        } catch (IllegalArgumentException | IOException e) {
            context.getSource().sendFailure(Component.literal("Failed to update threshold: " + e.getMessage()));
            return 0;
        }

        ChatFormatting color = PingUtils.getThresholdColor(tier);
        Component response = Component.literal("The threshold for ")
                .append(Component.literal(PingUtils.getThresholdLabel(tier)).withStyle(style -> style.withColor(color)))
                .append(Component.literal(" ping has been set to " + phrase + " "))
                .append(Component.literal(String.valueOf(value)).withStyle(style -> style.withColor(color)))
                .append(Component.literal(" ms."));

        context.getSource().sendSuccess(() -> response, true);
        return 1;
    }

    private static int executeRangeUpdate(CommandContext<CommandSourceStack> context, PingSettings.ThresholdTier tier) {
        int min = IntegerArgumentType.getInteger(context, "min");
        int max = IntegerArgumentType.getInteger(context, "max");

        if (min > max) {
            context.getSource().sendFailure(Component.literal("Invalid range: minimum value cannot be greater than maximum value. Example: /pingplayer threshold fair 101 250"));
            return 0;
        }

        try {
            PingSettings.getInstance().updateThreshold(tier, min, max);
        } catch (IllegalArgumentException | IOException e) {
            context.getSource().sendFailure(Component.literal("Failed to update threshold: " + e.getMessage()));
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
        source.sendSuccess(() -> Component.literal("/pingplayer threshold <excellent|good|fair|poor|terrible> <equal-or-more|equal-or-less|equal> <value>")
                .withStyle(style -> style.withColor(ChatFormatting.AQUA))
                .append(Component.literal(" - Set a threshold using comparison text.")
                        .withStyle(style -> style.withColor(ChatFormatting.GOLD))), false);
        source.sendSuccess(() -> Component.literal("/pingplayer threshold <excellent|good|fair|poor|terrible> <min> <max>")
                .withStyle(style -> style.withColor(ChatFormatting.AQUA))
                .append(Component.literal(" - Set a threshold interval.")
                        .withStyle(style -> style.withColor(ChatFormatting.GOLD))), false);
        source.sendSuccess(() -> Component.literal("Examples: /pingplayer threshold terrible equal-or-more 500, /pingplayer threshold fair 101 250")
                .withStyle(style -> style.withColor(ChatFormatting.GRAY)), false);
        source.sendSuccess(() -> Component.literal("Threshold changes are applied immediately.")
                .withStyle(style -> style.withColor(ChatFormatting.GREEN)), false);
        return 1;
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
