package dev.codedsakura.blossom.lib.teleport;

import dev.codedsakura.blossom.lib.BlossomGlobals;
import dev.codedsakura.blossom.lib.utils.CubicBezierCurve;

public class TeleportConfig {

    public BossBarConfig bossBar;


    public TitleMessageConfig titleMessage;


    public boolean actionBarMessageEnabled = false;


    public CubicBezierCurve fovEffectBefore = new CubicBezierCurve(new double[]{1, 0, 1, 0}, 1, .5, 10, false);

    public CubicBezierCurve fovEffectAfter = new CubicBezierCurve(new double[]{0, 1, 0, 1.25}, .5, 1, 10, false);


    public ParticleAnimation particleAnimation = ParticleAnimation.OFF;


    public boolean allowBack = true;

    public boolean cancelOnMove = false;


    public enum ParticleAnimation {
        OFF
    }


    public TeleportConfig(boolean defaults) {
        bossBar = defaults ? new BossBarConfig(true) : null;
        titleMessage = defaults ? new TitleMessageConfig(true) : null;
    }

    public TeleportConfig cloneMerge() {
        TeleportConfig defaultConf = BlossomGlobals.CONFIG.baseTeleportation;

        TeleportConfig newOne = new TeleportConfig(false);

        newOne.bossBar = this.bossBar == null ? defaultConf.bossBar : defaultConf.bossBar.cloneMerge(this.bossBar);
        newOne.titleMessage = this.titleMessage == null ? defaultConf.titleMessage : defaultConf.titleMessage.cloneMerge(this.titleMessage);

        newOne.allowBack = this.allowBack && defaultConf.allowBack;

        return newOne;
    }
}
