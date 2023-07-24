package dev.codedsakura.blossom.lib.text;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;

import java.util.Arrays;

import static dev.codedsakura.blossom.lib.BlossomGlobals.CONFIG;

public class TextUtils {
    public static MutableText variable(String str) {
        return Text.literal(str)
                .styled(style -> style.withColor(TextColor.parse(CONFIG.colors.variable)));
    }

    public static MutableText variable(int var) {
        return variable(Integer.toString(var));
    }

    public static MutableText variable(long var) {
        return variable(Long.toString(var));
    }

    public static MutableText variable(float var) {
        return variable(Float.toString(var));
    }

    public static MutableText variable(double var) {
        return variable(Double.toString(var));
    }

    public static MutableText variable(Object var) {
        if (var instanceof PlayerEntity) {
            return player((PlayerEntity) var);
        } else if (var instanceof CommandTextBuilder) {
            return ((CommandTextBuilder) var).asColoredText().copy();
        } else if (var instanceof Text) {
            return ((Text) var).copy();
        }
        return variable(var.toString());
    }


    public static MutableText player(PlayerEntity player) {
        return player.getDisplayName().copy()
                .styled(style -> {
                    if (style.getColor() == null) {
                        return style.withColor(TextColor.parse(CONFIG.colors.player));
                    }
                    return style;
                });
    }


    public enum Type {ERROR, WARN, INFO, SUCCESS}

    public static MutableText fTranslation(String key, Type t, Object... args) {
        return Text.translatable(
                key,
                Arrays.stream(args).map(TextUtils::variable).toArray()
        ).styled(style -> switch (t) {
            case ERROR -> style.withColor(TextColor.parse(CONFIG.colors.error));
            case WARN -> style.withColor(TextColor.parse(CONFIG.colors.warn));
            case INFO -> style.withColor(TextColor.parse(CONFIG.colors.base));
            case SUCCESS -> style.withColor(TextColor.parse(CONFIG.colors.success));
        });
    }

    public static MutableText translation(String key, Object... args) {
        return fTranslation(key, Type.INFO, args);
    }

    public static void send(CommandContext<ServerCommandSource> ctx, String key, Object... args) {
        ctx.getSource().sendFeedback(translation(key, args), false);
    }

    public static void sendOps(CommandContext<ServerCommandSource> ctx, String key, Object... args) {
        ctx.getSource().sendFeedback(translation(key, args), true);
    }

    public static void sendWarn(CommandContext<ServerCommandSource> ctx, String key, Object... args) {
        ctx.getSource().sendFeedback(fTranslation(key, Type.WARN, args), false);
    }

    public static void sendWarnOps(CommandContext<ServerCommandSource> ctx, String key, Object... args) {
        ctx.getSource().sendFeedback(fTranslation(key, Type.WARN, args), true);
    }

    public static void sendSuccess(CommandContext<ServerCommandSource> ctx, String key, Object... args) {
        ctx.getSource().sendFeedback(fTranslation(key, Type.SUCCESS, args), false);
    }

    public static void sendSuccessOps(CommandContext<ServerCommandSource> ctx, String key, Object... args) {
        ctx.getSource().sendFeedback(fTranslation(key, Type.SUCCESS, args), true);
    }

    public static void sendErr(CommandContext<ServerCommandSource> ctx, String key, Object... args) {
        ctx.getSource().sendError(fTranslation(key, Type.ERROR, args));
    }
}
