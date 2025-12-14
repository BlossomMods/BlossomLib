package dev.codedsakura.blossom.lib.mixin;

import net.minecraft.network.protocol.game.ClientboundSetExperiencePacket;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public class MC124177Fix {
    @Inject(method = "setServerLevel", at = @At("TAIL"))
    void BlossomLib$teleportFix(ServerLevel destination, CallbackInfo cir) {
        ServerPlayer self = (ServerPlayer) (Object) this;

        if (self.connection == null) {
            return;
        }

        for (MobEffectInstance statusEffectInstance : self.getActiveEffects())
            self.connection.send(new ClientboundUpdateMobEffectPacket(self.getId(), statusEffectInstance, false));

        self.connection.send(new ClientboundSetExperiencePacket(self.experienceProgress, self.totalExperience, self.experienceLevel));
    }
}
