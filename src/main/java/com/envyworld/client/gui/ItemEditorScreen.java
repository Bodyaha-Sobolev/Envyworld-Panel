package com.envyworld.client.gui;

import com.envyworld.EnvyWorld;
import com.envyworld.items.CustomItemPreset;
import com.envyworld.items.CustomItems;
import com.envyworld.items.ItemText;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** A responsive item editor with independent catalog, form, enchantment and detail scrolls. */
public final class ItemEditorScreen extends Screen implements MenuScreen {
    private static final int ROW = 20;
    private int section;
    private int formHeight() { return section == 2 ? Math.max(formH, 86 + draft.attributes.size() * 140) : section == 0 ? 526 : section == 1 ? formH : 280; }
    private int enchantHeight() { return Math.max(40, formH - 50); }
    public static final List<String> ENCHANTS = List.of("sharpness", "smite", "bane_of_arthropods", "knockback", "fire_aspect", "looting", "sweeping_edge", "efficiency", "silk_touch", "unbreaking", "fortune", "power", "punch", "flame", "infinity", "protection", "fire_protection", "blast_protection", "projectile_protection", "respiration", "aqua_affinity", "thorns", "feather_falling", "depth_strider", "frost_walker", "soul_speed", "swift_sneak", "mending", "loyalty", "impaling", "riptide", "channeling", "multishot", "quick_charge", "piercing", "luck_of_the_sea", "lure", "density", "breach", "wind_burst", "binding_curse", "vanishing_curse");
    private static final String[] COLORS = {"#FFFFFF", "#AAAAAA", "#55FF55", "#55FFFF", "#FF55FF", "#FFAA00", "#FF5555", "#5555FF", "#FFFF55", "#55AAFF"};
    private record FormWidget(AbstractWidget widget, int offset) {}
    private final Screen parent;
    private final Consumer<CustomItemPreset> saved;
    private final CustomItemPreset draft;
    private final List<FormWidget> form = new ArrayList<>();
    private final ScrollBars bars = new ScrollBars();
    private List<Item> items = List.of();
    private String itemQuery = "", enchantQuery = "", status = "", signature = "", previewError = "";
    private int gridScroll, formScroll, enchantScroll, detailsScroll;
    private int gridX, gridY, gridW, gridH, formX, formY, formW, formH, detailX, detailW;
    private int enchantY;
    private boolean compactPreview;
    private ItemStack preview = ItemStack.EMPTY;
    private List<Component> details = List.of();
    private EditBox name, lore, color, model, count, enchants, components;
    private ItemTextForm textForm;
    private ColorPickerPopup colorPopup;
    private final ItemTextForm.Colors textColors=new ItemTextForm.Colors();
    private long deleteArmed;

    public ItemEditorScreen(Screen parent, CustomItemPreset preset, Consumer<CustomItemPreset> saved) {
        super(Component.literal("Редактор предмета"));
        this.parent = parent; this.saved = saved;
        this.draft = preset == null ? new CustomItemPreset() : preset.copy();
        if (draft.id.isBlank()) draft.id = CustomItems.nextId();
        textColors.start=hexColor(draft.color);
    }
    public CustomItemPreset draft() { return draft; }

    @Override protected void init() {
        colorPopup=null;
        form.clear();
        int bodyBottom = height - 52;
        gridX = 10; gridY = 98; gridW = Math.max(110, Math.min(200, width * 27 / 100)); gridH = Math.max(60, bodyBottom - gridY);
        boolean threeColumns = width >= 620;
        if (threeColumns) compactPreview = false;
        detailW = threeColumns ? Math.max(156, width * 28 / 100) : 0;
        formX = gridX + gridW + 12; formY = 98;
        formW = width - formX - 16 - (threeColumns ? detailW + 12 : 0); formH = Math.max(90, bodyBottom - formY);
        detailX = formX + formW + 18;
        button("Назад", width - 86, 10, 76, 20, this::onClose);
        if (!threeColumns) button(compactPreview ? "К полям" : "Предпросмотр", formX, 10, Math.min(120, formW), 20, () -> { compactPreview = !compactPreview; init(width, height); });
        EditBox search = new UiEditBox(font, gridX, 72, gridW, 20, Component.literal("Поиск базы предмета"));
        search.setValue(itemQuery); search.setResponder(value -> { itemQuery = value; gridScroll = 0; rebuildItems(); }); addRenderableWidget(search);
        rebuildItems();
        String[] pages={"Текст","Зачарования","Атрибуты","Правила","Еда","Инструмент","Другие теги"};
        int stride=(width-20)/pages.length;
        for(int i=0;i<pages.length;i++){final int page=i;var tab=button(pages[i],10+i*stride,42,stride-3,21,()->{section=page;formScroll=0;init(width,height);});tab.active=true;if(tab instanceof UiButton styled)styled.selected(page==section);}
        if(section==0) {
            textForm=new ItemTextForm(draft,textColors,formX,formY,formW,(widget,offset)->{addRenderableWidget(widget);form.add(new FormWidget(widget,offset));},this::openColorPicker);
            model=input(332,Integer.toString(draft.model),"0 - 9",2,v->draft.model=number(v,0));model.setWidth((formW-10)/2);
            count=input(332,Integer.toString(draft.count),"1 - 64",3,v->draft.count=number(v,1));count.setX(formX+(formW+10)/2);count.setWidth((formW-10)/2);
            formButton("Неразрушимый: "+(draft.unbreakable?"да":"нет"),368,0,(formW-8)/2,()->{draft.unbreakable=!draft.unbreakable;rebuildForm();});
            formButton("Блеск: "+(draft.glint?"да":"нет"),368,(formW+8)/2,(formW-8)/2,()->{draft.glint=!draft.glint;rebuildForm();});
            formButton("Редкость: "+(draft.rarity.isBlank()?"по умолчанию":draft.rarity),404,0,formW,()->{draft.rarity=cycle(draft.rarity,new String[]{"","common","uncommon","rare","epic"});rebuildForm();});
            formButton("Подсказка: "+(draft.hideTooltip?"скрыта":"видна"),440,0,formW,()->{draft.hideTooltip=!draft.hideTooltip;rebuildForm();});
            input(492,draft.tooltipStyle,"minecraft:custom_tooltip",128,v->draft.tooltipStyle=v);
        } else if(section==1) {
            input(12,enchantQuery,"Поиск зачарования",64,v->{enchantQuery=v;enchantScroll=0;}).setWidth(Math.max(60,formW-92));
            formButton(draft.hideEnchantments?"Скрыты":"Видны",12,formW-86,86,()->{draft.hideEnchantments=!draft.hideEnchantments;rebuildForm();});
        } else if(section==2) {
            formButton(draft.hideAttributes?"Атрибуты скрыты":"Атрибуты видны",12,0,formW,()->{draft.hideAttributes=!draft.hideAttributes;rebuildForm();});
            for(int i=0;i<draft.attributes.size();i++){
                final int index=i;var attr=draft.attributes.get(i);int y=54+i*140;
                String attrLabel=BuiltInRegistries.ATTRIBUTE.containsKey(Identifier.parse(attr.type))?Component.translatable(BuiltInRegistries.ATTRIBUTE.getValue(Identifier.parse(attr.type)).getDescriptionId()).getString():attr.type;
                formButton(attrLabel,y,0,formW,()->chooseAttribute(index));
                input(y+27,Double.toString(attr.amount),"Значение",32,v->{try{attr.amount=Double.parseDouble(v);}catch(NumberFormatException ignored){attr.amount=Double.NaN;}});
                formButton("Операция: "+operationLabel(attr.operation),y+54,0,formW,()->minecraft.setScreen(new ChoiceScreen(this,"Операция атрибута",List.of(new ChoiceScreen.Choice("add_value","Добавить значение"),new ChoiceScreen.Choice("add_multiplied_base","Умножить базовое значение"),new ChoiceScreen.Choice("add_multiplied_total","Умножить итоговое значение")),v->{attr.operation=v;signature="";})));
                formButton("Слот: "+slotLabel(attr.slot),y+81,0,(formW-8)/2,()->{
                    var choices=java.util.Arrays.stream(new String[]{"any","mainhand","offhand","hand","head","chest","legs","feet","body"}).map(v->new ChoiceScreen.Choice(v,slotLabel(v))).toList();
                    minecraft.setScreen(new ChoiceScreen(this,"Где действует атрибут",choices,v->{attr.slot=v;signature="";}));
                });
                formButton("Удалить",y+81,(formW+8)/2,(formW-8)/2,()->{draft.attributes.remove(index);rebuildForm();});
            }
            formButton("+ Добавить атрибут",54+draft.attributes.size()*140,0,formW,()->{draft.attributes.add(new CustomItemPreset.AttributeSpec());rebuildForm();});
        } else if(section==3) {
            input(12,draft.canPlace,"minecraft:stone, minecraft:dirt",2048,v->draft.canPlace=v);
            formButton("+ Блок для установки",40,0,formW,()->chooseBlock(v->{draft.canPlace=append(draft.canPlace,v);rebuildForm();}));
            formButton(draft.hideCanPlace?"Подсказка установки скрыта":"Подсказка установки видна",68,0,formW,()->{draft.hideCanPlace=!draft.hideCanPlace;rebuildForm();});
            input(122,draft.canBreak,"minecraft:stone, minecraft:dirt",2048,v->draft.canBreak=v);
            formButton("+ Блок для разрушения",150,0,formW,()->chooseBlock(v->{draft.canBreak=append(draft.canBreak,v);rebuildForm();}));
            formButton(draft.hideCanBreak?"Подсказка разрушения скрыта":"Подсказка разрушения видна",178,0,formW,()->{draft.hideCanBreak=!draft.hideCanBreak;rebuildForm();});
        } else if(section==4) {
            formButton("Еда: "+(draft.foodEnabled?"да":"нет"),12,0,formW,()->{draft.foodEnabled=!draft.foodEnabled;rebuildForm();});
            input(62,Integer.toString(draft.nutrition),"Питательность",4,v->draft.nutrition=number(v,-1));
            input(112,Float.toString(draft.saturation),"Насыщение",12,v->draft.saturation=decimal(v));
            formButton("Можно есть сытым: "+(draft.alwaysEdible?"да":"нет"),152,0,formW,()->{draft.alwaysEdible=!draft.alwaysEdible;rebuildForm();});
        } else if(section==5) {
            formButton("Инструмент: "+(draft.toolEnabled?"да":"нет"),12,0,formW,()->{draft.toolEnabled=!draft.toolEnabled;rebuildForm();});
            input(62,draft.toolBlocks,"minecraft:stone",2048,v->draft.toolBlocks=v);
            formButton("+ Выбрать блок",90,0,formW,()->chooseBlock(v->{draft.toolBlocks=append(draft.toolBlocks,v);rebuildForm();}));
            input(140,Float.toString(draft.miningSpeed),"Скорость",16,v->draft.miningSpeed=decimal(v));
            input(190,Integer.toString(draft.damagePerBlock),"Расход прочности за блок",8,v->draft.damagePerBlock=number(v,-1));
        } else components=input(12,draft.components,"minecraft:custom_data={example:1b}",32767,v->draft.components=v);
        int footerW = Math.max(50, (width - 38) / 4);
        button("Сохранить", 10, height - 30, footerW, 20, this::save);
        button("Выдать", 18 + footerW, height - 30, footerW, 20, this::give);
        button("Копировать NBT", 26 + footerW * 2, height - 30, footerW, 20, () -> { if (!preview.isEmpty()) { minecraft.keyboardHandler.setClipboard(CustomItems.nbt(preview)); status = "NBT скопирован"; } });
        button("Удалить", 34 + footerW * 3, height - 30, footerW, 20, () -> {
            if (System.currentTimeMillis() - deleteArmed > 3000) { deleteArmed = System.currentTimeMillis(); status = "Нажмите «Удалить» ещё раз для подтверждения"; return; }
            EnvyWorld.config().customItems.removeIf(preset -> preset.id.equals(draft.id)); CustomItems.invalidate(); EnvyWorld.saveConfig(); onClose();
        });
        positionForm(); signature = "";
    }
    private void openColorPicker(AbstractWidget anchor,String initial,Consumer<String> apply,Runnable cancel){
        colorPopup=new ColorPickerPopup(anchor,initial,apply,cancel,width,height);
    }
    private void rebuildForm() { init(width, height); }
    private EditBox input(int offset, String value, String hint, int max, Consumer<String> changed) {
        EditBox field = new UiEditBox(font, formX, formY + offset, formW, 20, Component.literal(hint));
        field.setMaxLength(max); field.setValue(value == null ? "" : value);
        field.setResponder(text -> { changed.accept(text); signature = ""; });
        addRenderableWidget(field); form.add(new FormWidget(field, offset)); return field;
    }
    private Button button(String label, int x, int y, int w, int h, Runnable action) {
        return addRenderableWidget(UiButton.builder(Component.literal(label), button -> action.run()).bounds(x, y, w, h).build());
    }
    private void formButton(String label, int offset, int xOffset, int w, Runnable action) {
        Button b = button(label, formX + xOffset, formY + offset, w, 20, action); form.add(new FormWidget(b, offset));
    }
    private void positionForm() {
        formScroll = Math.max(0, Math.min(formScroll, Math.max(0, formHeight() - formH)));
        for (FormWidget entry : form) {
            AbstractWidget widget = entry.widget(); widget.setY(formY + entry.offset() - formScroll);
            widget.visible = !compactPreview && widget.getY() >= formY && widget.getBottom() <= formY + formH;
            if (!widget.visible && widget.isFocused()) { widget.setFocused(false); setFocused(null); }
        }
        enchantY = formY + 42;
    }
    private void rebuildItems() {
        String query = itemQuery.toLowerCase(Locale.ROOT);
        items = BuiltInRegistries.ITEM.stream().filter(item -> item != Items.AIR && (query.isBlank()
                || BuiltInRegistries.ITEM.getKey(item).toString().contains(query)
                || new ItemStack(item).getHoverName().getString().toLowerCase(Locale.ROOT).contains(query))).toList();
    }
    private void refreshPreview() {
        String key = new com.google.gson.GsonBuilder().serializeSpecialFloatingPointValues().create().toJson(draft);
        if (key.equals(signature)) return;
        signature = key;
        try {
            preview = CustomItems.build(draft, draft.count); details = CustomItems.describe(draft, preview);
            if (status.equals(previewError)) status = ""; previewError = "";
        }
        catch (RuntimeException invalid) {
            preview = ItemStack.EMPTY; previewError = invalid.getMessage() == null ? "Не удалось сформировать предмет" : invalid.getMessage();
            details = List.of(Component.literal(previewError)); status = previewError;
        }
    }
    @Override public void renderBackground(GuiGraphics graphics, int x, int y, float delta) { minecraft.gui.renderDeferredSubtitles(); }
    @Override public void render(GuiGraphics original, int mouseX, int mouseY, float delta) {
        UiRenderer.beginFrame(); bars.beginFrame(); positionForm(); refreshPreview();
        GuiGraphics g = new UiGraphics(original, mouseX, mouseY);
        HudSurface.fullscreen(width,height);
        g.fill(0,0,width,38,0x70000000);
        UiFont.draw(g, "Редактор предмета", 12, 17, MenuTheme.PALETTE.textPrimary());
        g.hLine(10, width - 10, 37, MenuTheme.PALETTE.separator());
        drawGrid(g, mouseX, mouseY);
        if (!compactPreview) drawForm(g, mouseX, mouseY);
        drawDetails(g, compactPreview ? formX : detailX, compactPreview ? formW : detailW, mouseX, mouseY);
        super.render(g, mouseX, mouseY, delta);
        if (!status.isBlank()) UiFont.draw(g, clipped(status, width - 24), 12, height - 41, 6, 0xFFB5C8E2);
        if(colorPopup!=null){colorPopup.render(g,mouseX,mouseY,delta);if(colorPopup.finished())colorPopup=null;}
    }
    private void drawGrid(GuiGraphics g, int mouseX, int mouseY) {
        g.fill(gridX, gridY, gridX + gridW, gridY + gridH, MenuTheme.PALETTE.backgroundControl());
        int columns = Math.max(1, (gridW - 6) / ROW), totalHeight = ((items.size() + columns - 1) / columns) * ROW;
        gridScroll = Math.max(0, Math.min(gridScroll, Math.max(0, totalHeight - gridH)));
        g.enableScissor(gridX, gridY, gridX + gridW - 6, gridY + gridH);
        int first = gridScroll / ROW, offset = gridScroll % ROW;
        for (int row = 0; row < (gridH + ROW - 1) / ROW + 1; row++) for (int col = 0; col < columns; col++) {
            int index = (first + row) * columns + col; if (index >= items.size()) break;
            Item item = items.get(index); int x = gridX + col * ROW, y = gridY + row * ROW - offset;
            boolean selected = BuiltInRegistries.ITEM.getKey(item).toString().equals(draft.baseItem);
            if (selected) UiGraphics.rounded(x + 1, y + 1, 18, 18, 4, 0x384B5A70);
            g.renderItem(new ItemStack(item), x + 2, y + 2);
            if (inside(mouseX, mouseY, x, y, ROW, ROW) && inside(mouseX, mouseY, gridX, gridY, gridW, gridH))
                g.setTooltipForNextFrame(font, new ItemStack(item).getHoverName(), mouseX, mouseY);
        }
        g.disableScissor(); bar(g, gridX + gridW - 3, gridY, gridH, gridScroll, gridH, totalHeight, value -> gridScroll = value);
    }
    private void drawForm(GuiGraphics g,int mouseX,int mouseY) {
        g.enableScissor(formX,formY,formX+formW,formY+formH);
        if(section==0){textForm.draw(g,formX,formY);
            label(g,"Модель",320);UiFont.draw(g,"Количество",formX+(formW+10)/2,formY+320-formScroll,6,0xFF9BA7B9);
            label(g,"Стиль подсказки",480);
        } else if(section==1){label(g,"Зачарования  |  минус / плюс меняют уровень",0);drawEnchants(g,mouseX,mouseY);}
        else if(section==3){label(g,"Можно устанавливать на блоки",0);label(g,"Можно разрушать блоки",110);label(g,"Правила действуют в режиме приключения",226);}
        else if(section==4){label(g,"Питательность",50);label(g,"Насыщение",100);}
        else if(section==5){label(g,"Блоки",50);label(g,"Скорость добычи",128);label(g,"Расход прочности",178);}
        else if(section==6){label(g,"Дополнительные компоненты / NBT",0);label(g,"Исходные теги импорта сохраняются",48);label(g,"Этот патч добавляет или заменяет компоненты",64);}
        g.disableScissor();
        bar(g,formX+formW+2,formY,formH,formScroll,formH,formHeight(),v->{formScroll=v;positionForm();});
    }
    private static String cycle(String value,String[] values){for(int i=0;i<values.length;i++)if(values[i].equals(value))return values[(i+1)%values.length];return values[0];}
    private static String operationLabel(String value){return switch(value){case "add_value"->"сложение";case "add_multiplied_base"->"умножение базы";default->"умножение итога";};}
    private static String slotLabel(String value){return switch(value){case "any"->"любой";case "mainhand"->"основная рука";case "offhand"->"вторая рука";case "hand"->"любая рука";case "head"->"голова";case "chest"->"нагрудник";case "legs"->"поножи";case "feet"->"ботинки";default->"тело";};}
    private static float decimal(String text){try{return Float.parseFloat(text);}catch(NumberFormatException e){return Float.NaN;}}
    private static String append(String list,String id){return list.isBlank()?id:list+", "+id;}
    private void chooseAttribute(int index){
        var choices=BuiltInRegistries.ATTRIBUTE.keySet().stream().sorted().map(id->new ChoiceScreen.Choice(id.toString(),Component.translatable(BuiltInRegistries.ATTRIBUTE.getValue(id).getDescriptionId()).getString())).toList();
        minecraft.setScreen(new ChoiceScreen(this,"Выберите атрибут",choices,v->{draft.attributes.get(index).type=v;signature="";}));
    }
    private void chooseBlock(java.util.function.Consumer<String> accept){
        var choices=BuiltInRegistries.BLOCK.stream().filter(b->b.asItem()!=Items.AIR).map(b->new ChoiceScreen.Choice(BuiltInRegistries.BLOCK.getKey(b).toString(),new ItemStack(b.asItem()).getHoverName().getString())).toList();
        minecraft.setScreen(new ChoiceScreen(this,"Выберите блок",choices,accept));
    }
    private List<String> filteredEnchants() {
        String query = enchantQuery.toLowerCase(Locale.ROOT);
        var ids=minecraft.level==null?ENCHANTS.stream().map(id->"minecraft:"+id).toList():minecraft.level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).listElements().map(h->h.getRegisteredName()).sorted().toList();
        return ids.stream().filter(id -> query.isBlank() || id.contains(query) || enchantLabel(id).toLowerCase(Locale.ROOT).contains(query)).toList();
    }
    private String enchantLabel(String id) {
        if (minecraft.level != null) {
            var key = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENCHANTMENT, Identifier.parse(id));
            return minecraft.level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).get(key)
                    .map(holder -> holder.value().description().getString()).orElse(id);
        }
        return switch (id.replace("minecraft:","")) { case "sharpness" -> "Острота"; case "unbreaking" -> "Прочность"; case "mending" -> "Починка"; case "efficiency" -> "Эффективность"; case "protection" -> "Защита"; case "fortune" -> "Удача"; case "looting" -> "Добыча"; default -> id.replace("minecraft:",""); };
    }
    private void drawEnchants(GuiGraphics g, int mouseX, int mouseY) {
        int panelTop = Math.max(formY, enchantY), panelBottom = Math.min(formY + formH, enchantY + enchantHeight());
        if (panelBottom <= panelTop) return;
        List<String> ids = filteredEnchants(); enchantScroll = Math.max(0, Math.min(enchantScroll, Math.max(0, ids.size() * ROW - enchantHeight())));
        g.fill(formX, enchantY, formX + formW, enchantY + enchantHeight(), MenuTheme.PALETTE.backgroundControl());
        g.enableScissor(formX, panelTop, formX + formW - 6, panelBottom);
        Map<String,Integer> levels;
        try { levels = CustomItems.enchantments(draft.enchantments); } catch (RuntimeException invalid) { levels = Map.of(); }
        for (int i = enchantScroll / ROW; i < ids.size() && i * ROW - enchantScroll < enchantHeight(); i++) {
            int y = enchantY + i * ROW - enchantScroll; String id = ids.get(i); int level = levels.getOrDefault(id, 0);
            if (level > 0) UiGraphics.rounded(formX + 1, y, formW - 2, ROW, 0, 0x242A3C56);
            UiFont.draw(g, clipped(enchantLabel(id), formW - 88), formX + 6, UiFont.centeredY(y, ROW, 7), level > 0 ? 0xFFD3E5FF : 0xFFA6B1C1);
            UiFont.draw(g, Integer.toString(level), formX + formW - 52, UiFont.centeredY(y, ROW, 7), 0xFFE3E8EF);
            UiIcons.minus(formX + formW - 69,y+10,0xFFAEBBCD);
            UiIcons.plus(formX + formW - 24, y + 10, 0xFFAEBBCD);
        }
        g.disableScissor();
        if (enchantY >= formY && enchantY + enchantHeight() <= formY + formH) bar(g, formX + formW - 2, enchantY, enchantHeight(), enchantScroll, enchantHeight(), ids.size() * ROW, value -> enchantScroll = value);
    }
    private void drawDetails(GuiGraphics g, int x, int w, int mouseX, int mouseY) {
        if (w <= 0) return;
        int y = formY, h = formH;
        g.fill(x, y, x + w, y + h, MenuTheme.PALETTE.backgroundControl());
        UiFont.draw(g, "ПРЕДПРОСМОТР", x + 10, y + 10, 6, 0xFF929FAF);
        if (!preview.isEmpty()) {
            g.pose().pushMatrix(); g.pose().translate(x + 10, y + 28); g.pose().scale(2, 2); g.renderItem(preview, 0, 0); g.pose().popMatrix();
            UiText.component(g, preview.getHoverName(), x + 49, y + 33, w - 60, 7, 0xFFE5EAF2);
        }
        List<Component> wrapped = new ArrayList<>();
        for (Component component : details) wrapped.addAll(UiText.wrap(component, w - 24, 6));
        int top = y + 76, available = Math.max(20, h - 86);
        detailsScroll = Math.max(0, Math.min(detailsScroll, Math.max(0, wrapped.size() * 11 - available)));
        g.enableScissor(x + 8, top, x + w - 6, top + available);
        for (int i = detailsScroll / 11; i < wrapped.size() && i * 11 - detailsScroll < available; i++)
            UiText.component(g, wrapped.get(i), x + 10, top + i * 11 - detailsScroll, w - 24, 6, 0xFFB6C0CF);
        g.disableScissor(); bar(g, x + w - 3, top, available, detailsScroll, available, wrapped.size() * 11, value -> detailsScroll = value);
    }
    private void label(GuiGraphics g, String text, int offset) { UiFont.draw(g, clipped(text, formW), formX, formY + offset - formScroll, 6, 0xFF9BA7B9); }
    private void bar(GuiGraphics g, int x, int y, int h, int value, int visible, int total, java.util.function.IntConsumer setter) {
        bars.add(x, y, h, value, visible, total, setter);
        var b = new ScrollBars.Bar(x, y, h, value, visible, total, setter);
        UiGraphics.rounded(x-1,y,4,h,1,0x20B8C5D6);
        if(total<=visible)return;
        UiGraphics.rounded(x, b.thumbY(), 2, b.thumbHeight(), 1, 0x9CB4C2D5);
    }
    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        if(colorPopup!=null)return colorPopup.press(event,doubled);
        if (event.button() == 0 && bars.press(event.x(), event.y())) return true;
        if(section==0&&textForm!=null)textForm.press(event.x(),event.y());
        if (super.mouseClicked(event, doubled)) return true;
        if (event.button() != 0) return false;
        if (inside(event.x(), event.y(), gridX, gridY, gridW - 6, gridH)) {
            int columns = Math.max(1, (gridW - 6) / ROW);
            int column=(int)((event.x()-gridX)/ROW);if(column>=columns)return true;
            int index = (int) ((event.y() - gridY + gridScroll) / ROW) * columns + column;
            if (index < items.size()) { draft.baseItem = BuiltInRegistries.ITEM.getKey(items.get(index)).toString(); signature = ""; }
            return true;
        }
        if (!compactPreview && inside(event.x(), event.y(), formX, formY, formW, formH)) {
            int relativeY = (int) event.y() - formY + formScroll;
            if (section == 1 && relativeY >= 42 && relativeY < 42 + enchantHeight()) {
                List<String> ids = filteredEnchants(); int index = (relativeY - 42 + enchantScroll) / ROW;
                if (index < ids.size()) {
                    try {
                        var levels = CustomItems.enchantments(draft.enchantments); String id = ids.get(index);
                        int level = levels.getOrDefault(id, 0);
                        int delta=event.x()>=formX+formW-78 && event.x()<formX+formW-60?-1:event.x()>=formX+formW-33 && event.x()<formX+formW-14?1:0;
                        if(delta==0)return true;
                        level = Math.max(0, Math.min(255, level + delta));
                        if (level == 0) levels.remove(id); else levels.put(id, level);
                        draft.enchantments = levels.entrySet().stream().map(entry -> entry.getKey() + ':' + entry.getValue()).collect(java.util.stream.Collectors.joining(", "));
                        signature = "";
                    } catch (RuntimeException invalid) { status = invalid.getMessage(); }
                }
                return true;
            }
        }
        return false;
    }
    @Override public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) { return colorPopup!=null?colorPopup.drag(event,dx,dy):bars.drag(event.y()) || super.mouseDragged(event, dx, dy); }
    @Override public boolean mouseReleased(MouseButtonEvent event) { return colorPopup!=null?colorPopup.release(event):bars.release() || super.mouseReleased(event); }
    @Override public boolean mouseScrolled(double x, double y, double dx, double dy) {
        if(colorPopup!=null)return true;
        if (UiGraphics.scrollTooltip(dy)) return true;
        int amount = (int) Math.round(dy * 24);
        if (inside(x, y, gridX, gridY, gridW, gridH)) gridScroll = Math.max(0, gridScroll - amount);
        else if (section == 1 && !compactPreview && inside(x, y, formX, Math.max(formY, enchantY), formW, Math.max(0, Math.min(enchantY + enchantHeight(), formY + formH) - Math.max(formY, enchantY)))) {
            int before = enchantScroll;
            enchantScroll = Math.max(0, Math.min(Math.max(0, filteredEnchants().size() * ROW - enchantHeight()), enchantScroll - amount));
            if (before == enchantScroll) { formScroll = Math.max(0, formScroll - amount); positionForm(); }
        }
        else if (!compactPreview && inside(x, y, formX, formY, formW + 8, formH)) { formScroll = Math.max(0, formScroll - amount); positionForm(); }
        else detailsScroll = Math.max(0, detailsScroll - amount);
        return true;
    }
    @Override public boolean keyPressed(net.minecraft.client.input.KeyEvent event){return colorPopup!=null?colorPopup.key(event):super.keyPressed(event);}
    @Override public boolean charTyped(net.minecraft.client.input.CharacterEvent event){return colorPopup!=null?colorPopup.character(event):super.charTyped(event);}
    private boolean save() {
        refreshPreview();
        if(preview.isEmpty()){status=previewError;return false;}
        if(section==0 && color!=null && !color.getValue().matches("#[0-9a-fA-F]{6}")){status="Укажите цвет в формате #RRGGBB";return false;}
        if (draft.name.isBlank() || preview.isEmpty() || !hexColor(draft.color).matches("#[0-9a-fA-F]{6}") || draft.model < 0 || draft.model > 9 || draft.count < 1 || draft.count > 64) { status = "Проверьте название, HEX-цвет, модель (0 - 9), количество (1 - 64) и компоненты"; return false; }
        if (draft.author == null || draft.author.isBlank()) draft.author = minecraft.getUser().getName();
        if (draft.createdAt == 0) draft.createdAt = System.currentTimeMillis();
        CustomItemPreset stored = draft.copy();
        int index = -1;
        for (int i = 0; i < EnvyWorld.config().customItems.size(); i++) if (EnvyWorld.config().customItems.get(i).id.equals(stored.id)) { index = i; break; }
        if (index < 0) EnvyWorld.config().customItems.add(stored); else EnvyWorld.config().customItems.set(index, stored);
        CustomItems.invalidate(); EnvyWorld.saveConfig(); saved.accept(stored); signature = ""; status = "Сохранено  |  автор " + stored.author;
        return true;
    }
    private void give() {
        if(!save())return;
        refreshPreview();
        if (preview.isEmpty()) return;
        status = CustomItems.give(minecraft, preview);
    }
    @Override public void onClose() { if(parent instanceof EnvyScreen menu)menu.refreshSavedSelection();minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
    private static boolean inside(double x, double y, int left, int top, int w, int h) { return x >= left && x < left + w && y >= top && y < top + h; }
    private static int number(String value, int fallback) { try { return Integer.parseInt(value); } catch (NumberFormatException ignored) { return fallback; } }
    private static String clipped(String value, int width) {
        if (UiFont.width(value) <= width) return value;
        while (!value.isEmpty() && UiFont.width(value + "...") > width) value = value.substring(0, value.length() - 1);
        return value + "...";
    }
    private static String hexColor(String value) {
        if (value != null && value.matches("#[0-9a-fA-F]{6}")) return value;
        try { Integer rgb = net.minecraft.ChatFormatting.valueOf(value.toUpperCase(Locale.ROOT)).getColor(); if (rgb != null) return String.format("#%06X", rgb); }
        catch (RuntimeException ignored) {}
        return "#FFFFFF";
    }
}
