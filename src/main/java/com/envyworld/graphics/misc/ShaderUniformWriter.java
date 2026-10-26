package com.envyworld.graphics.misc;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.function.IntConsumer;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL33;

public class ShaderUniformWriter {
   private int location;
   private static final FloatBuffer MATRIX_BUFFER = BufferUtils.createFloatBuffer(16);
   private final String name;

   public ShaderUniformWriter(String name) {
      this.name = name;
   }

   public void set(float x, float y, float z, float w) {
      GL33.glUniform4f(this.location, x, y, z, w);
   }

   public IntConsumer getIntConsumer() {
      return this::resolveLocation;
   }

   public void set(float value) {
      GL33.glUniform1f(this.location, value);
   }

   public void set(int value) {
      GL33.glUniform1i(this.location, value);
   }

   public void set(IntBuffer values) {
      GL33.glUniform1iv(this.location, values);
   }

   public void set(Matrix4f matrix) {
      GL33.glUniformMatrix4fv(this.location, false, matrix.get(MATRIX_BUFFER));
   }

   public void set(float x, float y) {
      GL33.glUniform2f(this.location, x, y);
   }

   public void set(FloatBuffer values) {
      GL33.glUniform1fv(this.location, values);
   }

   public void set(float x, float y, float z) {
      GL33.glUniform3f(this.location, x, y, z);
   }

   private void resolveLocation(int programId) {
      this.location = GL33.glGetUniformLocation(programId, this.name);
   }
}
