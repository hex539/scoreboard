package edu.clics.api;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.protobuf.Duration;
import com.google.protobuf.Timestamp;

import java.lang.reflect.Type;
import java.time.Instant;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

class Deserializers {
  static class TimestampDeserializer implements JsonDeserializer<Timestamp> {
    @Override
    public Timestamp deserialize(JsonElement json, Type type, JsonDeserializationContext context)
        throws JsonParseException {
      JsonPrimitive primitive = json.getAsJsonPrimitive();
      if (primitive.isString()) {
        try {
          final Instant instant = OffsetDateTime.parse(primitive.getAsString()).toInstant();
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
     * Yes, we're rolling our own duration parser. No, LocalTime.parse() won't work.
     *
     * CLICS, being designed by some of the best programmers in the world, uses a "slight
     * modification of ISO 8601" with the slight modification being that the first digit of the
     * hour may be dropped.
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
            res = res.plusMillis(Math.abs(Long.parseLong(elements[3])));
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
