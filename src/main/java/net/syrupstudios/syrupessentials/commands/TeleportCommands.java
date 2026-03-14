package net.syrupstudios.syrupessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class TeleportCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        //TODO: TPA is its own command fuckface, tpaccept tpdeny
        dispatcher.register(Commands.literal("tpa")
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(TeleportCommands::tpa))
                .then(Commands.literal("accept")
                        .executes(TeleportCommands::tpaAccept))
                .then(Commands.literal("deny")
                        .executes(TeleportCommands::tpaDeny)));

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
        return 0;
    }

    private static int listHomes(CommandContext<CommandSourceStack> context) {

        try{
            ServerPlayer player = context.getSource().getPlayerOrException();

        }
        catch (Exception e) {
           context.getSource().getPlayer().sendSystemMessage(Component.literal(""));
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
