package com.mogdop.mod.client.gui;

import com.mogdop.mod.network.UpdateMobSpawnerSlabPayload;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.component.TextBoxComponent;
import io.wispforest.owo.ui.component.CheckboxComponent;
import io.wispforest.owo.ui.component.EntityComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.core.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

public class MobSpawnerSlabScreen extends BaseOwoScreen<FlowLayout> {

    private final BlockPos pos;

    private String mobId;
    private int intervalSeconds;
    private int maxMobs;
    private boolean active;
    private int spawnRange;

    private FlowLayout previewContainer;
    private TextBoxComponent mobFieldRef;

    public MobSpawnerSlabScreen(BlockPos pos, String mobId, int intervalTicks, int maxMobs, boolean active, int spawnRange) {
        this.pos = pos;
        this.mobId = mobId;
        this.intervalSeconds = intervalTicks / 20; // 20 тиков = 1 секунда
        this.maxMobs = maxMobs;
        this.active = active;
        this.spawnRange = spawnRange;
    }

    @Override
    public boolean shouldPause() { return false; }

    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, Containers::verticalFlow);
    }

    @Override
    protected void build(FlowLayout rootComponent) {
        rootComponent.surface(Surface.flat(0xCC141414));
        rootComponent.horizontalAlignment(HorizontalAlignment.CENTER);
        rootComponent.verticalAlignment(VerticalAlignment.CENTER);
        rootComponent.padding(Insets.of(20));

        LabelComponent title = Components.label(Text.literal("★ Настройка Спавнер-Полублока ★"));
        title.color(Color.ofArgb(0xFF00C8FF));
        title.margins(Insets.bottom(15));
        rootComponent.child(title);

        FlowLayout mainBox = Containers.horizontalFlow(Sizing.fixed(520), Sizing.fixed(240));
        mainBox.surface(Surface.flat(0xFA1A1A1A));
        mainBox.padding(Insets.of(12));
        mainBox.gap(15);

        // ЛЕВАЯ КОЛОНКА (Настройки параметров)
        FlowLayout leftCol = Containers.verticalFlow(Sizing.fixed(320), Sizing.content());
        leftCol.gap(10);

        // 1. Тумблер активации (Active)
        FlowLayout activeRow = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
        activeRow.verticalAlignment(VerticalAlignment.CENTER);
        activeRow.gap(10);
        LabelComponent activeLabel = Components.label(Text.literal("Запустить спавнер:"));
        activeLabel.sizing(Sizing.fixed(120), Sizing.content());
        activeRow.child(activeLabel);

        CheckboxComponent activeCheck = Components.checkbox(Text.literal(""));
        activeCheck.checked(active);
        activeCheck.onChanged(state -> this.active = state);
        activeRow.child(activeCheck);
        leftCol.child(activeRow);

        // 2. Текстовое поле ввода ID моба
        FlowLayout mobRow = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
        mobRow.verticalAlignment(VerticalAlignment.CENTER);
        mobRow.gap(10);
        LabelComponent mobLabel = Components.label(Text.literal("ID сущности:"));
        mobLabel.sizing(Sizing.fixed(120), Sizing.content());
        mobRow.child(mobLabel);

        mobFieldRef = Components.textBox(Sizing.fixed(150));
        mobFieldRef.setText(mobId);
        mobFieldRef.onChanged().subscribe(val -> {
            this.mobId = val;
            updatePreviewEntity();
        });
        mobRow.child(mobFieldRef);
        leftCol.child(mobRow);

        // 3. Точный ввод секунд интервала со скролл-кнопками +/-
        FlowLayout intervalRow = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
        intervalRow.verticalAlignment(VerticalAlignment.CENTER);
        intervalRow.gap(10);
        LabelComponent intLabel = Components.label(Text.literal("Интервал (сек):"));
        intLabel.sizing(Sizing.fixed(120), Sizing.content());
        intervalRow.child(intLabel);

        TextBoxComponent intField = Components.textBox(Sizing.fixed(40));
        intField.setTextPredicate(text -> text.matches("\\d*"));
        intField.setText(String.valueOf(intervalSeconds));
        intField.onChanged().subscribe(val -> {
            try {
                this.intervalSeconds = Math.max(1, Integer.parseInt(val));
            } catch (Exception ignored) {}
        });

        FlowLayout decInt = createFlatButton(18, 18, Text.literal("-"), () -> {
            this.intervalSeconds = Math.max(1, this.intervalSeconds - 1);
            intField.setText(String.valueOf(this.intervalSeconds));
        });
        FlowLayout incInt = createFlatButton(18, 18, Text.literal("+"), () -> {
            this.intervalSeconds = Math.min(3600, this.intervalSeconds + 1);
            intField.setText(String.valueOf(this.intervalSeconds));
        });
        intervalRow.child(decInt).child(intField).child(incInt);
        leftCol.child(intervalRow);

        // 4. Ограничение количества мобов
        FlowLayout maxRow = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
        maxRow.verticalAlignment(VerticalAlignment.CENTER);
        maxRow.gap(10);
        LabelComponent maxLabel = Components.label(Text.literal("Лимит сущностей:"));
        maxLabel.sizing(Sizing.fixed(120), Sizing.content());
        maxRow.child(maxLabel);

        LabelComponent maxVal = Components.label(Text.literal(String.valueOf(maxMobs)));
        FlowLayout decMax = createFlatButton(18, 18, Text.literal("-"), () -> {
            this.maxMobs = Math.max(1, this.maxMobs - 1);
            maxVal.text(Text.literal(String.valueOf(this.maxMobs)));
        });
        FlowLayout incMax = createFlatButton(18, 18, Text.literal("+"), () -> {
            this.maxMobs = Math.min(100, this.maxMobs + 1);
            maxVal.text(Text.literal(String.valueOf(this.maxMobs)));
        });
        maxRow.child(decMax).child(maxVal).child(incMax);
        leftCol.child(maxRow);

        // 5. Разброс спавна мобов (Radius)
        FlowLayout rangeRow = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
        rangeRow.verticalAlignment(VerticalAlignment.CENTER);
        rangeRow.gap(10);
        LabelComponent rangeLabel = Components.label(Text.literal("Разброс (радиус):"));
        rangeLabel.sizing(Sizing.fixed(120), Sizing.content());
        rangeRow.child(rangeLabel);

        LabelComponent rangeVal = Components.label(Text.literal(String.valueOf(spawnRange)));
        FlowLayout decRange = createFlatButton(18, 18, Text.literal("-"), () -> {
            this.spawnRange = Math.max(1, this.spawnRange - 1);
            rangeVal.text(Text.literal(String.valueOf(this.spawnRange)));
        });
        FlowLayout incRange = createFlatButton(18, 18, Text.literal("+"), () -> {
            this.spawnRange = Math.min(16, this.spawnRange + 1);
            rangeVal.text(Text.literal(String.valueOf(this.spawnRange)));
        });
        rangeRow.child(decRange).child(rangeVal).child(incRange);
        leftCol.child(rangeRow);

        mainBox.child(leftCol);

        // ПРАВАЯ КОЛОНКА (3D Предпросмотр моба + Кнопка выбора шаблона)
        previewContainer = Containers.verticalFlow(Sizing.fill(100), Sizing.fill(100));
        previewContainer.surface(Surface.flat(0xFF222222));
        previewContainer.padding(Insets.of(10));
        previewContainer.gap(5);
        previewContainer.horizontalAlignment(HorizontalAlignment.CENTER);
        previewContainer.verticalAlignment(VerticalAlignment.CENTER);
        mainBox.child(previewContainer);

        rootComponent.child(mainBox);

        // КНОПКА СОХРАНЕНИЯ ВНИЗУ
        FlowLayout footer = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
        footer.horizontalAlignment(HorizontalAlignment.CENTER);
        footer.margins(Insets.top(15));
        footer.gap(20);

        FlowLayout saveBtn = createFlatButton(120, 24, Text.literal("Сохранить"), () -> {
            ClientPlayNetworking.send(new UpdateMobSpawnerSlabPayload(pos, mobId, intervalSeconds * 20, maxMobs, active, spawnRange));
            this.close();
        });
        footer.child(saveBtn);
        rootComponent.child(footer);

        updatePreviewEntity();
    }

    // Метод динамического рендера 3D модели выбранного моба
    private void updatePreviewEntity() {
        previewContainer.clearChildren();
        
        Identifier id = Identifier.tryParse(this.mobId);
        if (id != null && Registries.ENTITY_TYPE.containsId(id)) {
            EntityType<?> type = Registries.ENTITY_TYPE.get(id);
            Entity entity = null;
            try {
                entity = type.create(MinecraftClient.getInstance().world);
            } catch (Exception ignored) {}

            if (entity != null) {
                EntityComponent<Entity> entityComp = Components.entity(Sizing.fixed(60), entity);
                entityComp.allowMouseRotation(true);
                entityComp.lookAtCursor(true);
                previewContainer.child(entityComp);
                
                // Отображаем локализованное имя под моделью
                previewContainer.child(Components.label(type.getName()).margins(Insets.top(4)));
            } else {
                previewContainer.child(Components.label(Text.literal("Ошибка загрузки")).color(Color.ofArgb(0xFFFF5555)));
            }
        } else {
            previewContainer.child(Components.label(Text.literal("Неизвестный моб")).color(Color.ofArgb(0xFFFF5555)));
        }

        // Кнопка переключения в меню выбора шаблонов
        previewContainer.child(createFlatButton(130, 20, Text.literal("Выбрать моба"), this::openPresetSelection).margins(Insets.top(8)));
    }

    // Окно выбора готовых популярных шаблонов мобов
    private void openPresetSelection() {
        previewContainer.clearChildren();
        
        LabelComponent selectLabel = Components.label(Text.literal("Шаблоны:"));
        selectLabel.color(Color.ofArgb(0xFFFFAA00));
        previewContainer.child(selectLabel);
        
        FlowLayout grid = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        grid.gap(4);
        grid.margins(Insets.top(5));
        
        String[][] presets = {
            {"Zombie", "minecraft:zombie"},
            {"Skeleton", "minecraft:skeleton"},
            {"Creeper", "minecraft:creeper"},
            {"Spider", "minecraft:spider"},
            {"Cow", "minecraft:cow"},
            {"Sheep", "minecraft:sheep"},
            {"Pig", "minecraft:pig"},
            {"Chicken", "minecraft:chicken"}
        };
        
        for (String[] preset : presets) {
            grid.child(createFlatButton(130, 16, Text.literal(preset[0]), () -> {
                this.mobId = preset[1];
                mobFieldRef.setText(this.mobId);
                updatePreviewEntity();
            }));
        }
        
        ScrollContainer<FlowLayout> presetScroll = Containers.verticalScroll(Sizing.fill(100), Sizing.fixed(120), grid);
        previewContainer.child(presetScroll);
        
        previewContainer.child(createFlatButton(130, 20, Text.literal("Назад"), this::updatePreviewEntity).margins(Insets.top(10)));
    }

    private FlowLayout createFlatButton(int width, int height, Text text, Runnable onClick) {
        FlowLayout btn = Containers.horizontalFlow(Sizing.fixed(width), Sizing.fixed(height));
        btn.surface(Surface.flat(0xFF444444));
        btn.cursorStyle(CursorStyle.HAND);
        btn.horizontalAlignment(HorizontalAlignment.CENTER);
        btn.verticalAlignment(VerticalAlignment.CENTER);
        
        LabelComponent lbl = Components.label(text);
        lbl.color(Color.ofArgb(0xFFFFFFFF));
        btn.child(lbl);

        btn.mouseEnter().subscribe(() -> btn.surface(Surface.flat(0xFF555555)));
        btn.mouseLeave().subscribe(() -> btn.surface(Surface.flat(0xFF444444)));
        btn.mouseDown().subscribe((mX, mY, button) -> {
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                onClick.run();
                return true;
            }
            return false;
        });

        return btn;
    }
}