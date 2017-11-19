package me.hex539.app.intent;

import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

public class IntentUtils {
  private IntentUtils() {}

  public static boolean validateIntent(Intent intent, Class requiredExtras, String tag) {
    List<String> missingExtras = new ArrayList<>();
    for (Field field : requiredExtras.getDeclaredFields()) {
      if (Modifier.isStatic(field.getModifiers())) {
        try {
          if (!intent.hasExtra((String) field.get(null))) {
            missingExtras.add((String) field.get(null));
          }
        } catch (IllegalAccessException skipped) {
        }
      }
    }
    if (!missingExtras.isEmpty()) {
      Log.e(tag, "Missing intent extras: " + TextUtils.join(", ", missingExtras));
      return false;
    }
    return true;
  }

  public static boolean validateIntent(Intent intent, Class requiredExtras) {
    return validateIntent(intent, requiredExtras, getCallingClass());
  }

  private static String getCallingClass() {
    final String self = IntentUtils.class.getName();
    for (StackTraceElement stack : new Throwable().getStackTrace()) {
      String c = stack.getClassName();
      if (!self.equals(c)) {
        final int dotIndex = c.lastIndexOf('.');
        return dotIndex != -1 ? c.substring(dotIndex) : c;
      }
    }
    return IntentUtils.class.getSimpleName();
  }
}
