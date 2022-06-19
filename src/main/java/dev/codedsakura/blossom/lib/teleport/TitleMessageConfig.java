package dev.codedsakura.blossom.lib.teleport;

import dev.codedsakura.blossom.lib.text.TextUtils;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.TextColor;

public class TitleMessageConfig {
    public boolean enabled = true;

    public CounterTitleConfig titleCounting;
    public CounterTitleConfig subtitleCounting;
    public TitleConfig titleDone;
    public TitleConfig subtitleDone;

    public static class TitleConfig {
        public String color;
        public String modifiers;

        public TitleConfig(String color, String modifiers) {
            this.color = color;
            this.modifiers = modifiers;
        }

        public Style getStyle(Style style) {
            return style
                    .withColor(TextColor.parse(color))
                    .withBold(modifiers.indexOf('b') >= 0)
                    .withItalic(modifiers.indexOf('i') >= 0)
                    .withUnderline(modifiers.indexOf('u') >= 0)
                    .withObfuscated(modifiers.indexOf('o') >= 0)
                    .withStrikethrough(modifiers.indexOf('s') >= 0);
        }

        public MutableText getText(String key) {
            return TextUtils.translation(key).styled(this::getStyle);
        }
    }

    public static class CounterTitleConfig extends TitleConfig {
        public String counterColor;

        public CounterTitleConfig(String color, String modifiers, String counterColor) {
            super(color, modifiers);
            this.counterColor = counterColor;
        }

        public MutableText getText(String key, int counter) {
            return TextUtils.translation(
                    key,
                    new LiteralText(Integer.toString(counter))
                            .styled(style -> style.withColor(TextColor.parse(counterColor)))
            ).styled(this::getStyle);
        }
    }

    public TitleMessageConfig(boolean defaults) {
        titleCounting = defaults ? new CounterTitleConfig("light_purple", "b", "gold") : null;
        subtitleCounting = defaults ? new CounterTitleConfig("red", "i", "gold") : null;
        titleDone = defaults ? new TitleConfig("green", "b") : null;
        subtitleDone = defaults ? new TitleConfig("green", "i") : null;
    }

    public TitleMessageConfig cloneMerge(TitleMessageConfig old) {
        TitleMessageConfig newOne = new TitleMessageConfig(false);

        newOne.titleCounting = this.titleCounting != null ? this.titleCounting : old.titleCounting;
        newOne.subtitleCounting = this.subtitleCounting != null ? this.subtitleCounting : old.subtitleCounting;
        newOne.titleDone = this.titleDone != null ? this.titleDone : old.titleDone;
        newOne.subtitleDone = this.subtitleDone != null ? this.subtitleDone : old.subtitleDone;

        return newOne;
    }
}
