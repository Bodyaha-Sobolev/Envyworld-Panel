package com.envyworld.graphics.misc;

import java.util.function.Function;

public class ResourceResolver {
   private final Function<String, ResourceData> resourceLoader;
   private final String basePath;

   public ResourceResolver(String basePath, Function<String, ResourceData> resourceLoader) {
      this.basePath = basePath;
      this.resourceLoader = resourceLoader;
   }

   private static String normalizePath(String path) {
      if (path != null && !path.isEmpty()) {
         String normalizedPath = path.replace('\\', '/').trim();

         while (normalizedPath.startsWith("/")) {
            normalizedPath = normalizedPath.substring(1);
         }

         while (normalizedPath.endsWith("/")) {
            normalizedPath = normalizedPath.substring(0, normalizedPath.length() - 1);
         }

         return normalizedPath;
      } else {
         return "";
      }
   }

   public ResourceData resolve(String relativePath) {
      Object resolvedPath = (normalizePath(this.basePath) + "/" + normalizePath(relativePath)).replaceAll("/{2,}", "/");
      if (!((String)resolvedPath).startsWith("/")) {
         resolvedPath = "/" + (String)resolvedPath;
      }

      return this.resourceLoader.apply((String)resolvedPath);
   }
}
