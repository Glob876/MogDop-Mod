package com.mogdop.mod.client.gui;

import com.mogdop.mod.MogDopSMod;
import com.mogdop.mod.client.MogDopSModClient;
import com.mogdop.mod.network.*;
import com.mojang.blaze3d.systems.RenderSystem;
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
import net.minecraft.component.type.UnbreakableComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.Box;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.function.Consumer;

public class SpawnerScreen extends BaseOwoScreen<FlowLayout> {

    public static float uiOpacity = 0.95F;
    public static int accentColor = 0xFF1E1E1E;
    public static boolean vanillaSkin = false;

    private static final Identifier LOGO_TEXTURE = Identifier.of("mogdops-mod", "icon.png");

    private FlowLayout root;
    private FlowLayout tabsBar;
    private FlowLayout tabContentWrapper;

    private final List<TabModule> tabs = new ArrayList<>();
    private TabModule currentTabModule;

    private final List<EntityType<?>> allSpawnableEntities = new ArrayList<>();
    private final List<Entity> nearbyEntities = new ArrayList<>();

    private final List<FlowLayout> activeCards = new ArrayList<>();
    private final List<Component> activeIcons = new ArrayList<>();

    private boolean showMisc = false;
    private int categoryIndex = 0;

    private final String[] categoryKeys = {
            "mogdops-mod.category.all",
            "mogdops-mod.category.hostile",
            "mogdops-mod.category.passive",
            "mogdops-mod.category.ambient"
    };

    private int maxItemsPerRow = 6;
    private boolean stackedMode = false;
    private long lastSpawnTime = 0;

    public SpawnerScreen() {
        tabs.add(new MainMenuTab());
        tabs.add(new MobSpawnerTab());
        tabs.add(new ItemGiverTab());
        tabs.add(new UtilitiesTab());
        currentTabModule = tabs.get(0);
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

        // Главное окно на весь экран с ПРОЗРАЧНОЙ поверхностью (0x88101014)
        FlowLayout windowBox = Containers.verticalFlow(Sizing.fill(100), Sizing.fill(100));
        windowBox.surface(Surface.flat(0x88101014));
        windowBox.padding(Insets.of(12));
        windowBox.gap(8);

        // ================= 1. ВЕРХНИЙ БАР ВКЛАДОК =================
        tabsBar = Containers.horizontalFlow(Sizing.fill(100), Sizing.fixed(26));
        tabsBar.gap(6);
        tabsBar.verticalAlignment(VerticalAlignment.CENTER);

        for (TabModule tab : tabs) {
            FlowLayout tabBtn = createFlatButton(130, 24, Components.label(tab.getTitle()), () -> {
                this.currentTabModule = tab;
                tab.onSelected();
                rebuildTabUI();
            });
            tabsBar.child(tabBtn);
        }
        windowBox.child(tabsBar);

        // ================= 2. ОБЛАСТЬ КОНТЕНТА ВКЛАДКИ =================
        tabContentWrapper = Containers.verticalFlow(Sizing.fill(100), Sizing.fill(100));
        windowBox.child(tabContentWrapper);

        rootComponent.child(windowBox);
        rebuildTabUI();
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

        // Прозрачное мягкое затемнение заднего плана
        int bgAlpha = (int) (100 * uiAlpha * uiOpacity);
        context.fill(0, 0, this.width, this.height, (bgAlpha << 24) | 0x000000);

        // Четкая 1px окантовка вокруг экрана
        context.drawBorder(0, 0, this.width, this.height, 0xFF00C8FF);

        super.render(context, mouseX, mouseY, delta);

        context.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
    }

    private void scanNearbyEntities() {
        nearbyEntities.clear();
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;
        Box scanBox = client.player.getBoundingBox().expand(50.0D);
        List<Entity> list = client.world.getOtherEntities(client.player, scanBox, e -> e.isAlive() && !(e instanceof PlayerEntity));
        nearbyEntities.addAll(list);
    }

    private void rebuildTabUI() {
        if (tabsBar != null) {
            for (int i = 0; i < tabs.size(); i++) {
                FlowLayout btn = (FlowLayout) tabsBar.children().get(i);
                LabelComponent label = (LabelComponent) btn.children().get(0);
                if (tabs.get(i) == currentTabModule) {
                    btn.surface(vanillaSkin ? Surface.DARK_PANEL : Surface.flat(0xCC00C8FF));
                    label.color(Color.ofArgb(0xFFFFFFFF));
                } else {
                    btn.surface(vanillaSkin ? Surface.flat(0x33FFFFFF) : Surface.flat(0x88222222));
                    label.color(Color.ofArgb(0xFFAAAAAA));
                }
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
        var label = Components.label(Text.literal(labelText));
        label.horizontalTextAlignment(HorizontalAlignment.CENTER);

        FlowLayout labelWrapper = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
        labelWrapper.horizontalAlignment(HorizontalAlignment.CENTER);
        labelWrapper.padding(Insets.of(2));
        labelWrapper.child(label);

        FlowLayout card = Containers.verticalFlow(Sizing.fixed(80), Sizing.fixed(70));
        card.surface(Surface.flat(0xAA2A2A2A));
        card.horizontalAlignment(HorizontalAlignment.CENTER);
        card.verticalAlignment(VerticalAlignment.CENTER);
        card.padding(Insets.of(3));
        card.gap(4);
        card.cursorStyle(CursorStyle.HAND);
        card.child(icon).child(labelWrapper);

        card.mouseEnter().subscribe(() -> card.surface(Surface.flat(0xDD444444)));
        card.mouseLeave().subscribe(() -> card.surface(Surface.flat(0xAA2A2A2A)));
        card.mouseDown().subscribe((mX, mY, button) -> {
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && onLeftClick != null) { onLeftClick.run(); return true; }
            if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && onRightClick != null) { onRightClick.accept(card); return true; }
            return false;
        });

        return card;
    }

    public FlowLayout createFlatButton(int width, int height, LabelComponent label, Runnable onClick) {
        FlowLayout btn = Containers.horizontalFlow(Sizing.fixed(width), Sizing.fixed(height));
        btn.surface(Surface.flat(0xAA333333));
        btn.cursorStyle(CursorStyle.HAND);
        btn.horizontalAlignment(HorizontalAlignment.CENTER);
        btn.verticalAlignment(VerticalAlignment.CENTER);
        btn.child(label);

        btn.mouseEnter().subscribe(() -> btn.surface(Surface.flat(0xDD555555)));
        btn.mouseLeave().subscribe(() -> btn.surface(Surface.flat(0xAA333333)));
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
        return createFlatButton(width, height, Components.label(text), onClick);
    }

    // ================= ВКЛАДКА 0: ГЛАВНОЕ МЕНЮ (ПРОЗРАЧНЫЙ БАННЕР 16:9 + СКРОЛЛ) =================
    private class MainMenuTab extends TabModule {
        @Override
        public Text getTitle() { return Text.translatable("mogdops-mod.tab.main"); }

        @Override
        public void populateTab(FlowLayout container) {
            container.gap(10);
            container.horizontalAlignment(HorizontalAlignment.CENTER);

            // Прозрачный баннер 16:9
            FlowLayout bannerBox = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
            bannerBox.surface(Surface.flat(0x881E1E24));
            bannerBox.padding(Insets.of(6));
            bannerBox.verticalAlignment(VerticalAlignment.CENTER);
            bannerBox.horizontalAlignment(HorizontalAlignment.CENTER);

            Component bannerComp = Components.texture(LOGO_TEXTURE, 0, 0, 512, 288, 512, 288);
            bannerComp.sizing(Sizing.fixed(320), Sizing.fixed(180));

            bannerBox.child(bannerComp);
            container.child(bannerBox);

            // Скроллируемая полная информация о моде
            FlowLayout infoList = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
            infoList.gap(8);
            infoList.padding(Insets.of(4, 4, 12, 4));

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
                row.gap(8);
                row.child(Components.label(Text.literal("•")).color(Color.ofArgb(0xFF00C8FF)));
                LabelComponent txt = Components.label(Text.translatable(key));
                txt.color(Color.ofArgb(0xFFDDDDDD));
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
            topBar.gap(10);
            topBar.margins(Insets.bottom(6));

            TextBoxComponent searchBox = Components.textBox(Sizing.fixed(160));
            searchBox.setPlaceholder(Text.translatable("mogdops-mod.search"));
            searchBox.setText(search);
            searchBox.onChanged().subscribe(text -> { search = text; rebuildTabUI(); });
            topBar.child(searchBox);

            LabelComponent categoryLabel = Components.label(Text.literal(Text.translatable(categoryKeys[categoryIndex]).getString() + " ▼"));
            FlowLayout categoryBtn = createFlatButton(140, 20, categoryLabel, () -> {
                categoryIndex = (categoryIndex + 1) % categoryKeys.length;
                categoryLabel.text(Text.literal(Text.translatable(categoryKeys[categoryIndex]).getString() + " ▼"));
                rebuildTabUI();
            });
            topBar.child(categoryBtn);

            CheckboxComponent miscToggle = Components.checkbox(Text.translatable("mogdops-mod.include_misc"));
            miscToggle.checked(showMisc);
            miscToggle.onChanged(state -> { showMisc = state; rebuildTabUI(); });
            topBar.child(miscToggle);

            container.child(topBar);

            FlowLayout grid = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
            grid.gap(8);

            String lowerFilter = search.toLowerCase();
            FlowLayout currentRow = null;
            int itemsInRow = 0;

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

                EntityComponent<Entity> icon = Components.entity(Sizing.fixed(40), entityToRender);
                icon.allowMouseRotation(true); icon.lookAtCursor(true);

                Component card = createCard(icon, name, () -> {
                    ClientPlayNetworking.send(new SpawnEntityPayload(Registries.ENTITY_TYPE.getId(type).toString(), "", false, false, false, false, false, 0, 0));
                    SpawnerScreen.this.triggerSpawnEffect();
                }, (clickedCard) -> {
                    configuringMob = type;
                    rebuildTabUI();
                });

                if (currentRow == null || itemsInRow >= maxItemsPerRow) {
                    if (currentRow != null) grid.child(currentRow);
                    currentRow = Containers.horizontalFlow(Sizing.content(), Sizing.content());
                    currentRow.gap(10);
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
            container.child(createFlatButton(120, 20, Text.translatable("mogdops-mod.back_to_list"), () -> {
                configuringMob = null;
                rebuildTabUI();
            }).margins(Insets.bottom(6)));

            FlowLayout mainContainer = Containers.horizontalFlow(Sizing.fill(100), Sizing.fill(100));
            mainContainer.gap(15);

            FlowLayout leftCol = Containers.verticalFlow(Sizing.fixed(260), Sizing.content());
            leftCol.surface(Surface.flat(0x88222222));
            leftCol.padding(Insets.of(8));
            leftCol.gap(6);

            TextBoxComponent nameBox = Components.textBox(Sizing.fixed(100));
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

            ScrollContainer<FlowLayout> leftScroll = Containers.verticalScroll(Sizing.fixed(260), Sizing.fill(100), leftCol);
            mainContainer.child(leftScroll);

            FlowLayout rightCol = Containers.verticalFlow(Sizing.fill(100), Sizing.fill(100));
            rightCol.surface(Surface.flat(0x88222222));
            rightCol.padding(Insets.of(8));
            rightCol.horizontalAlignment(HorizontalAlignment.CENTER);

            Entity entity;
            try { entity = configuringMob.create(MinecraftClient.getInstance().world); } catch (Exception e) { entity = null; }
            if (entity != null) {
                EntityComponent<Entity> entityComp = Components.entity(Sizing.fixed(80), entity);
                entityComp.allowMouseRotation(true); entityComp.lookAtCursor(true);
                rightCol.child(entityComp);
            }

            rightCol.child(createFlatButton(180, 22, Text.translatable("mogdops-mod.mob_editor.spawn_click"), () -> {
                ClientPlayNetworking.send(new SpawnEntityPayload(
                        Registries.ENTITY_TYPE.getId(configuringMob).toString(),
                        mobCustomName, mobNameVisible, mobNoGravity, mobSilent, mobGlowing, mobIsBaby, mobSlimeSize, mobFireTicks
                ));
                SpawnerScreen.this.triggerSpawnEffect();
            }).margins(Insets.top(8)));

            mainContainer.child(rightCol);
            container.child(mainContainer);
        }

        private FlowLayout createLabelRow(String labelText, Component input) {
            FlowLayout row = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
            row.verticalAlignment(VerticalAlignment.CENTER);
            row.gap(8);
            LabelComponent lbl = Components.label(Text.literal(labelText));
            lbl.sizing(Sizing.fixed(130), Sizing.content());
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
            TextBoxComponent searchBox = Components.textBox(Sizing.fixed(180));
            searchBox.setPlaceholder(Text.translatable("mogdops-mod.item_creator.search_item"));
            searchBox.setText(search);
            searchBox.onChanged().subscribe(text -> { search = text; rebuildTabUI(); });
            container.child(searchBox.margins(Insets.bottom(6)));

            FlowLayout grid = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
            grid.gap(8);

            String lowerFilter = search.toLowerCase();
            FlowLayout currentRow = null;
            int itemsInRow = 0;
            int renderCount = 0;

            for (Item item : Registries.ITEM) {
                if (item instanceof BlockItem) continue;

                String name = item.getName().getString();
                String id = Registries.ITEM.getId(item).getPath();
                if (!search.isEmpty() && !name.toLowerCase().contains(lowerFilter) && !id.toLowerCase().contains(lowerFilter)) continue;

                FlowLayout iconWrapper = Containers.horizontalFlow(Sizing.fixed(32), Sizing.fixed(32));
                iconWrapper.horizontalAlignment(HorizontalAlignment.CENTER);
                iconWrapper.verticalAlignment(VerticalAlignment.CENTER);
                iconWrapper.child(Components.item(new ItemStack(item)));

                Component card = createCard(iconWrapper, name, () -> {
                    configuringItem = item;
                    rebuildTabUI();
                }, null);

                if (currentRow == null || itemsInRow >= 6) {
                    if (currentRow != null) grid.child(currentRow);
                    currentRow = Containers.horizontalFlow(Sizing.content(), Sizing.content());
                    currentRow.gap(10);
                    itemsInRow = 0;
                }
                currentRow.child(card); itemsInRow++;

                renderCount++;
                if (renderCount >= 120) break;
            }
            if (currentRow != null && itemsInRow > 0) grid.child(currentRow);

            ScrollContainer<FlowLayout> scroll = Containers.verticalScroll(Sizing.fill(100), Sizing.fill(100), grid);
            scroll.scrollbar(ScrollContainer.Scrollbar.flat(Color.ofArgb(0xAAFFFFFF)));
            container.child(scroll);
        }

        private void buildConfiguratorLayout(FlowLayout container) {
            container.child(createFlatButton(120, 20, Text.translatable("mogdops-mod.back_to_list"), () -> {
                configuringItem = null;
                rebuildTabUI();
            }).margins(Insets.bottom(6)));

            FlowLayout mainContainer = Containers.horizontalFlow(Sizing.fill(100), Sizing.fill(100));
            mainContainer.gap(15);

            FlowLayout rightCol = Containers.verticalFlow(Sizing.fill(100), Sizing.fill(100));
            rightCol.surface(Surface.flat(0x88222222));
            rightCol.padding(Insets.of(10));
            rightCol.horizontalAlignment(HorizontalAlignment.CENTER);

            rightCol.child(Components.item(new ItemStack(configuringItem)).margins(Insets.bottom(10)));
            rightCol.child(Components.label(configuringItem.getName()).margins(Insets.bottom(10)));

            FlowLayout countRow = Containers.horizontalFlow(Sizing.content(), Sizing.content());
            countRow.verticalAlignment(VerticalAlignment.CENTER);
            countRow.gap(10);
            countRow.child(Components.label(Text.translatable("mogdops-mod.item_creator.count")));

            TextBoxComponent countBox = Components.textBox(Sizing.fixed(50));
            countBox.setTextPredicate(text -> text.matches("\\d*"));
            countBox.setText(itemCountText);
            countBox.onChanged().subscribe(t -> itemCountText = t);
            countRow.child(countBox);

            rightCol.child(countRow.margins(Insets.bottom(15)));

            rightCol.child(createFlatButton(180, 24, Text.translatable("mogdops-mod.item_creator.give_item"), () -> {
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
            grid.gap(12);

            grid.child(Components.label(Text.literal("★ WorldEdit Shortcuts")).color(Color.ofArgb(0xFF00C8FF)));
            FlowLayout weRow = Containers.horizontalFlow(Sizing.content(), Sizing.content());
            weRow.gap(10);

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

            grid.child(Components.label(Text.literal("★ World & Player Cheats")).color(Color.ofArgb(0xFFFFAA00)).margins(Insets.top(6)));
            FlowLayout cheatRow = Containers.horizontalFlow(Sizing.content(), Sizing.content());
            cheatRow.gap(10);

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