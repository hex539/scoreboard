package org.domjudge.api;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.google.protobuf.Int32Value;
import com.google.protobuf.Int64Value;
import com.google.protobuf.StringValue;
import com.google.protobuf.UInt32Value;
import com.google.protobuf.UInt64Value;

final class Deserializers {
  private Deserializers() {}

  static class WellKnownTypeAdapterFactory implements TypeAdapterFactory {
    @SuppressWarnings("unchecked")
    private final Map<Class, TypeAdapter<?>> wellKnownTypes = new HashMap<Class, TypeAdapter<?>>() {{
      put(Int32Value.class, make((w, o) -> w.value(o.getValue()),
          (r) -> Int32Value.newBuilder().setValue((int) r.nextLong()).build()));
      put(Int64Value.class, make((w, o) -> w.value(o.getValue()),
          (r) -> Int64Value.newBuilder().setValue((long) r.nextLong()).build()));
      put(UInt32Value.class, make((w, o) -> w.value(o.getValue()),
          (r) -> UInt32Value.newBuilder().setValue((int) r.nextLong()).build()));
      put(UInt64Value.class, make((w, o) -> w.value(o.getValue()),
          (r) -> UInt64Value.newBuilder().setValue((long) r.nextLong()).build()));
      put(StringValue.class, make((w, o) -> w.value(o.getValue()),
          (r) -> StringValue.newBuilder().setValue(r.nextString()).build()));
    }};

    @SuppressWarnings("unchecked")
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
      return (TypeAdapter<T>) wellKnownTypes.get(type.getRawType());
    }

    private <T, V> TypeAdapter<T> make(IOBiConsumer<JsonWriter, T> writeValue, IOFunction<JsonReader, T> readValue) {
      return new TypeAdapter<T>() {
        @Override
        public void write(JsonWriter writer, T value) throws IOException {
          if (value != null) {
            writeValue.accept(writer, value);
          } else {
            writer.nullValue();
          }
        }

        @Override
        public T read(JsonReader reader) throws IOException {
          if (reader.peek() != null) {
            return readValue.apply(reader);
          } else {
            return null;
          }
        }
      };
    }
  }

  @FunctionalInterface
  private interface IOBiConsumer<A, B> {
    void accept(A a, B b) throws IOException;
  }

  @FunctionalInterface
  private interface IOFunction<A, R> {
    R apply(A a) throws IOException;
  }
}
