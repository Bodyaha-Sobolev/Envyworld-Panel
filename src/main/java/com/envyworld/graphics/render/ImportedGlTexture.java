package com.envyworld.graphics.render;

import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.opengl.GlTextureView;

public final class ImportedGlTexture extends GlTexture {
   private static final int USAGE_SAMPLED_AND_COPY_SOURCE = 7;
   private final GpuTextureView view = new ImportedGlTexture.ImportedGlTextureView(this);

   private ImportedGlTexture(int glId, int width, int height) {
      super(7, "envyworld/imported_texture", TextureFormat.RGBA8, width, height, 1, 1, glId);
   }

   public static ImportedGlTexture wrap(int glId, int width, int height) {
      if (glId > 0 && width > 0 && height > 0) {
         return new ImportedGlTexture(glId, width, height);
      } else {
         throw new IllegalArgumentException("Invalid imported texture");
      }
   }

   public GpuTextureView view() {
      return this.view;
   }

   @Override
   public void close() {
      this.view.close();
      super.close();
   }

   private static final class ImportedGlTextureView extends GlTextureView {
      private ImportedGlTextureView(GlTexture texture) {
         super(texture, 0, 1);
      }
   }
}
