package dev.codedsakura.blossom.lib.text;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.player.Player;

import java.util.Arrays;

import static dev.codedsakura.blossom.lib.BlossomGlobals.CONFIG;

public class TextUtils {
    public static MutableComponent variable(String str) {
        return Component.literal(str)
                .withStyle(style -> style.withColor(TextUtils.parseColor(CONFIG.colors.variable)));
    }

    public static MutableComponent variable(int var) {
        return variable(Integer.toString(var));
    }

    public static MutableComponent variable(long var) {
        return variable(Long.toString(var));
    }

    public static MutableComponent variable(float var) {
        return variable(Float.toString(var));
    }

    public static MutableComponent variable(double var) {
        return variable(Double.toString(var));
    }

    public static MutableComponent variable(Object var) {
        if (var instanceof Player) {
            return player((Player) var);
        } else if (var instanceof CommandTextBuilder) {
            return ((CommandTextBuilder) var).asColoredText().copy();
        } else if (var instanceof Component) {
            return ((Component) var).copy();
        }
        return variable(var.toString());
    }


    public static MutableComponent player(Player player) {
        return player.getDisplayName().copy()
                .withStyle(style -> {
                    if (style.getColor() == null) {
                        return style.withColor(TextUtils.parseColor(CONFIG.colors.player));
                    }
                    return style;
                });
    }


    public enum Type {ERROR, WARN, INFO, SUCCESS}

    public static MutableComponent fTranslation(String key, Type t, Object... args) {
        return Component.translatable(
                key,
                Arrays.stream(args).map(TextUtils::variable).toArray()
        ).withStyle(style -> switch (t) {
            case ERROR -> style.withColor(TextUtils.parseColor(CONFIG.colors.error));
            case WARN -> style.withColor(TextUtils.parseColor(CONFIG.colors.warn));
            case INFO -> style.withColor(TextUtils.parseColor(CONFIG.colors.base));
            case SUCCESS -> style.withColor(TextUtils.parseColor(CONFIG.colors.success));
        });
    }

    public static TextColor parseColor(String color) {
        return TextColor.parseColor(color)
                .getOrThrow(e -> {
                    throw new RuntimeException(e);
                });
    }

    public static MutableComponent translation(String key, Object... args) {
        return fTranslation(key, Type.INFO, args);
    }

    public static void send(CommandContext<CommandSourceStack> ctx, String key, Object... args) {
        ctx.getSource().sendSuccess(() -> translation(key, args), false);
    }

    public static void sendOps(CommandContext<CommandSourceStack> ctx, String key, Object... args) {
        ctx.getSource().sendSuccess(() -> translation(key, args), true);
    }

    public static void sendWarn(CommandContext<CommandSourceStack> ctx, String key, Object... args) {
        ctx.getSource().sendSuccess(() -> fTranslation(key, Type.WARN, args), false);
    }

    public static void sendWarnOps(CommandContext<CommandSourceStack> ctx, String key, Object... args) {
        ctx.getSource().sendSuccess(() -> fTranslation(key, Type.WARN, args), true);
    }

    public static void sendSuccess(CommandContext<CommandSourceStack> ctx, String key, Object... args) {
        ctx.getSource().sendSuccess(() -> fTranslation(key, Type.SUCCESS, args), false);
    }

    public static void sendSuccessOps(CommandContext<CommandSourceStack> ctx, String key, Object... args) {
        ctx.getSource().sendSuccess(() -> fTranslation(key, Type.SUCCESS, args), true);
    }

    public static void sendErr(CommandContext<CommandSourceStack> ctx, String key, Object... args) {
        ctx.getSource().sendFailure(fTranslation(key, Type.ERROR, args));
    }
}
