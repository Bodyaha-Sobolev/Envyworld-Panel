package com.envyworld.graphics.util;

import java.nio.ByteBuffer;
import org.joml.Matrix4f;
import com.envyworld.graphics.misc.TextureHandle;

public interface GuiRenderDriver {
   default void endFrame() {
   }

   void setViewportSize(int var1, int var2);

   default int getBackdropTextureSlot() {
      return 5;
   }

   void endLayerFrame();

   int bindTexture(int var1, int var2, int var3);

   void setProjectionMatrix(Matrix4f var1);

   default TextureHandle blurTextureRange(TextureHandle texture, float startRadius, float endRadius, float startOffset, float endOffset, int frameId) {
      return this.blurTexture(texture, Math.max(startRadius, endRadius), frameId);
   }

   void setStencilMode(int var1);

   void close();

   default void prepareBackdrop() {
   }

   default void setBackdropBlurRadius(float radius) {
   }

   void setScissor(int var1, int var2, int var3, int var4);

   void resetTextureBindings();

   void drawIndexed(ByteBuffer var1, int var2, ByteBuffer var3, int var4);

   default void resetFrameResources() {
   }

   void beginFrame(int var1, int var2);

   default TextureHandle blurTexture(TextureHandle texture, float radius, int frameId) {
      return texture;
   }

   void beginLayer(TextureHandle var1);

   void beginStencil(int var1);

   default TextureHandle acquireLayer(int width, int height) {
      return this.acquireDedicatedLayer(width, height);
   }

   TextureHandle acquireDedicatedLayer(int var1, int var2);

   void endStencil();

   void applyStencil(int var1);
}
