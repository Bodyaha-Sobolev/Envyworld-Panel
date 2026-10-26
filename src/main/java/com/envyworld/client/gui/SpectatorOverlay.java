package com.envyworld.client.gui;

import com.envyworld.EnvyWorld;
import com.envyworld.client.WorldProjection;
import net.minecraft.network.chat.Component;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.*;
import org.joml.Matrix4f;
import org.joml.Vector4f;


/** Spectator-only overlay. A shared projection and tick-cached labels keep render work bounded. */
public final class SpectatorOverlay {
    private static final EquipmentSlot[] SLOTS={EquipmentSlot.MAINHAND,EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET};
    private record Gear(ItemStack stack,boolean hand) {}
    private record Penalty(com.envyworld.logs.PunishLogClient.Row row,List<String> details,String deadline,float width) {}
    private record Label(Component name,String hands,String hp,float width,float nameWidth,float hpWidth,float handsWidth,ItemStack offhand,List<Gear> items,List<Penalty> punishments,List<ProfileChecks.Tag> checks) {}
    private static final Map<UUID,Label> labels=new HashMap<>();
    private static final List<Player> players=new ArrayList<>();
    private static final Matrix4f projection=new Matrix4f();
    private static final Vector4f clip=new Vector4f();
    private static final float[] screenBox=new float[4];
    private static long nextLabels;
    private static Object level;
    private SpectatorOverlay() {}
    public static void reset(){players.clear();labels.clear();level=null;nextLabels=0;WorldProjection.reset();NameplatePunishments.reset();}
    public static boolean active(){var c=Minecraft.getInstance();return EnvyWorld.config().client.spectatorEsp&&c.player!=null&&c.level!=null&&c.gameMode!=null&&c.gameMode.getPlayerMode()==GameType.SPECTATOR;}
    public static Player target(){
        if(!active())return null;var c=Minecraft.getInstance();Vec3 eye=c.player.getEyePosition(),end=eye.add(c.player.getLookAngle().scale(128));
        Player best=null;double distance=128*128;
        for(var p:c.level.players()){if(p==c.player||com.envyworld.client.PlayerIdentityFilter.npc(p,c))continue;var point=p.getBoundingBox().inflate(.15).clip(eye,end);if(point.isPresent()&&eye.distanceToSqr(point.get())<distance){best=p;distance=eye.distanceToSqr(point.get());}}
        return best;
    }
    private static void collect(Minecraft c){
        var s=EnvyWorld.config().client;long now=System.currentTimeMillis();
        if(level!=c.level){level=c.level;players.clear();labels.clear();nextLabels=0;NameplatePunishments.reset();}
        if(now<nextLabels)return;nextLabels=now+150;
        players.clear();double range=Math.max(16,Math.min(512,s.espDistance));
        for(var p:c.level.players())if(p!=c.player&&!p.isRemoved()&&!com.envyworld.client.PlayerIdentityFilter.npc(p,c)&&p.distanceToSqr(c.player)<=range*range)players.add(p);
        players.sort(Comparator.comparingDouble(p->p.distanceToSqr(c.player)));
        if(players.size()>48)players.subList(48,players.size()).clear();
        if(s.espPunishments)NameplatePunishments.request(players.stream().map(p->p.getGameProfile().name()).toList());
        labels.clear();
        for(var p:players){
            String hp=s.espHealth?Math.round(p.getHealth())+"HP":"";
            var items=new ArrayList<Gear>(5);var offhand=s.espEquipment?p.getOffhandItem():ItemStack.EMPTY;
            if(s.espEquipment)for(var slot:SLOTS){var stack=p.getItemBySlot(slot);if(!stack.isEmpty())items.add(new Gear(stack,slot==EquipmentSlot.MAINHAND));}
            String hands="";
            if(s.espEquipment){if(!p.getOffhandItem().isEmpty())hands="Л: "+HeldItemNames.label(p.getOffhandItem());if(!p.getMainHandItem().isEmpty())hands+=(hands.isEmpty()?"":"  ")+"П: "+HeldItemNames.label(p.getMainHandItem());}
            Component name=Component.literal(p.getGameProfile().name());float nw=TagFont.measure(name,11),hw=hp.isEmpty()?0:TagFont.measure(hp,11)+6;
            float width=8+(offhand.isEmpty()?0:16)+nw+(hw==0?0:hw+6)+(items.isEmpty()?0:items.size()*16+4);
            labels.put(p.getUUID(),new Label(name,hands,hp,width,nw,hw,TagFont.measure(hands,9)+8,offhand,items,s.espPunishments?NameplatePunishments.active(p.getGameProfile().name()).stream().map(SpectatorOverlay::penalty).sorted(Comparator.comparingDouble(Penalty::width)).toList():List.of(),s.espChecks?ProfileChecks.tags(p.getGameProfile().name()):List.of()));
        }
    }
    private static Penalty penalty(com.envyworld.logs.PunishLogClient.Row row){
        var lines=new ArrayList<String>();String text=row.reason()+"  |  "+com.envyworld.logs.ServerModes.label(row.server());
        StringBuilder line=new StringBuilder();
        for(String word:text.replaceAll("\\s+"," ").split(" ")){
            if(line.length()>0&&TagFont.measure(line+word,9)>320){lines.add(line.toString().strip());line.setLength(0);}
            for(int offset=0;offset<word.length();){
                int end=word.offsetByCodePoints(offset,1);String part=word.substring(offset,end);
                if(TagFont.measure(line+part,9)>320&&line.length()>0){lines.add(line.toString().strip());line.setLength(0);}
                line.append(part);offset=end;
            }
            line.append(' ');
        }
        if(!line.isEmpty())lines.add(line.toString().strip());
        lines.add("Выдал: "+row.punisher()+"  |  "+row.date());
        String deadline=com.envyworld.logs.PunishmentClock.deadline(row);
        float width=TagFont.measure(deadline,9);for(String value:lines)width=Math.max(width,TagFont.measure(value,9));
        return new Penalty(row,List.copyOf(lines),deadline,width);
    }
    private static void pill(float x,float y,float w,float h,float radius,boolean blur){
        if(blur)UiRenderer.submit((draw,matrix)->draw.drawBlurredRoundedRectangle(matrix,x,y,w,h,radius));
        UiGraphics.rounded(x,y,w,h,radius,0xBF0D121A);
    }
    private static void item(GuiGraphics g,ItemStack stack,float x,float y,float size){g.pose().pushMatrix();g.pose().translate(x,y);g.pose().scale(size/16,size/16);g.renderItem(stack,0,0);g.pose().popMatrix();}
    public static void draw(GuiGraphics g){
        if(!active()){reset();return;}
        var c=Minecraft.getInstance();if(c.screen!=null||g==null)return;
        collect(c);var s=EnvyWorld.config().client;int width=c.getWindow().getGuiScaledWidth(),height=c.getWindow().getGuiScaledHeight();
        if(!WorldProjection.copy(projection))return;
        Vec3 eye=WorldProjection.eye();float partial=WorldProjection.partial();
        float px=1f/(float)c.getWindow().getGuiScale();
        Vec3 right=WorldProjection.right();

        for(var p:players){
            if(p.isRemoved())continue;var label=labels.get(p.getUUID());if(label==null)continue;
            Vec3 position=p.getPosition(partial);AABB bounds=p.getBoundingBox().move(position.subtract(p.position()));
            if(!com.envyworld.client.PlayerModelBounds.copy(p.getId(),2*px,screenBox)
                &&!com.envyworld.client.PlayerBoxProjection.project(projection,eye,right,bounds,p.getBbWidth()*.5,width,height,clip,screenBox))continue;
            float minX=screenBox[0],minY=screenBox[1],maxX=screenBox[2],maxY=screenBox[3];
            float w=maxX-minX,h=maxY-minY;if(w<1||h<2||w>width*2||h>height*2)continue;
            int color=p.isInvisible()?0xFFFFB66E:0xFFE0D8EF;
            if(s.espBoxes){
                float pixel=1f/(float)c.getWindow().getGuiScale();
                float bx=(float)Math.floor(minX/pixel)*pixel,by=(float)Math.floor(minY/pixel)*pixel,bw=(float)Math.ceil(maxX/pixel)*pixel-bx,bh=(float)Math.ceil(maxY/pixel)*pixel-by;
                UiRenderer.submit((draw,matrix)->{
                    draw.drawRoundedOutline(matrix,bx-pixel,by-pixel,bw+2*pixel,bh+2*pixel,0,1,0xD0080B10);
                    draw.drawRoundedOutline(matrix,bx,by,bw,bh,0,1,color);
                    if(bw>4*pixel&&bh>4*pixel)draw.drawRoundedOutline(matrix,bx+pixel,by+pixel,bw-2*pixel,bh-2*pixel,0,1,0xC0080B10);
                });
            }
            float hp=Math.max(0,Math.min(1,p.getHealth()/Math.max(1,p.getMaxHealth())));int health=hp<.3?0xFFFF7C83:hp<.65?0xFFFFB66E:0xFF7DE0B4;
            if(s.espHealth){float pixel=1f/(float)c.getWindow().getGuiScale();UiGraphics.rounded(minX-5*pixel,minY-pixel,3*pixel,h+2*pixel,0,0xB0080B10);UiGraphics.rounded(minX-4*pixel,minY+h*(1-hp),pixel,h*hp,0,health);}
            if(!s.espNames)continue;
            // Keep nameplates at a consistent physical size across GUI scales.
            float anchorX=(minX+maxX)*.5f,anchorY=minY;
            float tw=label.width()*px,rowHeight=20*px,handHeight=label.hands().isEmpty()?0:14*px;
            var placed=NameplateLayout.place(anchorX,anchorY,tw,rowHeight+handHeight,2*px);
            float x=placed[0],y=placed[1];
            float checksTop=y-3*px;
            double maxCheckWidth=label.punishments().stream().mapToDouble(tag->Math.max(TagFont.measure(tag.row().typeLabel()+"  |  "+com.envyworld.logs.PunishmentClock.status(tag.row()).label(),10),tag.width())).max().orElse(200);
            for(var row:ProfileChecks.tagRows(label.checks(),maxCheckWidth,text->TagFont.measure(text,9))){
                float content=0;for(var check:row)content+=TagFont.measure(check.text(),9);
                content+=(row.size()-1)*TagFont.measure(" · ",9);
                float scale=(float)Math.min(1,maxCheckWidth/Math.max(1,content));
                float cw=(content*scale+12)*px,cy=checksTop-16*px;
                pill(anchorX-cw/2,cy,cw,16*px,5,s.espBlur);
                float cx=anchorX-cw/2+6*px;
                for(int i=0;i<row.size();i++){
                    var check=row.get(i);float size=9*px*scale;
                    TagFont.draw(g,check.text(),cx,TagFont.top(cy,16*px,size),size,check.color());cx+=TagFont.measure(check.text(),9)*px*scale;
                    if(i+1<row.size()){TagFont.draw(g," · ",cx,TagFont.top(cy,16*px,size),size,0xFF929BAA);cx+=TagFont.measure(" · ",9)*px*scale;}
                }
                checksTop=cy-3*px;
            }
            if(s.espPunishments){
                float top=checksTop;
                for(var tag:label.punishments()){
                    var punishment=tag.row();
                    if(!com.envyworld.logs.PunishmentClock.active(punishment))continue;
                    var clock=com.envyworld.logs.PunishmentClock.status(punishment);
                    String title=punishment.typeLabel()+"  |  "+clock.label();
                    float pw=(Math.max(TagFont.measure(title,10),tag.width())+12)*px;
                    float ph=(29+tag.details().size()*11)*px,py=top-ph,xx=anchorX-pw/2;
                    pill(xx,py,pw,ph,6,s.espBlur);
                    TagFont.draw(g,title,xx+6*px,py+4*px,10*px,clock.color());
                    for(int i=0;i<tag.details().size();i++)TagFont.draw(g,tag.details().get(i),xx+6*px,py+(17+i*11)*px,9*px,i==tag.details().size()-1?0xFFAEBFD3:0xFFF1F1F1);
                    TagFont.draw(g,tag.deadline(),xx+6*px,py+(17+tag.details().size()*11)*px,9*px,clock.color());
                    top=py-3*px;
                }
            }
            pill(x,y,tw,rowHeight,6,s.espBlur);
            float cursor=x+4*px;
            if(!label.offhand().isEmpty()){item(g,label.offhand(),cursor,y+px,16*px);cursor+=16*px;}
            TagFont.component(g,label.name(),cursor,TagFont.top(y,rowHeight,11*px),11*px,0xFFF4F4F4);cursor+=label.nameWidth()*px+6*px;
            if(!label.hp().isEmpty()){
                UiGraphics.rounded(cursor,y+3*px,label.hpWidth()*px,14*px,3,0x70427A36);
                TagFont.draw(g,label.hp(),cursor+3*px,TagFont.top(y+3*px,14*px,11*px),11*px,0xFF78DC64);cursor+=(label.hpWidth()+4)*px;
            }
            for(var gear:label.items()){
                item(g,gear.stack(),cursor,y+(gear.hand()?1:2)*px,16*px);
                cursor+=16*px;
            }
            if(!label.hands().isEmpty()){
                float hw=label.handsWidth()*px,hx=anchorX-hw/2,hy=y+rowHeight+px;
                pill(hx,hy,hw,12*px,4,s.espBlur);TagFont.draw(g,label.hands(),hx+4*px,TagFont.top(hy,12*px,9*px),9*px,0xFFF1F1F1);
            }
        }
    }
}
