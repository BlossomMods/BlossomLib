package dev.codedsakura.blossom.lib.mixin;

import dev.codedsakura.blossom.lib.BlossomGlobals;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class MixinConfigPlugin implements IMixinConfigPlugin {
    private final int packageNameLength = this.getClass().getPackage().getName().length() + 1;

    @Override
    public void onLoad(String mixinPackage) {
        BlossomGlobals.LOGGER.info("BlossomLib mixins loading...");
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        String[] mixin = mixinClassName.substring(packageNameLength).split("\\.");

        if (mixin.length == 1) {
            if (mixin[0].equals("MC124177Fix")) {
                return BlossomGlobals.CONFIG.enableMC124177Fix;
            }
        }

        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
