package com.mogdop.mod.client.gui;

import com.mogdop.mod.client.MogDopSModClient;
import com.mogdop.mod.client.render.ClientImageTextureManager;
import com.mogdop.mod.network.SpawnImagePayload;
import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.component.TextBoxComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.core.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class ImageSelectorScreen extends BaseOwoScreen<FlowLayout> {

    private String currentRelPath = "";
    private String searchFilter = "";
    private String selectedFilename = null;

    private LabelComponent pathBreadcrumbLabel;
    private FlowLayout fileListContainer;
    private FlowLayout rightPreviewPanel;

    @Override
    public boolean shouldPause() { return false; }

    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, Containers::verticalFlow);
    }

    private File getBasePicsFolder() {
        File folder = Platform.getConfigFolder().resolve("pics").toFile();
        if (!folder.exists()) folder.mkdirs();
        return folder;
    }

    private File getCurrentFolder() {
        File base = getBasePicsFolder();
        if (currentRelPath.isEmpty()) return base;
        File target = new File(base, currentRelPath);
        try {
            if (!target.getCanonicalPath().startsWith(base.getCanonicalPath())) {
                currentRelPath = "";
                return base;
            }
        } catch (Exception e) {
            currentRelPath = "";
            return base;
        }
        return target;
    }

    private void navigateUp() {
        if (currentRelPath.isEmpty()) return;
        int lastSlash = currentRelPath.lastIndexOf('/');
        if (lastSlash == -1) lastSlash = currentRelPath.lastIndexOf('\\');
        if (lastSlash <= 0) currentRelPath = "";
        else currentRelPath = currentRelPath.substring(0, lastSlash);

        selectedFilename = null;
        updatePathDisplay();
        rebuildFileList();
        updatePreviewPanel();
    }

    private void enterDirectory(String dirName) {
        if (currentRelPath.isEmpty()) currentRelPath = dirName;
        else currentRelPath = currentRelPath + "/" + dirName;

        selectedFilename = null;
        updatePathDisplay();
        rebuildFileList();
        updatePreviewPanel();
    }

    private void updatePathDisplay() {
        if (pathBreadcrumbLabel != null) {
            String display = "/pics" + (currentRelPath.isEmpty() ? "/" : "/" + currentRelPath + "/");
            pathBreadcrumbLabel.text(Text.literal("📁 " + display));
        }
    }

    @Override
    protected void build(FlowLayout rootComponent) {
        rootComponent.surface(Surface.flat(0xCC141414));
        rootComponent.horizontalAlignment(HorizontalAlignment.CENTER);
        rootComponent.verticalAlignment(VerticalAlignment.CENTER);
        rootComponent.padding(Insets.of(15));

        LabelComponent title = Components.label(Text.translatable("mogdops-mod.image.title"));
        title.color(Color.ofArgb(0xFF00C8FF));
        title.margins(Insets.bottom(12));
        rootComponent.child(title);

        FlowLayout mainBox = Containers.horizontalFlow(Sizing.fixed(520), Sizing.fixed(240));
        mainBox.surface(Surface.flat(0xFA1A1A1A));
        mainBox.padding(Insets.of(10));
        mainBox.gap(15);

        FlowLayout leftCol = Containers.verticalFlow(Sizing.fixed(235), Sizing.fill(100));
        leftCol.gap(6);

        pathBreadcrumbLabel = Components.label(Text.literal(""));
        pathBreadcrumbLabel.color(Color.ofArgb(0xFF55FFFF));
        pathBreadcrumbLabel.sizing(Sizing.fill(100), Sizing.content());
        updatePathDisplay();
        leftCol.child(pathBreadcrumbLabel);

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
            File folder = getCurrentFolder();
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

        rightPreviewPanel = Containers.verticalFlow(Sizing.fixed(250), Sizing.fill(100));
        rightPreviewPanel.surface(Surface.flat(0xFF222222));
        rightPreviewPanel.padding(Insets.of(8));
        rightPreviewPanel.gap(6);
        rightPreviewPanel.horizontalAlignment(HorizontalAlignment.CENTER);

        mainBox.child(rightPreviewPanel);
        rootComponent.child(mainBox);

        rebuildFileList();
        updatePreviewPanel();
    }

    private void rebuildFileList() {
        if (fileListContainer == null) return;
        fileListContainer.clearChildren();

        File currentFolder = getCurrentFolder();

        if (!currentRelPath.isEmpty()) {
            FlowLayout upRow = Containers.horizontalFlow(Sizing.fill(100), Sizing.fixed(22));
            upRow.surface(Surface.flat(0xFF2D2D35));
            upRow.padding(Insets.of(3));
            upRow.verticalAlignment(VerticalAlignment.CENTER);
            upRow.cursorStyle(CursorStyle.HAND);

            upRow.child(Components.item(new ItemStack(Items.CHEST)));

            LabelComponent upLbl = Components.label(Text.literal("..  (На уровень вверх)"));
            upLbl.color(Color.ofArgb(0xFFFFAA00));
            upLbl.sizing(Sizing.fill(100), Sizing.content());
            upRow.child(upLbl);

            upRow.mouseEnter().subscribe(() -> upRow.surface(Surface.flat(0xFF40404C)));
            upRow.mouseLeave().subscribe(() -> upRow.surface(Surface.flat(0xFF2D2D35)));
            upRow.mouseDown().subscribe((mX, mY, button) -> {
                if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                    navigateUp();
                    return true;
                }
                return false;
            });
            fileListContainer.child(upRow);
        }

        File[] subFiles = currentFolder.listFiles();
        if (subFiles == null) subFiles = new File[0];
        Arrays.sort(subFiles, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));

        List<File> directories = new ArrayList<>();
        List<File> imageFiles = new ArrayList<>();

        for (File f : subFiles) {
            if (f.isDirectory()) directories.add(f);
            else {
                String lower = f.getName().toLowerCase();
                if (lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".webp") || lower.endsWith(".bmp")) {
                    imageFiles.add(f);
                }
            }
        }

        String lowerFilter = searchFilter.toLowerCase();

        for (File dir : directories) {
            String dirName = dir.getName();
            if (!searchFilter.isEmpty() && !dirName.toLowerCase().contains(lowerFilter)) continue;

            FlowLayout dirRow = Containers.horizontalFlow(Sizing.fill(100), Sizing.fixed(22));
            dirRow.surface(Surface.flat(0xFF252528));
            dirRow.padding(Insets.of(3));
            dirRow.verticalAlignment(VerticalAlignment.CENTER);
            dirRow.cursorStyle(CursorStyle.HAND);

            dirRow.child(Components.item(new ItemStack(Items.BARREL)));

            LabelComponent dirLbl = Components.label(Text.literal("📁 " + dirName));
            dirLbl.color(Color.ofArgb(0xFF55FFFF));
            dirLbl.sizing(Sizing.fill(100), Sizing.content());
            dirRow.child(dirLbl);

            dirRow.mouseEnter().subscribe(() -> dirRow.surface(Surface.flat(0xFF383842)));
            dirRow.mouseLeave().subscribe(() -> dirRow.surface(Surface.flat(0xFF252528)));
            dirRow.mouseDown().subscribe((mX, mY, button) -> {
                if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                    enterDirectory(dirName);
                    return true;
                }
                return false;
            });

            fileListContainer.child(dirRow);
        }

        int imagesShown = 0;
        for (File img : imageFiles) {
            String fileName = img.getName();
            if (!searchFilter.isEmpty() && !fileName.toLowerCase().contains(lowerFilter)) continue;
            imagesShown++;

            String fullRelativeName = currentRelPath.isEmpty() ? fileName : currentRelPath + "/" + fileName;
            boolean isSelected = fullRelativeName.equals(selectedFilename);

            FlowLayout row = Containers.horizontalFlow(Sizing.fill(100), Sizing.fixed(22));
            row.surface(Surface.flat(isSelected ? 0xFF00AAFF : 0xFF333333));
            row.padding(Insets.of(3));
            row.verticalAlignment(VerticalAlignment.CENTER);
            row.cursorStyle(CursorStyle.HAND);

            row.child(Components.item(new ItemStack(Items.PAINTING)));

            LabelComponent nameLbl = Components.label(Text.literal(fileName));
            nameLbl.color(Color.ofArgb(isSelected ? 0xFFFFFFFF : 0xFFDDDDDD));
            nameLbl.sizing(Sizing.fill(100), Sizing.content());
            row.child(nameLbl);

            row.mouseEnter().subscribe(() -> {
                if (!fullRelativeName.equals(selectedFilename)) row.surface(Surface.flat(0xFF555555));
            });
            row.mouseLeave().subscribe(() -> {
                if (!fullRelativeName.equals(selectedFilename)) row.surface(Surface.flat(0xFF333333));
                else row.surface(Surface.flat(0xFF00AAFF));
            });
            row.mouseDown().subscribe((mX, mY, button) -> {
                if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                    this.selectedFilename = fullRelativeName;
                    rebuildFileList();
                    updatePreviewPanel();
                    return true;
                }
                return false;
            });

            fileListContainer.child(row);
        }

        if (directories.isEmpty() && imagesShown == 0) {
            fileListContainer.child(Components.label(Text.translatable("mogdops-mod.image.no_files")).color(Color.ofArgb(0x88FFFFFF)).margins(Insets.top(10)));
        }
    }

    private void updatePreviewPanel() {
        if (rightPreviewPanel == null) return;
        rightPreviewPanel.clearChildren();

        LabelComponent title = Components.label(Text.literal("Предпросмотр:"));
        title.color(Color.ofArgb(0xFF00C8FF));
        rightPreviewPanel.child(title);

        if (selectedFilename == null || selectedFilename.isEmpty()) {
            rightPreviewPanel.child(Components.label(Text.literal("Выберите файл из списка")).color(Color.ofArgb(0x88FFFFFF)).margins(Insets.top(15)));
        } else {
            ClientImageTextureManager.ImageTextureInfo info = ClientImageTextureManager.getTexture(selectedFilename);
            if (info != null) {
                LabelComponent nameLbl = Components.label(Text.literal(selectedFilename + " (" + info.width() + "x" + info.height() + "px)"));
                nameLbl.color(Color.ofArgb(0xFFFFAA00));
                rightPreviewPanel.child(nameLbl);

                int maxBoxW = 180;
                int maxBoxH = 85;
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

                rightPreviewPanel.child(imgBox.margins(Insets.vertical(2)));
            } else {
                rightPreviewPanel.child(Components.label(Text.literal("Ошибка загрузки изображения")).color(Color.ofArgb(0xFFFF5555)));
            }
        }

        Vec3d p1 = MogDopSModClient.imagePos1;
        Vec3d p2 = MogDopSModClient.imagePos2;

        FlowLayout statusBox = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        statusBox.gap(2);

        if (p1 != null && p2 != null) {
            double sizeX = Math.abs(p1.x - p2.x);
            double sizeY = Math.abs(p1.y - p2.y);
            double sizeZ = Math.abs(p1.z - p2.z);

            double w = (MogDopSModClient.imageSide.getAxis() == Direction.Axis.X) ? sizeZ : sizeX;
            double h = (MogDopSModClient.imageSide.getAxis() == Direction.Axis.Y) ? sizeZ : sizeY;
            int pxW = (int) Math.round(w * 16.0);
            int pxH = (int) Math.round(h * 16.0);

            statusBox.child(Components.label(Text.literal(String.format(Locale.ROOT, "Размер: %.2fx%.2f бл. (%dx%d px)", w, h, pxW, pxH))).color(Color.ofArgb(0xFF55FFFF)));
            statusBox.child(Components.label(Text.literal("Грань: " + MogDopSModClient.imageSide.asString().toUpperCase())).color(Color.ofArgb(0xFFFFAA00)));
        } else {
            statusBox.child(Components.label(Text.translatable("mogdops-mod.image.no_points_error")).color(Color.ofArgb(0xFFFF5555)));
        }
        rightPreviewPanel.child(statusBox);

        FlowLayout placeBtn = createFlatButton(200, 22, Text.translatable("mogdops-mod.image.btn_place"), () -> {
            if (selectedFilename != null && p1 != null && p2 != null) {
                NetworkManager.sendToServer(new SpawnImagePayload(
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