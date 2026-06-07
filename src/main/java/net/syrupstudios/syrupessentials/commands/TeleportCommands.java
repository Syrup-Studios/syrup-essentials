package net.syrupstudios.syrupessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.syrupstudios.syrupessentials.data.PlayerData;
import net.syrupstudios.syrupessentials.data.WorldData;
import net.syrupstudios.syrupessentials.util.CommandUtil;
import net.syrupstudios.syrupessentials.util.DataManager;
import net.syrupstudios.syrupessentials.util.TeleportManager;
import net.syrupstudios.syrupessentials.util.TeleportPos;
import org.slf4j.Logger;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static net.syrupstudios.syrupessentials.util.TeleportManager.teleportPlayer;

public class TeleportCommands {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("tpa")
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(TeleportCommands::tpa)));

        dispatcher.register(Commands.literal("tpaccept")
                .then(Commands.argument("UUID", UuidArgument.uuid())
                        .executes(TeleportCommands::tpaAcceptPlayerUUID))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(TeleportCommands::tpaAcceptPlayer))
                .executes(TeleportCommands::tpaAccept));

        dispatcher.register(Commands.literal("tpdeny")
                .then(Commands.argument("UUID", UuidArgument.uuid())
                        .executes(TeleportCommands::tpaDenyPlayerUUID))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(TeleportCommands::tpaDenyPlayer))
                .executes(TeleportCommands::tpaDeny));

        dispatcher.register(Commands.literal("home")
                .then(Commands.argument("home_name", StringArgumentType.string())
                        .suggests(TeleportCommands::suggestHomes)
                        .executes(TeleportCommands::namedHome))
                .executes(TeleportCommands::home));

        dispatcher.register(Commands.literal("listhomes")
                .executes(TeleportCommands::listHomes));

        dispatcher.register(Commands.literal("delhome")
                .then(Commands.argument("home_name", StringArgumentType.string())
                        .suggests(TeleportCommands::suggestHomes)
                        .executes(TeleportCommands::delHome))
                .executes(TeleportCommands::delDefaultHome));

        dispatcher.register(Commands.literal("sethome")
                .then(Commands.argument("home_name", StringArgumentType.string())
                        .executes(TeleportCommands::setHome))
                .executes(TeleportCommands::setDefaultHome));

        dispatcher.register(Commands.literal("warp")
                .then(Commands.argument("warp_name", StringArgumentType.string())
                        .suggests(TeleportCommands::suggestWarps)
                        .executes(TeleportCommands::warp)));

        dispatcher.register(Commands.literal("setwarp")
                .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.argument("warp_name", StringArgumentType.string())
                        .executes(TeleportCommands::setWarp)));

        dispatcher.register(Commands.literal("delwarp")
                .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.argument("warp_name", StringArgumentType.string())
                        .suggests(TeleportCommands::suggestWarps)
                        .executes(TeleportCommands::delWarp)));

        dispatcher.register(Commands.literal("listwarps")
                .executes(TeleportCommands::listWarps));

        dispatcher.register(Commands.literal("back")
                .executes(TeleportCommands::back));
    }

    private static CompletableFuture<Suggestions> suggestHomes(
            CommandContext<CommandSourceStack> context,
            SuggestionsBuilder suggestionsBuilder
    ) {
        try{
            ServerPlayer serverPlayer = context.getSource().getPlayerOrException();
            PlayerData player = DataManager.getOrCreatePlayer(serverPlayer).orElseThrow();
            player.getHomes().getDestinations().forEach((key, value) -> suggestionsBuilder.suggest(key));
        } catch (Exception e) {
            LOGGER.error("Error gathering home suggestion",e);
        }
        return suggestionsBuilder.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestWarps(
            CommandContext<CommandSourceStack> context,
            SuggestionsBuilder suggestionsBuilder
    ) {
        try{
            WorldData worldData = DataManager.getOrCreateWorld(context.getSource().getServer()).orElseThrow();
            worldData.getWarps().getDestinations().forEach((key, value) -> suggestionsBuilder.suggest(key));
        } catch (Exception e) {
            LOGGER.error("Error gathering warp suggestion",e);
        }
        return suggestionsBuilder.buildFuture();
    }

    private static int back(CommandContext<CommandSourceStack> context) {
        try{
            ServerPlayer serverPlayer = context.getSource().getPlayerOrException();
            PlayerData player = DataManager.getOrCreatePlayer(serverPlayer).orElseThrow();
            Optional<TeleportPos> lastLocation = player.popLocationHistory();

            if(lastLocation.isPresent()){
                teleportPlayer(lastLocation.get(), serverPlayer, false);
                CommandUtil.commandSuccess("Returned to last location", context);
                return 1;
            }
            else {
                CommandUtil.commandFailure("No Previous location to teleport to..", context);
            }

        } catch (Exception e) {
            LOGGER.error("Error teleporting player to last location", e);
        }
        return 0;
    }

    private static int delWarp(CommandContext<CommandSourceStack> context) {
        try {
            MinecraftServer server = context.getSource().getServer();
            String warpName = context.getArgument("warp_name", String.class);
            WorldData worldData = DataManager.getOrCreateWorld(server).orElseThrow();

            if(worldData.getWarps().getDestinations().containsKey(warpName)){
                worldData.deleteWarp(warpName);
                if(worldData.getWarps().getDestinations().containsKey(warpName)){
                    CommandUtil.commandFailure("Unable to Delete Warp", context);
                    return 0;
                }
                CommandUtil.commandSuccess(
                        String.format("Successfully Removed Warp: %s", warpName), context);
                return 1;
            }
        } catch (Exception e){
            CommandUtil.commandFailure("Unable to Delete Warp", context);
            return 0;
        }
        return 1;
    }

    private static int setWarp(CommandContext<CommandSourceStack> context) {
        try {
            MinecraftServer server = context.getSource().getServer();
            String warpName = context.getArgument("warp_name", String.class);
            WorldData worldData = DataManager.getOrCreateWorld(server).orElseThrow();
            worldData.createWarp(warpName, Objects.requireNonNull(context.getSource().getPlayer()));
            CommandUtil.commandSuccess(
                    String.format("Successfully Created Warp: %s", warpName), context);
        } catch (Exception e) {
            CommandUtil.commandFailure("Unable to Create Warp", context);
            return 0;
        }
        return 1;
    }

    private static int warp(CommandContext<CommandSourceStack> context) {
        try{
            ServerPlayer serverPlayer = context.getSource().getPlayerOrException();
            String warpName = context.getArgument("warp_name", String.class);

            WorldData worldData = DataManager.getOrCreateWorld(context.getSource().getServer()).orElseThrow();
            if(worldData.getWarps().getDestinations().containsKey(warpName)){
                CommandUtil.commandSuccess(
                        String.format("Warping to: %s", warpName), context);
                teleportPlayer(
                        worldData.getWarps().getDestinations().get(warpName),
                        serverPlayer,
                        true
                );
            }
            else {
                CommandUtil.commandFailure(
                        String.format("No Warps Found Named: %s", warpName), context);
                return 0;
            }
        } catch (Exception e){
            CommandUtil.commandFailure("Error Occurred While Warping", context);
            return 0;
        }
        return 1;
    }

    private static int listWarps(CommandContext<CommandSourceStack> context) {
        try{
            ServerPlayer serverPlayer = context.getSource().getPlayerOrException();
            DataManager.getOrCreateWorld(
                    context.getSource().getServer()).orElseThrow()
                    .getWarps()
                    .getDestinations()
                    .keySet()
                    .forEach(w -> serverPlayer.sendSystemMessage(Component.literal(w)));
        } catch (Exception e) {
            CommandUtil.commandFailure("Error Occurred While Listing Warps", context);
            return 0;
        }
        return 1;
    }

    private static int setDefaultHome(CommandContext<CommandSourceStack> context) {
        try{
            ServerPlayer serverPlayer = context.getSource().getPlayerOrException();
            DataManager.getOrCreatePlayer(serverPlayer).orElseThrow().addHome("home", serverPlayer);
            CommandUtil.commandSuccess("Successfully set home", context);
            return 1;
        } catch (Exception e) {
            CommandUtil.commandFailure("Unable to Set Home", context);
            return 0;
        }
    }

    private static int setHome(CommandContext<CommandSourceStack> context) {
        try{
            ServerPlayer serverPlayer = context.getSource().getPlayerOrException();
            String homeName = context.getArgument("home_name", String.class);
            DataManager.getOrCreatePlayer(serverPlayer).orElseThrow().addHome(homeName, serverPlayer);
            CommandUtil.commandSuccess(
                    String.format("Successfully added home: %s", homeName), context);
        }
        catch (Exception e){
            CommandUtil.commandFailure("Unable to Set Home", context);
            LOGGER.error("Error occurred while trying to set home: {}", e.toString());
        }
        return 1;
    }

    private static int listHomes(CommandContext<CommandSourceStack> context) {
        try{
            CommandUtil.commandSuccess(
                    DataManager.getOrCreatePlayer(context.getSource().getPlayerOrException())
                            .orElseThrow()
                            .getHomes()
                            .listNames(),
                    context
            );

        }
        catch (Exception e) {
           CommandUtil.commandFailure("Unable To List Homes", context);
        }
        return 1;
    }

    private static int delDefaultHome(CommandContext<CommandSourceStack> context) {
        try{
            ServerPlayer serverPlayer = context.getSource().getPlayerOrException();
            PlayerData player = DataManager.getOrCreatePlayer(serverPlayer).orElseThrow();
            Map<String, TeleportPos> homes = player.getHomes().getDestinations();

            if(homes.size() == 1){
                player.getHomes().getDestinations().clear();
                CommandUtil.commandSuccess("Successfully removed home.", context);
                return 1;
            }
            CommandUtil.commandFailure("Please specify which home to delete. /delhome (name of home here)", context);
        } catch (Exception e) {
            CommandUtil.commandFailure("Unable to delete home", context);
            LOGGER.error("Error deleting default home: {}", e.toString());
        }
        return 0;
    }

    private static int delHome(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer serverPlayer = context.getSource().getPlayerOrException();
            PlayerData player = DataManager.getOrCreatePlayer(serverPlayer).orElseThrow();
            Map<String, TeleportPos> homes = player.getHomes().getDestinations();
            String homeName = context.getArgument("home_name", String.class);
            if(!homes.containsKey(homeName)){
                CommandUtil.commandFailure("No home saved with name: "+homeName, context);
                return 0;
            }
            player.removeHome(homeName);

            if(player.getHomes().getDestinations().containsKey(homeName)){
                CommandUtil.commandFailure("Unable to delete home", context);
                return 0;
            }
            CommandUtil.commandSuccess("Successfully removed home with name: "+homeName, context);
            player.triggerUpdate();
            return 1;
        } catch (Exception e){
            CommandUtil.commandFailure("Unable to delete home", context);
            LOGGER.error("Error occurred while deleting a player home: ", e);
        }
        return 0;
    }

    private static int home(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer serverPlayer = context.getSource().getPlayerOrException();
            PlayerData player = DataManager.getOrCreatePlayer(serverPlayer).orElseThrow();
            Map<String, TeleportPos> homes = player.getHomes().getDestinations();

            if(homes.size() == 1){
                teleportPlayer(homes.get(homes.keySet().iterator().next()), serverPlayer, true);
                return 1;
            }
            if(homes.containsKey("home")){
                teleportPlayer(homes.get("home"), serverPlayer, true);
                return 1;
            }
            else {
                CommandUtil.commandFailure("No default home set.", context);
            }
        } catch (Exception e) {

            CommandUtil.commandFailure("Unable To Teleport player to desired home.", context);
        }
        return 0;
    }

    private static int namedHome(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer serverPlayer = context.getSource().getPlayerOrException();
            PlayerData player = DataManager.getOrCreatePlayer(serverPlayer).orElseThrow();
            player.addTeleportHistory(serverPlayer);
            String homeName = context.getArgument("home_name", String.class);

            if(player.getHomes().getDestinations().containsKey(homeName)){
                teleportPlayer(player.getHomes().getDestinations().get(homeName), serverPlayer, true);
                return 1;
            }
            else {
                CommandUtil.commandFailure(String.format("No home with name: %s", homeName), context);
            }
        } catch (Exception e) {
            CommandUtil.commandFailure("Unable To Teleport player to desired home.", context);
        }
        return 0;
    }

    private static int tpaDeny(CommandContext<CommandSourceStack> context) {
        return TeleportManager.denyTeleportRequest(context.getSource().getPlayer());
    }

    private static int tpaDenyPlayer(CommandContext<CommandSourceStack> context) {
        try{
            return TeleportManager.denyTeleportRequestPlayer(
                    context.getSource().getPlayer(),
                    EntityArgument.getPlayer(context, "player"));
        } catch (Exception e) {
            return 0;
        }
    }

    private static int tpaAccept(CommandContext<CommandSourceStack> context) {
        return TeleportManager.approveTeleportRequest(context.getSource().getPlayer());
    }

    private static int tpaAcceptPlayer(CommandContext<CommandSourceStack> context) {
        try{
            return TeleportManager.approveTeleportRequestPlayer(
                    context.getSource().getPlayer(),
                    EntityArgument.getPlayer(context, "player"));
        } catch (Exception e) {
            return 0;
        }
    }

    private static int tpaAcceptPlayerUUID(CommandContext<CommandSourceStack> context) {
        return TeleportManager.approveTeleportRequestPlayer(
                context.getSource().getPlayer(),
                context.getSource().getServer().getPlayerList().getPlayer(UuidArgument.getUuid(context, "UUID")));
    }

    private static int tpaDenyPlayerUUID(CommandContext<CommandSourceStack> context) {
        return TeleportManager.denyTeleportRequestPlayer(
                context.getSource().getPlayer(),
                context.getSource().getServer().getPlayerList().getPlayer(UuidArgument.getUuid(context, "UUID")));
    }

    private static int tpa(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer serverPlayer = context.getSource().getPlayerOrException();
            PlayerData player = DataManager.getOrCreatePlayer(serverPlayer).orElseThrow();
            player.addTeleportHistory(serverPlayer);
            Integer result = TeleportManager.teleportRequest(
                    serverPlayer,
                    context.getSource().getServer(),
                    EntityArgument.getPlayer(context, "player").getDisplayName().getString()
            );
            if(result.equals(1)){
                CommandUtil.commandSuccess("Sending TPA Request..", context);
            }
            if(result.equals(0)){
                CommandUtil.commandFailure("Unable to Send Teleport Request", context);
            }
            return result;
        } catch (Exception e) {
            CommandUtil.commandFailure("Unable To Process TPA Request.", context);
            LOGGER.error("Error processing TPA Request.",e);
        }
        return 0;
    }
}
