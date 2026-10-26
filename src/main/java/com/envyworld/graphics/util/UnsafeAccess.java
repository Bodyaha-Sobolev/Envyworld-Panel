package com.envyworld.graphics.util;

import java.lang.reflect.Field;
import sun.misc.Unsafe;

public final class UnsafeAccess {
   private static final Unsafe INSTANCE = findUnsafe();

   private UnsafeAccess() {
   }

   public static Unsafe get() {
      return INSTANCE;
   }

   private static Unsafe findUnsafe() {
      try {
         Field field = Unsafe.class.getDeclaredField("theUnsafe");
         field.setAccessible(true);
         return (Unsafe)field.get(null);
      } catch (ReflectiveOperationException exception) {
         throw new IllegalStateException("Unable to access the JVM native-memory API", exception);
      }
   }
}
