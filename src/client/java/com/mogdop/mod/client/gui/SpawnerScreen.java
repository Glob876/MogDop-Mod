package com.mogdop.mod.client.gui;

import com.mogdop.mod.client.MogDopSModClient;
import com.mogdop.mod.network.*;
import com.mojang.blaze3d.systems.RenderSystem;
import io.wispforest.owo.ui.base.BaseComponent;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.CheckboxComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.EntityComponent;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.component.TextBoxComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.core.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
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

    private FlowLayout root;
    private FlowLayout tabsSidebar;
    private FlowLayout tabContentWrapper;

    private final List<TabModule> tabs = new ArrayList<>();
    private TabModule currentTabModule;

    private final List<EntityType<?>> allSpawnableEntities = new ArrayList<>();
    private final List<Entity> nearbyEntities = new ArrayList<>();

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
        tabs.add(new MainMenuTab());
        tabs.add(new MobSpawnerTab());
        tabs.add(new ItemGiverTab());
        tabs.add(new UtilitiesTab());
        currentTabModule = tabs.get(0);
    }

    // Компонент изображения с режимом заполнения Cover (без искажения и дублирования)
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

            // Вычисляем масштаб для полного заполнения контейнера
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

            // Плавное затемнение снизу для мягкого перехода к тексту
            context.fillGradient(this.x, this.y + this.height - 35, this.x + this.width, this.y + this.height, 0x00101014, 0xEE101014);

            context.disableScissor();
        }

        @Override
        protected int determineHorizontalContentSize(Sizing sizing) {
            return 100;
        }

        @Override
        protected int determineVerticalContentSize(Sizing sizing) {
            return 100;
        }
    }

    // Компонент компактного текста с уменьшенным масштабом шрифта
    public static class SmallLabelComponent extends BaseComponent {
        private final Text text;
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

        // ================= 1. ЛЕВЫЙ САЙДБАР =================
        FlowLayout leftSidebar = Containers.verticalFlow(Sizing.fixed(110), Sizing.fill(100));
        leftSidebar.surface(Surface.flat(0xCC14141A));
        leftSidebar.padding(Insets.of(6));
        leftSidebar.gap(6);

        FlowLayout headerBox = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        headerBox.horizontalAlignment(HorizontalAlignment.CENTER);
        headerBox.margins(Insets.bottom(6));
        headerBox.child(smallLabel(Text.literal("MOGDOP'S MOD"), 0.85f, 0xFF00C8FF).horizontalAlignment(HorizontalAlignment.CENTER));
        headerBox.child(smallLabel(Text.literal("v0.2.0"), 0.65f, 0x88AAAAAA).horizontalAlignment(HorizontalAlignment.CENTER));
        leftSidebar.child(headerBox);

        tabsSidebar = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        tabsSidebar.gap(4);

        for (TabModule tab : tabs) {
            FlowLayout tabBtn = createSidebarTabButton(tab);
            tabsSidebar.child(tabBtn);
        }
        leftSidebar.child(tabsSidebar);

        windowBox.child(leftSidebar);

        // ================= 2. ПРАВАЯ ОБЛАСТЬ КОНТЕНТА =================
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
        if (tabsSidebar != null) {
            tabsSidebar.clearChildren();
            for (TabModule tab : tabs) {
                tabsSidebar.child(createSidebarTabButton(tab));
            }
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

    // ================= ВКЛАДКА 0: ГЛАВНОЕ МЕНЮ (Cover-обложка + скролл) =================
    private class MainMenuTab extends TabModule {
        @Override
        public Text getTitle() { return Text.translatable("mogdops-mod.tab.main"); }

        @Override
        public void populateTab(FlowLayout container) {
            container.gap(6);
            container.horizontalAlignment(HorizontalAlignment.CENTER);

            // Обложка с автоматическим заполнением Cover
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
                    ClientPlayNetworking.send(new SpawnEntityPayload(Registries.ENTITY_TYPE.getId(type).toString(), "", false, false, false, false, false, 0, 0));
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
                ClientPlayNetworking.send(new SpawnEntityPayload(
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
            int renderCount = 0;
            int maxPerRow = 7;

            for (Item item : Registries.ITEM) {
                if (item instanceof BlockItem) continue;

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

                renderCount++;
                if (renderCount >= 140) break;
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
                ClientPlayNetworking.send(new GiveItemPayload(finalStack));
                SpawnerScreen.this.triggerSpawnEffect();
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

            ItemStack axe = new ItemStack(Items.IRON_AXE);
            axe.set(DataComponentTypes.CUSTOM_NAME, Text.translatable("mogdops-mod.tool.selection_axe.name"));
            axe.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);

            weRow.child(createCard(Components.item(axe), Text.translatable("mogdops-mod.worldedit.get_axe").getString(), () -> {
                ClientPlayNetworking.send(new GiveItemPayload(axe));
                SpawnerScreen.this.triggerSpawnEffect();
            }, null));

            weRow.child(createCard(Components.item(new ItemStack(Items.BRICKS)), Text.translatable("mogdops-mod.worldedit.fill").getString(), () -> {
                if (MogDopSModClient.getSelectionPoints().isEmpty()) {
                    MinecraftClient client = MinecraftClient.getInstance();
                    if (client.player != null) client.player.sendMessage(Text.translatable("mogdops-mod.error.positions_not_set"), false);
                } else {
                    String blockId = Registries.BLOCK.getId(MogDopSModClient.activeBlock).toString();
                    ClientPlayNetworking.send(new FillAreaPayload(MogDopSModClient.getSelectionPoints(), MogDopSModClient.currentSelectionMode, blockId));
                    SpawnerScreen.this.triggerSpawnEffect();
                }
            }, null));

            weRow.child(createCard(Components.item(new ItemStack(Items.FEATHER)), Text.translatable("mogdops-mod.worldedit.undo").getString(), () -> {
                ClientPlayNetworking.send(new UndoPayload());
                SpawnerScreen.this.triggerSpawnEffect();
            }, null));

            weRow.child(createCard(Components.item(new ItemStack(Items.GUNPOWDER)), Text.translatable("mogdops-mod.worldedit.redo").getString(), () -> {
                ClientPlayNetworking.send(new RedoPayload());
                SpawnerScreen.this.triggerSpawnEffect();
            }, null));

            grid.child(weRow);

            grid.child(smallLabel(Text.literal("★ World & Player Cheats"), 0.82f, 0xFFFFAA00).margins(Insets.top(4)));
            FlowLayout cheatRow = Containers.horizontalFlow(Sizing.content(), Sizing.content());
            cheatRow.gap(6);

            cheatRow.child(createCard(Components.item(new ItemStack(Items.SUNFLOWER)), Text.translatable("mogdops-mod.world.day").getString(), () -> ClientPlayNetworking.send(new WorldActionPayload("DAY")), null));
            cheatRow.child(createCard(Components.item(new ItemStack(Items.COAL)), Text.translatable("mogdops-mod.world.night").getString(), () -> ClientPlayNetworking.send(new WorldActionPayload("NIGHT")), null));
            cheatRow.child(createCard(Components.item(new ItemStack(Items.FEATHER)), Text.translatable("mogdops-mod.player.fly").getString(), () -> ClientPlayNetworking.send(new PlayerActionPayload("FLY")), null));
            cheatRow.child(createCard(Components.item(new ItemStack(Items.SUGAR)), Text.translatable("mogdops-mod.player.speed").getString(), () -> ClientPlayNetworking.send(new PlayerActionPayload("SPEED")), null));
            cheatRow.child(createCard(Components.item(new ItemStack(Items.MILK_BUCKET)), Text.translatable("mogdops-mod.player.clear").getString(), () -> ClientPlayNetworking.send(new PlayerActionPayload("CLEAR")), null));

            grid.child(cheatRow);

            ScrollContainer<FlowLayout> scroll = Containers.verticalScroll(Sizing.fill(100), Sizing.fill(100), grid);
            scroll.scrollbar(ScrollContainer.Scrollbar.flat(Color.ofArgb(0xAAFFFFFF)));
            container.child(scroll);
        }
    }
}