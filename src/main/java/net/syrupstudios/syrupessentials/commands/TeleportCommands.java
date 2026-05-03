package net.syrupstudios.syrupessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import net.syrupstudios.syrupessentials.data.PlayerData;
import net.syrupstudios.syrupessentials.util.CommandUtil;
import net.syrupstudios.syrupessentials.util.DataManager;
import net.syrupstudios.syrupessentials.util.TeleportPos;

import java.util.Objects;

public class TeleportCommands {

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
                        .executes(TeleportCommands::setWarp)));

        dispatcher.register(Commands.literal("delwarp")
                .then(Commands.argument("warp_name", StringArgumentType.string())
                        .executes(TeleportCommands::delWarp)));

        dispatcher.register(Commands.literal("listwarps")
                    .executes(TeleportCommands::delWarp));
    }

    private static int delWarp(CommandContext<CommandSourceStack> context) {
        return 0;
    }

    private static int setWarp(CommandContext<CommandSourceStack> context) {
        return 0;
    }

    private static int warp(CommandContext<CommandSourceStack> context) {
        return 0;
    }

    private static int listWarps(CommandContext<CommandSourceStack> context) {
        return 0;
    }

    private static int setDefaultHome(CommandContext<CommandSourceStack> context) {
        return 0;
    }

    private static int setHome(CommandContext<CommandSourceStack> context) {
        try{
            ServerPlayer serverPlayer = context.getSource().getPlayerOrException();

            DataManager.getOrCreate(serverPlayer).orElseThrow().addHome(context.getInput(), serverPlayer);
            CommandUtil.commandSuccess(
                    String.format("Successfully added home: %s", context.getInput()), context);
        }
        catch (Exception e){
            CommandUtil.commandFailure("Unable to Set Home", context);
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
        return 0;
    }

    private static int delHome(CommandContext<CommandSourceStack> context) {
        return 0;
    }

    private static int home(CommandContext<CommandSourceStack> context) {
        return 0;
    }

    private static int namedHome(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer serverPlayer = context.getSource().getPlayerOrException();
            PlayerData player = DataManager.getOrCreate(serverPlayer).orElseThrow();
            player.addTeleportHistory(serverPlayer);

            if(player.getHomes().getDestinations().containsKey(context.getInput())){
                TeleportPos tpos = player.getHomes().getDestinations().get(context.getInput());
                serverPlayer.teleportTo(
                        Objects.requireNonNull(serverPlayer.getServer()).getLevel(tpos.getDimensionId()),
                        tpos.getPos().getX(),
                        tpos.getPos().getY(),
                        tpos.getPos().getZ(),
                        0,
                        0
                );
            }
            else {
                CommandUtil.commandFailure(String.format("No home with name: %s", context.getInput()), context);
            }
        } catch (Exception e) {
            CommandUtil.commandFailure("Unable To Teleport player to desired home", context);
        }
        return 1;
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
