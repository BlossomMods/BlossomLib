package dev.codedsakura.blossom.lib.data;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public abstract class ListDataController<T> extends DataController<List<T>> {
    public abstract Class<T[]> getArrayClassType();

    @Override
    protected List<T> readJson(InputStreamReader reader) {
        return new ArrayList<>(List.of(GSON.fromJson(new BufferedReader(reader), getArrayClassType())));
    }
}
