package dev.codedsakura.blossom.lib.text;

import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;

import static dev.codedsakura.blossom.lib.BlossomGlobals.CONFIG;

public class CommandTextBuilder {
    private String commandDisplay;
    private String commandRun;
    private Component description = null;
    private boolean suggest = true;
    private boolean hoverShowDisplay = true;
    private String displayKey = "blossom.text.command.display";

    public CommandTextBuilder(String command) {
        this.commandDisplay = command;
        this.commandRun = command;
    }

    public CommandTextBuilder setCommandDisplay(String commandDisplay) {
        this.commandDisplay = commandDisplay;
        return this;
    }

    public CommandTextBuilder setCommandRun(String commandRun) {
        this.commandRun = commandRun;
        return this;
    }

    public CommandTextBuilder setDescription(Component description) {
        this.description = description;
        return this;
    }

    public CommandTextBuilder clearDescription() {
        this.description = null;
        return this;
    }

    public CommandTextBuilder setClickSuggest(boolean suggest) {
        this.suggest = suggest;
        return this;
    }

    public CommandTextBuilder setClickSuggest() {
        this.suggest = true;
        return this;
    }

    public CommandTextBuilder setClickRun() {
        this.suggest = false;
        return this;
    }

    public CommandTextBuilder setHoverShowDisplay(boolean hoverShowDisplay) {
        this.hoverShowDisplay = hoverShowDisplay;
        return this;
    }

    public CommandTextBuilder setHoverShowDisplay() {
        this.hoverShowDisplay = true;
        return this;
    }

    public CommandTextBuilder setHoverShowRun() {
        this.hoverShowDisplay = false;
        return this;
    }

    public CommandTextBuilder setDisplayKey(String newKey) {
        displayKey = newKey;
        return this;
    }

    private Component getDescription() {
        MutableComponent command = Component.literal(hoverShowDisplay ? commandDisplay : commandRun)
                .withStyle(descriptionStyle -> descriptionStyle.withColor(TextUtils.parseColor(CONFIG.colors.command)));
        if (description == null) {
            return TextUtils.translation(
                    "blossom.text.command.plain",
                    command
            );
        }
        return TextUtils.translation(
                "blossom.text.command.description",
                command,
                description.copy()
                        .withStyle(descriptionStyle -> descriptionStyle.withColor(TextUtils.parseColor(CONFIG.colors.commandDescription)))
        );
    }

    public Component asColoredText() {
        return TextUtils.translation(
                displayKey,
                commandDisplay
        ).withStyle(style -> style
                .withColor(TextUtils.parseColor(CONFIG.colors.command))
                .withHoverEvent(new HoverEvent.ShowText(
                        this.getDescription()
                ))
                .withClickEvent(suggest ?
                        new ClickEvent.SuggestCommand(commandRun) :
                        new ClickEvent.RunCommand(commandRun)));
    }
}
