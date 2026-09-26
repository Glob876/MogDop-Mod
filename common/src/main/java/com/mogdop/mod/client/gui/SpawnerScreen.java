package com.mogdop.mod.client.gui;

import com.mogdop.mod.MogDopSMod;
import com.mogdop.mod.client.MogDopSModClient;
import com.mogdop.mod.network.*;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.architectury.networking.NetworkManager;
import io.wispforest.owo.ui.base.BaseComponent;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.CheckboxComponent;
import io.wispforest.owo.ui.component.ColorPickerComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.EntityComponent;
import io.wispforest.owo.ui.component.TextBoxComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.core.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class SpawnerScreen extends BaseOwoScreen<FlowLayout> {

    public static float uiOpacity = 0.95F;
    public static boolean vanillaSkin = false;

    private static final Identifier LOGO_TEXTURE = Identifier.of("mogdops-mod", "icon.png");

    private final Screen parent;
    private FlowLayout root;
    private FlowLayout tabsSidebarTop;
    private FlowLayout bottomSettingsBox;
    private FlowLayout tabContentWrapper;

    private final List<TabModule> tabs = new ArrayList<>();
    private TabModule currentTabModule;

    private final List<EntityType<?>> allSpawnableEntities = new ArrayList<>();
    private boolean showMisc = false;
    private int categoryIndex = 0;

    private final String[] categoryKeys = {
            "mogdops-mod.category.all",
            "mogdops-mod.category.hostile",
            "mogdops-mod.category.passive",
            "mogdops-mod.category.ambient"
    };

    private long lastSpawnTime = 0;

    public SpawnerScreen() {
        this(null, 0);
    }

    public SpawnerScreen(@Nullable Screen parent) {
        this(parent, 0);
    }

    public SpawnerScreen(@Nullable Screen parent, int initialTabIndex) {
        this.parent = parent;
        initTabs();
        if (initialTabIndex >= 0 && initialTabIndex < tabs.size()) {
            currentTabModule = tabs.get(initialTabIndex);
        } else {
            currentTabModule = tabs.get(0);
        }
    }

    private void initTabs() {
        tabs.clear();
        tabs.add(new MainMenuTab());     // 0: Главная
        tabs.add(new MobSpawnerTab());   // 1: Мобы
        tabs.add(new ItemGiverTab());    // 2: Предметы
        tabs.add(new UtilitiesTab());    // 3: Утилиты
        tabs.add(new NextbotsTab());     // 4: NextBots
        tabs.add(new SettingsTab());     // 5: Настройки
    }

    // Компонент изображения с режимом заполнения Cover
    public static class CoverImageComponent extends BaseComponent {
        private final Identifier texture;
        private final int imageWidth;
        private final int imageHeight;

        public CoverImageComponent(Identifier texture, int imageWidth, int imageHeight) {
            this.texture = texture;
            this.imageWidth = imageWidth;
            this.imageHeight = imageHeight;
        }

        @Override
        public void draw(OwoUIDrawContext context, int mouseX, int mouseY, float partialTicks, float delta) {
            if (this.width <= 0 || this.height <= 0) return;

            float scale = Math.max((float) this.width / this.imageWidth, (float) this.height / this.imageHeight);
            int drawW = Math.round(this.imageWidth * scale);
            int drawH = Math.round(this.imageHeight * scale);
            int drawX = this.x + (this.width - drawW) / 2;
            int drawY = this.y + (this.height - drawH) / 2;

            context.enableScissor(this.x, this.y, this.width, this.height);

            RenderSystem.enableBlend();
            context.drawTexture(
                    texture,
                    drawX, drawY,
                    0, 0,
                    drawW, drawH,
                    drawW, drawH
            );

            context.fillGradient(this.x, this.y + this.height - 35, this.x + this.width, this.y + this.height, 0x00101014, 0xEE101014);
            context.disableScissor();
        }

        @Override
        protected int determineHorizontalContentSize(Sizing sizing) { return 100; }

        @Override
        protected int determineVerticalContentSize(Sizing sizing) { return 100; }
    }

    // Компонент компактного текста с уменьшенным масштабом шрифта
    public static class SmallLabelComponent extends BaseComponent {
        private Text text;
        private final float scale;
        private final int color;
        private final boolean shadow;
        private HorizontalAlignment horizontalAlignment = HorizontalAlignment.LEFT;

        public SmallLabelComponent(Text text, float scale, int color, boolean shadow) {
            this.text = text;
            this.scale = scale;
            this.color = color;
            this.shadow = shadow;
        }

        public SmallLabelComponent text(Text text) {
            this.text = text;
            return this;
        }

        public SmallLabelComponent horizontalAlignment(HorizontalAlignment align) {
            this.horizontalAlignment = align;
            return this;
        }

        @Override
        public void draw(OwoUIDrawContext context, int mouseX, int mouseY, float partialTicks, float delta) {
            var textRenderer = MinecraftClient.getInstance().textRenderer;
            if (textRenderer == null) return;
            var matrices = context.getMatrices();
            matrices.push();

            int textWidth = textRenderer.getWidth(text);
            float renderX = this.x;
            if (horizontalAlignment == HorizontalAlignment.CENTER) {
                renderX = this.x + (this.width - textWidth * scale) / 2.0f;
            } else if (horizontalAlignment == HorizontalAlignment.RIGHT) {
                renderX = this.x + this.width - textWidth * scale;
            }

            matrices.translate(renderX, this.y, 0);
            matrices.scale(scale, scale, 1.0f);

            if (shadow) {
                context.drawTextWithShadow(textRenderer, text, 0, 0, color);
            } else {
                context.drawText(textRenderer, text, 0, 0, color, false);
            }
            matrices.pop();
        }

        @Override
        protected int determineHorizontalContentSize(Sizing sizing) {
            var textRenderer = MinecraftClient.getInstance().textRenderer;
            return textRenderer != null ? (int) Math.ceil(textRenderer.getWidth(text) * scale) : 10;
        }

        @Override
        protected int determineVerticalContentSize(Sizing sizing) {
            var textRenderer = MinecraftClient.getInstance().textRenderer;
            return textRenderer != null ? (int) Math.ceil(textRenderer.fontHeight * scale) : 8;
        }
    }

    public static SmallLabelComponent smallLabel(Text text, float scale, int color) {
        return new SmallLabelComponent(text, scale, color, true);
    }

    public static SmallLabelComponent smallLabel(String text, float scale, int color) {
        return new SmallLabelComponent(Text.literal(text), scale, color, true);
    }

    @Override
    public boolean shouldPause() { return false; }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (chr == 'ё' || chr == 'Ё' || chr == '`' || chr == '~') {
            return false;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (MogDopSModClient.openSpawnerKey.matchesKey(keyCode, scanCode)) {
            this.close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void close() {
        MogDopSModClient.CONFIG.save();
        if (this.parent != null) {
            MinecraftClient.getInstance().setScreen(this.parent);
        } else {
            super.close();
        }
    }

    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, Containers::verticalFlow);
    }

    @Override
    protected void build(FlowLayout rootComponent) {
        this.root = rootComponent;
        rootComponent.surface(Surface.flat(0x00000000));
        rootComponent.horizontalAlignment(HorizontalAlignment.CENTER);
        rootComponent.verticalAlignment(VerticalAlignment.CENTER);

        allSpawnableEntities.clear();
        for (EntityType<?> type : Registries.ENTITY_TYPE) allSpawnableEntities.add(type);

        FlowLayout windowBox = Containers.horizontalFlow(Sizing.fill(100), Sizing.fill(100));
        windowBox.surface(Surface.flat(0x88101014));
        windowBox.padding(Insets.of(8));
        windowBox.gap(8);

        // ================= 1. ЛЕВЫЙ САЙДБАР С ВКЛАДКАМИ =================
        FlowLayout leftSidebar = Containers.verticalFlow(Sizing.fixed(110), Sizing.fill(100));
        leftSidebar.surface(Surface.flat(0xCC14141A));
        leftSidebar.padding(Insets.of(6));
        leftSidebar.gap(4);

        // Шапка слева вверху
        FlowLayout headerBox = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        headerBox.horizontalAlignment(HorizontalAlignment.CENTER);
        headerBox.margins(Insets.bottom(4));
        headerBox.child(smallLabel(Text.literal("MOGDOP'S MOD"), 0.85f, 0xFF00C8FF).horizontalAlignment(HorizontalAlignment.CENTER));
        headerBox.child(smallLabel(Text.literal("v0.2.0"), 0.65f, 0x88AAAAAA).horizontalAlignment(HorizontalAlignment.CENTER));
        leftSidebar.child(headerBox);

        // Верхние основные вкладки
        tabsSidebarTop = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        tabsSidebarTop.gap(4);
        leftSidebar.child(tabsSidebarTop);

        // Разделительная линия перед настройками
        FlowLayout divider = Containers.horizontalFlow(Sizing.fill(100), Sizing.fixed(1));
        divider.surface(Surface.flat(0x44FFFFFF));
        divider.margins(Insets.vertical(6));
        leftSidebar.child(divider);

        // Нижняя секция (Вкладка Настройки)
        bottomSettingsBox = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        leftSidebar.child(bottomSettingsBox);

        windowBox.child(leftSidebar);

        // ================= 2. ПРАВАЯ РАБОЧАЯ ОБЛАСТЬ КОНТЕНТА =================
        tabContentWrapper = Containers.verticalFlow(Sizing.fill(100), Sizing.fill(100));
        tabContentWrapper.surface(Surface.flat(0xAA16161E));
        tabContentWrapper.padding(Insets.of(8));
        windowBox.child(tabContentWrapper);

        rootComponent.child(windowBox);
        rebuildTabUI();
    }

    private FlowLayout createSidebarTabButton(TabModule tab) {
        boolean isSelected = (tab == currentTabModule);
        FlowLayout btn = Containers.horizontalFlow(Sizing.fill(100), Sizing.fixed(24));
        btn.surface(Surface.flat(isSelected ? 0xCC00C8FF : 0x6622222A));
        btn.cursorStyle(CursorStyle.HAND);
        btn.horizontalAlignment(HorizontalAlignment.CENTER);
        btn.verticalAlignment(VerticalAlignment.CENTER);
        btn.padding(Insets.of(2));

        SmallLabelComponent lbl = smallLabel(tab.getTitle(), 0.78f, isSelected ? 0xFFFFFFFF : 0xFFAAAAAA);
        lbl.horizontalAlignment(HorizontalAlignment.CENTER);
        btn.child(lbl);

        btn.mouseEnter().subscribe(() -> {
            if (tab != currentTabModule) btn.surface(Surface.flat(0xAA333340));
        });
        btn.mouseLeave().subscribe(() -> {
            if (tab != currentTabModule) btn.surface(Surface.flat(0x6622222A));
            else btn.surface(Surface.flat(0xCC00C8FF));
        });
        btn.mouseDown().subscribe((mX, mY, button) -> {
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                this.currentTabModule = tab;
                tab.onSelected();
                rebuildTabUI();
                return true;
            }
            return false;
        });

        return btn;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        float uiAlpha = 1f;
        if (lastSpawnTime > 0) {
            long elapsed = System.currentTimeMillis() - lastSpawnTime;
            if (elapsed < 1000) uiAlpha = 0.15f + 0.85f * (elapsed / 1000f);
            else lastSpawnTime = 0;
        }

        RenderSystem.enableBlend();
        context.setShaderColor(1f, 1f, 1f, uiAlpha);

        int bgAlpha = (int) (100 * uiAlpha * uiOpacity);
        context.fill(0, 0, this.width, this.height, (bgAlpha << 24) | 0x000000);
        context.drawBorder(0, 0, this.width, this.height, 0xFF00C8FF);

        super.render(context, mouseX, mouseY, delta);

        context.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
    }

    private void rebuildTabUI() {
        if (tabsSidebarTop != null && tabs.size() > 0) {
            tabsSidebarTop.clearChildren();
            for (int i = 0; i < 5 && i < tabs.size(); i++) {
                tabsSidebarTop.child(createSidebarTabButton(tabs.get(i)));
            }
        }

        if (bottomSettingsBox != null && tabs.size() > 5) {
            bottomSettingsBox.clearChildren();
            bottomSettingsBox.child(createSidebarTabButton(tabs.get(5)));
        }

        tabContentWrapper.clearChildren();
        currentTabModule.populateTab(tabContentWrapper);
    }

    public void triggerSpawnEffect() {
        lastSpawnTime = System.currentTimeMillis();
    }

    public abstract class TabModule {
        public String search = "";
        public abstract Text getTitle();
        public void onSelected() {}
        public abstract void populateTab(FlowLayout container);
    }

    public FlowLayout createCard(Component icon, String labelText, Runnable onLeftClick, @Nullable Consumer<FlowLayout> onRightClick) {
        var label = smallLabel(Text.literal(labelText), 0.72f, 0xFFFFFFFF).horizontalAlignment(HorizontalAlignment.CENTER);

        FlowLayout labelWrapper = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
        labelWrapper.horizontalAlignment(HorizontalAlignment.CENTER);
        labelWrapper.padding(Insets.of(1));
        labelWrapper.child(label);

        FlowLayout card = Containers.verticalFlow(Sizing.fixed(64), Sizing.fixed(54));
        card.surface(Surface.flat(0xAA25252D));
        card.horizontalAlignment(HorizontalAlignment.CENTER);
        card.verticalAlignment(VerticalAlignment.CENTER);
        card.padding(Insets.of(2));
        card.gap(2);
        card.cursorStyle(CursorStyle.HAND);
        card.child(icon).child(labelWrapper);

        card.mouseEnter().subscribe(() -> card.surface(Surface.flat(0xDD3E3E4C)));
        card.mouseLeave().subscribe(() -> card.surface(Surface.flat(0xAA25252D)));
        card.mouseDown().subscribe((mX, mY, button) -> {
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && onLeftClick != null) { onLeftClick.run(); return true; }
            if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && onRightClick != null) { onRightClick.accept(card); return true; }
            return false;
        });

        return card;
    }

    public FlowLayout createFlatButton(int width, int height, Component labelComp, Runnable onClick) {
        FlowLayout btn = Containers.horizontalFlow(Sizing.fixed(width), Sizing.fixed(height));
        btn.surface(Surface.flat(0xAA2F2F38));
        btn.cursorStyle(CursorStyle.HAND);
        btn.horizontalAlignment(HorizontalAlignment.CENTER);
        btn.verticalAlignment(VerticalAlignment.CENTER);
        btn.child(labelComp);

        btn.mouseEnter().subscribe(() -> btn.surface(Surface.flat(0xDD4A4A58)));
        btn.mouseLeave().subscribe(() -> btn.surface(Surface.flat(0xAA2F2F38)));
        btn.mouseDown().subscribe((mX, mY, button) -> {
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                onClick.run();
                return true;
            }
            return false;
        });
        return btn;
    }

    public FlowLayout createFlatButton(int width, int height, Text text, Runnable onClick) {
        return createFlatButton(width, height, smallLabel(text, 0.75f, 0xFFFFFFFF), onClick);
    }

    // ================= ВКЛАДКА 0: ГЛАВНОЕ МЕНЮ =================
    private class MainMenuTab extends TabModule {
        @Override
        public Text getTitle() { return Text.translatable("mogdops-mod.tab.main"); }

        @Override
        public void populateTab(FlowLayout container) {
            container.gap(6);
            container.horizontalAlignment(HorizontalAlignment.CENTER);

            CoverImageComponent cover = new CoverImageComponent(LOGO_TEXTURE, 512, 288);
            cover.sizing(Sizing.fill(100), Sizing.fixed(110));
            container.child(cover);

            FlowLayout infoList = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
            infoList.gap(5);
            infoList.padding(Insets.of(2, 4, 4, 4));

            String[] textKeys = {
                    "mogdops-mod.main_tab.info1",
                    "mogdops-mod.main_tab.info2",
                    "mogdops-mod.main_tab.info3",
                    "mogdops-mod.main_tab.info4",
                    "mogdops-mod.main_tab.info5",
                    "mogdops-mod.main_tab.info6"
            };

            for (String key : textKeys) {
                FlowLayout row = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
                row.gap(6);
                row.child(smallLabel("•", 0.75f, 0xFF00C8FF));
                SmallLabelComponent txt = smallLabel(Text.translatable(key), 0.74f, 0xFFCCCCCC);
                txt.sizing(Sizing.fill(100), Sizing.content());
                row.child(txt);
                infoList.child(row);
            }

            ScrollContainer<FlowLayout> scroll = Containers.verticalScroll(Sizing.fill(100), Sizing.fill(100), infoList);
            scroll.scrollbar(ScrollContainer.Scrollbar.flat(Color.ofArgb(0xAAFFFFFF)));
            container.child(scroll);
        }
    }

    // ================= ВКЛАДКА 1: СПАВНЕР МОБОВ =================
    private class MobSpawnerTab extends TabModule {
        public EntityType<?> configuringMob = null;
        private String mobCustomName = "";
        private boolean mobNameVisible = false;
        private boolean mobNoGravity = false;
        private boolean mobSilent = false;
        private boolean mobGlowing = false;
        private boolean mobIsBaby = false;
        private int mobSlimeSize = 0;
        private int mobFireTicks = 0;

        @Override
        public Text getTitle() { return Text.translatable("mogdops-mod.tab.mobs"); }

        @Override
        public void populateTab(FlowLayout container) {
            if (configuringMob != null) {
                buildMobConfiguratorLayout(container);
            } else {
                buildMobsGridWithTopBar(container);
            }
        }

        private void buildMobsGridWithTopBar(FlowLayout container) {
            FlowLayout topBar = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
            topBar.verticalAlignment(VerticalAlignment.CENTER);
            topBar.gap(8);
            topBar.margins(Insets.bottom(4));

            TextBoxComponent searchBox = Components.textBox(Sizing.fixed(140));
            searchBox.setPlaceholder(Text.translatable("mogdops-mod.search"));
            searchBox.setText(search);
            searchBox.onChanged().subscribe(text -> { search = text; rebuildTabUI(); });
            topBar.child(searchBox);

            Component categoryLabel = smallLabel(Text.literal(Text.translatable(categoryKeys[categoryIndex]).getString() + " ▼"), 0.75f, 0xFFFFFFFF);
            FlowLayout categoryBtn = createFlatButton(120, 18, categoryLabel, () -> {
                categoryIndex = (categoryIndex + 1) % categoryKeys.length;
                rebuildTabUI();
            });
            topBar.child(categoryBtn);

            CheckboxComponent miscToggle = Components.checkbox(Text.translatable("mogdops-mod.include_misc"));
            miscToggle.checked(showMisc);
            miscToggle.onChanged(state -> { showMisc = state; rebuildTabUI(); });
            topBar.child(miscToggle);

            container.child(topBar);

            FlowLayout grid = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
            grid.gap(6);

            String lowerFilter = search.toLowerCase();
            FlowLayout currentRow = null;
            int itemsInRow = 0;
            int maxPerRow = 7;

            for (EntityType<?> type : allSpawnableEntities) {
                SpawnGroup group = type.getSpawnGroup();
                if (group == SpawnGroup.MISC && !showMisc) continue;

                if (group != SpawnGroup.MISC) {
                    if (categoryIndex == 1 && group != SpawnGroup.MONSTER) continue;
                    if (categoryIndex == 2 && group != SpawnGroup.CREATURE) continue;
                    if (categoryIndex == 3 && (group == SpawnGroup.MONSTER || group == SpawnGroup.CREATURE)) continue;
                }

                String name = type.getName().getString();
                String id = Registries.ENTITY_TYPE.getId(type).getPath();
                if (!search.isEmpty() && !name.toLowerCase().contains(lowerFilter) && !id.toLowerCase().contains(lowerFilter)) continue;

                Entity entityToRender;
                try { entityToRender = type.create(MinecraftClient.getInstance().world); } catch (Exception e) { continue; }
                if (entityToRender == null) continue;

                EntityComponent<Entity> icon = Components.entity(Sizing.fixed(32), entityToRender);
                icon.allowMouseRotation(true); icon.lookAtCursor(true);

                Component card = createCard(icon, name, () -> {
                    NetworkManager.sendToServer(new SpawnEntityPayload(Registries.ENTITY_TYPE.getId(type).toString(), "", false, false, false, false, false, 0, 0));
                    SpawnerScreen.this.triggerSpawnEffect();
                }, (clickedCard) -> {
                    configuringMob = type;
                    rebuildTabUI();
                });

                if (currentRow == null || itemsInRow >= maxPerRow) {
                    if (currentRow != null) grid.child(currentRow);
                    currentRow = Containers.horizontalFlow(Sizing.content(), Sizing.content());
                    currentRow.gap(6);
                    itemsInRow = 0;
                }
                currentRow.child(card); itemsInRow++;
            }
            if (currentRow != null && itemsInRow > 0) grid.child(currentRow);

            ScrollContainer<FlowLayout> scroll = Containers.verticalScroll(Sizing.fill(100), Sizing.fill(100), grid);
            scroll.scrollbar(ScrollContainer.Scrollbar.flat(Color.ofArgb(0xAAFFFFFF)));
            container.child(scroll);
        }

        private void buildMobConfiguratorLayout(FlowLayout container) {
            container.child(createFlatButton(110, 18, Text.translatable("mogdops-mod.back_to_list"), () -> {
                configuringMob = null;
                rebuildTabUI();
            }).margins(Insets.bottom(6)));

            FlowLayout mainContainer = Containers.horizontalFlow(Sizing.fill(100), Sizing.fill(100));
            mainContainer.gap(12);

            FlowLayout leftCol = Containers.verticalFlow(Sizing.fixed(220), Sizing.content());
            leftCol.surface(Surface.flat(0x88222222));
            leftCol.padding(Insets.of(6));
            leftCol.gap(5);

            TextBoxComponent nameBox = Components.textBox(Sizing.fixed(90));
            nameBox.setText(mobCustomName);
            nameBox.onChanged().subscribe(t -> mobCustomName = t);
            leftCol.child(createLabelRow(Text.translatable("mogdops-mod.mob_editor.custom_name").getString(), nameBox));

            CheckboxComponent visibleCheck = Components.checkbox(Text.literal(""));
            visibleCheck.checked(mobNameVisible);
            visibleCheck.onChanged(s -> mobNameVisible = s);
            leftCol.child(createLabelRow(Text.translatable("mogdops-mod.mob_editor.name_visible").getString(), visibleCheck));

            CheckboxComponent gravityCheck = Components.checkbox(Text.literal(""));
            gravityCheck.checked(mobNoGravity);
            gravityCheck.onChanged(s -> mobNoGravity = s);
            leftCol.child(createLabelRow(Text.translatable("mogdops-mod.mob_editor.no_gravity").getString(), gravityCheck));

            CheckboxComponent silentCheck = Components.checkbox(Text.literal(""));
            silentCheck.checked(mobSilent);
            silentCheck.onChanged(s -> mobSilent = s);
            leftCol.child(createLabelRow(Text.translatable("mogdops-mod.mob_editor.silent").getString(), silentCheck));

            CheckboxComponent glowingCheck = Components.checkbox(Text.literal(""));
            glowingCheck.checked(mobGlowing);
            glowingCheck.onChanged(s -> mobGlowing = s);
            leftCol.child(createLabelRow(Text.translatable("mogdops-mod.mob_editor.glowing").getString(), glowingCheck));

            ScrollContainer<FlowLayout> leftScroll = Containers.verticalScroll(Sizing.fixed(220), Sizing.fill(100), leftCol);
            mainContainer.child(leftScroll);

            FlowLayout rightCol = Containers.verticalFlow(Sizing.fill(100), Sizing.fill(100));
            rightCol.surface(Surface.flat(0x88222222));
            rightCol.padding(Insets.of(6));
            rightCol.horizontalAlignment(HorizontalAlignment.CENTER);

            Entity entity;
            try { entity = configuringMob.create(MinecraftClient.getInstance().world); } catch (Exception e) { entity = null; }
            if (entity != null) {
                EntityComponent<Entity> entityComp = Components.entity(Sizing.fixed(70), entity);
                entityComp.allowMouseRotation(true); entityComp.lookAtCursor(true);
                rightCol.child(entityComp);
            }

            rightCol.child(createFlatButton(160, 20, Text.translatable("mogdops-mod.mob_editor.spawn_click"), () -> {
                NetworkManager.sendToServer(new SpawnEntityPayload(
                        Registries.ENTITY_TYPE.getId(configuringMob).toString(),
                        mobCustomName, mobNameVisible, mobNoGravity, mobSilent, mobGlowing, mobIsBaby, mobSlimeSize, mobFireTicks
                ));
                SpawnerScreen.this.triggerSpawnEffect();
            }).margins(Insets.top(6)));

            mainContainer.child(rightCol);
            container.child(mainContainer);
        }

        private FlowLayout createLabelRow(String labelText, Component input) {
            FlowLayout row = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
            row.verticalAlignment(VerticalAlignment.CENTER);
            row.gap(6);
            Component lbl = smallLabel(labelText, 0.74f, 0xFFDDDDDD);
            lbl.sizing(Sizing.fixed(110), Sizing.content());
            row.child(lbl).child(input);
            return row;
        }
    }

    // ================= ВКЛАДКА 2: КОНСТРУКТОР ПРЕДМЕТОВ =================
    private class ItemGiverTab extends TabModule {
        private Item configuringItem = null;
        private String itemCountText = "1";

        @Override public Text getTitle() { return Text.translatable("mogdops-mod.tab.items"); }

        @Override
        public void populateTab(FlowLayout container) {
            if (configuringItem != null) {
                buildConfiguratorLayout(container);
            } else {
                buildItemsGridWithTopBar(container);
            }
        }

        private void buildItemsGridWithTopBar(FlowLayout container) {
            TextBoxComponent searchBox = Components.textBox(Sizing.fixed(160));
            searchBox.setPlaceholder(Text.translatable("mogdops-mod.item_creator.search_item"));
            searchBox.setText(search);
            searchBox.onChanged().subscribe(text -> { search = text; rebuildTabUI(); });
            container.child(searchBox.margins(Insets.bottom(4)));

            FlowLayout grid = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
            grid.gap(6);

            String lowerFilter = search.toLowerCase();
            FlowLayout currentRow = null;
            int itemsInRow = 0;
            int maxPerRow = 7;

            // Собираем список предметов, помещая Творческий посох в самое начало
            List<Item> itemList = new ArrayList<>();
            itemList.add(MogDopSMod.STAFF.get());
            for (Item item : Registries.ITEM) {
                if (item != MogDopSMod.STAFF.get() && !(item instanceof BlockItem)) {
                    itemList.add(item);
                }
            }

            for (Item item : itemList) {
                String name = item.getName().getString();
                String id = Registries.ITEM.getId(item).getPath();
                if (!search.isEmpty() && !name.toLowerCase().contains(lowerFilter) && !id.toLowerCase().contains(lowerFilter)) continue;

                FlowLayout iconWrapper = Containers.horizontalFlow(Sizing.fixed(24), Sizing.fixed(24));
                iconWrapper.horizontalAlignment(HorizontalAlignment.CENTER);
                iconWrapper.verticalAlignment(VerticalAlignment.CENTER);
                iconWrapper.child(Components.item(new ItemStack(item)));

                Component card = createCard(iconWrapper, name, () -> {
                    configuringItem = item;
                    rebuildTabUI();
                }, null);

                if (currentRow == null || itemsInRow >= maxPerRow) {
                    if (currentRow != null) grid.child(currentRow);
                    currentRow = Containers.horizontalFlow(Sizing.content(), Sizing.content());
                    currentRow.gap(6);
                    itemsInRow = 0;
                }
                currentRow.child(card); itemsInRow++;
            }
            if (currentRow != null && itemsInRow > 0) grid.child(currentRow);

            ScrollContainer<FlowLayout> scroll = Containers.verticalScroll(Sizing.fill(100), Sizing.fill(100), grid);
            scroll.scrollbar(ScrollContainer.Scrollbar.flat(Color.ofArgb(0xAAFFFFFF)));
            container.child(scroll);
        }

        private void buildConfiguratorLayout(FlowLayout container) {
            container.child(createFlatButton(110, 18, Text.translatable("mogdops-mod.back_to_list"), () -> {
                configuringItem = null;
                rebuildTabUI();
            }).margins(Insets.bottom(6)));

            FlowLayout mainContainer = Containers.horizontalFlow(Sizing.fill(100), Sizing.fill(100));
            mainContainer.gap(12);

            FlowLayout rightCol = Containers.verticalFlow(Sizing.fill(100), Sizing.fill(100));
            rightCol.surface(Surface.flat(0x88222222));
            rightCol.padding(Insets.of(8));
            rightCol.horizontalAlignment(HorizontalAlignment.CENTER);

            rightCol.child(Components.item(new ItemStack(configuringItem)).margins(Insets.bottom(6)));
            rightCol.child(smallLabel(configuringItem.getName(), 0.80f, 0xFFFFAA00).margins(Insets.bottom(8)));

            FlowLayout countRow = Containers.horizontalFlow(Sizing.content(), Sizing.content());
            countRow.verticalAlignment(VerticalAlignment.CENTER);
            countRow.gap(8);
            countRow.child(smallLabel(Text.translatable("mogdops-mod.item_creator.count"), 0.75f, 0xFFDDDDDD));

            TextBoxComponent countBox = Components.textBox(Sizing.fixed(45));
            countBox.setTextPredicate(text -> text.matches("\\d*"));
            countBox.setText(itemCountText);
            countBox.onChanged().subscribe(t -> itemCountText = t);
            countRow.child(countBox);

            rightCol.child(countRow.margins(Insets.bottom(12)));

            rightCol.child(createFlatButton(160, 22, Text.translatable("mogdops-mod.item_creator.give_item"), () -> {
                int count = 1;
                try { count = Integer.parseInt(itemCountText); } catch (Exception ignored) {}
                ItemStack finalStack = new ItemStack(configuringItem, count);
                NetworkManager.sendToServer(new GiveItemPayload(finalStack));
                SpawnerScreen.this.triggerSpawnEffect();
                if (MinecraftClient.getInstance().player != null) {
                    MinecraftClient.getInstance().player.sendMessage(Text.literal("§a[MogDop] Выдано: " + configuringItem.getName().getString() + " x" + count), true);
                }
            }));

            mainContainer.child(rightCol);
            container.child(mainContainer);
        }
    }

    // ================= ВКЛАДКА 3: УТИЛИТЫ И ЧИТЫ =================
    private class UtilitiesTab extends TabModule {
        @Override
        public Text getTitle() { return Text.translatable("mogdops-mod.tab.utilities"); }

        @Override
        public void populateTab(FlowLayout container) {
            FlowLayout grid = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
            grid.gap(8);

            grid.child(smallLabel(Text.literal("★ WorldEdit Shortcuts"), 0.82f, 0xFF00C8FF));
            FlowLayout weRow = Containers.horizontalFlow(Sizing.content(), Sizing.content());
            weRow.gap(6);

            ItemStack staff = new ItemStack(MogDopSMod.STAFF.get());
            staff.set(DataComponentTypes.CUSTOM_NAME, Text.translatable("item.mogdops-mod.staff"));
            staff.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);

            weRow.child(createCard(Components.item(staff), Text.translatable("mogdops-mod.worldedit.get_staff").getString(), () -> {
                NetworkManager.sendToServer(new GiveItemPayload(staff));
                SpawnerScreen.this.triggerSpawnEffect();
                if (MinecraftClient.getInstance().player != null) {
                    MinecraftClient.getInstance().player.sendMessage(Text.literal("§a[MogDop] Творческий Посох добавлен в инвентарь!"), true);
                }
            }, null));

            weRow.child(createCard(Components.item(new ItemStack(Items.BRICKS)), Text.translatable("mogdops-mod.worldedit.fill").getString(), () -> {
                if (MogDopSModClient.getSelectionPoints().isEmpty()) {
                    MinecraftClient client = MinecraftClient.getInstance();
                    if (client.player != null) client.player.sendMessage(Text.translatable("mogdops-mod.error.positions_not_set"), false);
                } else {
                    String blockId = Registries.BLOCK.getId(MogDopSModClient.activeBlock).toString();
                    NetworkManager.sendToServer(new FillAreaPayload(MogDopSModClient.getSelectionPoints(), MogDopSModClient.currentSelectionMode, blockId));
                    SpawnerScreen.this.triggerSpawnEffect();
                }
            }, null));

            weRow.child(createCard(Components.item(new ItemStack(Items.FEATHER)), Text.translatable("mogdops-mod.worldedit.undo").getString(), () -> {
                NetworkManager.sendToServer(new UndoPayload());
                SpawnerScreen.this.triggerSpawnEffect();
            }, null));

            weRow.child(createCard(Components.item(new ItemStack(Items.GUNPOWDER)), Text.translatable("mogdops-mod.worldedit.redo").getString(), () -> {
                NetworkManager.sendToServer(new RedoPayload());
                SpawnerScreen.this.triggerSpawnEffect();
            }, null));

            grid.child(weRow);

            grid.child(smallLabel(Text.literal("★ World & Player Cheats"), 0.82f, 0xFFFFAA00).margins(Insets.top(4)));
            FlowLayout cheatRow = Containers.horizontalFlow(Sizing.content(), Sizing.content());
            cheatRow.gap(6);

            cheatRow.child(createCard(Components.item(new ItemStack(Items.SUNFLOWER)), Text.translatable("mogdops-mod.world.day").getString(), () -> NetworkManager.sendToServer(new WorldActionPayload("DAY")), null));
            cheatRow.child(createCard(Components.item(new ItemStack(Items.COAL)), Text.translatable("mogdops-mod.world.night").getString(), () -> NetworkManager.sendToServer(new WorldActionPayload("NIGHT")), null));
            cheatRow.child(createCard(Components.item(new ItemStack(Items.FEATHER)), Text.translatable("mogdops-mod.player.fly").getString(), () -> NetworkManager.sendToServer(new PlayerActionPayload("FLY")), null));
            cheatRow.child(createCard(Components.item(new ItemStack(Items.SUGAR)), Text.translatable("mogdops-mod.player.speed").getString(), () -> NetworkManager.sendToServer(new PlayerActionPayload("SPEED")), null));
            cheatRow.child(createCard(Components.item(new ItemStack(Items.MILK_BUCKET)), Text.translatable("mogdops-mod.player.clear").getString(), () -> NetworkManager.sendToServer(new PlayerActionPayload("CLEAR")), null));

            grid.child(cheatRow);

            ScrollContainer<FlowLayout> scroll = Containers.verticalScroll(Sizing.fill(100), Sizing.fill(100), grid);
            scroll.scrollbar(ScrollContainer.Scrollbar.flat(Color.ofArgb(0xAAFFFFFF)));
            container.child(scroll);
        }
    }

    // ================= ВКЛАДКА 4: NEXTBOTS (полноценный редактор пресета) =================
    private class NextbotsTab extends TabModule {
        private String fileName = "";
        private String audioName = "";
        private float speed = 0.3F;
        private float damage = 100.0F;
        private float size = 1.0F;
        private boolean forceCrop = false;
        private String status = "";
        private boolean draftLoaded = false;

        @Override
        public Text getTitle() { return Text.translatable("mogdops-mod.tab.nextbots"); }

        private void loadDraft() {
            if (draftLoaded) return;
            draftLoaded = true;
            if (MogDopSModClient.nextbotFileName != null) fileName = MogDopSModClient.nextbotFileName;
            if (MogDopSModClient.nextbotSpeed > 0) speed = MogDopSModClient.nextbotSpeed;
            if (MogDopSModClient.nextbotDamage > 0) damage = MogDopSModClient.nextbotDamage;
            if (MogDopSModClient.nextbotSize > 0) size = MogDopSModClient.nextbotSize;
            if (MogDopSModClient.nextbotAudio != null) audioName = MogDopSModClient.nextbotAudio;
            forceCrop = MogDopSModClient.nextbotForceCrop;
        }

        /** Перед уходом в браузер файлов — stash черновика в статики, чтобы пережить пересоздание экрана. */
        private void stashDraft() {
            MogDopSModClient.nextbotFileName = fileName;
            MogDopSModClient.nextbotAudio = audioName;
            MogDopSModClient.nextbotSpeed = speed;
            MogDopSModClient.nextbotDamage = damage;
            MogDopSModClient.nextbotSize = size;
            MogDopSModClient.nextbotForceCrop = forceCrop;
        }

        private void fail(String key, Object... args) {
            status = Text.translatable(key, args).getString();
            rebuildTabUI();
        }

        /** Валидация картинки 1:1 + аудио; возвращает итоговое имя картинки либо null. */
        private String validate() {
            String resolved = com.mogdop.mod.client.NextbotHelper.resolveTexture(fileName, forceCrop, err -> {
                if (err.startsWith("mogdops-mod.nextbot.err_notsquare|")) {
                    String[] parts = err.split("\\|");
                    status = Text.translatable("mogdops-mod.nextbot.err_notsquare",
                            parts.length > 1 ? parts[1] : "?", parts.length > 2 ? parts[2] : "?").getString();
                } else {
                    status = Text.translatable(err).getString();
                }
            });
            if (resolved == null) {
                rebuildTabUI();
                return null;
            }
            fileName = resolved;
            if (audioName != null && !audioName.isEmpty()
                    && !com.mogdop.mod.client.NextbotHelper.audioExists(audioName)) {
                status = Text.translatable("mogdops-mod.nextbot.err_noaudio", audioName).getString();
                rebuildTabUI();
                return null;
            }
            status = "";
            return resolved;
        }

        private void savePreset(String resolved) {
            MogDopSModClient.nextbotFileName = resolved;
            MogDopSModClient.nextbotAudio = audioName;
            MogDopSModClient.nextbotSpeed = speed;
            MogDopSModClient.nextbotDamage = damage;
            MogDopSModClient.nextbotSize = size;
            MogDopSModClient.nextbotForceCrop = forceCrop;
        }

        private void doSpawn(String resolved) {
            var player = MinecraftClient.getInstance().player;
            if (player == null) return;
            var hit = player.raycast(64.0, 1.0F, false);
            net.minecraft.util.math.Vec3d spawnPos;
            if (hit.getType() == net.minecraft.util.hit.HitResult.Type.BLOCK) {
                var blockHit = (net.minecraft.util.hit.BlockHitResult) hit;
                var side = blockHit.getSide();
                spawnPos = hit.getPos().add(side.getOffsetX() * 0.5, 0.1, side.getOffsetZ() * 0.5);
            } else {
                spawnPos = player.getEyePos().add(player.getRotationVec(1.0F).multiply(3.0));
            }
            NetworkManager.sendToServer(new SpawnNextbotPayload(resolved, spawnPos.x, spawnPos.y, spawnPos.z,
                    speed, damage, size, audioName == null ? "" : audioName));
            savePreset(resolved);
            SpawnerScreen.this.triggerSpawnEffect();
            player.sendMessage(Text.translatable("mogdops-mod.nextbot.spawned", resolved), true);
            rebuildTabUI();
        }

        private FlowLayout stepperRow(String labelKey, String valueText, Runnable dec, Runnable inc) {
            FlowLayout row = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
            row.verticalAlignment(VerticalAlignment.CENTER);
            row.gap(6);
            Component lbl = smallLabel(Text.translatable(labelKey), 0.74f, 0xFFDDDDDD);
            lbl.sizing(Sizing.fixed(90), Sizing.content());
            row.child(lbl);
            SmallLabelComponent val = smallLabel(valueText, 0.75f, 0xFFFFFFFF);
            val.sizing(Sizing.fixed(52), Sizing.content());
            FlowLayout decBtn = createFlatButton(16, 16, smallLabel("-", 0.75f, 0xFFFFFFFF), () -> {
                dec.run();
                val.text(Text.literal(currentStepperText(labelKey)));
            });
            FlowLayout incBtn = createFlatButton(16, 16, smallLabel("+", 0.75f, 0xFFFFFFFF), () -> {
                inc.run();
                val.text(Text.literal(currentStepperText(labelKey)));
            });
            row.child(decBtn).child(val).child(incBtn);
            return row;
        }

        private String currentStepperText(String labelKey) {
            return switch (labelKey) {
                case "mogdops-mod.nextbot.size" -> String.format(java.util.Locale.ROOT, "%.2f", size);
                case "mogdops-mod.nextbot.speed" -> String.format(java.util.Locale.ROOT, "%.2f", speed);
                case "mogdops-mod.nextbot.damage" -> String.format(java.util.Locale.ROOT, "%.0f", damage);
                default -> "";
            };
        }

        @Override
        public void populateTab(FlowLayout container) {
            loadDraft();
            FlowLayout content = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
            content.gap(6);
            content.child(smallLabel(Text.translatable("mogdops-mod.nextbot.tab_hint"), 0.74f, 0xFFCCCCCC));

            FlowLayout main = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
            main.gap(10);

            // ---- Слева: превью картинки ----
            FlowLayout previewCol = Containers.verticalFlow(Sizing.fixed(132), Sizing.content());
            previewCol.surface(Surface.flat(0x88222222));
            previewCol.padding(Insets.of(6));
            previewCol.gap(4);
            previewCol.horizontalAlignment(HorizontalAlignment.CENTER);
            if (fileName != null && !fileName.isEmpty()) {
                var info = com.mogdop.mod.client.render.ClientImageTextureManager.getTexture(fileName);
                if (info != null) {
                    FlowLayout imgBox = Containers.horizontalFlow(Sizing.fixed(112), Sizing.fixed(112));
                    imgBox.surface(Surface.flat(0xFF000000));
                    imgBox.horizontalAlignment(HorizontalAlignment.CENTER);
                    imgBox.verticalAlignment(VerticalAlignment.CENTER);
                    Component tex = Components.texture(info.id(), 0, 0, 112, 112, 112, 112);
                    tex.sizing(Sizing.fixed(112), Sizing.fixed(112));
                    imgBox.child(tex);
                    previewCol.child(imgBox);
                    boolean square = info.width() == info.height();
                    previewCol.child(smallLabel(info.width() + "x" + info.height(), 0.72f,
                            square ? 0xFF55FF55 : 0xFFFF5555).horizontalAlignment(HorizontalAlignment.CENTER));
                    if (!square) {
                        previewCol.child(smallLabel(Text.translatable("mogdops-mod.nextbot.need_square"),
                                0.68f, 0xFFFF5555).horizontalAlignment(HorizontalAlignment.CENTER));
                    }
                } else {
                    previewCol.child(smallLabel(Text.translatable("mogdops-mod.nextbot.err_load"), 0.72f, 0xFFFF5555));
                }
            } else {
                previewCol.child(smallLabel(Text.translatable("mogdops-mod.nextbot.no_preset"), 0.72f, 0xFFAAAAAA));
            }
            main.child(previewCol);

            // ---- Справа: файл, аудио, параметры ----
            FlowLayout controls = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
            controls.gap(5);

            FlowLayout fileRow = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
            fileRow.verticalAlignment(VerticalAlignment.CENTER);
            fileRow.gap(6);
            SmallLabelComponent fileLbl = smallLabel(
                    fileName == null || fileName.isEmpty() ? "—" : fileName, 0.72f, 0xFFFFAA00);
            fileLbl.sizing(Sizing.fill(60), Sizing.content());
            fileRow.child(fileLbl);
            fileRow.child(createFlatButton(80, 18, Text.translatable("mogdops-mod.image.browse"), () -> {
                stashDraft();
                MinecraftClient.getInstance().setScreen(new ImageSelectorScreen(selected -> {
                    if (selected != null) MogDopSModClient.nextbotFileName = selected;
                    com.mogdop.mod.client.render.ClientImageTextureManager.clearCache();
                    MinecraftClient.getInstance().setScreen(new SpawnerScreen(null, 4));
                }));
            }));
            fileRow.child(createFlatButton(30, 18, smallLabel("📁", 0.75f, 0xFFFFFFFF), () -> {
                var folder = com.mogdop.mod.client.NextbotHelper.getPicsFolder();
                net.minecraft.util.Util.getOperatingSystem().open(folder);
            }));
            controls.child(fileRow);

            FlowLayout audioRow = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
            audioRow.verticalAlignment(VerticalAlignment.CENTER);
            audioRow.gap(6);
            SmallLabelComponent audioLbl = smallLabel(
                    Text.translatable("mogdops-mod.nextbot.audio",
                            audioName == null || audioName.isEmpty()
                                    ? Text.translatable("mogdops-mod.nextbot.audio_none").getString() : audioName),
                    0.72f, 0xFF55FFFF);
            audioLbl.sizing(Sizing.fill(60), Sizing.content());
            audioRow.child(audioLbl);
            audioRow.child(createFlatButton(80, 18, Text.translatable("mogdops-mod.nextbot.browse_audio"), () -> {
                stashDraft();
                MinecraftClient.getInstance().setScreen(new AudioSelectorScreen(selected -> {
                    if (selected != null) MogDopSModClient.nextbotAudio = selected;
                    MinecraftClient.getInstance().setScreen(new SpawnerScreen(null, 4));
                }));
            }));
            audioRow.child(createFlatButton(30, 18, smallLabel("📁", 0.75f, 0xFFFFFFFF), () -> {
                net.minecraft.util.Util.getOperatingSystem().open(com.mogdop.mod.client.NextbotHelper.getAudioFolder());
            }));
            controls.child(audioRow);

            controls.child(stepperRow("mogdops-mod.nextbot.size",
                    String.format(java.util.Locale.ROOT, "%.2f", size),
                    () -> size = Math.max(0.25F, size - 0.1F),
                    () -> size = Math.min(3.0F, size + 0.1F)));
            controls.child(stepperRow("mogdops-mod.nextbot.speed",
                    String.format(java.util.Locale.ROOT, "%.2f", speed),
                    () -> speed = Math.max(0.05F, speed - 0.05F),
                    () -> speed = Math.min(1.0F, speed + 0.05F)));
            controls.child(stepperRow("mogdops-mod.nextbot.damage",
                    String.format(java.util.Locale.ROOT, "%.0f", damage),
                    () -> damage = Math.max(1.0F, damage - 10.0F),
                    () -> damage = Math.min(1000.0F, damage + 10.0F)));

            CheckboxComponent cropCheck = Components.checkbox(Text.translatable("mogdops-mod.nextbot.force_crop"));
            cropCheck.checked(forceCrop);
            cropCheck.onChanged(state -> forceCrop = state);
            controls.child(cropCheck);

            if (!status.isEmpty()) {
                controls.child(smallLabel(status, 0.72f, 0xFFFF5555));
            }

            FlowLayout btnRow = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
            btnRow.gap(6);
            btnRow.child(createFlatButton(150, 20, Text.translatable("mogdops-mod.nextbot.spawn"), () -> {
                String resolved = validate();
                if (resolved != null) doSpawn(resolved);
            }));
            controls.child(btnRow);
            main.child(controls);
            content.child(main);

            // ---- Последний пресет ----
            String lastPreset = MogDopSModClient.nextbotFileName == null || MogDopSModClient.nextbotFileName.isEmpty()
                    ? Text.translatable("mogdops-mod.nextbot.no_preset").getString()
                    : String.format(java.util.Locale.ROOT, "%s | %.2f | %.2f | %.0f | %s",
                            MogDopSModClient.nextbotFileName, MogDopSModClient.nextbotSize,
                            MogDopSModClient.nextbotSpeed, MogDopSModClient.nextbotDamage,
                            MogDopSModClient.nextbotAudio == null || MogDopSModClient.nextbotAudio.isEmpty()
                                    ? Text.translatable("mogdops-mod.nextbot.audio_none").getString()
                                    : MogDopSModClient.nextbotAudio);
            content.child(smallLabel(Text.translatable("mogdops-mod.nextbot.preset", lastPreset), 0.72f, 0xFFFFAA00));
            content.child(createFlatButton(220, 20, Text.translatable("mogdops-mod.nextbot.spawn_last"), () -> {
                String file = MogDopSModClient.nextbotFileName;
                if (file == null || file.isEmpty()) {
                    fail("mogdops-mod.nextbot.err_nofile");
                    return;
                }
                var info = com.mogdop.mod.client.render.ClientImageTextureManager.getTexture(file);
                if (info == null) {
                    fail("mogdops-mod.nextbot.err_load");
                    return;
                }
                if (info.width() != info.height()) {
                    fail("mogdops-mod.nextbot.err_notsquare", info.width(), info.height());
                    return;
                }
                speed = MogDopSModClient.nextbotSpeed;
                damage = MogDopSModClient.nextbotDamage;
                size = MogDopSModClient.nextbotSize;
                audioName = MogDopSModClient.nextbotAudio;
                fileName = file;
                status = "";
                doSpawn(file);
            }));

            ScrollContainer<FlowLayout> scroll = Containers.verticalScroll(Sizing.fill(100), Sizing.fill(100), content);
            scroll.scrollbar(ScrollContainer.Scrollbar.flat(Color.ofArgb(0xAAFFFFFF)));
            container.child(scroll);
        }
    }

    // ================= ВКЛАДКА 5: НАСТРОЙКИ (ВНИЗУ) =================
    private class SettingsTab extends TabModule {
        private TextBoxComponent colorFieldRef;
        private ColorPickerComponent colorPickerRef;
        private FlowLayout colorPreviewBox;

        @Override
        public Text getTitle() { return Text.literal("⚙ Настройки"); }

        private int parseHexColor(String hex) {
            if (hex.startsWith("#")) hex = hex.substring(1);
            try {
                int rgb = Integer.parseInt(hex, 16);
                return (0xFF << 24) | rgb;
            } catch (Exception e) {
                return 0xFFFFAA00;
            }
        }

        private void updateColorFromText(String val) {
            try {
                int colorInt = parseHexColor(val);
                if (colorPickerRef != null) colorPickerRef.selectedColor(Color.ofArgb(colorInt));
                if (colorPreviewBox != null) colorPreviewBox.surface(Surface.flat(colorInt));
            } catch (Exception ignored) {}
        }

        private FlowLayout createToggleRow(String key, boolean currentVal, boolean defaultVal, Consumer<Boolean> setter) {
            FlowLayout row = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
            row.verticalAlignment(VerticalAlignment.CENTER);
            row.gap(6);

            Component lbl = smallLabel(Text.translatable(key), 0.74f, 0xFFDDDDDD);
            lbl.sizing(Sizing.fixed(160), Sizing.content());
            row.child(lbl);

            CheckboxComponent check = Components.checkbox(Text.literal(""));
            check.checked(currentVal);
            check.onChanged(setter::accept);
            row.child(check);

            FlowLayout resetBtn = createFlatButton(16, 16, smallLabel("↺", 0.7f, 0xFFAAAAAA), () -> {
                check.checked(defaultVal);
                setter.accept(defaultVal);
            });
            row.child(resetBtn);

            return row;
        }

        @Override
        public void populateTab(FlowLayout container) {
            FlowLayout contentCol = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
            contentCol.gap(6);
            contentCol.padding(Insets.of(4));

            contentCol.child(smallLabel(Text.translatable("text.config.mogdops-mod"), 0.85f, 0xFFFFAA00));

            FlowLayout guideBtn = createFlatButton(200, 20, Text.translatable("text.config.mogdops-mod.open_guide"), () -> {
                MinecraftClient.getInstance().setScreen(new WelcomeScreen());
            });
            contentCol.child(guideBtn.margins(Insets.bottom(4)));

            contentCol.child(createToggleRow("text.config.mogdops-mod.option.hideChatHUD",
                    MogDopSModClient.CONFIG.hideChatHUD(), true, MogDopSModClient.CONFIG::hideChatHUD));

            contentCol.child(createToggleRow("text.config.mogdops-mod.option.enableCustomNotifications",
                    MogDopSModClient.CONFIG.enableCustomNotifications(), true, MogDopSModClient.CONFIG::enableCustomNotifications));

            contentCol.child(createToggleRow("text.config.mogdops-mod.option.vanillaSkin",
                    MogDopSModClient.CONFIG.vanillaSkin(), false, MogDopSModClient.CONFIG::vanillaSkin));

            contentCol.child(createToggleRow("text.config.mogdops-mod.option.enableSelectionAnimation",
                    MogDopSModClient.CONFIG.enableSelectionAnimation(), true, MogDopSModClient.CONFIG::enableSelectionAnimation));

            contentCol.child(createToggleRow("text.config.mogdops-mod.option.enableSelectionParticles",
                    MogDopSModClient.CONFIG.enableSelectionParticles(), true, MogDopSModClient.CONFIG::enableSelectionParticles));

            contentCol.child(createToggleRow("text.config.mogdops-mod.option.toolExplosionFire",
                    MogDopSModClient.CONFIG.toolExplosionFire(), false, MogDopSModClient.CONFIG::toolExplosionFire));

            FlowLayout radiusRow = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
            radiusRow.verticalAlignment(VerticalAlignment.CENTER);
            radiusRow.gap(6);
            Component radLbl = smallLabel(Text.translatable("text.config.mogdops-mod.option.toolRemoverRadius"), 0.74f, 0xFFDDDDDD);
            radLbl.sizing(Sizing.fixed(160), Sizing.content());
            radiusRow.child(radLbl);

            SmallLabelComponent radVal = smallLabel(String.valueOf(MogDopSModClient.CONFIG.toolRemoverRadius()), 0.75f, 0xFFFFFFFF);
            FlowLayout decRad = createFlatButton(16, 16, smallLabel("-", 0.75f, 0xFFFFFFFF), () -> {
                int nextVal = Math.max(1, MogDopSModClient.CONFIG.toolRemoverRadius() - 1);
                MogDopSModClient.CONFIG.toolRemoverRadius(nextVal);
                radVal.text(Text.literal(String.valueOf(nextVal)));
            });
            FlowLayout incRad = createFlatButton(16, 16, smallLabel("+", 0.75f, 0xFFFFFFFF), () -> {
                int nextVal = Math.min(16, MogDopSModClient.CONFIG.toolRemoverRadius() + 1);
                MogDopSModClient.CONFIG.toolRemoverRadius(nextVal);
                radVal.text(Text.literal(String.valueOf(nextVal)));
            });
            radiusRow.child(decRad).child(radVal).child(incRad);
            contentCol.child(radiusRow);

            FlowLayout powerRow = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
            powerRow.verticalAlignment(VerticalAlignment.CENTER);
            powerRow.gap(6);
            Component powLbl = smallLabel(Text.translatable("text.config.mogdops-mod.option.toolExplosionPower"), 0.74f, 0xFFDDDDDD);
            powLbl.sizing(Sizing.fixed(160), Sizing.content());
            powerRow.child(powLbl);

            SmallLabelComponent powVal = smallLabel(String.format("%.1f", MogDopSModClient.CONFIG.toolExplosionPower()), 0.75f, 0xFFFFFFFF);
            FlowLayout decPow = createFlatButton(16, 16, smallLabel("-", 0.75f, 0xFFFFFFFF), () -> {
                float nextVal = Math.max(1.0F, MogDopSModClient.CONFIG.toolExplosionPower() - 0.5F);
                MogDopSModClient.CONFIG.toolExplosionPower(nextVal);
                powVal.text(Text.literal(String.format("%.1f", nextVal)));
            });
            FlowLayout incPow = createFlatButton(16, 16, smallLabel("+", 0.75f, 0xFFFFFFFF), () -> {
                float nextVal = Math.min(50.0F, MogDopSModClient.CONFIG.toolExplosionPower() + 0.5F);
                MogDopSModClient.CONFIG.toolExplosionPower(nextVal);
                powVal.text(Text.literal(String.format("%.1f", nextVal)));
            });
            powerRow.child(decPow).child(powVal).child(incPow);
            contentCol.child(powerRow);

            FlowLayout colorRow = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
            colorRow.verticalAlignment(VerticalAlignment.CENTER);
            colorRow.gap(6);

            Component colorLabel = smallLabel(Text.translatable("text.config.mogdops-mod.option.toolSelectionColor"), 0.74f, 0xFFDDDDDD);
            colorLabel.sizing(Sizing.fixed(160), Sizing.content());
            colorRow.child(colorLabel);

            colorFieldRef = Components.textBox(Sizing.fixed(75));
            colorFieldRef.setText(MogDopSModClient.CONFIG.toolSelectionColor());
            colorFieldRef.onChanged().subscribe(val -> {
                MogDopSModClient.CONFIG.toolSelectionColor(val);
                updateColorFromText(val);
            });
            colorRow.child(colorFieldRef);

            FlowLayout resetColorBtn = createFlatButton(16, 16, smallLabel("↺", 0.7f, 0xFFAAAAAA), () -> {
                MogDopSModClient.CONFIG.toolSelectionColor("#FFAA00");
                colorFieldRef.setText("#FFAA00");
                updateColorFromText("#FFAA00");
            });
            colorRow.child(resetColorBtn);
            contentCol.child(colorRow);

            FlowLayout paletteBox = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
            paletteBox.gap(4);

            colorPickerRef = new ColorPickerComponent();
            colorPickerRef.sizing(Sizing.fixed(200), Sizing.fixed(80));
            colorPickerRef.showAlpha(false);
            colorPickerRef.selectedColor(Color.ofArgb(parseHexColor(MogDopSModClient.CONFIG.toolSelectionColor())));
            colorPickerRef.onChanged().subscribe(color -> {
                String hex = String.format("#%06X", (color.argb() & 0xFFFFFF));
                MogDopSModClient.CONFIG.toolSelectionColor(hex);
                if (colorFieldRef != null) colorFieldRef.setText(hex);
                if (colorPreviewBox != null) colorPreviewBox.surface(Surface.flat((0xFF << 24) | (color.argb() & 0xFFFFFF)));
            });
            paletteBox.child(colorPickerRef);

            colorPreviewBox = Containers.horizontalFlow(Sizing.fixed(200), Sizing.fixed(8));
            colorPreviewBox.surface(Surface.flat(parseHexColor(MogDopSModClient.CONFIG.toolSelectionColor())));
            paletteBox.child(colorPreviewBox);

            FlowLayout paletteRow = Containers.horizontalFlow(Sizing.content(), Sizing.content());
            paletteRow.gap(4);
            String[] hexSwatches = {"#FFAA00", "#00C8FF", "#FF5555", "#55FF55", "#AA00FF", "#FFFF55", "#FF55FF", "#FFFFFF"};
            for (String hex : hexSwatches) {
                int colorInt = parseHexColor(hex);
                FlowLayout swatch = Containers.horizontalFlow(Sizing.fixed(14), Sizing.fixed(14));
                swatch.surface(Surface.flat(colorInt));
                swatch.cursorStyle(CursorStyle.HAND);
                swatch.mouseDown().subscribe((mX, mY, button) -> {
                    if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                        MogDopSModClient.CONFIG.toolSelectionColor(hex);
                        colorFieldRef.setText(hex);
                        updateColorFromText(hex);
                        return true;
                    }
                    return false;
                });
                paletteRow.child(swatch);
            }
            paletteBox.child(paletteRow);
            contentCol.child(paletteBox);

            FlowLayout saveBtn = createFlatButton(140, 20, Text.translatable("text.config.mogdops-mod.save"), () -> {
                MogDopSModClient.CONFIG.save();
                if (SpawnerScreen.this.parent != null) {
                    MinecraftClient.getInstance().setScreen(SpawnerScreen.this.parent);
                } else {
                    MinecraftClient client = MinecraftClient.getInstance();
                    if (client.player != null) client.player.sendMessage(Text.literal("§a[Настройки] Конфигурация успешно сохранена!"), true);
                }
            });
            contentCol.child(saveBtn.margins(Insets.top(6)));

            ScrollContainer<FlowLayout> scroll = Containers.verticalScroll(Sizing.fill(100), Sizing.fill(100), contentCol);
            scroll.scrollbar(ScrollContainer.Scrollbar.flat(Color.ofArgb(0xAAFFFFFF)));
            container.child(scroll);
        }
    }
}