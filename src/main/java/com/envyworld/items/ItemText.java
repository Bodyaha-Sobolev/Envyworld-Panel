package com.envyworld.items;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

/** Legacy color codes and &#RRGGBB without modifying the stored original text. */
public final class ItemText {
    private ItemText() {}
    public static Component font(Component text,String id) { return text.copy().withStyle(style->style.withFont(new net.minecraft.network.chat.FontDescription.Resource(net.minecraft.resources.Identifier.parse(id)))); }
    public static String styleCode(Style style) {
        StringBuilder b=new StringBuilder("&r");if(style.getColor()!=null)b.append(String.format("&#%06X",style.getColor().getValue()));
        if(style.isBold())b.append("&l");if(style.isItalic())b.append("&o");if(style.isUnderlined())b.append("&n");if(style.isStrikethrough())b.append("&m");if(style.isObfuscated())b.append("&k");return b.toString();
    }
    public static String format(String text,String code) {
        Component component=parse(text,"WHITE");var flag=ChatFormatting.getByCode(code.charAt(1));
        boolean[] all={true};component.visit((style,value)->{if(!value.isEmpty())all[0]&=switch(flag){case BOLD->style.isBold();case ITALIC->style.isItalic();case UNDERLINE->style.isUnderlined();case STRIKETHROUGH->style.isStrikethrough();case OBFUSCATED->style.isObfuscated();default->false;};return java.util.Optional.empty();},Style.EMPTY);
        StringBuilder result=new StringBuilder();component.visit((style,value)->{
            Style changed=switch(flag){case BOLD->style.withBold(!all[0]);case ITALIC->style.withItalic(!all[0]);case UNDERLINE->style.withUnderlined(!all[0]);case STRIKETHROUGH->style.withStrikethrough(!all[0]);case OBFUSCATED->style.withObfuscated(!all[0]);default->Style.EMPTY.withItalic(false);};
            if(!value.isEmpty())result.append(styleCode(changed)).append(value);return java.util.Optional.empty();
        },Style.EMPTY);return result.toString();
    }
    public static String recolor(String text,String color) {
        int rgb=Integer.parseInt(color.replace("#",""),16);StringBuilder result=new StringBuilder();
        parse(text,"WHITE").visit((style,value)->{if(!value.isEmpty())result.append(styleCode(style.withColor(rgb))).append(value);return java.util.Optional.empty();},Style.EMPTY);return result.toString();
    }
    public static String codes(Component text) {
        StringBuilder result = new StringBuilder();
        text.visit((style, value) -> {
            result.append("&r"); if (style.getColor() != null) result.append(String.format("&#%06X", style.getColor().getValue()));
            if (style.isBold()) result.append("&l"); if (style.isItalic()) result.append("&o"); if (style.isUnderlined()) result.append("&n");
            if (style.isStrikethrough()) result.append("&m"); if (style.isObfuscated()) result.append("&k"); result.append(value);
            return java.util.Optional.empty();
        }, Style.EMPTY); return result.toString();
    }
    public static String gradient(String text, String start, String end) {
        int a = Integer.parseInt(start.replace("#",""),16), b = Integer.parseInt(end.replace("#",""),16);
        java.util.List<Integer> characters=new java.util.ArrayList<>(); java.util.List<Style> styles=new java.util.ArrayList<>();
        parse(text,"WHITE").visit((style,value)->{value.codePoints().forEach(cp->{characters.add(cp);styles.add(style);});return java.util.Optional.empty();},Style.EMPTY);
        int[] points = characters.stream().mapToInt(Integer::intValue).toArray(); StringBuilder result = new StringBuilder();
        for(int i=0;i<points.length;i++) {
            double t = points.length<2?0:(double)i/(points.length-1); int color=0;
            for(int shift=0;shift<=16;shift+=8) color |= (int)Math.round((a>>shift&255)*(1-t)+(b>>shift&255)*t)<<shift;
            result.append(String.format("&#%06X",color));Style style=styles.get(i);
            if(style.isBold())result.append("&l");if(style.isItalic())result.append("&o");if(style.isUnderlined())result.append("&n");if(style.isStrikethrough())result.append("&m");if(style.isObfuscated())result.append("&k");
            result.appendCodePoint(points[i]);
        }
        return result.toString();
    }
    public static Component parse(String input, String color) {
        Style base = Style.EMPTY.withItalic(false);
        try {
            if (color != null && color.matches("#[0-9a-fA-F]{6}")) base = base.withColor(Integer.parseInt(color.substring(1), 16));
            else if (color != null) base = base.withColor(ChatFormatting.valueOf(color.toUpperCase(java.util.Locale.ROOT)));
        } catch (IllegalArgumentException ignored) {}
        Style style = base;
        MutableComponent result = Component.empty();
        StringBuilder chunk = new StringBuilder();
        String value = input == null ? "" : input;
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            if ((character == '&' || character == '§') && i + 1 < value.length()) {
                Style next = null;
                int skip = 1;
                if (i + 7 < value.length() && value.charAt(i + 1) == '#' && value.substring(i + 2, i + 8).matches("[0-9a-fA-F]{6}")) {
                    next = style.withColor(Integer.parseInt(value.substring(i + 2, i + 8), 16)); skip = 7;
                } else {
                    ChatFormatting format = ChatFormatting.getByCode(value.charAt(i + 1));
                    if (format == ChatFormatting.RESET) next = base;
                    else if (format != null) next = format.isColor() ? base.withColor(format) : style.applyFormat(format);
                }
                if (next != null) {
                    if (!chunk.isEmpty()) { result.append(Component.literal(chunk.toString()).withStyle(style)); chunk.setLength(0); }
                    style = next; i += skip; continue;
                }
            }
            chunk.append(character);
        }
        if (!chunk.isEmpty()) result.append(Component.literal(chunk.toString()).withStyle(style));
        return result;
    }
}
