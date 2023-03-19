package dev.codedsakura.blossom.lib.teleport;

public class BossBarConfig {
    public boolean enabled = true;
    public String color;
    public String textColor;

    public BossBarConfig(boolean defaults) {
        color = defaults ? "blue" : null;
        textColor = defaults ? "white" : null;
    }

    public BossBarConfig cloneMerge(BossBarConfig old) {
        BossBarConfig newOne = new BossBarConfig(false);

        newOne.color = this.color != null ? this.color : old.color;
        newOne.textColor = this.textColor != null ? this.textColor : old.textColor;

        return newOne;
    }
}
