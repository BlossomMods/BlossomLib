package dev.codedsakura.blossom.lib.utils;

import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.network.packet.s2c.play.PlayerAbilitiesS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;

public class PlayerSetFoV {
    /**
     * Set the player's field-of-view to have the specified multiplier
     *
     * @param player     - the player to whom to apply the FoV effect
     * @param multiplier - FoV multiplier, works from about .1 to 2
     * @implNote The player abilities work is such a way that potion effects are applied on top,
     * thus this works even with speed effects and such. This implementation avoids changing the
     * abilities on the server, only sending the player a copy with the modified attributes.
     * To reset use the {@link PlayerSetFoV#resetPlayerFoV} method.
     */
    public static void setPlayerFoV(ServerPlayerEntity player, float multiplier) {

        // invert math done in AbstractClientPlayerEntity#getFovMultiplier
        float genericMovementSpeed = (float) player.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        multiplier = (genericMovementSpeed / 2f) / (multiplier - .5f);

        // clone player abilities, setting walkSpeed to expected value
        PlayerAbilities abilities = new PlayerAbilities();
        abilities.invulnerable = player.getAbilities().invulnerable;
        abilities.flying = player.getAbilities().flying;
        abilities.allowFlying = player.getAbilities().allowFlying;
        abilities.creativeMode = player.getAbilities().creativeMode;
        abilities.allowModifyWorld = player.getAbilities().allowModifyWorld;
        abilities.setFlySpeed(player.getAbilities().getFlySpeed());
        abilities.setWalkSpeed(multiplier);
        player.networkHandler.sendPacket(new PlayerAbilitiesS2CPacket(abilities));
    }

    public static void resetPlayerFoV(ServerPlayerEntity player) {
        player.sendAbilitiesUpdate();
    }
}
