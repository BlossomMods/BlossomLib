package dev.codedsakura.blossom.lib.utils;

import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.network.packet.s2c.play.PlayerAbilitiesS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;

public class PlayerSetFoV {
    public static void setPlayerFoV(ServerPlayerEntity player, float multiplier) {

        // invert math done in AbstractClientPlayerEntity#getFovMultiplier
        float genericMovementSpeed = (float) player.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        multiplier = (genericMovementSpeed / 2f) / (multiplier - .5f);

        var abilities = new PlayerAbilities();
        abilities.setWalkSpeed(multiplier);
        player.networkHandler.sendPacket(new PlayerAbilitiesS2CPacket(abilities));
    }
}
