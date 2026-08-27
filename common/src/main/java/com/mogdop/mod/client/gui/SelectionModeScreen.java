package com.mogdop.mod.client.gui;

import com.mogdop.mod.client.MogDopSModClient;
import com.mojang.blaze3d.systems.RenderSystem;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.HorizontalAlignment;
import io.wispforest.owo.ui.core.OwoUIAdapter;
import io.wispforest.owo.ui.core.Surface;
import io.wispforest.owo.ui.core.VerticalAlignment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class SelectionModeScreen extends BaseOwoScreen<FlowLayout> {

    static class ModeOption {
        final String titleKey;
        final String descKey;
        final Item iconItem;
        final int modeIndex;
        final double angleDeg;

        ModeOption(String titleKey, String descKey, Item iconItem, int modeIndex, double angleDeg) {
            this.titleKey = titleKey;
            this.descKey = descKey;
            this.iconItem = iconItem;
            this.modeIndex = modeIndex;
            this.angleDeg = angleDeg;
        }
    }

    private final List<ModeOption> options = new ArrayList<>();

    private int hoverIndex = -1;
    private double cubeX = 0;
    private double cubeY = 0;
    private boolean initialized = false;
    private float animTimer = 0f;

    public SelectionModeScreen() {
        initOptions();
    }

    private void initOptions() {
        options.clear();
        // Равномерное круговое распределение 3 режимов WorldEdit (0°, 120°, 240°)
        options.add(new ModeOption("mogdops-mod.selection_mode.cuboid", "mogdops-mod.selection_mode.cuboid.desc", Items.WOODEN_AXE, 0, -90.0));
        options.add(new ModeOption("mogdops-mod.selection_mode.poly", "mogdops-mod.selection_mode.poly.desc", Items.COMPASS, 1, 30.0));
        options.add(new ModeOption("mogdops-mod.selection_mode.convex", "mogdops-mod.selection_mode.convex.desc", Items.NETHER_STAR, 2, 150.0));
    }

    @Override
    public boolean shouldPause() { return false; }

    @Override
    protected void init() {
        super.init();
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.getWindow() != null) {
            GLFW.glfwSetInputMode(client.getWindow().getHandle(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_HIDDEN);
        }
    }

    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, Containers::verticalFlow);
    }

    @Override
    protected void build(FlowLayout rootComponent) {
        rootComponent.surface(Surface.flat(0x44000000));
        rootComponent.horizontalAlignment(HorizontalAlignment.CENTER);
        rootComponent.verticalAlignment(VerticalAlignment.CENTER);
    }

    private double normalizeAngle(double angle) {
        while (angle <= -Math.PI) angle += Math.PI * 2;
        while (angle > Math.PI) angle -= Math.PI * 2;
        return angle;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        animTimer += delta;

        int screenWidth = this.width;
        int screenHeight = this.height;
        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;

        int radius = 80;
        int cardWidth = 110;
        int cardHeight = 65;

        double mDx = mouseX - centerX;
        double mDy = mouseY - centerY;
        double distFromCenter = Math.sqrt(mDx * mDx + mDy * mDy);

        if (distFromCenter < 30.0) {
            hoverIndex = -1; // В мертвой зоне в центре кубик клеится к центру экрана
        } else {
            double mouseAngle = Math.atan2(mDy, mDx);
            double minDiff = Double.MAX_VALUE;
            int bestIndex = -1;

            for (int i = 0; i < options.size(); i++) {
                double itemAngle = Math.toRadians(options.get(i).angleDeg);
                double diff = Math.abs(normalizeAngle(mouseAngle - itemAngle));
                if (diff < minDiff) {
                    minDiff = diff;
                    bestIndex = i;
                }
            }
            hoverIndex = bestIndex;
        }

        double targetCubeX = centerX;
        double targetCubeY = centerY - 25; // Приподнятый центр!

        if (hoverIndex >= 0 && hoverIndex < options.size()) {
            ModeOption opt = options.get(hoverIndex);
            double rad = Math.toRadians(opt.angleDeg);
            targetCubeX = centerX + radius * Math.cos(rad);
            targetCubeY = centerY + radius * Math.sin(rad);
        } else {
            targetCubeX = centerX;
            targetCubeY = centerY - 25;
        }

        if (!initialized) {
            cubeX = targetCubeX;
            cubeY = targetCubeY;
            initialized = true;
        }

        // Ease-In-Out плавная слежка кубика-курсора
        double dx = targetCubeX - cubeX;
        double dy = targetCubeY - cubeY;
        double dist = Math.sqrt(dx * dx + dy * dy);

        if (dist > 0.1) {
            double speed = Math.min(1.0, 0.22 + (dist / 300.0) * 0.12);
            cubeX += dx * speed;
            cubeY += dy * speed;
        } else {
            cubeX = targetCubeX;
            cubeY = targetCubeY;
        }

        super.render(context, mouseX, mouseY, delta);

        // ================= ИНФОРМАЦИОННЫЙ ЗАГОЛОВОК =================
        int topY = 30;
        context.drawTextWithShadow(this.textRenderer, Text.translatable("mogdops-mod.selection_mode.title"), centerX - this.textRenderer.getWidth(Text.translatable("mogdops-mod.selection_mode.title")) / 2, topY, 0xFF00C8FF);

        if (hoverIndex >= 0) {
            ModeOption currentOpt = options.get(hoverIndex);
            Text titleText = Text.translatable(currentOpt.titleKey);
            Text descText = Text.translatable(currentOpt.descKey);

            context.drawTextWithShadow(this.textRenderer, titleText, centerX - this.textRenderer.getWidth(titleText) / 2, topY + 16, 0xFFFFAA00);
            context.drawTextWithShadow(this.textRenderer, descText, centerX - this.textRenderer.getWidth(descText) / 2, topY + 30, 0xFFAAAAAA);
        }

        // ================= КРУГОВАЯ ОТРИСОВКА 3 РЕЖИМОВ =================
        for (int i = 0; i < options.size(); i++) {
            ModeOption opt = options.get(i);
            boolean isSelected = (i == hoverIndex);
            boolean isCurrentActive = (MogDopSModClient.currentSelectionMode == opt.modeIndex);

            double rad = Math.toRadians(opt.angleDeg);
            int cardX = (int) (centerX + radius * Math.cos(rad) - cardWidth / 2.0);
            int cardY = (int) (centerY + radius * Math.sin(rad) - cardHeight / 2.0);

            int bgColor = isSelected ? 0xDD00C8FF : (isCurrentActive ? 0xAA00AA00 : 0xAA14141A);

            context.fill(cardX, cardY, cardX + cardWidth, cardY + cardHeight, bgColor);

            if (isSelected) {
                context.drawBorder(cardX - 1, cardY - 1, cardWidth + 2, cardHeight + 2, 0xFFFFFFFF);
            } else {
                context.drawBorder(cardX, cardY, cardWidth, cardHeight, 0x33FFFFFF);
            }

            ItemStack iconStack = new ItemStack(opt.iconItem);
            context.drawItem(iconStack, cardX + cardWidth / 2 - 8, cardY + 8);

            Text cardTitle = Text.translatable(opt.titleKey);
            int titleWidth = this.textRenderer.getWidth(cardTitle);
            context.drawTextWithShadow(this.textRenderer, cardTitle, cardX + cardWidth / 2 - titleWidth / 2, cardY + 38, isSelected ? 0xFFFFFFFF : 0x88FFFFFF);
        }

        // ================= 3D КУБИК =================
        RenderSystem.disableDepthTest();
        context.getMatrices().push();
        context.getMatrices().translate(0, 0, 1000f);

        drawCubeCursor(context, (float) cubeX, (float) cubeY, animTimer);

        context.getMatrices().pop();
        RenderSystem.enableDepthTest();
    }

    private void drawCubeCursor(DrawContext context, float cx, float cy, float time) {
        int r = 10;
        int x = (int) cx;
        int y = (int) cy;

        int alphaGlow = (int) (160 + 80 * Math.sin(time * 0.25));
        int glowColor = (alphaGlow << 24) | 0x00C8FF;

        context.fill(x - r - 4, y - r - 4, x + r + 5, y + r + 5, glowColor & 0x4400C8FF);

        context.fill(x - r, y - r, x + r, y, 0xFF00E5FF);
        context.fill(x - r, y, x, y + r, 0xFF0099DD);
        context.fill(x, y, x + r, y + r, 0xFF0055AA);

        context.drawBorder(x - r, y - r, r * 2 + 1, r * 2 + 1, 0xFFFFFFFF);
    }

    private void executeSelectMode() {
        if (hoverIndex >= 0 && hoverIndex < options.size()) {
            ModeOption opt = options.get(hoverIndex);
            MogDopSModClient.currentSelectionMode = opt.modeIndex;
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
                client.player.sendMessage(Text.translatable("mogdops-mod.tool.mode_changed", Text.translatable(opt.titleKey)), true);
            }
            this.close();
        } else {
            this.close();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            executeSelectMode();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (MogDopSModClient.openBlockSelectorKey.matchesKey(keyCode, scanCode)) {
            executeSelectMode();
            return true;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public void removed() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.getWindow() != null) {
            GLFW.glfwSetInputMode(client.getWindow().getHandle(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
        }
        super.removed();
    }
}