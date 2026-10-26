package com.envyworld.graphics.render;

import java.nio.FloatBuffer;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.opengl.GlTexture;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL33;
import com.envyworld.graphics.misc.ClasspathResource;
import com.envyworld.graphics.misc.FrameRateLimiter;
import com.envyworld.graphics.misc.ResourceResolver;
import com.envyworld.graphics.misc.ShaderUniformWriter;
import com.envyworld.graphics.util.GlRenderStateSnapshot;

public final class KawaseBlurRenderer implements AutoCloseable {
   private static final int PASS_COUNT = 3;
   private static final int TARGET_COUNT = 4;
   private static final int REFRESH_RATE = 120;
   private static final int RESULT_TEXTURE_UNIT = 5;
   private static final float DEFAULT_OFFSET = 2.0F;
   private final GlShaderProgram downsampleShader;
   private final GlShaderProgram upsampleShader;
   private final ShaderUniformWriter downsampleImageUniform;
   private final ShaderUniformWriter downsampleOffsetUniform;
   private final ShaderUniformWriter downsampleResolutionUniform;
   private final ShaderUniformWriter upsampleImageUniform;
   private final ShaderUniformWriter upsampleOffsetUniform;
   private final ShaderUniformWriter upsampleResolutionUniform;
   private final int[] textures = new int[4];
   private final int[] framebuffers = new int[4];
   private final FrameRateLimiter refreshLimiter = new FrameRateLimiter(120);
   private int width = -1;
   private int height = -1;
   private int lastRenderedFrame = Integer.MIN_VALUE;
   private int vertexArrayId;
   private int vertexBufferId;

   public KawaseBlurRenderer() {
      ResourceResolver resources = new ResourceResolver("/assets/envyworld/renderer/shaders/", ClasspathResource::new);
      this.downsampleShader = new GlShaderProgram(resources.resolve("gl/kawase_down.frag"), resources.resolve("gl/fullscreen_quad.vert"));
      this.downsampleImageUniform = this.downsampleShader.registerUniform("image");
      this.downsampleOffsetUniform = this.downsampleShader.registerUniform("offset");
      this.downsampleResolutionUniform = this.downsampleShader.registerUniform("resolution");
      this.upsampleShader = new GlShaderProgram(resources.resolve("gl/kawase_up.frag"), resources.resolve("gl/fullscreen_quad.vert"));
      this.upsampleImageUniform = this.upsampleShader.registerUniform("image");
      this.upsampleOffsetUniform = this.upsampleShader.registerUniform("offset");
      this.upsampleResolutionUniform = this.upsampleShader.registerUniform("resolution");
   }

   public void captureMainFramebuffer(int frame, int width, int height) {
      long now = System.nanoTime();
      boolean targetsReusable = this.width == width && this.height == height && this.textures[0] != 0;
      if (!targetsReusable || this.lastRenderedFrame != frame && this.refreshLimiter.canRun(now)) {
         int sourceTextureId = getMainFramebufferTextureId();
         if (sourceTextureId > 0) {
            this.ensureFullscreenQuad();
            this.ensureTargets(width, height);
            this.renderBlurPyramid(sourceTextureId, width, height, 2.0F, 3);
            this.lastRenderedFrame = frame;
            this.refreshLimiter.markRun(now);
         }
      }
   }

   public void bindResultTexture() {
      if (this.textures[0] != 0) {
         GL13.glActiveTexture(33989);
         GL11.glBindTexture(3553, this.textures[0]);
         GL33.glBindSampler(5, 0);
         GL13.glActiveTexture(33984);
      }
   }

   public void releaseIfStale(int currentFrame, int maximumIdleFrames) {
      if (this.textures[0] != 0 && currentFrame - this.lastRenderedFrame >= maximumIdleFrames) {
         this.deleteTargets();
      }
   }

   private void ensureFullscreenQuad() {
      if (this.vertexArrayId == 0) {
         float[] vertices = new float[]{-1.0F, -1.0F, 0.0F, 0.0F, 1.0F, -1.0F, 1.0F, 0.0F, -1.0F, 1.0F, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F};
         FloatBuffer vertexData = BufferUtils.createFloatBuffer(vertices.length);
         vertexData.put(vertices).flip();
         this.vertexArrayId = GL30.glGenVertexArrays();
         this.vertexBufferId = GL15.glGenBuffers();
         GL30.glBindVertexArray(this.vertexArrayId);
         GL15.glBindBuffer(34962, this.vertexBufferId);
         GL15.glBufferData(34962, vertexData, 35044);
         GL20.glEnableVertexAttribArray(0);
         GL20.glVertexAttribPointer(0, 2, 5126, false, 16, 0L);
         GL20.glEnableVertexAttribArray(1);
         GL20.glVertexAttribPointer(1, 2, 5126, false, 16, 8L);
         GL15.glBindBuffer(34962, 0);
         GL30.glBindVertexArray(0);
      }
   }

   private void ensureTargets(int width, int height) {
      if (width > 0 && height > 0) {
         if (this.width != width || this.height != height || this.textures[0] == 0) {
            this.deleteTargets();
            this.width = width;
            this.height = height;

            for (int level = 0; level < 4; level++) {
               this.textures[level] = GL11.glGenTextures();
               GL11.glBindTexture(3553, this.textures[level]);
               GL11.glTexImage2D(3553, 0, 32856, mipDimension(width, level), mipDimension(height, level), 0, 6408, 5121, 0L);
               GL11.glTexParameteri(3553, 10241, 9729);
               GL11.glTexParameteri(3553, 10240, 9729);
               GL11.glTexParameteri(3553, 10242, 33071);
               GL11.glTexParameteri(3553, 10243, 33071);
               this.framebuffers[level] = GL30.glGenFramebuffers();
               GL30.glBindFramebuffer(36160, this.framebuffers[level]);
               GL30.glFramebufferTexture2D(36160, 36064, 3553, this.textures[level], 0);
               int status = GL30.glCheckFramebufferStatus(36160);
               if (status != 36053) {
                  throw new IllegalStateException("Kawase blur framebuffer is incomplete: " + status);
               }
            }

            GL11.glBindTexture(3553, 0);
            GL30.glBindFramebuffer(36160, 0);
         }
      }
   }

   private static int getMainFramebufferTextureId() {
      int n;
      if (Minecraft.getInstance().getMainRenderTarget().getColorTexture() instanceof GlTexture glTexture) {
         n = glTexture.glId();
      } else {
         n = -1;
      }

      return n;
   }

   private static int mipDimension(int dimension, int level) {
      return Math.max(1, dimension >> level);
   }

   private void renderBlurPyramid(int sourceTextureId, int width, int height, float offset, int passCount) {
      GlRenderStateSnapshot previousState = GlRenderStateSnapshot.captureRenderState();
      GL13.glActiveTexture(33984);
      GL33.glBindSampler(0, 0);
      GL11.glDisable(3042);
      GL11.glDisable(2884);
      GL11.glDisable(2929);
      GL11.glDisable(3089);
      this.renderPass(
         this.downsampleShader,
         this.downsampleImageUniform,
         this.downsampleOffsetUniform,
         this.downsampleResolutionUniform,
         sourceTextureId,
         this.framebuffers[1],
         width,
         height,
         mipDimension(width, 1),
         mipDimension(height, 1),
         offset
      );

      for (int level = 2; level <= passCount; level++) {
         this.renderPass(
            this.downsampleShader,
            this.downsampleImageUniform,
            this.downsampleOffsetUniform,
            this.downsampleResolutionUniform,
            this.textures[level - 1],
            this.framebuffers[level],
            mipDimension(width, level - 1),
            mipDimension(height, level - 1),
            mipDimension(width, level),
            mipDimension(height, level),
            offset
         );
      }

      for (int var8 = passCount; var8 >= 1; var8--) {
         this.renderPass(
            this.upsampleShader,
            this.upsampleImageUniform,
            this.upsampleOffsetUniform,
            this.upsampleResolutionUniform,
            this.textures[var8],
            this.framebuffers[var8 - 1],
            mipDimension(width, var8),
            mipDimension(height, var8),
            mipDimension(width, var8 - 1),
            mipDimension(height, var8 - 1),
            offset
         );
      }

      previousState.restore();
   }

   private void renderPass(
      GlShaderProgram shader,
      ShaderUniformWriter imageUniform,
      ShaderUniformWriter offsetUniform,
      ShaderUniformWriter resolutionUniform,
      int inputTextureId,
      int outputFramebufferId,
      int inputWidth,
      int inputHeight,
      int outputWidth,
      int outputHeight,
      float offset
   ) {
      GL30.glBindFramebuffer(36160, outputFramebufferId);
      GL11.glViewport(0, 0, outputWidth, outputHeight);
      GL11.glClearColor(0.0F, 0.0F, 0.0F, 0.0F);
      GL11.glClear(16384);
      shader.bind();
      imageUniform.set(0);
      offsetUniform.set(offset);
      resolutionUniform.set(1.0F / inputWidth, 1.0F / inputHeight);
      GL13.glActiveTexture(33984);
      GL11.glBindTexture(3553, inputTextureId);
      GL30.glBindVertexArray(this.vertexArrayId);
      GL11.glDrawArrays(5, 0, 4);
   }

   private void deleteTargets() {
      for (int index = 0; index < 4; index++) {
         if (this.framebuffers[index] != 0) {
            GL30.glDeleteFramebuffers(this.framebuffers[index]);
            this.framebuffers[index] = 0;
         }

         if (this.textures[index] != 0) {
            GL11.glDeleteTextures(this.textures[index]);
            this.textures[index] = 0;
         }
      }

      this.width = -1;
      this.height = -1;
      this.lastRenderedFrame = Integer.MIN_VALUE;
      this.refreshLimiter.reset();
   }

   @Override
   public void close() {
      this.deleteTargets();
      if (this.vertexBufferId != 0) {
         GL15.glDeleteBuffers(this.vertexBufferId);
         this.vertexBufferId = 0;
      }

      if (this.vertexArrayId != 0) {
         GL30.glDeleteVertexArrays(this.vertexArrayId);
         this.vertexArrayId = 0;
      }

      this.downsampleShader.close();
      this.upsampleShader.close();
   }
}
