package com.mogdop.mod.client.gui;

import com.mogdop.mod.client.MogDopSModClient;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.component.TextBoxComponent;
import io.wispforest.owo.ui.component.CheckboxComponent;
import io.wispforest.owo.ui.component.ColorPickerComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.core.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

public class MogDopConfigScreen extends BaseOwoScreen<FlowLayout> {

    private final Screen parent;
    private TextBoxComponent colorFieldRef;
    private ColorPickerComponent colorPickerRef;
    private FlowLayout colorPreviewBox;

    // Двухстадийное подтверждение отмены
    private boolean cancelConfirmStage = false;
    private long cancelStageTime = 0;
    private LabelComponent cancelLabelRef;

    public MogDopConfigScreen(@Nullable Screen parent) {
        this.parent = parent;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, Containers::verticalFlow);
    }

    @Override
    public void tick() {
        super.tick();
        if (cancelConfirmStage && (System.currentTimeMillis() - cancelStageTime) > 5000) {
            cancelConfirmStage = false;
            if (cancelLabelRef != null) {
                cancelLabelRef.text(Text.translatable("text.config.mogdops-mod.cancel"));
            }
        }
    }

    @Override
    protected void build(FlowLayout rootComponent) {
        rootComponent.surface(Surface.flat(0xCC141414));
        rootComponent.horizontalAlignment(HorizontalAlignment.CENTER);
        rootComponent.verticalAlignment(VerticalAlignment.CENTER);
        rootComponent.padding(Insets.of(15));

        // 1. ЗАГОЛОВОК ЭКРАНА
        LabelComponent title = Components.label(Text.translatable("text.config.mogdops-mod"));
        title.color(Color.ofArgb(0xFFFFAA00));
        title.margins(Insets.bottom(10));
        rootComponent.child(title);

        // 2. ЦЕНТРАЛЬНЫЙ БОКС С НАСТРОЙКАМИ (Компактный контейнер 480x180)
        FlowLayout mainBox = Containers.horizontalFlow(Sizing.fixed(480), Sizing.fixed(180));
        mainBox.surface(Surface.flat(0xFA1A1A1A));
        mainBox.padding(Insets.of(10));

        // ВЕРТИКАЛЬНАЯ ЛЕНТА НАСТРОЕК
        FlowLayout contentCol = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        contentCol.gap(10);

        // Кнопка переоткрытия Приветственного Руководства
        FlowLayout guideBtn = createFlatButton(220, 22, Text.translatable("text.config.mogdops-mod.open_guide"), () -> {
            MinecraftClient.getInstance().setScreen(new WelcomeScreen());
        });
        contentCol.child(guideBtn.margins(Insets.bottom(5)));

        // Опция 1: Скрывать стандартный чат (Дефолт: true)
        contentCol.child(createToggleRow("text.config.mogdops-mod.option.hideChatHUD",
                MogDopSModClient.CONFIG.hideChatHUD(), true,
                MogDopSModClient.CONFIG::hideChatHUD));

        // Опция 2: Показывать уведомления (Дефолт: true)
        contentCol.child(createToggleRow("text.config.mogdops-mod.option.enableCustomNotifications",
                MogDopSModClient.CONFIG.enableCustomNotifications(), true,
                MogDopSModClient.CONFIG::enableCustomNotifications));

        // Опция 3: Классическая тема UI (Дефолт: false)
        contentCol.child(createToggleRow("text.config.mogdops-mod.option.vanillaSkin",
                MogDopSModClient.CONFIG.vanillaSkin(), false,
                MogDopSModClient.CONFIG::vanillaSkin));

        // Опция 4: Плавная анимация расширения выделения (Дефолт: true)
        contentCol.child(createToggleRow("text.config.mogdops-mod.option.enableSelectionAnimation",
                MogDopSModClient.CONFIG.enableSelectionAnimation(), true,
                MogDopSModClient.CONFIG::enableSelectionAnimation));

        // Опция 5: Вспышки партиклов выделения (Дефолт: true)
        contentCol.child(createToggleRow("text.config.mogdops-mod.option.enableSelectionParticles",
                MogDopSModClient.CONFIG.enableSelectionParticles(), true,
                MogDopSModClient.CONFIG::enableSelectionParticles));

        // Опция 6: Цвет обводки выделения
        FlowLayout colorContainer = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        colorContainer.gap(6);

        FlowLayout colorRow = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
        colorRow.verticalAlignment(VerticalAlignment.CENTER);
        colorRow.gap(8);

        LabelComponent colorLabel = Components.label(Text.translatable("text.config.mogdops-mod.option.toolSelectionColor"));
        colorLabel.sizing(Sizing.fixed(200), Sizing.content());
        colorRow.child(colorLabel);

        colorFieldRef = Components.textBox(Sizing.fixed(90));
        colorFieldRef.setText(MogDopSModClient.CONFIG.toolSelectionColor());
        colorFieldRef.onChanged().subscribe(val -> {
            MogDopSModClient.CONFIG.toolSelectionColor(val);
            updateColorFromText(val);
        });
        colorRow.child(colorFieldRef);

        FlowLayout resetColorBtn = createFlatButton(18, 18, Text.literal("↺"), () -> {
            MogDopSModClient.CONFIG.toolSelectionColor("#FFAA00");
            colorFieldRef.setText("#FFAA00");
            updateColorFromText("#FFAA00");
        });
        colorRow.child(resetColorBtn);
        colorContainer.child(colorRow);

        FlowLayout paletteBox = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        paletteBox.gap(5);
        paletteBox.horizontalAlignment(HorizontalAlignment.LEFT);

        LabelComponent paletteTitle = Components.label(Text.translatable("text.config.mogdops-mod.palette"));
        paletteTitle.color(Color.ofArgb(0xFFFFAA00));
        paletteBox.child(paletteTitle);

        colorPickerRef = new ColorPickerComponent();
        colorPickerRef.sizing(Sizing.fixed(240), Sizing.fixed(110));
        colorPickerRef.showAlpha(false);
        colorPickerRef.selectedColor(Color.ofArgb(parseHexColor(MogDopSModClient.CONFIG.toolSelectionColor())));

        colorPickerRef.onChanged().subscribe(color -> {
            String hex = String.format("#%06X", (color.argb() & 0xFFFFFF));
            MogDopSModClient.CONFIG.toolSelectionColor(hex);
            if (colorFieldRef != null) {
                colorFieldRef.setText(hex);
            }
            if (colorPreviewBox != null) {
                colorPreviewBox.surface(Surface.flat((0xFF << 24) | (color.argb() & 0xFFFFFF)));
            }
        });
        paletteBox.child(colorPickerRef);

        colorPreviewBox = Containers.horizontalFlow(Sizing.fixed(240), Sizing.fixed(12));
        colorPreviewBox.surface(Surface.flat(parseHexColor(MogDopSModClient.CONFIG.toolSelectionColor())));
        paletteBox.child(colorPreviewBox);

        FlowLayout paletteRow = Containers.horizontalFlow(Sizing.content(), Sizing.content());
        paletteRow.gap(5);
        paletteRow.horizontalAlignment(HorizontalAlignment.LEFT);

        String[] hexSwatches = {
                "#FFAA00", "#00C8FF", "#FF5555", "#55FF55",
                "#AA00FF", "#FFFF55", "#FF55FF", "#FFFFFF"
        };

        for (String hex : hexSwatches) {
            int colorInt = parseHexColor(hex);
            FlowLayout swatch = Containers.horizontalFlow(Sizing.fixed(16), Sizing.fixed(16));
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

        colorContainer.child(paletteBox);
        contentCol.child(colorContainer);

        ScrollContainer<FlowLayout> scroll = Containers.verticalScroll(Sizing.fill(100), Sizing.fill(100), contentCol);
        scroll.scrollbar(ScrollContainer.Scrollbar.flat(Color.ofArgb(0xAAFFFFFF)));
        mainBox.child(scroll);

        rootComponent.child(mainBox);

        // 3. ФУТЕР
        FlowLayout footer = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
        footer.horizontalAlignment(HorizontalAlignment.CENTER);
        footer.margins(Insets.top(10));
        footer.gap(15);

        FlowLayout saveBtn = createFlatButton(120, 24, Text.translatable("text.config.mogdops-mod.save"), () -> {
            MogDopSModClient.CONFIG.save();
            this.close();
        });

        cancelLabelRef = Components.label(Text.translatable("text.config.mogdops-mod.cancel"));
        FlowLayout cancelBtn = createFlatButton(120, 24, cancelLabelRef, () -> {
            if (!cancelConfirmStage) {
                cancelConfirmStage = true;
                cancelStageTime = System.currentTimeMillis();
                cancelLabelRef.text(Text.translatable("text.config.mogdops-mod.cancel.sure"));
            } else {
                MogDopSModClient.CONFIG.load();
                this.close();
            }
        });

        footer.child(saveBtn).child(cancelBtn);
        rootComponent.child(footer);
    }

    private void updateColorFromText(String val) {
        try {
            int colorInt = parseHexColor(val);
            if (colorPickerRef != null) {
                colorPickerRef.selectedColor(Color.ofArgb(colorInt));
            }
            if (colorPreviewBox != null) {
                colorPreviewBox.surface(Surface.flat(colorInt));
            }
        } catch (Exception ignored) {}
    }

    private int parseHexColor(String hex) {
        if (hex.startsWith("#")) hex = hex.substring(1);
        try {
            int rgb = Integer.parseInt(hex, 16);
            return (0xFF << 24) | rgb;
        } catch (Exception e) {
            return 0xFFFFAA00;
        }
    }

    private FlowLayout createToggleRow(String key, boolean currentVal, boolean defaultVal, java.util.function.Consumer<Boolean> setter) {
        FlowLayout row = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
        row.verticalAlignment(VerticalAlignment.CENTER);
        row.gap(8);

        LabelComponent lbl = Components.label(Text.translatable(key));
        lbl.sizing(Sizing.fixed(200), Sizing.content());
        row.child(lbl);

        CheckboxComponent check = Components.checkbox(Text.literal(""));
        check.checked(currentVal);
        check.onChanged(setter);
        row.child(check);

        FlowLayout resetBtn = createFlatButton(18, 18, Text.literal("↺"), () -> {
            check.checked(defaultVal);
            setter.accept(defaultVal);
        });
        row.child(resetBtn);

        return row;
    }

    private FlowLayout createFlatButton(int width, int height, LabelComponent label, Runnable onClick) {
        FlowLayout btn = Containers.horizontalFlow(Sizing.fixed(width), Sizing.fixed(height));
        btn.surface(Surface.flat(0xFF444444));
        btn.cursorStyle(CursorStyle.HAND);
        btn.horizontalAlignment(HorizontalAlignment.CENTER);
        btn.verticalAlignment(VerticalAlignment.CENTER);
        btn.child(label);

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

    private FlowLayout createFlatButton(int width, int height, Text text, Runnable onClick) {
        return createFlatButton(width, height, Components.label(text), onClick);
    }

    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(this.parent);
    }
}