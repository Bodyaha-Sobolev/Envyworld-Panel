package com.envyworld.items;

import com.envyworld.bind.Binding;
import com.envyworld.config.RulesConfig;
import com.google.gson.*;
import java.util.*;

/** Data-only interchange. Import never executes commands or installs keyboard bindings. */
public final class PresetExchange {
    private static final Gson JSON=new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
    public static final class Pack {
        public String format="envyworld-presets";
        public int version=1;
        public List<CustomItemPreset> items=new ArrayList<>();
        public List<Binding> macros=new ArrayList<>();
    }
    private PresetExchange(){}
    public static Pack selection(RulesConfig config,CustomItemPreset item,Binding macro){
        var pack=new Pack();
        if(item!=null){pack.items.add(item.copy());for(var b:config.binds)if(item.id.equals(b.customItem))pack.macros.add(copy(b));}
        if(macro!=null){pack.macros.add(copy(macro));if(macro.customItem!=null&&!macro.customItem.isBlank())for(var p:config.customItems)if(p.id.equals(macro.customItem))pack.items.add(p.copy());}
        return pack;
    }
    public static Pack all(RulesConfig c){var p=new Pack();for(var i:c.customItems)p.items.add(i.copy());for(var b:c.binds)if(b.trigger!=com.envyworld.bind.BindTrigger.PROJECTILE_HIT&&b.trigger!=com.envyworld.bind.BindTrigger.ATTACK_BLOCK)p.macros.add(copy(b));return p;}
    private static Binding copy(Binding b){var c=JSON.fromJson(JSON.toJson(b),Binding.class);c.keyCode=0;c.keyModifiers=0;return c;}
    public static String encode(Pack p){return JSON.toJson(p);}
    public static Pack decode(String source){
        if(source==null||source.length()>8_000_000)throw new IllegalArgumentException("Файл слишком большой: максимум 8 МБ");
        String data=source.trim();if(data.startsWith("```")){int first=data.indexOf('\n'),last=data.lastIndexOf("```");if(first>=0&&last>first)data=data.substring(first+1,last).trim();}
        try{
            var object=JsonParser.parseString(data).getAsJsonObject();
            if(!object.has("format")||!object.get("format").getAsString().equals("envyworld-presets")||!object.has("version")||object.get("version").getAsInt()!=1)throw new IllegalArgumentException("Нужен пакет EnvyWorld версии 1");
            var pack=JSON.fromJson(object,Pack.class);
            if(pack.items==null||pack.macros==null||pack.items.size()>1000||pack.macros.size()>1000)throw new IllegalArgumentException("Максимум 1000 предметов и 1000 макросов");
            Set<String> ids=new HashSet<>();
            for(var item:pack.items){if(item==null)throw new IllegalArgumentException("Пустой предмет");normalizeStrings(item,new CustomItemPreset());if(item.id.isBlank()||!ids.add(item.id))throw new IllegalArgumentException("Повторяющийся или пустой ID предмета");if(item.attributes==null)item.attributes=new ArrayList<>();if(item.attributes.size()>128)throw new IllegalArgumentException("Слишком много атрибутов");for(var a:item.attributes){if(a==null||!Double.isFinite(a.amount))throw new IllegalArgumentException("Некорректный атрибут");normalizeStrings(a,new CustomItemPreset.AttributeSpec());}}
            ids.clear();for(var b:pack.macros){if(b==null)throw new IllegalArgumentException("Пустой макрос");normalizeStrings(b,new Binding());if(b.id.isBlank()||!ids.add(b.id)||b.trigger==null||b.command.isBlank())throw new IllegalArgumentException("Проверьте ID, триггер и команду макроса");if(b.commandDelaysMs==null)b.commandDelaysMs=new ArrayList<>();if(b.commandDelaysMs.size()>16)throw new IllegalArgumentException("Максимум 16 задержек команд");for(int n=0;n<b.commandDelaysMs.size();n++){Integer delay=b.commandDelaysMs.get(n);b.commandDelaysMs.set(n,delay==null?0:Math.max(0,Math.min(3600000,delay)));}b.keyCode=0;b.keyModifiers=0;b.radius=Math.max(0,Math.min(128,b.radius));b.lookDistance=Double.isFinite(b.lookDistance)?Math.max(1,Math.min(128,b.lookDistance)):64;b.cooldownTicks=Math.max(0,Math.min(72000,b.cooldownTicks));b.repeatDelayMs=Math.max(50,Math.min(3600000,b.repeatDelayMs));b.repeatCount=Math.max(0,Math.min(10000,b.repeatCount));if(!List.of("PRESS","HOLD","TOGGLE").contains(b.keyMode))b.keyMode="PRESS";}
            if(pack.items.isEmpty()&&pack.macros.isEmpty())throw new IllegalArgumentException("Пакет пуст");return pack;
        }catch(IllegalArgumentException e){throw e;}catch(Exception e){throw new IllegalArgumentException("Не удалось прочитать JSON пакета EnvyWorld",e);}
    }
    private static void normalizeStrings(Object value,Object defaults)throws IllegalAccessException{
        for(var f:value.getClass().getFields())if(f.getType()==String.class){String s=(String)f.get(value);if(s==null){s=(String)f.get(defaults);f.set(value,s==null?"":s);}else if(s.length()>262144)throw new IllegalArgumentException("Слишком длинное поле: "+f.getName());}
    }
    public static String merge(RulesConfig config,Pack source){
        // Parse and build everything before modifying the destination collection.
        Pack p=decode(encode(source));
        for(var item:p.items)CustomItems.build(item,1);
        Map<String,String> remap=new HashMap<>();var takenItems=new HashSet<String>();var knownItems=new HashMap<String,String>();
        for(var i:config.customItems){takenItems.add(i.id);knownItems.put(fingerprint(i),i.id);}
        var addItems=new ArrayList<CustomItemPreset>();var addMacros=new ArrayList<Binding>();
        for(var i:p.items){String old=i.id,fp=fingerprint(i);if(knownItems.containsKey(fp)){remap.put(old,knownItems.get(fp));continue;}while(!takenItems.add(i.id))i.id="import_"+UUID.randomUUID();remap.put(old,i.id);knownItems.put(fp,i.id);addItems.add(i);}
        var takenMacros=new HashSet<String>();var knownMacros=new HashSet<String>();for(var b:config.binds){takenMacros.add(b.id);knownMacros.add(fingerprint(b));}
        for(var b:p.macros){if(b.trigger==com.envyworld.bind.BindTrigger.ATTACK_BLOCK)throw new IllegalArgumentException("Режим разрушения удалён");if(b.trigger==com.envyworld.bind.BindTrigger.PROJECTILE_HIT)throw new IllegalArgumentException("Режим попадания удалён");if(!b.customItem.isBlank()){b.customItem=remap.getOrDefault(b.customItem,b.customItem);if(!takenItems.contains(b.customItem))throw new IllegalArgumentException("В пакете нет предмета для макроса "+b.id);}if(!knownMacros.add(fingerprint(b)))continue;String name=b.id;int n=2;while(!takenMacros.add(b.id))b.id=name+" ("+(n++)+")";addMacros.add(b);}
        config.customItems.addAll(addItems);config.binds.addAll(addMacros);CustomItems.invalidate();
        return "Добавлено предметов: "+addItems.size()+", макросов: "+addMacros.size()+" · дубликаты пропущены";
    }
    private static String fingerprint(Object object){var json=JSON.toJsonTree(object).getAsJsonObject();for(String key:List.of("id","author","description","createdAt","keyCode","keyModifiers","enabled"))json.remove(key);if(object instanceof Binding b)json.addProperty("command",b.command.strip().replaceFirst("^/","").replaceAll("\\s+"," "));return json.toString();}
}
