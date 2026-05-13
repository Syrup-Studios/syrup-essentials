package net.syrupstudios.syrupessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import net.syrupstudios.syrupessentials.data.PlayerData;
import net.syrupstudios.syrupessentials.util.CommandUtil;
import net.syrupstudios.syrupessentials.util.DataManager;
import net.syrupstudios.syrupessentials.util.TeleportPos;
import org.slf4j.Logger;

import java.util.Map;
import java.util.Objects;

import static net.syrupstudios.syrupessentials.SyrupEssentials.teleportPlayer;

public class TeleportCommands {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("tpa")
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(TeleportCommands::tpa)));

        dispatcher.register(Commands.literal("tpaccept")
                .executes(TeleportCommands::tpaAccept));

        dispatcher.register(Commands.literal("tpdeny")
                .executes(TeleportCommands::tpaDeny));

        dispatcher.register(Commands.literal("home")
                .then(Commands.argument("home_name", StringArgumentType.string())
                        .executes(TeleportCommands::namedHome))
                .executes(TeleportCommands::home));

        dispatcher.register(Commands.literal("listhomes")
                .executes(TeleportCommands::listHomes));

        dispatcher.register(Commands.literal("delhome")
                .then(Commands.argument("home_name", StringArgumentType.string())
                        .executes(TeleportCommands::delHome))
                .executes(TeleportCommands::delDefaultHome));

        dispatcher.register(Commands.literal("sethome")
                .then(Commands.argument("home_name", StringArgumentType.string())
                        .executes(TeleportCommands::setHome))
                .executes(TeleportCommands::setDefaultHome));

        dispatcher.register(Commands.literal("warp")
                .then(Commands.literal("list")
                        .executes(TeleportCommands::listWarps))
                .then(Commands.argument("warp_name", StringArgumentType.string()))
                        .executes(TeleportCommands::warp));

        dispatcher.register(Commands.literal("setwarp")
                .then(Commands.argument("warp_name", StringArgumentType.string())
                        .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(TeleportCommands::setWarp)));

        dispatcher.register(Commands.literal("delwarp")
                .then(Commands.argument("warp_name", StringArgumentType.string())
                        .executes(TeleportCommands::delWarp)));

        dispatcher.register(Commands.literal("listwarps")
                .executes(TeleportCommands::delWarp));

        dispatcher.register(Commands.literal("back")
                .executes(TeleportCommands::back));
    }

    private static int back(CommandContext<CommandSourceStack> context) {
        try{
            ServerPlayer serverPlayer = context.getSource().getPlayerOrException();
            PlayerData player = DataManager.getOrCreate(serverPlayer).orElseThrow();
            TeleportPos lastLocation = player.getLastLocation();

            context.getSource().getServer();

            if(lastLocation != null){
                player.popLocationHistory();
                teleportPlayer(lastLocation, serverPlayer);
                CommandUtil.commandSuccess("Returned to last location", context);
            }
            else {

            }

        } catch (Exception e) {

        }
        return 0;
    }

    private static int delWarp(CommandContext<CommandSourceStack> context) {
        return 0;
    }

    private static int setWarp(CommandContext<CommandSourceStack> context) {
        context.getSource()
        return 0;
    }

    private static int warp(CommandContext<CommandSourceStack> context) {
        return 0;
    }

    private static int listWarps(CommandContext<CommandSourceStack> context) {
        return 0;
    }

    private static int setDefaultHome(CommandContext<CommandSourceStack> context) {
        try{
            ServerPlayer serverPlayer = context.getSource().getPlayerOrException();
            DataManager.getOrCreate(serverPlayer).orElseThrow().addHome("home", serverPlayer);
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
            DataManager.getOrCreate(serverPlayer).orElseThrow().addHome(homeName, serverPlayer);
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
                    DataManager.getOrCreate(context.getSource().getPlayerOrException())
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
            PlayerData player = DataManager.getOrCreate(serverPlayer).orElseThrow();
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
            PlayerData player = DataManager.getOrCreate(serverPlayer).orElseThrow();
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
            PlayerData player = DataManager.getOrCreate(serverPlayer).orElseThrow();
            Map<String, TeleportPos> homes = player.getHomes().getDestinations();


            if(homes.size() == 1){
                teleportPlayer(homes.get(homes.keySet().iterator().next()), serverPlayer);
                return 1;
            }
            if(homes.containsKey("home")){
                teleportPlayer(homes.get("home"), serverPlayer);
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
            PlayerData player = DataManager.getOrCreate(serverPlayer).orElseThrow();
            player.addTeleportHistory(serverPlayer);
            String homeName = context.getArgument("home_name", String.class);

            if(player.getHomes().getDestinations().containsKey(homeName)){
                teleportPlayer(player.getHomes().getDestinations().get(homeName), serverPlayer);
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
        return 0;
    }

    private static int tpaAccept(CommandContext<CommandSourceStack> context) {
        return 0;
    }

    private static int tpa(CommandContext<CommandSourceStack> context) {
        return 1;
    }
}
