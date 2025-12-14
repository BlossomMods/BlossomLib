package dev.codedsakura.blossom.lib.utils;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class PlayerSetFoV {
    public static void setPlayerFoV(ServerPlayer player, float multiplier) {

        // invert math done in AbstractClientPlayerEntity#getFovMultiplier
        float genericMovementSpeed = (float) player.getAttributeValue(Attributes.MOVEMENT_SPEED);
        multiplier = (genericMovementSpeed / 2f) / (multiplier - .5f);

        player.getAbilities().setWalkingSpeed(multiplier);
        player.onUpdateAbilities();
    }
}
