package com.mogdop.mod.client.gui;

import com.mogdop.mod.client.MogDopSModClient;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.component.CheckboxComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

public class ToolSelectorScreen extends BaseOwoScreen<FlowLayout> {

    private FlowLayout root;
    private FlowLayout rightConfigPanel;

    @Override
    public boolean shouldPause() { return false; }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (MogDopSModClient.openToolSelectorKey.matchesKey(keyCode, scanCode)) {
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
        rootComponent.surface(Surface.flat(0xCC141414));
        rootComponent.horizontalAlignment(HorizontalAlignment.CENTER);
        rootComponent.verticalAlignment(VerticalAlignment.CENTER);
        rootComponent.padding(Insets.of(20));

        LabelComponent title = Components.label(Text.translatable("mogdops-mod.tool_selector.title"));
        title.color(Color.ofArgb(0xFFFFAA00));
        title.margins(Insets.bottom(15));
        rootComponent.child(title);

        FlowLayout mainBox = Containers.horizontalFlow(Sizing.fixed(480), Sizing.fixed(240));
        mainBox.surface(Surface.flat(0xFA1A1A1A));
        mainBox.padding(Insets.of(10));
        mainBox.gap(15);

        FlowLayout leftPanel = Containers.verticalFlow(Sizing.fixed(160), Sizing.fill(100));
        leftPanel.gap(6);
        leftPanel.horizontalAlignment(HorizontalAlignment.CENTER);

        LabelComponent listTitle = Components.label(Text.translatable("mogdops-mod.tool_selector.modes"));
        listTitle.color(Color.ofArgb(0xFF55FFFF));
        listTitle.margins(Insets.bottom(5));
        leftPanel.child(listTitle);

        for (int i = 0; i < MogDopSModClient.TOOL_MODE_KEYS.length; i++) {
            FlowLayout btn = createModeButton(150, 24, MogDopSModClient.TOOL_MODE_KEYS[i], i);
            leftPanel.child(btn);
        }

        mainBox.child(leftPanel);

        rightConfigPanel = Containers.verticalFlow(Sizing.fill(100), Sizing.fill(100));
        rightConfigPanel.surface(Surface.flat(0xFF222222));
        rightConfigPanel.padding(Insets.of(12));
        rightConfigPanel.gap(10);

        mainBox.child(rightConfigPanel);
        rootComponent.child(mainBox);

        rebuildConfigPanel();
    }

    private FlowLayout createModeButton(int width, int height, String key, int modeIndex) {
        boolean isSelected = MogDopSModClient.currentToolMode == modeIndex;
        int color = isSelected ? 0xFF00AAFF : 0xFF333333;

        FlowLayout btn = Containers.horizontalFlow(Sizing.fixed(width), Sizing.fixed(height));
        btn.surface(Surface.flat(color));
        btn.cursorStyle(CursorStyle.HAND);
        btn.horizontalAlignment(HorizontalAlignment.CENTER);
        btn.verticalAlignment(VerticalAlignment.CENTER);

        LabelComponent lbl = Components.label(Text.translatable(key));
        lbl.color(Color.ofArgb(isSelected ? 0xFFFFFFFF : 0xFFAAAAAA));
        btn.child(lbl);

        btn.mouseEnter().subscribe(() -> {
            if (MogDopSModClient.currentToolMode != modeIndex) {
                btn.surface(Surface.flat(0xFF555555));
            }
        });
        btn.mouseLeave().subscribe(() -> {
            if (MogDopSModClient.currentToolMode != modeIndex) {
                btn.surface(Surface.flat(0xFF333333));
            } else {
                btn.surface(Surface.flat(0xFF00AAFF));
            }
        });
        btn.mouseDown().subscribe((mX, mY, button) -> {
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                MogDopSModClient.currentToolMode = modeIndex;
                if (MinecraftClient.getInstance().player != null) {
                    MinecraftClient.getInstance().player.sendMessage(Text.translatable("mogdops-mod.tool.mode_changed", Text.translatable(MogDopSModClient.TOOL_MODE_KEYS[modeIndex])), true);
                }

                root.clearChildren();
                build(root);
                return true;
            }
            return false;
        });

        return btn;
    }

    private void rebuildConfigPanel() {
        rightConfigPanel.clearChildren();
        int mode = MogDopSModClient.currentToolMode;

        LabelComponent title = Components.label(Text.translatable("mogdops-mod.tool_selector.properties", Text.translatable(MogDopSModClient.TOOL_MODE_KEYS[mode])));
        title.color(Color.ofArgb(0xFFFFAA00));
        title.margins(Insets.bottom(8));
        rightConfigPanel.child(title);

        switch (mode) {
            case 0 -> { // Выделение
                rightConfigPanel.child(Components.label(Text.translatable("mogdops-mod.tool_selector.mode.selection.desc")).color(Color.ofArgb(0xFFBBBBBB)));
                rightConfigPanel.child(Components.label(Text.translatable("mogdops-mod.tool_selector.mode.selection.lmb")).color(Color.ofArgb(0xFF55FF55)));
                rightConfigPanel.child(Components.label(Text.translatable("mogdops-mod.tool_selector.mode.selection.rmb")).color(Color.ofArgb(0xFF55FF55)));

                String notSet = Text.translatable("mogdops-mod.hud.not_set").getString();
                String p1Text = MogDopSModClient.pos1 == null ? notSet : MogDopSModClient.pos1.toShortString();
                String p2Text = MogDopSModClient.pos2 == null ? notSet : MogDopSModClient.pos2.toShortString();

                rightConfigPanel.child(Components.label(Text.translatable("mogdops-mod.tool_selector.pos1", p1Text)).margins(Insets.top(5)));
                rightConfigPanel.child(Components.label(Text.translatable("mogdops-mod.tool_selector.pos2", p2Text)));

                FlowLayout clearBtn = createFlatButton(140, 20, Text.translatable("mogdops-mod.tool_selector.reset_positions"), () -> {
                    MogDopSModClient.pos1 = null;
                    MogDopSModClient.pos2 = null;
                    rebuildConfigPanel();
                });
                rightConfigPanel.child(clearBtn.margins(Insets.top(10)));
            }
            case 1 -> { // Уничтожитель
                rightConfigPanel.child(Components.label(Text.translatable("mogdops-mod.tool_selector.mode.remover.desc")).color(Color.ofArgb(0xFFBBBBBB)));

                FlowLayout radiusRow = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
                radiusRow.verticalAlignment(VerticalAlignment.CENTER);
                radiusRow.gap(10);
                radiusRow.child(Components.label(Text.translatable("mogdops-mod.tool_selector.radius")));

                LabelComponent radiusVal = Components.label(Text.literal(String.valueOf(MogDopSModClient.CONFIG.toolRemoverRadius())));

                FlowLayout decRadius = createFlatButton(20, 20, Text.literal("-"), () -> {
                    int nextVal = Math.max(1, MogDopSModClient.CONFIG.toolRemoverRadius() - 1);
                    MogDopSModClient.CONFIG.toolRemoverRadius(nextVal);
                    radiusVal.text(Text.literal(String.valueOf(nextVal)));
                });

                FlowLayout incRadius = createFlatButton(20, 20, Text.literal("+"), () -> {
                    int nextVal = Math.min(16, MogDopSModClient.CONFIG.toolRemoverRadius() + 1);
                    MogDopSModClient.CONFIG.toolRemoverRadius(nextVal);
                    radiusVal.text(Text.literal(String.valueOf(nextVal)));
                });

                radiusRow.child(decRadius).child(radiusVal).child(incRadius);
                rightConfigPanel.child(radiusRow);
            }
            case 2 -> { // Взрыватель
                rightConfigPanel.child(Components.label(Text.translatable("mogdops-mod.tool_selector.mode.explosion.desc")).color(Color.ofArgb(0xFFBBBBBB)));

                FlowLayout powerRow = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
                powerRow.verticalAlignment(VerticalAlignment.CENTER);
                powerRow.gap(10);
                powerRow.child(Components.label(Text.translatable("mogdops-mod.tool_selector.power")));

                LabelComponent powerVal = Components.label(Text.literal(String.format("%.1f", MogDopSModClient.CONFIG.toolExplosionPower())));

                FlowLayout decPower = createFlatButton(20, 20, Text.literal("-"), () -> {
                    float nextVal = Math.max(1.0F, MogDopSModClient.CONFIG.toolExplosionPower() - 0.5F);
                    MogDopSModClient.CONFIG.toolExplosionPower(nextVal);
                    powerVal.text(Text.literal(String.format("%.1f", nextVal)));
                });

                FlowLayout incPower = createFlatButton(20, 20, Text.literal("+"), () -> {
                    float nextVal = Math.min(50.0F, MogDopSModClient.CONFIG.toolExplosionPower() + 0.5F);
                    MogDopSModClient.CONFIG.toolExplosionPower(nextVal);
                    powerVal.text(Text.literal(String.format("%.1f", nextVal)));
                });

                powerRow.child(decPower).child(powerVal).child(incPower);
                rightConfigPanel.child(powerRow);

                FlowLayout fireRow = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
                fireRow.verticalAlignment(VerticalAlignment.CENTER);
                fireRow.gap(10);
                fireRow.child(Components.label(Text.translatable("mogdops-mod.tool_selector.fire")));

                CheckboxComponent fireCheck = Components.checkbox(Text.literal(""));
                fireCheck.checked(MogDopSModClient.CONFIG.toolExplosionFire());
                fireCheck.onChanged(state -> MogDopSModClient.CONFIG.toolExplosionFire(state));
                fireRow.child(fireCheck);
                rightConfigPanel.child(fireRow);
            }
            case 3 -> { // Телепортер
                rightConfigPanel.child(Components.label(Text.translatable("mogdops-mod.tool_selector.mode.teleport.desc")).color(Color.ofArgb(0xFFBBBBBB)));
                rightConfigPanel.child(Components.label(Text.translatable("mogdops-mod.tool_selector.mode.teleport.range")).color(Color.ofArgb(0xAAFFFFFF)));
            }
            case 4 -> { // Спавнер
                rightConfigPanel.child(Components.label(Text.translatable("mogdops-mod.tool_selector.mode.spawner.desc")).color(Color.ofArgb(0xFFBBBBBB)));

                Identifier id = Identifier.tryParse(MogDopSModClient.activeSpawnId);
                String mobName = id != null ? Registries.ENTITY_TYPE.get(id).getName().getString() : "Cow";
                rightConfigPanel.child(Components.label(Text.translatable("mogdops-mod.tool_selector.selected_mob", mobName)));

                FlowLayout openSpawnerBtn = createFlatButton(180, 24, Text.translatable("mogdops-mod.tool_selector.open_spawner"), () -> {
                    this.close();
                    MinecraftClient.getInstance().setScreen(new SpawnerScreen());
                });
                rightConfigPanel.child(openSpawnerBtn.margins(Insets.top(10)));
            }
            case 5 -> { // Схематики
                rightConfigPanel.child(Components.label(Text.translatable("mogdops-mod.tool_selector.mode.schematics.desc")).color(Color.ofArgb(0xFFBBBBBB)));
                FlowLayout openSchemBtn = createFlatButton(180, 24, Text.translatable("mogdops-mod.schematic.title"), () -> {
                    this.close();
                    MinecraftClient.getInstance().setScreen(new SchematicScreen());
                });
                rightConfigPanel.child(openSchemBtn.margins(Insets.top(10)));
            }
            case 6 -> { // Изображения
                rightConfigPanel.child(Components.label(Text.translatable("mogdops-mod.tool_selector.mode.image.desc")).color(Color.ofArgb(0xFFBBBBBB)));
                FlowLayout openImgBtn = createFlatButton(180, 24, Text.translatable("mogdops-mod.image.title"), () -> {
                    this.close();
                    MinecraftClient.getInstance().setScreen(new ImageSelectorScreen());
                });
                rightConfigPanel.child(openImgBtn.margins(Insets.top(10)));
            }
        }
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