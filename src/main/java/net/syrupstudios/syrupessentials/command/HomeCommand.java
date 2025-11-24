package net.syrupstudios.syrupessentials.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.syrupstudios.syrupessentials.SyrupEssentials;
import net.syrupstudios.syrupessentials.data.PlayerData;
import net.syrupstudios.syrupessentials.util.TeleportUtil;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class HomeCommand {

    // Suggestion provider for home names
    private static final SuggestionProvider<ServerCommandSource> HOME_SUGGESTIONS = (context, builder) -> {
        try {
            ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
            PlayerData data = SyrupEssentials.getPlayerDataManager().getPlayerData(player);

            // Add all home names as suggestions
            for (String homeName : data.getHomes().keySet()) {
                builder.suggest(homeName);
            }
        } catch (CommandSyntaxException e) {
            // Player not found, no suggestions
        }
        return builder.buildFuture();
    };

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        // /sethome <name>
        dispatcher.register(CommandManager.literal("sethome")
                .then(CommandManager.argument("name", StringArgumentType.word())
                        .executes(HomeCommand::setHome)));

        // /home <name> - with suggestions
        dispatcher.register(CommandManager.literal("home")
                .then(CommandManager.argument("name", StringArgumentType.word())
                        .suggests(HOME_SUGGESTIONS)
                        .executes(HomeCommand::teleportHome))
                .executes(HomeCommand::listHomes)); // /home with no args lists homes

        // /delhome <name>
        dispatcher.register(CommandManager.literal("delhome")
                .then(CommandManager.argument("name", StringArgumentType.word())
                        .executes(HomeCommand::deleteHome)));

        // /listhome - list all homes
        dispatcher.register(CommandManager.literal("listhome")
                .executes(HomeCommand::listHomes));
    }

    private static int setHome(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        String homeName = StringArgumentType.getString(context, "name");

        PlayerData data = SyrupEssentials.getPlayerDataManager().getPlayerData(player);

        // Check if home already exists
        boolean isUpdate = data.getHome(homeName) != null;

        // Check if player has reached max homes
        if (!isUpdate && data.getHomeCount() >= data.getMaxHomes()) {
            player.sendMessage(Text.literal("You have reached your maximum number of homes (" +
                    data.getMaxHomes() + ")!").formatted(Formatting.RED), false);
            return 0;
        }

        // Add the home
        data.addHome(homeName, player);
        SyrupEssentials.getPlayerDataManager().savePlayerData(player);

        if (isUpdate) {
            player.sendMessage(Text.literal("Home '" + homeName + "' updated!").formatted(Formatting.GREEN), true);
        } else {
            player.sendMessage(Text.literal("Home '" + homeName + "' set! (" +
                    data.getHomeCount() + "/" + data.getMaxHomes() + ")").formatted(Formatting.GREEN), true);
        }

        return 1;
    }

    private static int teleportHome(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        String homeName = StringArgumentType.getString(context, "name");

        PlayerData data = SyrupEssentials.getPlayerDataManager().getPlayerData(player);
        PlayerData.HomeData home = data.getHome(homeName);

        if (home == null) {
            player.sendMessage(Text.literal("Home '" + homeName + "' not found!").formatted(Formatting.RED), true);
            return 0;
        }

        // Teleport the player
        boolean success = TeleportUtil.teleportPlayer(player, home);

        if (success) {
            player.sendMessage(Text.literal("Teleported to home '" + homeName + "'!").formatted(Formatting.GREEN), true);
            return 1;
        } else {
            player.sendMessage(Text.literal("Failed to teleport to home!").formatted(Formatting.RED), true);
            return 0;
        }
    }

    private static int deleteHome(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        String homeName = StringArgumentType.getString(context, "name");

        PlayerData data = SyrupEssentials.getPlayerDataManager().getPlayerData(player);

        if (data.removeHome(homeName)) {
            SyrupEssentials.getPlayerDataManager().savePlayerData(player);
            player.sendMessage(Text.literal("Home '" + homeName + "' deleted!").formatted(Formatting.GREEN), true);
            return 1;
        } else {
            player.sendMessage(Text.literal("Home '" + homeName + "' not found!").formatted(Formatting.RED), true);
            return 0;
        }
    }

    private static int listHomes(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        PlayerData data = SyrupEssentials.getPlayerDataManager().getPlayerData(player);

        if (data.getHomeCount() == 0) {
            player.sendMessage(Text.literal("You don't have any homes set!").formatted(Formatting.YELLOW), true);
            return 0;
        }

        player.sendMessage(Text.literal("Your homes (" + data.getHomeCount() + "/" +
                data.getMaxHomes() + "):").formatted(Formatting.GOLD), false);

        for (String homeName : data.getHomes().keySet()) {
            player.sendMessage(Text.literal("  - " + homeName).formatted(Formatting.AQUA), false);
        }

        return 1;
    }
}