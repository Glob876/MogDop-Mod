package com.mogdop.mod.client.gui;

import com.mogdop.mod.client.MogDopSModClient;
import com.mogdop.mod.network.*;
import dev.architectury.networking.NetworkManager;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.CheckboxComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.component.TextBoxComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.core.*;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class SchematicScreen extends BaseOwoScreen<FlowLayout> {

    public static final List<String> cachedSchematicsList = new ArrayList<>();

    private TextBoxComponent saveNameBox;
    private CheckboxComponent ignoreAirCheck;
    private FlowLayout filesListContainer;

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
        rootComponent.padding(Insets.of(15));

        LabelComponent title = Components.label(Text.translatable("mogdops-mod.schematic.title"));
        title.color(Color.ofArgb(0xFF00C8FF));
        title.margins(Insets.bottom(12));
        rootComponent.child(title);

        FlowLayout mainBox = Containers.horizontalFlow(Sizing.fixed(520), Sizing.fixed(220));
        mainBox.surface(Surface.flat(0xFA1A1A1A));
        mainBox.padding(Insets.of(10));
        mainBox.gap(15);

        FlowLayout leftCol = Containers.verticalFlow(Sizing.fixed(235), Sizing.fill(100));
        leftCol.gap(8);

        leftCol.child(Components.label(Text.translatable("mogdops-mod.schematic.save_header")).color(Color.ofArgb(0xFFFFAA00)));

        FlowLayout saveRow = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
        saveRow.verticalAlignment(VerticalAlignment.CENTER);
        saveRow.gap(6);

        saveNameBox = Components.textBox(Sizing.fixed(120));
        saveNameBox.setPlaceholder(Text.literal("my_house"));

        FlowLayout saveBtn = createFlatButton(95, 20, Text.translatable("mogdops-mod.schematic.btn_save"), () -> {
            String name = saveNameBox.getText().trim();
            if (!name.isEmpty()) {
                NetworkManager.sendToServer(new SaveSchematicPayload(
                        name,
                        MogDopSModClient.getSelectionPoints(),
                        MogDopSModClient.currentSelectionMode
                ));
            }
        });
        saveRow.child(saveNameBox).child(saveBtn);
        leftCol.child(saveRow);

        leftCol.child(Components.label(Text.translatable("mogdops-mod.schematic.controls_header")).color(Color.ofArgb(0xFF55FFFF)).margins(Insets.top(6)));

        FlowLayout transformRow = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
        transformRow.gap(8);

        FlowLayout rot90Btn = createFlatButton(105, 22, Text.translatable("mogdops-mod.schematic.btn_rot90"), () -> {
            NetworkManager.sendToServer(new RotateClipboardPayload(90));
            if (MogDopSModClient.schematicPreviewActive) {
                int tmp = MogDopSModClient.schematicSizeX;
                MogDopSModClient.schematicSizeX = MogDopSModClient.schematicSizeZ;
                MogDopSModClient.schematicSizeZ = tmp;
            }
        });
        FlowLayout rot180Btn = createFlatButton(105, 22, Text.translatable("mogdops-mod.schematic.btn_rot180"), () -> {
            NetworkManager.sendToServer(new RotateClipboardPayload(180));
        });
        transformRow.child(rot90Btn).child(rot180Btn);
        leftCol.child(transformRow);

        FlowLayout airRow = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
        airRow.verticalAlignment(VerticalAlignment.CENTER);
        airRow.gap(8);
        airRow.child(Components.label(Text.translatable("mogdops-mod.schematic.ignore_air")));
        ignoreAirCheck = Components.checkbox(Text.literal(""));
        ignoreAirCheck.checked(false);
        airRow.child(ignoreAirCheck);
        leftCol.child(airRow.margins(Insets.top(4)));

        FlowLayout pasteBtn = createFlatButton(220, 26, Text.translatable("mogdops-mod.schematic.btn_paste"), () -> {
            NetworkManager.sendToServer(new PasteClipboardPayload(ignoreAirCheck.isChecked()));
            this.close();
        });
        pasteBtn.surface(Surface.flat(0xFF00AA00));
        leftCol.child(pasteBtn.margins(Insets.top(8)));

        mainBox.child(leftCol);

        FlowLayout rightCol = Containers.verticalFlow(Sizing.fixed(250), Sizing.fill(100));
        rightCol.surface(Surface.flat(0xFF222222));
        rightCol.padding(Insets.of(8));
        rightCol.gap(6);

        FlowLayout rightHeader = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
        rightHeader.verticalAlignment(VerticalAlignment.CENTER);
        rightHeader.child(Components.label(Text.translatable("mogdops-mod.schematic.browser_title")).color(Color.ofArgb(0xFF00C8FF)));

        FlowLayout refreshBtn = createFlatButton(20, 16, Text.literal("↺"), () -> {
            NetworkManager.sendToServer(new RequestSchematicsListPayload());
        });
        rightHeader.child(refreshBtn.margins(Insets.left(10)));
        rightCol.child(rightHeader);

        filesListContainer = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        filesListContainer.gap(4);

        ScrollContainer<FlowLayout> filesScroll = Containers.verticalScroll(Sizing.fill(100), Sizing.fill(100), filesListContainer);
        filesScroll.scrollbar(ScrollContainer.Scrollbar.flat(Color.ofArgb(0xAAFFFFFF)));
        rightCol.child(filesScroll);

        mainBox.child(rightCol);
        rootComponent.child(mainBox);

        NetworkManager.sendToServer(new RequestSchematicsListPayload());
        rebuildFilesUI();
    }

    public void rebuildFilesUI() {
        if (filesListContainer == null) return;
        filesListContainer.clearChildren();

        if (cachedSchematicsList.isEmpty()) {
            filesListContainer.child(Components.label(Text.translatable("mogdops-mod.schematic.no_files")).color(Color.ofArgb(0x88FFFFFF)).margins(Insets.top(10)));
            return;
        }

        for (String filename : cachedSchematicsList) {
            FlowLayout row = Containers.horizontalFlow(Sizing.fill(100), Sizing.fixed(22));
            row.surface(Surface.flat(0xFF333333));
            row.padding(Insets.of(3));
            row.verticalAlignment(VerticalAlignment.CENTER);
            row.cursorStyle(CursorStyle.HAND);

            row.child(Components.item(new ItemStack(Items.PAPER)));

            LabelComponent nameLbl = Components.label(Text.literal(filename));
            nameLbl.color(Color.ofArgb(0xFFDDDDDD));
            nameLbl.sizing(Sizing.fixed(150), Sizing.content());
            row.child(nameLbl);

            FlowLayout loadBtn = createFlatButton(40, 16, Text.translatable("mogdops-mod.schematic.btn_load"), () -> {
                NetworkManager.sendToServer(new LoadSchematicPayload(filename));
                this.close();
            });
            row.child(loadBtn);

            row.mouseEnter().subscribe(() -> row.surface(Surface.flat(0xFF555555)));
            row.mouseLeave().subscribe(() -> row.surface(Surface.flat(0xFF333333)));

            filesListContainer.child(row);
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