package dev.codedsakura.blossom.lib.text;

import dev.codedsakura.blossom.lib.BlossomGlobals;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.StringJoiner;

public class DimName {
    public static String get(ServerLevel world) {
        return get(world.dimension().identifier());
    }

    public static String get(Identifier dimensionIdentifier) {
        if (BlossomGlobals.CONFIG.dimNameOverrides != null) {
            String key = dimensionIdentifier.toString();
            if (BlossomGlobals.CONFIG.dimNameOverrides.containsKey(key)) {
                return BlossomGlobals.CONFIG.dimNameOverrides.get(key);
            }
        }

        StringJoiner joiner = new StringJoiner(" ");

        Arrays.stream(dimensionIdentifier.getPath().split("_"))
                .map(StringUtils::capitalize)
                .forEachOrdered(joiner::add);

        return joiner.toString();
    }

    public static String get(String dimensionIdentifier) {
        return get(Identifier.parse(dimensionIdentifier));
    }
}
