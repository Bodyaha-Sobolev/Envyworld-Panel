package com.envyworld.graphics.render;

import java.nio.ByteBuffer;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL33;
import com.envyworld.graphics.misc.TextureHandle;

public final class LayerFramebuffer implements TextureHandle {
   private int generation;
   private final int framebufferId;
   private boolean inUse;
   private int lastUsedFrame = RenderFrameClock.currentFrame();
   private int width;
   private final int depthStencilId;
   private int height;
   private final int colorTextureId;

   public LayerFramebuffer(int width, int height) {
      int previousFramebuffer = GL33.glGetInteger(36006);
      int previousTexture = GL33.glGetInteger(32873);
      int previousRenderbuffer = GL33.glGetInteger(36007);
      this.colorTextureId = GL33.glGenTextures();
      GL33.glBindTexture(3553, this.colorTextureId);
      GL33.glTexParameteri(3553, 10241, 9728);
      GL33.glTexParameteri(3553, 10240, 9728);
      GL33.glTexParameteri(3553, 10242, 33071);
      GL33.glTexParameteri(3553, 10243, 33071);
      this.framebufferId = GL33.glGenFramebuffers();
      this.depthStencilId = GL33.glGenRenderbuffers();
      this.resize(width, height);
      GL33.glBindRenderbuffer(36161, previousRenderbuffer);
      GL33.glBindTexture(3553, previousTexture);
      GL33.glBindFramebuffer(36160, previousFramebuffer);
   }

   public void resize(int width, int height) {
      int previousFramebuffer = GL33.glGetInteger(36006);
      int previousTexture = GL33.glGetInteger(32873);
      int previousRenderbuffer = GL33.glGetInteger(36007);
      this.width = width;
      this.height = height;

      try {
         GL33.glBindTexture(3553, this.colorTextureId);
         GL33.glTexImage2D(3553, 0, 32856, width, height, 0, 6408, 5121, (ByteBuffer)null);
         GL33.glBindRenderbuffer(36161, this.depthStencilId);
         GL33.glRenderbufferStorage(36161, 35056, width, height);
         GL33.glBindFramebuffer(36160, this.framebufferId);
         GL33.glFramebufferTexture2D(36160, 36064, 3553, this.colorTextureId, 0);
         GL33.glFramebufferRenderbuffer(36160, 33306, 36161, this.depthStencilId);
         int status = GL33.glCheckFramebufferStatus(36160);
         if (status != 36053) {
            throw new IllegalStateException("Layer framebuffer is incomplete: " + status);
         }
      } finally {
         GL33.glBindRenderbuffer(36161, previousRenderbuffer);
         GL33.glBindTexture(3553, previousTexture);
         GL33.glBindFramebuffer(36160, previousFramebuffer);
      }
   }

   public void delete() {
      Minecraft client = Minecraft.getInstance();
      if (client != null && !client.isSameThread()) {
         client.execute(this::delete);
      } else {
         GL33.glDeleteFramebuffers(this.framebufferId);
         GL33.glDeleteRenderbuffers(this.depthStencilId);
         GL33.glDeleteTextures(this.colorTextureId);
      }
   }

   public int framebufferId() {
      return this.framebufferId;
   }

   public int width() {
      return this.width;
   }

   public int height() {
      return this.height;
   }

   public int generation() {
      return this.generation;
   }

   public void incrementGeneration() {
      this.generation++;
   }

   public boolean inUse() {
      return this.inUse;
   }

   public void setInUse(boolean inUse) {
      this.inUse = inUse;
   }

   public int lastUsedFrame() {
      return this.lastUsedFrame;
   }

   public void markUsed(int frame) {
      this.lastUsedFrame = frame;
   }

   @Override
   public int getTextureId() {
      return this.colorTextureId;
   }

   @Override
   public int getHeight() {
      return this.height;
   }

   @Override
   public int getWidth() {
      return this.width;
   }

   @Override
   public int getGeneration() {
      return this.generation;
   }
}
