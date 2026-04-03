package net.syrupstudios.syrupessentials.util;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

import java.util.Objects;

public class CommandUtil {

    public static void commandSuccess(String message, CommandContext<CommandSourceStack> context){
        context.getSource().sendSuccess(() -> Component.literal(message), false);
    }

    public static void commandFailure(String message, CommandContext<CommandSourceStack> context) {
        Objects.requireNonNull(context.getSource().getPlayer()).sendSystemMessage(Component.literal(message));
    }
}
