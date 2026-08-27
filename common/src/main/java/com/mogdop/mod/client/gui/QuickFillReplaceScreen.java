package com.mogdop.mod.client.gui;

import com.mogdop.mod.client.MogDopSModClient;
import com.mogdop.mod.network.*;
import com.mojang.blaze3d.systems.RenderSystem;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.HorizontalAlignment;
import io.wispforest.owo.ui.core.OwoUIAdapter;
import io.wispforest.owo.ui.core.Surface;
import io.wispforest.owo.ui.core.VerticalAlignment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class QuickFillReplaceScreen extends BaseOwoScreen<FlowLayout> {

    static class QuickAction {
        final String titleKey;
        final String descKey;
        final Item iconItem;
        final int themeColor;
        final double angleDeg;
        final Runnable action;

        QuickAction(String titleKey, String descKey, Item iconItem, int themeColor, double angleDeg, Runnable action) {
            this.titleKey = titleKey;
            this.descKey = descKey;
            this.iconItem = iconItem;
            this.themeColor = themeColor;
            this.angleDeg = angleDeg;
            this.action = action;
        }
    }

    private final List<QuickAction> actions = new ArrayList<>();

    private int hoverIndex = -1; // -1 = Мертвая зона (Центр)
    private double cubeX = 0;
    private double cubeY = 0;
    private boolean initialized = false;
    private float animTimer = 0f;

    public QuickFillReplaceScreen() {
        initActions();
    }

    private void initActions() {
        actions.clear();

        // 1. ВЕРХНЯЯ ПАРА: UNDO (-110°) И REDO (-70°)
        actions.add(new QuickAction("mogdops-mod.quick_select.undo", "mogdops-mod.quick_select.undo.desc", Items.FEATHER, 0xFFFFAA00, -110.0, () -> {
            ClientPlayNetworking.send(new UndoPayload());
        }));
        actions.add(new QuickAction("mogdops-mod.quick_select.redo", "mogdops-mod.quick_select.redo.desc", Items.GUNPOWDER, 0xFFFF5500, -70.0, () -> {
            ClientPlayNetworking.send(new RedoPayload());
        }));

        // 2. СРЕДНЕ-ВЕРХНЯЯ ПАРА: //SET (-150°) И //REPLACE (-30°)
        actions.add(new QuickAction("mogdops-mod.quick_select.set", "mogdops-mod.quick_select.set.desc", Items.BRICKS, 0xFF00AA00, -150.0, () -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null) client.setScreen(new BlockSelectorScreen(BlockSelectorScreen.TargetAction.FILL));
        }));
        actions.add(new QuickAction("mogdops-mod.quick_select.replace", "mogdops-mod.quick_select.replace.desc", Items.ANVIL, 0xFF00AAFF, -30.0, () -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (MogDopSModClient.getSelectionPoints().isEmpty()) {
                if (client != null && client.player != null) {
                    client.player.sendMessage(Text.translatable("mogdops-mod.error.positions_not_set"), false);
                }
            } else if (client != null) {
                client.setScreen(new ReplaceBlockScreen());
            }
        }));

        // 3. НИЖНЯЯ ДУГА КРУГА: СТЕНЫ (10°), КОРОБКА (50°), ОЧИСТИТЬ (90°), ОСУШЕНИЕ (130°), ДУБЛИКАТ (170°)
        actions.add(new QuickAction("mogdops-mod.quick_select.walls", "mogdops-mod.quick_select.walls.desc", Items.COBBLESTONE_WALL, 0xFF9966FF, 10.0, () -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (MogDopSModClient.getSelectionPoints().isEmpty()) {
                if (client != null && client.player != null) client.player.sendMessage(Text.translatable("mogdops-mod.error.positions_not_set"), false);
            } else if (client != null) {
                client.setScreen(new BlockSelectorScreen(BlockSelectorScreen.TargetAction.WALLS));
            }
        }));

        actions.add(new QuickAction("mogdops-mod.quick_select.outline", "mogdops-mod.quick_select.outline.desc", Items.GLASS, 0xFFFF55FF, 50.0, () -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (MogDopSModClient.getSelectionPoints().isEmpty()) {
                if (client != null && client.player != null) client.player.sendMessage(Text.translatable("mogdops-mod.error.positions_not_set"), false);
            } else if (client != null) {
                client.setScreen(new BlockSelectorScreen(BlockSelectorScreen.TargetAction.OUTLINE));
            }
        }));

        actions.add(new QuickAction("mogdops-mod.quick_select.clear", "mogdops-mod.quick_select.clear.desc", Items.BARRIER, 0xFFFF3333, 90.0, () -> {
            MogDopSModClient.pos1 = null;
            MogDopSModClient.pos2 = null;
            MogDopSModClient.selectionPoints.clear();
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.player != null) {
                client.player.sendMessage(Text.translatable("mogdops-mod.selection.cleared"), true);
            }
        }));

        actions.add(new QuickAction("mogdops-mod.quick_select.drain", "mogdops-mod.quick_select.drain.desc", Items.SPONGE, 0xFF3399FF, 130.0, () -> {
            ClientPlayNetworking.send(new DrainPayload(10));
        }));

        actions.add(new QuickAction("mogdops-mod.quick_select.stack", "mogdops-mod.quick_select.stack.desc", Items.REPEATER, 0xFF55FFFF, 170.0, () -> {
            if (MogDopSModClient.pos1 == null || MogDopSModClient.pos2 == null) {
                MinecraftClient client = MinecraftClient.getInstance();
                if (client != null && client.player != null) client.player.sendMessage(Text.translatable("mogdops-mod.error.positions_not_set"), false);
            } else {
                ClientPlayNetworking.send(new StackPayload(MogDopSModClient.pos1, MogDopSModClient.pos2, 1, "FORWARD"));
            }
        }));
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

        int radius = 130;

        double mDx = mouseX - centerX;
        double mDy = mouseY - centerY;
        double distFromCenter = Math.sqrt(mDx * mDx + mDy * mDy);

        if (distFromCenter < 35.0) {
            hoverIndex = -1;
        } else {
            double mouseAngle = Math.atan2(mDy, mDx);
            double minDiff = Double.MAX_VALUE;
            int bestIndex = -1;

            for (int i = 0; i < actions.size(); i++) {
                double itemAngle = Math.toRadians(actions.get(i).angleDeg);
                double diff = Math.abs(normalizeAngle(mouseAngle - itemAngle));
                if (diff < minDiff) {
                    minDiff = diff;
                    bestIndex = i;
                }
            }
            hoverIndex = bestIndex;
        }

        double targetCubeX = centerX;
        double targetCubeY = centerY;
        QuickAction currentAction = null;

        if (hoverIndex >= 0 && hoverIndex < actions.size()) {
            currentAction = actions.get(hoverIndex);
            double rad = Math.toRadians(currentAction.angleDeg);
            targetCubeX = centerX + radius * Math.cos(rad);
            targetCubeY = centerY + radius * Math.sin(rad);
        } else {
            targetCubeX = centerX;
            targetCubeY = centerY;
        }

        if (!initialized) {
            cubeX = targetCubeX;
            cubeY = targetCubeY;
            initialized = true;
        }

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

        int topY = 20;
        context.drawTextWithShadow(this.textRenderer, Text.translatable("mogdops-mod.quick_select.title"), centerX - this.textRenderer.getWidth(Text.translatable("mogdops-mod.quick_select.title")) / 2, topY, 0xFF00C8FF);

        if (currentAction != null) {
            Text titleText = Text.translatable(currentAction.titleKey);
            Text descText = Text.translatable(currentAction.descKey);
            context.drawTextWithShadow(this.textRenderer, titleText, centerX - this.textRenderer.getWidth(titleText) / 2, topY + 14, currentAction.themeColor);
            context.drawTextWithShadow(this.textRenderer, descText, centerX - this.textRenderer.getWidth(descText) / 2, topY + 26, 0xFFAAAAAA);
        }

        int cardW = 68;
        int cardH = 38;

        for (int i = 0; i < actions.size(); i++) {
            QuickAction action = actions.get(i);
            boolean isSel = (i == hoverIndex);
            double rad = Math.toRadians(action.angleDeg);

            int cardX = (int) (centerX + radius * Math.cos(rad) - cardW / 2.0);
            int cardY = (int) (centerY + radius * Math.sin(rad) - cardH / 2.0);

            drawCard(context, action, cardX, cardY, cardW, cardH, isSel);
        }

        RenderSystem.disableDepthTest();
        context.getMatrices().push();
        context.getMatrices().translate(0, 0, 1000f);

        drawCubeCursor(context, (float) cubeX, (float) cubeY, animTimer);

        context.getMatrices().pop();
        RenderSystem.enableDepthTest();
    }

    private void drawCard(DrawContext context, QuickAction action, int cardX, int cardY, int cardW, int cardH, boolean isSel) {
        int bgAlpha = isSel ? 0xDD000000 : 0xAA000000;
        int bgColor = bgAlpha | (isSel ? (action.themeColor & 0x00FFFFFF) : 0x14141A);

        context.fill(cardX, cardY, cardX + cardW, cardY + cardH, bgColor);

        if (isSel) {
            context.drawBorder(cardX - 1, cardY - 1, cardW + 2, cardH + 2, 0xFFFFFFFF);
            context.drawBorder(cardX, cardY, cardW, cardH, action.themeColor);
        } else {
            context.drawBorder(cardX, cardY, cardW, cardH, 0x33FFFFFF);
        }

        ItemStack iconStack = new ItemStack(action.iconItem);
        context.drawItem(iconStack, cardX + 5, cardY + cardH / 2 - 8);

        Text cardTitle = Text.translatable(action.titleKey);
        context.drawTextWithShadow(this.textRenderer, cardTitle, cardX + 24, cardY + cardH / 2 - 4, isSel ? 0xFFFFFFFF : 0x88FFFFFF);
    }

    private void drawCubeCursor(DrawContext context, float cx, float cy, float time) {
        int r = 9;
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

    private void executeSelectedAction() {
        if (hoverIndex >= 0 && hoverIndex < actions.size()) {
            QuickAction act = actions.get(hoverIndex);
            this.close();
            act.action.run();
        } else {
            this.close();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            executeSelectedAction();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (MogDopSModClient.quickFillKey.matchesKey(keyCode, scanCode)) {
            executeSelectedAction();
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