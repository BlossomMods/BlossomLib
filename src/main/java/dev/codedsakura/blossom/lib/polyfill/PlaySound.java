package dev.codedsakura.blossom.lib.polyfill;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;

public class PlaySound {
    public static void playToPlayer(ServerPlayerEntity player, Sound sound) {
        player.playSoundToPlayer(
                SoundEvent.of(IdentifierPolyfill.of(sound.id())),
                SoundCategory.PLAYERS,
                sound.volume(),
                sound.pitch()
        );
    }

    public record Sound(String id, float volume, float pitch) {
    }
}
