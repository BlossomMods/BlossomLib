package dev.codedsakura.blossom.lib.teleport;

import dev.codedsakura.blossom.lib.text.TextUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

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
                    .withColor(TextUtils.parseColor(color))
                    .withBold(modifiers.indexOf('b') >= 0)
                    .withItalic(modifiers.indexOf('i') >= 0)
                    .withUnderlined(modifiers.indexOf('u') >= 0)
                    .withObfuscated(modifiers.indexOf('o') >= 0)
                    .withStrikethrough(modifiers.indexOf('s') >= 0);
        }

        public MutableComponent getText(String key) {
            return TextUtils.translation(key).withStyle(this::getStyle);
        }
    }

    public static class CounterTitleConfig extends TitleConfig {
        public String counterColor;

        public CounterTitleConfig(String color, String modifiers, String counterColor) {
            super(color, modifiers);
            this.counterColor = counterColor;
        }

        public MutableComponent getText(String key, int counter) {
            return TextUtils.translation(
                    key,
                    Component.literal(Integer.toString(counter))
                            .withStyle(style -> style.withColor(TextUtils.parseColor(counterColor)))
            ).withStyle(this::getStyle);
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
