package dev.codedsakura.blossom.lib.mixin;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.network.packet.s2c.play.EntityStatusEffectS2CPacket;
import net.minecraft.network.packet.s2c.play.ExperienceBarUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public class MC124177Fix {
    @Inject(method = "setServerWorld", at = @At("TAIL"))
    void BlossomLib$teleportFix(ServerWorld destination, CallbackInfo cir) {
        ServerPlayerEntity self = (ServerPlayerEntity) (Object) this;

        if (self.networkHandler == null) {
            return;
        }

        for (StatusEffectInstance statusEffectInstance : self.getStatusEffects())
            self.networkHandler.sendPacket(new EntityStatusEffectS2CPacket(self.getId(), statusEffectInstance));

        self.networkHandler.sendPacket(new ExperienceBarUpdateS2CPacket(self.experienceProgress, self.totalExperience, self.experienceLevel));
    }
}
