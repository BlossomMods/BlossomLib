package dev.codedsakura.blossom.lib.polyfill;

import net.minecraft.util.Identifier;

public class IdentifierPolyfill {
    public static Identifier of(String namespace, String path) {
        return Identifier.of(namespace, path);
    }

    public static Identifier of(String id) {
        return Identifier.of(id);
    }
}
