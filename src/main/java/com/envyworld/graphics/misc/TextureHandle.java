package com.envyworld.graphics.misc;

public interface TextureHandle {
   int getTextureId();

   int getHeight();

   int getWidth();

   default int getGeneration() {
      return 0;
   }
}
