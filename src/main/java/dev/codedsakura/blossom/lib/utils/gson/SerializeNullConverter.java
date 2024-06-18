package dev.codedsakura.blossom.lib.utils.gson;

import com.google.gson.*;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.lang.reflect.Field;

// based on https://stackoverflow.com/a/60265364
public class SerializeNullConverter implements TypeAdapterFactory {
    void removeNonNullableNullFields(JsonObject jsonObject, Object value, Class<?> type) throws IllegalAccessException {
        if (value == null) return;

        for (Field field : type.getDeclaredFields()) {
            String jsonName = field.isAnnotationPresent(SerializedName.class) ?
                    field.getAnnotation(SerializedName.class).value() : field.getName();

            if (jsonObject.get(jsonName) instanceof JsonNull) {
                if (!field.isAnnotationPresent(SerializeNull.class)) {
                    jsonObject.remove(jsonName);
                }
            } else {
                if (jsonObject.has(jsonName) && jsonObject.get(jsonName).isJsonObject()) {
                    removeNonNullableNullFields(
                            jsonObject.get(jsonName).getAsJsonObject(),
                            field.get(value),
                            field.getType()
                    );
                }
            }
        }
    }

    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        boolean anyNullable = false;

        for (Field declaredField : type.getRawType().getDeclaredFields()) {
            if (declaredField.isAnnotationPresent(SerializeNull.class)) {
                anyNullable = true;
                break;
            }
        }

        if (!anyNullable) {
            return null;
        }

        TypeAdapter<T> delegateAdapter = gson.getDelegateAdapter(SerializeNullConverter.this, type);
        TypeAdapter<JsonElement> elementAdapter = gson.getAdapter(JsonElement.class);

        return new TypeAdapter<>() {
            @Override
            public void write(JsonWriter out, T value) throws IOException {
                JsonObject jsonObject = delegateAdapter.toJsonTree(value).getAsJsonObject();

                try {
                    removeNonNullableNullFields(jsonObject, value, type.getRawType());
                } catch (IllegalAccessException ignored) {
                }

                out.setSerializeNulls(true);
                elementAdapter.write(out, jsonObject);
            }

            @Override
            public T read(JsonReader in) throws IOException {
                return delegateAdapter.read(in);
            }
        };
    }
}
