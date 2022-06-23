package dev.codedsakura.blossom.lib;

import net.minecraft.text.*;

public class TeleportConfig {
    public TeleportConfig(boolean defaults) {
        bossBar = defaults ? new BossBar(true) : null;
        titleMessage = defaults ? new TitleMessage(true) : null;
    }

    BossBar bossBar;

    static class BossBar {
        boolean enabled = true;
        String color;
        String textColor;

        BossBar(boolean defaults) {
            color = defaults ? net.minecraft.entity.boss.BossBar.Color.BLUE.getName() : null;
            textColor = defaults ? "white" : null;
        }

        BossBar cloneMerge(BossBar old) {
            BossBar newOne = new BossBar(false);

            newOne.color = this.color != null ? this.color : old.color;
            newOne.textColor = this.textColor != null ? this.textColor : old.textColor;

            return newOne;
        }
    }

    TitleMessage titleMessage;

    static class TitleMessage {
        boolean enabled = true;

        TitleMessage.CounterTitleConfig titleCounting;
        TitleMessage.CounterTitleConfig subtitleCounting;
        TitleMessage.TitleConfig titleDone;
        TitleMessage.TitleConfig subtitleDone;

        static class TitleConfig {
            String color;
            String modifiers;

            TitleConfig(String color, String modifiers) {
                this.color = color;
                this.modifiers = modifiers;
            }

            Style getStyle(Style style) {
                return style
                        .withColor(TextColor.parse(color))
                        .withBold(modifiers.indexOf('b') >= 0)
                        .withItalic(modifiers.indexOf('i') >= 0)
                        .withUnderline(modifiers.indexOf('u') >= 0)
                        .withObfuscated(modifiers.indexOf('o') >= 0)
                        .withStrikethrough(modifiers.indexOf('s') >= 0);
            }

            MutableText getText(String key) {
                return new TranslatableText(key).styled(this::getStyle);
            }
        }

        static class CounterTitleConfig extends TitleMessage.TitleConfig {
            String counterColor;

            CounterTitleConfig(String color, String modifiers, String counterColor) {
                super(color, modifiers);
                this.counterColor = counterColor;
            }

            MutableText getText(String key, int counter) {
                return new TranslatableText(
                        key,
                        new LiteralText(Integer.toString(counter))
                                .styled(style -> style.withColor(TextColor.parse(counterColor)))
                ).styled(this::getStyle);
            }
        }

        public TitleMessage(boolean defaults) {
            titleCounting = defaults ? new TitleMessage.CounterTitleConfig("light_purple", "b", "gold") : null;
            subtitleCounting = defaults ? new TitleMessage.CounterTitleConfig("red", "i", "gold") : null;
            titleDone = defaults ? new TitleMessage.TitleConfig("green", "b") : null;
            subtitleDone = defaults ? new TitleMessage.TitleConfig("green", "i") : null;
        }

        TitleMessage cloneMerge(TitleMessage old) {
            TitleMessage newOne = new TitleMessage(false);

            newOne.titleCounting = this.titleCounting != null ? this.titleCounting : old.titleCounting;
            newOne.subtitleCounting = this.subtitleCounting != null ? this.subtitleCounting : old.subtitleCounting;
            newOne.titleDone = this.titleDone != null ? this.titleDone : old.titleDone;
            newOne.subtitleDone = this.subtitleDone != null ? this.subtitleDone : old.subtitleDone;

            return newOne;
        }
    }

    boolean actionBarMessageEnabled = false;

    boolean fovEffectEnabled = false;

    ParticleAnimation particleAnimation = ParticleAnimation.OFF;

    boolean allowBack = true;

    enum ParticleAnimation {
        OFF
    }

    TeleportConfig cloneMerge() {
        TeleportConfig defaultConf = BlossomLib.CONFIG.baseTeleportation;

        TeleportConfig newOne = new TeleportConfig(false);

        newOne.bossBar = this.bossBar == null ? defaultConf.bossBar : defaultConf.bossBar.cloneMerge(this.bossBar);
        newOne.titleMessage = this.titleMessage == null ? defaultConf.titleMessage : defaultConf.titleMessage.cloneMerge(this.titleMessage);

        newOne.allowBack = this.allowBack && defaultConf.allowBack;

        return newOne;
    }
}
