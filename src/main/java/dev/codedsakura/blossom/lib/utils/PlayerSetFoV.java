package dev.codedsakura.blossom.lib.utils;

import net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Abilities;

public class PlayerSetFoV {
    public static void setPlayerFoV(ServerPlayer player, float multiplier) {

        // invert math done in AbstractClientPlayerEntity#getFovMultiplier
        float genericMovementSpeed = (float) player.getAttributeValue(Attributes.MOVEMENT_SPEED);
        multiplier = (genericMovementSpeed / 2f) / (multiplier - .5f);

        var abilities = new Abilities();
        abilities.setWalkingSpeed(multiplier);
        player.connection.send(new ClientboundPlayerAbilitiesPacket(abilities));
    }
}
