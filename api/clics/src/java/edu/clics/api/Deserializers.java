package edu.clics.api;

import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.google.protobuf.Duration;
import com.google.protobuf.Timestamp;

import edu.clics.proto.ClicsProto.*;

import java.io.IOException;
import java.lang.reflect.Type;
import java.time.Instant;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

class Deserializers {
  static class EventFeedItemTypeAdapterFactory implements TypeAdapterFactory {
    @SuppressWarnings("unchecked")
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
      if (type.getRawType() != EventFeedItem.class) {
        return null;
      }
      return (TypeAdapter<T>) new EventFeedItemTypeAdapter(
          gson.getDelegateAdapter(this, (TypeToken<EventFeedItem>) type),
          gson.getAdapter(JsonElement.class));
    }

    private class EventFeedItemTypeAdapter extends TypeAdapter<EventFeedItem> {
      private final TypeAdapter<EventFeedItem> delegate;
      private final TypeAdapter<JsonElement> elementAdapter;

      public EventFeedItemTypeAdapter(
          TypeAdapter<EventFeedItem> delegate,
          TypeAdapter<JsonElement> elementAdapter) {
        super();
        this.delegate = delegate;
        this.elementAdapter = elementAdapter;
      }

      @Override
      public void write(JsonWriter writer, EventFeedItem value) throws IOException {
        JsonElement tree = delegate.toJsonTree(value);
        if (tree != null && tree.isJsonObject()) {
          JsonObject obj = tree.getAsJsonObject();
          if (obj.has("type") && obj.get("type").isJsonPrimitive()) {
            final String type = obj.get("type").getAsString();
            final String name = type.substring(0, type.length() - 1) + "_data";
            if (obj.has(name)) {
              obj.add("data", obj.get(name));
              obj.remove(name);
            }
          }
          tree = obj;
        }
        elementAdapter.write(writer, tree);
      }

      @Override
      public EventFeedItem read(JsonReader reader) throws IOException {
        JsonElement tree = elementAdapter.read(reader);
        if (tree != null && tree.isJsonObject()) {
          JsonObject obj = tree.getAsJsonObject();
          if (obj.has("type") && obj.get("type").isJsonPrimitive()) {
            final String type = obj.get("type").getAsString();
            final String name = type.substring(0, type.length() - 1) + "_data";
            if (obj.has("data")) {
              obj.add(name, obj.get("data"));
              obj.remove("data");
            }
          }
          tree = obj;
        }
        return delegate.fromJsonTree(tree);
      }
    }
  }

  static class TimestampDeserializer implements JsonDeserializer<Timestamp> {
    @Override
    public Timestamp deserialize(JsonElement json, Type type, JsonDeserializationContext context)
        throws JsonParseException {
      JsonPrimitive primitive = json.getAsJsonPrimitive();
      if (primitive.isString()) {
        try {
          // Workaround for servers with date strings that don't zero-pad the seconds or include
          // TZ minutes, for example "2021-03-27T09:24:1.482+00"
          //              instead of "2021-03-27T09:24:01.482+00:00"
          final Instant instant = OffsetDateTime.parse(
              primitive.getAsString()
                  .replaceAll(":([0-9])\\.", ":0$1.")
                  .replaceAll("\\+0+$", "+00:00"))
              .toInstant();
          return Timestamp.newBuilder()
              .setSeconds(instant.getEpochSecond())
              .setNanos(instant.getNano())
              .build();
        } catch (DateTimeParseException e) {
          throw new JsonParseException("Invalid timestamp string: " + primitive.getAsString(), e);
        }
      }
      throw new JsonParseException("Invalid timestamp value: " + primitive);
    }
  }

  static class DurationDeserializer implements JsonDeserializer<Duration> {
    @Override
    public Duration deserialize(JsonElement json, Type type, JsonDeserializationContext context)
        throws JsonParseException {
      JsonPrimitive primitive = json.getAsJsonPrimitive();
      if (primitive.isString()) {
        try {
          return parseIcpcDuration(primitive.getAsString());
        } catch (DateTimeParseException e) {
          throw new JsonParseException("Invalid duration string: " + primitive.getAsString(), e);
        }
      }
      throw new JsonParseException("Invalid duration value: " + primitive);
    }

    /**
     * LocalTime.parse() won't work for the following because CLICS uses a "slight modification of
     * ISO 8601" with the slight modification being that the first digit of the hour may be dropped.
     *
     * This fails java.time's validation checks so we need to do it by hand instead.
     */
    private static Duration parseIcpcDuration(String s) throws DateTimeParseException {
      String[] elements = s.split("\\.|:");
      if (elements.length == 3 || elements.length == 4) {
        try {
          java.time.Duration res = java.time.Duration.ZERO
                .plusHours(Math.abs(Long.parseLong(elements[0])))
                .plusMinutes(Math.abs(Long.parseLong(elements[1])))
                .plusSeconds(Math.abs(Long.parseLong(elements[2])));
          if (elements.length == 4) {
            res = res.plusNanos(Long.parseLong((elements[3] + "000000000").substring(0, 9)));
          }

          final boolean positive = Long.parseLong(elements[0]) >= 0;
          return Duration.newBuilder()
              .setSeconds(res.getSeconds() * (positive ? +1 : -1))
              .setNanos(res.getNano() * (positive ? +1 : -1))
              .build();
        } catch (Exception e) {
          // Throw the exception on the next line.
        }
        throw new DateTimeParseException("Text '" + s + "' could not be parsed", s, 0);
      } else {
        throw new DateTimeParseException("Text '" + s + "' has wrong number of items", s, 0);
      }
    }
  }
}
