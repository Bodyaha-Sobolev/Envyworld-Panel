package com.envyworld.client.gui;
import com.envyworld.graphics.misc.MsdfFontBuilder;
import com.envyworld.graphics.util.MsdfFontRenderer;
/** Official Lucide SVG contours rendered through Envyworld's signed-distance text pipeline. */
public final class UiIcons {
 private static MsdfFontRenderer font;
 private UiIcons(){}
 public static void draw(String name,float cx,float cy,float size,int color){
  String glyph=switch(name){
   case "chevron-down"->"A";case "chevron-up"->"B";case "chevron-right"->"C";case "check","circle-check"->"D";
   case "plus"->"E";case "minus"->"F";case "eye"->"G";case "eye-off"->"H";case "download"->"I";case "upload"->"J";
   case "trash"->"K";case "keyboard"->"L";case "terminal"->"M";case "circle-x"->"N";case "refresh-cw"->"O";case "message-circle"->"P";
   case "bold"->"Q";case "italic"->"R";case "underline"->"S";case "strikethrough"->"T";case "sparkles"->"U";case "eraser"->"V";case "telegram","send"->"W";default->"";
  };
  if(glyph.isEmpty())return;
  UiRenderer.submit((draw,matrix)->{if(font==null)font=new MsdfFontBuilder("/assets/envyworld/ui").process4("icons.json").process("icons.png").process2("EnvyWorld Lucide distance icons").getMsdfFontRenderer();font.process5(matrix,draw,glyph,cx-size/2,cy-size/2,size,color);});
 }
 public static void chevron(float x,float y,boolean up,int color){draw(up?"chevron-up":"chevron-down",x,y,10,color);}
 public static void plus(float x,float y,int color){draw("plus",x,y,10,color);}
 public static void minus(float x,float y,int color){draw("minus",x,y,10,color);}
 public static void check(float x,float y,int color){draw("check",x,y,10,color);}
 public static void watch(float x,float y,boolean active,int color){draw(active?"eye":"eye-off",x,y,11,color);}
 public static void format(String kind,float x,float y,int color){draw(switch(kind){case "strike"->"strikethrough";case "magic"->"sparkles";case "reset"->"eraser";default->kind;},x,y,12,color);}
 public static void close(){if(font!=null){font.close();font=null;}}
}
