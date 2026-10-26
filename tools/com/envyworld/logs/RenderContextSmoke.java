package com.envyworld.logs;
final class RenderContextSmoke {
 static void run()throws Exception{
  var method=com.envyworld.client.gui.UiRenderer.class.getDeclaredMethod("render",java.util.List.class,boolean.class);method.setAccessible(true);
  var batch=new java.util.ArrayList<java.util.function.BiConsumer<com.envyworld.graphics.util.GuiDrawApi,org.joml.Matrix4f>>();
  batch.add((draw,matrix)->{throw new AssertionError("Rendering without a GL context");});
  if(!Boolean.FALSE.equals(method.invoke(null,batch,false))||!batch.isEmpty())throw new AssertionError("Missing GL context was not handled");
  System.out.println("PASS: missing OpenGL context skips custom drawing without native GL calls");
 }
}
