package dev.codedsakura.blossom.lib.teleport;

import dev.codedsakura.blossom.lib.permissions.Permissions;
import dev.codedsakura.blossom.lib.text.TextUtils;
import dev.codedsakura.blossom.lib.utils.CubicBezierCurve;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class TeleportConfig {
    public static final TeleportConfig DEFAULT = new Builder()
            .setBossBar(new BossBarConfig(true))
            .setTitleMessage(new TitleMessageConfig(true))
            .setActionBarMessageEnabled(false)
            .setFovEffectBefore(new CubicBezierCurve(new double[]{1, 0, 1, 0}, 1, .5, 10, false))
            .setFovEffectAfter(new CubicBezierCurve(new double[]{0, 1, 0, 1.25}, .5, 1, 10, false))
            .setParticleAnimation(ParticleAnimation.OFF)
            .setAllowBack(true)
            .setCancelOnMove(false)
            .setStandStill(3)
            .setCooldown(30)
            .setDimensionBlacklist()
            .setUseDimBlacklistAsWhitelist(false)
            .setDimBlacklistBehavior(DimBlacklistBehavior.SOURCE_OR_DEST)
            .build();

    public BossBarConfig bossBar;
    public TitleMessageConfig titleMessage;

    public Boolean actionBarMessageEnabled;

    public CubicBezierCurve fovEffectBefore;
    public CubicBezierCurve fovEffectAfter;

    public ParticleAnimation particleAnimation;

    public Boolean allowBack;
    public Boolean cancelOnMove;

    public Integer standStill;
    public Integer cooldown;

    public List<String> dimensionBlacklist;
    public Boolean useDimBlacklistAsWhitelist;
    public DimBlacklistBehavior dimBlacklistBehavior;

    public Map<String, TeleportConfig> permissionOverrides = Map.of();


    public enum ParticleAnimation {
        OFF
    }

    public enum DimBlacklistBehavior {
        SOURCE,
        DEST,
        SOURCE_AND_DEST,
        SOURCE_OR_DEST,
    }


    public TeleportConfig() {
    }


    private TeleportConfig cloneMergeDefault() {
        return new Builder()
                .setBossBarOrDefault(this.bossBar)
                .setTitleMessageOrDefault(this.titleMessage)

                .setActionBarMessageEnabledOrDefault(this.actionBarMessageEnabled)

                .setFovEffectBeforeOrDefault(this.fovEffectBefore)
                .setFovEffectAfterOrDefault(this.fovEffectAfter)

                .setParticleAnimationOrDefault(this.particleAnimation)

                .setAllowBackOrDefault(this.allowBack)
                .setCancelOnMoveOrDefault(this.cancelOnMove)

                .setStandStillOrDefault(this.standStill)
                .setCooldownOrDefault(this.cooldown)

                .setDimensionBlacklistOrDefault(this.dimensionBlacklist)
                .setUseDimBlacklistAsWhitelistOrDefault(this.useDimBlacklistAsWhitelist)
                .setDimBlacklistBehavior(this.dimBlacklistBehavior)
                .build();
    }

    public TeleportConfig getPlayerSpecific(ServerPlayerEntity player) {
        if (DEFAULT.permissionOverrides.isEmpty()) {
            return this.cloneMergeDefault();
        }

        Optional<String> firstPerm = DEFAULT.permissionOverrides
                .keySet()
                .stream()
                .filter(perm -> Permissions.check(player, perm, false))
                .findFirst();

        if (firstPerm.isEmpty()) {
            return this.cloneMergeDefault();
        }

        return DEFAULT.permissionOverrides
                .get(firstPerm.get())
                .cloneMergeDefault();
    }


    private enum DimTeleportCheck {
        ALLOWED(""),
        SOURCE_IN_BLACKLIST("source-blacklist"),
        DEST_IN_BLACKLIST("dest-blacklist"),
        SOURCE_AND_DEST_IN_BLACKLIST("source-dest-blacklist"),
        SOURCE_NOT_IN_WHITELIST("source-whitelist"),
        DEST_NOT_IN_WHITELIST("dest-whitelist"),
        SOURCE_AND_DEST_NOT_IN_WHITELIST("source-dest-whitelist"),
        ;

        public final String localeKey;

        DimTeleportCheck(String localeKey) {
            this.localeKey = localeKey;
        }
    }

    public boolean teleportAllowed(ServerPlayerEntity player, String dest) {
        var config = this.getPlayerSpecific(player);
        var source = player.getWorld().getRegistryKey().getValue().toString();
        return dimTeleportCheck(config, source, dest) == DimTeleportCheck.ALLOWED;
    }

    public boolean teleportCheckAndInform(ServerPlayerEntity player, String dest) {
        var config = this.getPlayerSpecific(player);
        var source = player.getWorld().getRegistryKey().getValue().toString();
        var check = dimTeleportCheck(config, source, dest);

        if (check == DimTeleportCheck.ALLOWED) {
            return true;
        }

        player.sendMessage(TextUtils.fTranslation("blossom.error.teleport." + check.localeKey, TextUtils.Type.ERROR, source, dest), false);

        return false;
    }

    private static DimTeleportCheck dimTeleportCheck(TeleportConfig config, String source, String dest) {
        boolean sourceInList = config.dimensionBlacklist.contains(source);
        boolean destInList = config.dimensionBlacklist.contains(dest);

        if (config.useDimBlacklistAsWhitelist) {
            switch (config.dimBlacklistBehavior) {
                case SOURCE:
                    if (!sourceInList) {
                        return DimTeleportCheck.SOURCE_NOT_IN_WHITELIST;
                    }
                    break;

                case DEST:
                    if (!destInList) {
                        return DimTeleportCheck.DEST_NOT_IN_WHITELIST;
                    }
                    break;

                case SOURCE_AND_DEST:
                    if (!sourceInList) {
                        if (!destInList) {
                            return DimTeleportCheck.SOURCE_AND_DEST_NOT_IN_WHITELIST;
                        } else {
                            return DimTeleportCheck.SOURCE_NOT_IN_WHITELIST;
                        }
                    }
                    if (!destInList) {
                        return DimTeleportCheck.DEST_NOT_IN_WHITELIST;
                    }
                    break;

                case SOURCE_OR_DEST:
                    if (!sourceInList && !destInList) {
                        return DimTeleportCheck.SOURCE_AND_DEST_NOT_IN_WHITELIST;
                    }
                    break;
            }
        } else {
            switch (config.dimBlacklistBehavior) {
                case SOURCE:
                    if (sourceInList) {
                        return DimTeleportCheck.SOURCE_IN_BLACKLIST;
                    }
                    break;

                case DEST:
                    if (destInList) {
                        return DimTeleportCheck.DEST_IN_BLACKLIST;
                    }
                    break;

                case SOURCE_AND_DEST:
                    if (sourceInList && destInList) {
                        return DimTeleportCheck.SOURCE_AND_DEST_IN_BLACKLIST;
                    }
                    break;

                case SOURCE_OR_DEST:
                    if (sourceInList) {
                        if (destInList) {
                            return DimTeleportCheck.SOURCE_AND_DEST_IN_BLACKLIST;
                        } else {
                            return DimTeleportCheck.SOURCE_IN_BLACKLIST;
                        }
                    }
                    if (destInList) {
                        return DimTeleportCheck.DEST_IN_BLACKLIST;
                    }
                    break;
            }
        }

        return DimTeleportCheck.ALLOWED;
    }


    public static class Builder {
        TeleportConfig config = new TeleportConfig();


        public Builder setBossBar(BossBarConfig bossBar) {
            config.bossBar = bossBar;
            return this;
        }

        public Builder setTitleMessage(TitleMessageConfig titleMessage) {
            config.titleMessage = titleMessage;
            return this;
        }

        public Builder setActionBarMessageEnabled(boolean actionBarMessageEnabled) {
            config.actionBarMessageEnabled = actionBarMessageEnabled;
            return this;
        }

        public Builder setFovEffectBefore(CubicBezierCurve fovEffectBefore) {
            config.fovEffectBefore = fovEffectBefore;
            return this;
        }

        public Builder setFovEffectAfter(CubicBezierCurve fovEffectAfter) {
            config.fovEffectAfter = fovEffectAfter;
            return this;
        }

        public Builder setParticleAnimation(ParticleAnimation particleAnimation) {
            config.particleAnimation = particleAnimation;
            return this;
        }

        public Builder setAllowBack(boolean allowBack) {
            config.allowBack = allowBack;
            return this;
        }

        public Builder setCancelOnMove(boolean cancelOnMove) {
            config.cancelOnMove = cancelOnMove;
            return this;
        }

        public Builder setStandStill(int standStill) {
            config.standStill = standStill;
            return this;
        }

        public Builder setCooldown(int cooldown) {
            config.cooldown = cooldown;
            return this;
        }

        public Builder setDimensionBlacklist(List<String> dimensionBlacklist) {
            config.dimensionBlacklist = dimensionBlacklist;
            return this;
        }

        public Builder setDimensionBlacklist(String... dimensionBlacklist) {
            config.dimensionBlacklist = List.of(dimensionBlacklist);
            return this;
        }

        public Builder setUseDimBlacklistAsWhitelist(boolean useDimBlacklistAsWhitelist) {
            config.useDimBlacklistAsWhitelist = useDimBlacklistAsWhitelist;
            return this;
        }

        public Builder setDimBlacklistBehavior(DimBlacklistBehavior dimBlacklistBehavior) {
            config.dimBlacklistBehavior = dimBlacklistBehavior;
            return this;
        }


        public Builder setBossBarOrDefault(@Nullable BossBarConfig bossBar) {
            return this.setBossBar(Optional.ofNullable(bossBar).orElse(DEFAULT.bossBar));
        }

        public Builder setTitleMessageOrDefault(@Nullable TitleMessageConfig titleMessage) {
            return this.setTitleMessage(Optional.ofNullable(titleMessage).orElse(DEFAULT.titleMessage));
        }

        public Builder setActionBarMessageEnabledOrDefault(@Nullable Boolean actionBarMessageEnabled) {
            return this.setActionBarMessageEnabled(Optional.ofNullable(actionBarMessageEnabled).orElse(DEFAULT.actionBarMessageEnabled));
        }

        public Builder setFovEffectBeforeOrDefault(@Nullable CubicBezierCurve fovEffectBefore) {
            return this.setFovEffectBefore(Optional.ofNullable(fovEffectBefore).orElse(DEFAULT.fovEffectBefore));
        }

        public Builder setFovEffectAfterOrDefault(@Nullable CubicBezierCurve fovEffectAfter) {
            return this.setFovEffectAfter(Optional.ofNullable(fovEffectAfter).orElse(DEFAULT.fovEffectAfter));
        }

        public Builder setParticleAnimationOrDefault(@Nullable ParticleAnimation particleAnimation) {
            return this.setParticleAnimation(Optional.ofNullable(particleAnimation).orElse(DEFAULT.particleAnimation));
        }

        public Builder setAllowBackOrDefault(@Nullable Boolean allowBack) {
            return this.setAllowBack(Optional.ofNullable(allowBack).orElse(DEFAULT.allowBack));
        }

        public Builder setCancelOnMoveOrDefault(@Nullable Boolean cancelOnMove) {
            return this.setCancelOnMove(Optional.ofNullable(cancelOnMove).orElse(DEFAULT.cancelOnMove));
        }

        public Builder setStandStillOrDefault(@Nullable Integer standStill) {
            return this.setStandStill(Optional.ofNullable(standStill).orElse(DEFAULT.standStill));
        }

        public Builder setCooldownOrDefault(@Nullable Integer cooldown) {
            return this.setCooldown(Optional.ofNullable(cooldown).orElse(DEFAULT.cooldown));
        }

        public Builder setDimensionBlacklistOrDefault(@Nullable List<String> dimensionBlacklist) {
            return this.setDimensionBlacklist(Optional.ofNullable(dimensionBlacklist).orElse(DEFAULT.dimensionBlacklist));
        }

        public Builder setUseDimBlacklistAsWhitelistOrDefault(@Nullable Boolean useDimBlacklistAsWhitelist) {
            return this.setUseDimBlacklistAsWhitelist(Optional.ofNullable(useDimBlacklistAsWhitelist).orElse(DEFAULT.useDimBlacklistAsWhitelist));
        }

        public Builder setDimBlacklistBehaviorOrDefault(@Nullable DimBlacklistBehavior dimBlacklistBehavior) {
            return this.setDimBlacklistBehavior(Optional.ofNullable(dimBlacklistBehavior).orElse(DEFAULT.dimBlacklistBehavior));
        }


        public TeleportConfig build() {
            return config;
        }
    }
}
