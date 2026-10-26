package com.envyworld.graphics.util;

import java.nio.ByteBuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL33;

public final class GlRenderStateSnapshot {
   private int blendEquationAlpha;
   private int scissorHeight;
   private int elementArrayBufferBinding;
   private boolean depthTestEnabled;
   private int blendEquationRgb;
   private int texture2dBinding;
   private static final int[] polygonModeBuffer = new int[2];
   private int viewportHeight;
   private int polygonMode;
   private int stencilDepthPassAction;
   private int shaderProgram;
   private int stencilWriteMask;
   private boolean blendEnabled;
   private static final ByteBuffer colorMaskBuffer = BufferUtils.createByteBuffer(4);
   private int stencilDepthFailAction;
   private boolean cullEnabled;
   private int blendDestinationAlpha;
   private boolean alphaWriteEnabled;
   private boolean redWriteEnabled;
   private boolean stencilTestEnabled;
   private int blendSourceRgb;
   private boolean scissorTestEnabled;
   private int samplerBinding;
   private int scissorWidth;
   private int stencilFailAction;
   private int viewportWidth;
   private int scissorY;
   private boolean blueWriteEnabled;
   private boolean greenWriteEnabled;
   private int framebufferBinding;
   private int arrayBufferBinding;
   private int stencilFunction;
   private int stencilReference;
   private int vertexArrayBinding;
   private int stencilValueMask;
   private int blendSourceAlpha;
   private int viewportX;
   private static final int[] integerStateBuffer = new int[4];
   private int activeTextureUnit;
   private boolean framebufferSrgbEnabled;
   private int scissorX;
   private int viewportY;
   private int blendDestinationRgb;

   private GlRenderStateSnapshot() {
   }

   public static GlRenderStateSnapshot captureRenderState() {
      GlRenderStateSnapshot glRenderStateSnapshot = new GlRenderStateSnapshot();
      glRenderStateSnapshot.framebufferBinding = GL33.glGetInteger(36006);
      glRenderStateSnapshot.activeTextureUnit = GL33.glGetInteger(34016);
      glRenderStateSnapshot.shaderProgram = GL33.glGetInteger(35725);
      glRenderStateSnapshot.texture2dBinding = GL33.glGetInteger(32873);
      glRenderStateSnapshot.samplerBinding = GL33.glGetInteger(35097);
      glRenderStateSnapshot.arrayBufferBinding = GL33.glGetInteger(34964);
      glRenderStateSnapshot.elementArrayBufferBinding = GL33.glGetInteger(34965);
      glRenderStateSnapshot.vertexArrayBinding = GL33.glGetInteger(34229);
      GL33.glGetIntegerv(2880, polygonModeBuffer);
      glRenderStateSnapshot.polygonMode = polygonModeBuffer[0];
      GL33.glGetIntegerv(2978, integerStateBuffer);
      glRenderStateSnapshot.viewportX = integerStateBuffer[0];
      glRenderStateSnapshot.viewportY = integerStateBuffer[1];
      glRenderStateSnapshot.viewportWidth = integerStateBuffer[2];
      glRenderStateSnapshot.viewportHeight = integerStateBuffer[3];
      GL33.glGetIntegerv(3088, integerStateBuffer);
      glRenderStateSnapshot.scissorX = integerStateBuffer[0];
      glRenderStateSnapshot.scissorY = integerStateBuffer[1];
      glRenderStateSnapshot.scissorWidth = integerStateBuffer[2];
      glRenderStateSnapshot.scissorHeight = integerStateBuffer[3];
      glRenderStateSnapshot.blendSourceRgb = GL33.glGetInteger(32969);
      glRenderStateSnapshot.blendDestinationRgb = GL33.glGetInteger(32968);
      glRenderStateSnapshot.blendSourceAlpha = GL33.glGetInteger(32971);
      glRenderStateSnapshot.blendDestinationAlpha = GL33.glGetInteger(32970);
      glRenderStateSnapshot.blendEquationRgb = GL33.glGetInteger(32777);
      glRenderStateSnapshot.blendEquationAlpha = GL33.glGetInteger(34877);
      glRenderStateSnapshot.stencilFunction = GL33.glGetInteger(2962);
      glRenderStateSnapshot.stencilReference = GL33.glGetInteger(2967);
      glRenderStateSnapshot.stencilValueMask = GL33.glGetInteger(2963);
      glRenderStateSnapshot.stencilWriteMask = GL33.glGetInteger(2968);
      glRenderStateSnapshot.stencilFailAction = GL33.glGetInteger(2964);
      glRenderStateSnapshot.stencilDepthFailAction = GL33.glGetInteger(2965);
      glRenderStateSnapshot.stencilDepthPassAction = GL33.glGetInteger(2966);
      glRenderStateSnapshot.blendEnabled = GL33.glIsEnabled(3042);
      glRenderStateSnapshot.cullEnabled = GL33.glIsEnabled(2884);
      glRenderStateSnapshot.depthTestEnabled = GL33.glIsEnabled(2929);
      glRenderStateSnapshot.stencilTestEnabled = GL33.glIsEnabled(2960);
      glRenderStateSnapshot.scissorTestEnabled = GL33.glIsEnabled(3089);
      glRenderStateSnapshot.framebufferSrgbEnabled = GL33.glIsEnabled(36765);
      colorMaskBuffer.clear();
      GL33.glGetBooleanv(3107, colorMaskBuffer);
      glRenderStateSnapshot.redWriteEnabled = colorMaskBuffer.get(0) != 0;
      glRenderStateSnapshot.greenWriteEnabled = colorMaskBuffer.get(1) != 0;
      glRenderStateSnapshot.blueWriteEnabled = colorMaskBuffer.get(2) != 0;
      glRenderStateSnapshot.alphaWriteEnabled = colorMaskBuffer.get(3) != 0;
      return glRenderStateSnapshot;
   }

   public void restore() {
      GL33.glBindFramebuffer(36160, this.framebufferBinding);
      GL33.glUseProgram(this.shaderProgram);
      GL33.glBindSampler(0, this.samplerBinding);
      GL33.glActiveTexture(this.activeTextureUnit);
      GL33.glBindTexture(3553, this.texture2dBinding);
      GL33.glBindVertexArray(this.vertexArrayBinding);
      GL33.glBindBuffer(34962, this.arrayBufferBinding);
      GL33.glBindBuffer(34963, this.elementArrayBufferBinding);
      GL33.glBlendEquationSeparate(this.blendEquationRgb, this.blendEquationAlpha);
      GL33.glBlendFuncSeparate(this.blendSourceRgb, this.blendDestinationRgb, this.blendSourceAlpha, this.blendDestinationAlpha);
      GL33.glStencilMask(this.stencilWriteMask);
      GL33.glStencilFunc(this.stencilFunction, this.stencilReference, this.stencilValueMask);
      GL33.glStencilOp(this.stencilFailAction, this.stencilDepthFailAction, this.stencilDepthPassAction);
      GL33.glColorMask(this.redWriteEnabled, this.greenWriteEnabled, this.blueWriteEnabled, this.alphaWriteEnabled);
      this.setCapability(3042, this.blendEnabled);
      this.setCapability(2884, this.cullEnabled);
      this.setCapability(2929, this.depthTestEnabled);
      this.setCapability(2960, this.stencilTestEnabled);
      this.setCapability(3089, this.scissorTestEnabled);
      this.setCapability(36765, this.framebufferSrgbEnabled);
      GL33.glPolygonMode(1032, this.polygonMode);
      GL33.glViewport(this.viewportX, this.viewportY, this.viewportWidth, this.viewportHeight);
      GL33.glScissor(this.scissorX, this.scissorY, this.scissorWidth, this.scissorHeight);
   }

   private void setCapability(int n, boolean bl) {
      if (bl) {
         GL33.glEnable(n);
      } else {
         GL33.glDisable(n);
      }
   }
}
