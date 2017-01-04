package me.hex539.console;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Retention(value = RetentionPolicy.RUNTIME)
public @interface Command {
  String name();

  public static abstract class Annotations {
    public static Map<String, Method> all(Class c) {
      return Arrays.stream(Executive.class.getDeclaredMethods())
        .filter(f -> Modifier.isStatic(f.getModifiers()))
        .filter(f -> f.getAnnotation(Command.class) != null)
        .collect(Collectors.toMap(
          f -> f.getAnnotation(Command.class).name(),
          f -> f));
    }
  }
}
