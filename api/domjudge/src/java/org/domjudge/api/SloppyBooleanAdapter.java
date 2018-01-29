package org.domjudge.api;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;

import java.lang.reflect.Type;

/**
 * Type deserializer for Boolean that will also accept integer values of 0 and 1
 * as representing booleans. Will not accept strings or anything else including
 * other integer values.
 *
 * This is because some bits of the domjudge api, eg.
 * <code>/api/v3/judgement_types:compile-error</code>
 * are victims of php typecasting issues.
 */
class SloppyBooleanDeserializer implements JsonDeserializer<Boolean> {
  @Override
  public Boolean deserialize(JsonElement json, Type type, JsonDeserializationContext context)
      throws JsonParseException {
    JsonPrimitive primitive = json.getAsJsonPrimitive();
    if (primitive.isBoolean()) {
      return primitive.getAsBoolean();
    }
    if (primitive.isNumber()) {
      int value = primitive.getAsInt();
      if (value != 0 && value != 1) {
        throw new JsonParseException("Invalid boolean value: " + value);
      }
      return (value == 1);
    }
    throw new JsonParseException("Invalid boolean value: " + primitive);
  }
}
