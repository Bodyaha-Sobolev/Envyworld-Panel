package com.envyworld.graphics.render;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import com.envyworld.graphics.misc.ClasspathResource;
import com.envyworld.graphics.misc.ResourceResolver;
import com.envyworld.graphics.misc.ShaderUniformWriter;
import com.envyworld.graphics.misc.TextureHandle;
import com.envyworld.graphics.util.GlRenderStateSnapshot;

public final class GaussianBlurRenderer implements AutoCloseable {
   private static final int MAX_RADIUS = 32;
   private static final float MIN_SIGMA = 0.75F;
   private final GlShaderProgram shaderProgram;
   private final Map<Integer, GaussianBlurTarget> targetsBySourceTexture = new HashMap<>();
   private final ShaderUniformWriter imageSamplerUniform;
   private final ShaderUniformWriter directionUniform;
   private final ShaderUniformWriter texelSizeUniform;
   private final ShaderUniformWriter sigmaStartUniform;
   private final ShaderUniformWriter sigmaEndUniform;
   private final ShaderUniformWriter sigmaRangeUniform;
   private final ShaderUniformWriter radiusUniform;
   private int vertexArrayId;
   private int vertexBufferId;

   public GaussianBlurRenderer() {
      ResourceResolver resources = new ResourceResolver("/assets/envyworld/renderer/shaders/", ClasspathResource::new);
      this.shaderProgram = new GlShaderProgram(resources.resolve("gl/gauss_blur.frag"), resources.resolve("gl/fullscreen_quad.vert"));
      this.imageSamplerUniform = this.shaderProgram.registerUniform("image");
      this.directionUniform = this.shaderProgram.registerUniform("direction");
      this.texelSizeUniform = this.shaderProgram.registerUniform("texelSize");
      this.sigmaStartUniform = this.shaderProgram.registerUniform("sigmaStart");
      this.sigmaEndUniform = this.shaderProgram.registerUniform("sigmaEnd");
      this.sigmaRangeUniform = this.shaderProgram.registerUniform("sigmaRange");
      this.radiusUniform = this.shaderProgram.registerUniform("radius");
   }

   public void invalidateSourceTexture(int sourceTextureId) {
      GaussianBlurTarget target = this.targetsBySourceTexture.remove(sourceTextureId);
      if (target != null) {
         target.close();
      }
   }

   public TextureHandle blurRange(TextureHandle source, float sigmaStart, float sigmaEnd, float rangeStart, float rangeEnd, int frame) {
      float maximumSigma = Math.max(sigmaStart, sigmaEnd);
      if (source != null && !(maximumSigma < 0.75F)) {
         int radius = clampRadius((int)Math.ceil(maximumSigma * 3.0F));
         int sourceTextureId = source.getTextureId();
         GaussianBlurTarget target = this.targetsBySourceTexture
            .computeIfAbsent(sourceTextureId, ignored -> new GaussianBlurTarget(source.getWidth(), source.getHeight()));
         boolean sizeChanged = target.width != source.getWidth() || target.height != source.getHeight();
         int sourceGeneration = source.getGeneration();
         boolean sourceChanged = sourceGeneration != target.sourceGeneration;
         boolean parametersChanged = Math.abs(target.sigmaStart - sigmaStart) > 0.01F || Math.abs(target.sigmaEnd - sigmaEnd) > 0.01F;
         if (sizeChanged) {
            target.resize(source.getWidth(), source.getHeight());
         } else if (!sourceChanged) {
            if (target.lastRenderedFrame == frame && !parametersChanged) {
               return target;
            }

            if (target.lastRenderedFrame != frame && !target.refreshLimiter.canRun(System.nanoTime())) {
               return target;
            }
         }

         this.ensureFullscreenQuad();
         this.renderBlur(source, target, sigmaStart, sigmaEnd, rangeStart, rangeEnd, radius);
         target.lastRenderedFrame = frame;
         target.sourceGeneration = sourceGeneration;
         target.sigmaStart = sigmaStart;
         target.sigmaEnd = sigmaEnd;
         target.refreshLimiter.markRun(System.nanoTime());
         return target;
      } else {
         return source;
      }
   }

   public TextureHandle blur(TextureHandle source, float sigma, int frame) {
      return this.blurRange(source, sigma, sigma, 0.0F, 0.0F, frame);
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

   private void renderBlur(TextureHandle source, GaussianBlurTarget target, float sigmaStart, float sigmaEnd, float rangeStart, float rangeEnd, int radius) {
      GlRenderStateSnapshot previousState = GlRenderStateSnapshot.captureRenderState();
      GL11.glDisable(3042);
      GL11.glDisable(2884);
      GL11.glDisable(2929);
      GL11.glDisable(3089);
      GL11.glViewport(0, 0, target.width, target.height);
      this.shaderProgram.bind();
      this.imageSamplerUniform.set(0);
      this.sigmaStartUniform.set(sigmaStart);
      this.sigmaEndUniform.set(sigmaEnd);
      this.sigmaRangeUniform.set(rangeStart, rangeEnd);
      this.radiusUniform.set(radius);
      this.texelSizeUniform.set(1.0F / target.width, 1.0F / target.height);
      GL30.glBindFramebuffer(36160, target.framebuffers[0]);
      this.directionUniform.set(1.0F, 0.0F);
      GL13.glActiveTexture(33984);
      GL11.glBindTexture(3553, source.getTextureId());
      GL30.glBindVertexArray(this.vertexArrayId);
      GL11.glDrawArrays(5, 0, 4);
      GL30.glBindFramebuffer(36160, target.framebuffers[1]);
      this.directionUniform.set(0.0F, 1.0F);
      GL11.glBindTexture(3553, target.textures[0]);
      GL11.glDrawArrays(5, 0, 4);
      previousState.restore();
   }

   private static int clampRadius(int radius) {
      return Math.min(Math.max(radius, 1), 32);
   }

   @Override
   public void close() {
      for (GaussianBlurTarget target : this.targetsBySourceTexture.values()) {
         target.close();
      }

      this.targetsBySourceTexture.clear();
      if (this.vertexBufferId != 0) {
         GL15.glDeleteBuffers(this.vertexBufferId);
         this.vertexBufferId = 0;
      }

      if (this.vertexArrayId != 0) {
         GL30.glDeleteVertexArrays(this.vertexArrayId);
         this.vertexArrayId = 0;
      }

      this.shaderProgram.close();
   }
}
