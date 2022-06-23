package dev.codedsakura.blossom.lib;

import net.minecraft.text.*;

import static dev.codedsakura.blossom.lib.BlossomLib.CONFIG;

public class CommandTextBuilder {
    private String commandDisplay;
    private String commandRun;
    private Text description = null;
    private boolean suggest = true;
    private boolean hoverShowDisplay = true;

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

    public CommandTextBuilder setDescription(Text description) {
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

    private Text getDescription() {
        MutableText command = new LiteralText(hoverShowDisplay ? commandDisplay : commandRun)
                .styled(descriptionStyle -> descriptionStyle.withColor(TextColor.parse(CONFIG.colors.command)));
        if (description == null) {
            return new TranslatableText(
                    "blossom.text.command.plain",
                    command
            );
        }
        return new TranslatableText(
                "blossom.text.command.description",
                command,
                description.shallowCopy()
                        .styled(descriptionStyle -> descriptionStyle.withColor(TextColor.parse(CONFIG.colors.commandDescription)))
        );
    }

    public Text asColoredText() {
        return new TranslatableText(
                "blossom.text.command.display",
                commandDisplay
        ).styled(style -> style
                .withColor(TextColor.parse(CONFIG.colors.command))
                .withHoverEvent(new HoverEvent(
                        HoverEvent.Action.SHOW_TEXT,
                        this.getDescription()
                ))
                .withClickEvent(new ClickEvent(
                        suggest ? ClickEvent.Action.SUGGEST_COMMAND : ClickEvent.Action.RUN_COMMAND,
                        commandRun
                )));
    }
}
