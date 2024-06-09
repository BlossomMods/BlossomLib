package dev.codedsakura.blossom.lib.polyfill;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public class Polyfill {
    public static void sendCommandFeedback(CommandContext<ServerCommandSource> ctx, Text message) {
        sendCommandFeedback(ctx, message, false);
    }

    public static void sendCommandFeedback(CommandContext<ServerCommandSource> ctx, Text message, boolean broadcastToOps) {
        ctx.getSource().sendFeedback(() -> message, broadcastToOps);
    }
}
