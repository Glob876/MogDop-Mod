package com.mogdop.mod.client.gui;

import com.mogdop.mod.client.MogDopSModClient;
import com.mogdop.mod.client.render.ClientImageTextureManager;
import com.mogdop.mod.network.SpawnImagePayload;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.component.TextBoxComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.core.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ImageSelectorScreen extends BaseOwoScreen<FlowLayout> {

    private String searchFilter = "";
    private String selectedFilename = null;

    private FlowLayout fileListContainer;
    private FlowLayout rightPreviewPanel;

    @Override
    public boolean shouldPause() {
        return false;
    }

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

        // 1. Заголовок экрана
        LabelComponent title = Components.label(Text.translatable("mogdops-mod.image.title"));
        title.color(Color.ofArgb(0xFF00C8FF));
        title.margins(Insets.bottom(12));
        rootComponent.child(title);

        // 2. Главный бокс (520x240px)
        FlowLayout mainBox = Containers.horizontalFlow(Sizing.fixed(520), Sizing.fixed(240));
        mainBox.surface(Surface.flat(0xFA1A1A1A));
        mainBox.padding(Insets.of(10));
        mainBox.gap(15);

        // ================= ЛЕВАЯ КОЛОНКА (Список файлов + Поиск + Кнопка Открыть папку) =================
        FlowLayout leftCol = Containers.verticalFlow(Sizing.fixed(235), Sizing.fill(100));
        leftCol.gap(8);

        TextBoxComponent searchBox = Components.textBox(Sizing.fill(100));
        searchBox.setPlaceholder(Text.translatable("mogdops-mod.image.search"));
        searchBox.setText(searchFilter);
        searchBox.onChanged().subscribe(text -> {
            this.searchFilter = text;
            rebuildFileList();
        });
        leftCol.child(searchBox);

        FlowLayout btnRow = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
        btnRow.gap(6);

        FlowLayout openFolderBtn = createFlatButton(130, 20, Text.translatable("mogdops-mod.image.open_folder"), () -> {
            File folder = FabricLoader.getInstance().getConfigDir().resolve("pics").toFile();
            if (!folder.exists()) folder.mkdirs();
            Util.getOperatingSystem().open(folder);
        });

        FlowLayout refreshBtn = createFlatButton(95, 20, Text.translatable("mogdops-mod.image.refresh"), () -> {
            ClientImageTextureManager.clearCache();
            rebuildFileList();
            updatePreviewPanel();
        });

        btnRow.child(openFolderBtn).child(refreshBtn);
        leftCol.child(btnRow);

        fileListContainer = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        fileListContainer.gap(4);

        ScrollContainer<FlowLayout> filesScroll = Containers.verticalScroll(Sizing.fill(100), Sizing.fill(100), fileListContainer);
        filesScroll.scrollbar(ScrollContainer.Scrollbar.flat(Color.ofArgb(0xAAFFFFFF)));
        leftCol.child(filesScroll);

        mainBox.child(leftCol);

        // ================= ПРАВАЯ КОЛОНКА (Предпросмотр + Статус + Кнопка Разместить) =================
        rightPreviewPanel = Containers.verticalFlow(Sizing.fixed(250), Sizing.fill(100));
        rightPreviewPanel.surface(Surface.flat(0xFF222222));
        rightPreviewPanel.padding(Insets.of(8));
        rightPreviewPanel.gap(8);
        rightPreviewPanel.horizontalAlignment(HorizontalAlignment.CENTER);

        mainBox.child(rightPreviewPanel);
        rootComponent.child(mainBox);

        rebuildFileList();
        updatePreviewPanel();
    }

    private void rebuildFileList() {
        if (fileListContainer == null) return;
        fileListContainer.clearChildren();

        List<String> imageFiles = getImageFiles();
        String lowerFilter = searchFilter.toLowerCase();

        List<String> filtered = new ArrayList<>();
        for (String f : imageFiles) {
            if (searchFilter.isEmpty() || f.toLowerCase().contains(lowerFilter)) {
                filtered.add(f);
            }
        }

        if (filtered.isEmpty()) {
            fileListContainer.child(Components.label(Text.translatable("mogdops-mod.image.no_files")).color(Color.ofArgb(0x88FFFFFF)).margins(Insets.top(10)));
            return;
        }

        for (String filename : filtered) {
            boolean isSelected = filename.equals(selectedFilename);
            FlowLayout row = Containers.horizontalFlow(Sizing.fill(100), Sizing.fixed(22));
            row.surface(Surface.flat(isSelected ? 0xFF00AAFF : 0xFF333333));
            row.padding(Insets.of(3));
            row.verticalAlignment(VerticalAlignment.CENTER);
            row.cursorStyle(CursorStyle.HAND);

            row.child(Components.item(new ItemStack(Items.PAINTING)));

            LabelComponent nameLbl = Components.label(Text.literal(filename));
            nameLbl.color(Color.ofArgb(isSelected ? 0xFFFFFFFF : 0xFFDDDDDD));
            nameLbl.sizing(Sizing.fill(100), Sizing.content());
            row.child(nameLbl);

            row.mouseEnter().subscribe(() -> {
                if (!filename.equals(selectedFilename)) {
                    row.surface(Surface.flat(0xFF555555));
                }
            });
            row.mouseLeave().subscribe(() -> {
                if (!filename.equals(selectedFilename)) {
                    row.surface(Surface.flat(0xFF333333));
                } else {
                    row.surface(Surface.flat(0xFF00AAFF));
                }
            });
            row.mouseDown().subscribe((mX, mY, button) -> {
                if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                    this.selectedFilename = filename;
                    rebuildFileList();
                    updatePreviewPanel();
                    return true;
                }
                return false;
            });

            fileListContainer.child(row);
        }
    }

    private void updatePreviewPanel() {
        if (rightPreviewPanel == null) return;
        rightPreviewPanel.clearChildren();

        LabelComponent title = Components.label(Text.literal("Предпросмотр:"));
        title.color(Color.ofArgb(0xFF00C8FF));
        rightPreviewPanel.child(title);

        if (selectedFilename == null || selectedFilename.isEmpty()) {
            rightPreviewPanel.child(Components.label(Text.literal("Выберите изображение из списка")).color(Color.ofArgb(0x88FFFFFF)).margins(Insets.top(20)));
        } else {
            ClientImageTextureManager.ImageTextureInfo info = ClientImageTextureManager.getTexture(selectedFilename);
            if (info != null) {
                LabelComponent nameLbl = Components.label(Text.literal(selectedFilename + " (" + info.width() + "x" + info.height() + ")"));
                nameLbl.color(Color.ofArgb(0xFFFFAA00));
                rightPreviewPanel.child(nameLbl);

                int maxBoxW = 200;
                int maxBoxH = 110;
                float aspect = (float) info.width() / (float) info.height();

                int renderW = maxBoxW;
                int renderH = (int) (maxBoxW / aspect);
                if (renderH > maxBoxH) {
                    renderH = maxBoxH;
                    renderW = (int) (maxBoxH * aspect);
                }

                FlowLayout imgBox = Containers.horizontalFlow(Sizing.fixed(renderW), Sizing.fixed(renderH));
                imgBox.surface(Surface.flat(0xFF000000));
                imgBox.horizontalAlignment(HorizontalAlignment.CENTER);
                imgBox.verticalAlignment(VerticalAlignment.CENTER);

                Component texComp = Components.texture(info.id(), 0, 0, info.width(), info.height(), info.width(), info.height());
                texComp.sizing(Sizing.fixed(renderW), Sizing.fixed(renderH));
                imgBox.child(texComp);

                rightPreviewPanel.child(imgBox.margins(Insets.vertical(4)));
            } else {
                rightPreviewPanel.child(Components.label(Text.literal("Ошибка загрузки изображения")).color(Color.ofArgb(0xFFFF5555)));
            }
        }

        Vec3d p1 = MogDopSModClient.imagePos1;
        Vec3d p2 = MogDopSModClient.imagePos2;

        FlowLayout statusBox = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        statusBox.gap(2);

        if (p1 != null && p2 != null) {
            String strP1 = String.format("Pos1: %.2f, %.2f, %.2f", p1.x, p1.y, p1.z);
            String strP2 = String.format("Pos2: %.2f, %.2f, %.2f", p2.x, p2.y, p2.z);
            statusBox.child(Components.label(Text.literal(strP1)).color(Color.ofArgb(0xFF55FF55)));
            statusBox.child(Components.label(Text.literal(strP2)).color(Color.ofArgb(0xFF55FF55)));
        } else {
            statusBox.child(Components.label(Text.translatable("mogdops-mod.image.no_points_error")).color(Color.ofArgb(0xFFFF5555)));
        }
        rightPreviewPanel.child(statusBox);

        FlowLayout placeBtn = createFlatButton(200, 24, Text.translatable("mogdops-mod.image.btn_place"), () -> {
            if (selectedFilename != null && p1 != null && p2 != null) {
                ClientPlayNetworking.send(new SpawnImagePayload(
                        selectedFilename,
                        p1.x, p1.y, p1.z,
                        p2.x, p2.y, p2.z,
                        MogDopSModClient.imageSide.getId()
                ));
                MinecraftClient client = MinecraftClient.getInstance();
                if (client.player != null) {
                    client.player.sendMessage(Text.translatable("mogdops-mod.image.placed_success", selectedFilename), true);
                }
                this.close();
            }
        });
        if (selectedFilename == null || p1 == null || p2 == null) {
            placeBtn.surface(Surface.flat(0xFF444444));
        } else {
            placeBtn.surface(Surface.flat(0xFF00AA00));
        }
        rightPreviewPanel.child(placeBtn.margins(Insets.top(4)));
    }

    private List<String> getImageFiles() {
        List<String> list = new ArrayList<>();
        File folder = FabricLoader.getInstance().getConfigDir().resolve("pics").toFile();
        if (!folder.exists()) folder.mkdirs();
        File[] files = folder.listFiles((dir, name) -> {
            String lower = name.toLowerCase();
            return lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".webp") || lower.endsWith(".bmp");
        });
        if (files != null) {
            for (File f : files) list.add(f.getName());
        }
        return list;
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