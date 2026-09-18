package com.mogdop.mod.client.gui;

import com.mogdop.mod.client.MogDopSModClient;
import com.mogdop.mod.client.render.ClientImageTextureManager;
import com.mogdop.mod.network.SpawnImagePayload;
import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Редактор изображений: узкая панель справа, плавно выезжает при открытии
 * (как в picture-insert). Все параметры здесь: файл (‹ › + счётчик),
 * Browse для одноразового выбора из браузера, папка, reload, превью,
 * ширина/высота, замок пропорций, сдвиги по плоскости, fit-aspect, PLACE.
 * Каждое изменение сразу двигает imagePos2 — в мире виден живой черновик.
 */
public class ImageEditorPanelScreen extends Screen {

    private static final int SLIDE_MS = 250;

    private final long openTimeMs = Util.getMeasuringTimeMs();

    private List<String> files = new ArrayList<>();
    private int fileIndex = -1;

    private double picW = 2.0;
    private double picH = 2.0;
    private double offU = 0.0;
    private double offV = 0.0;
    private boolean dimensionsLinked = false;
    private double linkedAspect = 1.0;

    private SizeSlider widthSlider;
    private SizeSlider heightSlider;

    /** Базовые X виджетов для слайд-анимации (виджет -> baseX). */
    private final java.util.IdentityHashMap<ClickableWidget, Integer> widgetBaseX = new java.util.IdentityHashMap<>();

    // Компактные константы (как в picture-insert)
    private static final int BTN_H = 12;
    private static final int SLIDER_H = 12;
    private static final int SAVE_H = 14;
    private static final int GAP = 3;
    private static final int PAD = 5;
    private static final int PANEL_TOP = 6;

    public ImageEditorPanelScreen() {
        super(Text.translatable("mogdops-mod.image.editor_title"));
    }

    private int panelW() {
        return Math.max(150, Math.min(190, this.width / 5));
    }

    private int panelTargetX() {
        return this.width - panelW() - 4;
    }

    private int previewH() {
        return Math.max(70, Math.min(100, (int) (panelW() * 0.62f)));
    }

    /** 0..1 прогресс выезда. */
    private float slideProgress() {
        float p = (Util.getMeasuringTimeMs() - openTimeMs) / (float) SLIDE_MS;
        if (p >= 1f) return 1f;
        if (p <= 0f) return 0f;
        float inv = 1f - p;
        return 1f - inv * inv * inv;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    /** Мир не затемняем и не блюрим — панель живёт поверх мира. */
    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    }

    @Override
    public void blur() {
    }

    // ---------- данные ----------

    private void refreshFiles() {
        files = listAllImages();
        if (files.isEmpty()) {
            fileIndex = -1;
        } else if (fileIndex < 0 || fileIndex >= files.size()) {
            fileIndex = 0;
        }
    }

    /** Плоский отсортированный список всех картинок из config/pics (включая подпапки). */
    public static List<String> listAllImages() {
        List<String> out = new ArrayList<>();
        File base = Platform.getConfigFolder().resolve("pics").toFile();
        if (!base.exists()) base.mkdirs();
        collectImages(base, base, out);
        out.sort(String.CASE_INSENSITIVE_ORDER);
        return out;
    }

    private static void collectImages(File base, File dir, List<String> out) {
        File[] list = dir.listFiles();
        if (list == null) return;
        for (File f : list) {
            if (f.isDirectory()) {
                collectImages(base, f, out);
            } else {
                String lower = f.getName().toLowerCase();
                if (lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg")
                        || lower.endsWith(".webp") || lower.endsWith(".bmp")) {
                    String rel = base.toURI().relativize(f.toURI()).getPath();
                    out.add(rel);
                }
            }
        }
    }

    private String currentFile() {
        if (fileIndex >= 0 && fileIndex < files.size()) return files.get(fileIndex);
        return null;
    }

    private double textureAspect() {
        String f = currentFile();
        if (f == null) return 1.0;
        ClientImageTextureManager.ImageTextureInfo info = ClientImageTextureManager.getTexture(f);
        if (info == null || info.width() <= 0 || info.height() <= 0) return 1.0;
        return (double) info.height() / (double) info.width();
    }

    /** Пересчитать imagePos2 из якоря p1 + ширина/высота/сдвиги на плоскости грани. */
    private void pushRect() {
        Vec3d p1 = MogDopSModClient.imagePos1;
        Direction side = MogDopSModClient.imageSide;
        if (p1 == null || side == null) return;

        double u = MogDopSModClient.snap16(offU);
        double v = MogDopSModClient.snap16(offV);
        double w = Math.max(0.0625, picW);
        double h = Math.max(0.0625, picH);

        double x1 = p1.x, y1 = p1.y, z1 = p1.z;
        double x2 = x1, y2 = y1, z2 = z1;
        switch (side.getAxis()) {
            case Y -> {
                x1 = MogDopSModClient.snap16(p1.x + u);
                z1 = MogDopSModClient.snap16(p1.z + v);
                x2 = MogDopSModClient.snap16(x1 + w);
                z2 = MogDopSModClient.snap16(z1 + h);
                y2 = y1;
            }
            case Z -> {
                x1 = MogDopSModClient.snap16(p1.x + u);
                y1 = MogDopSModClient.snap16(p1.y + v);
                x2 = MogDopSModClient.snap16(x1 + w);
                y2 = MogDopSModClient.snap16(y1 + h);
                z2 = z1;
            }
            case X -> {
                z1 = MogDopSModClient.snap16(p1.z + u);
                y1 = MogDopSModClient.snap16(p1.y + v);
                z2 = MogDopSModClient.snap16(z1 + w);
                y2 = MogDopSModClient.snap16(y1 + h);
                x2 = x1;
            }
        }
        MogDopSModClient.imagePos1 = new Vec3d(x1, y1, z1);
        MogDopSModClient.imagePos2 = new Vec3d(x2, y2, z2);
    }

    private void setPictureWidth(double value) {
        picW = Math.min(64.0, Math.max(0.0625, value));
        if (dimensionsLinked) {
            picH = Math.min(64.0, Math.max(0.0625, picW * linkedAspect));
            if (heightSlider != null) heightSlider.setExternalValue(picH);
        }
        pushRect();
    }

    private void setPictureHeight(double value) {
        picH = Math.min(64.0, Math.max(0.0625, value));
        if (dimensionsLinked) {
            picW = Math.min(64.0, Math.max(0.0625, picH / linkedAspect));
            if (widthSlider != null) widthSlider.setExternalValue(picW);
        }
        pushRect();
    }

    private void fitAspect() {
        picH = Math.min(64.0, Math.max(0.0625, picW * textureAspect()));
        if (heightSlider != null) heightSlider.setExternalValue(picH);
        pushRect();
    }

    private void stepFile(int d) {
        if (files.isEmpty()) return;
        fileIndex = ((fileIndex + d) % files.size() + files.size()) % files.size();
        fitAspect();
        if (this.client != null) this.init(this.client, this.width, this.height);
    }

    // ---------- виджеты ----------

    private void track(ClickableWidget w) {
        widgetBaseX.put(w, w.getX());
        this.addDrawableChild(w);
    }

    @Override
    protected void init() {
        widgetBaseX.clear();
        if (MogDopSModClient.imagePos1 == null) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
                client.player.sendMessage(Text.translatable("mogdops-mod.image.need_pos1"), true);
            }
            this.close();
            return;
        }

        refreshFiles();
        if (fileIndex < 0 && !files.isEmpty()) {
            fileIndex = 0;
            fitAspect();
        }

        // Инициализация размеров из текущих точек (если p2 уже выставлена в мире)
        Vec3d p1 = MogDopSModClient.imagePos1;
        Vec3d p2 = MogDopSModClient.imagePos2;
        if (p1 != null && p2 != null) {
            Direction side = MogDopSModClient.imageSide;
            double w, h;
            if (side.getAxis() == Direction.Axis.Y) {
                w = Math.abs(p2.x - p1.x);
                h = Math.abs(p2.z - p1.z);
            } else if (side.getAxis() == Direction.Axis.Z) {
                w = Math.abs(p2.x - p1.x);
                h = Math.abs(p2.y - p1.y);
            } else {
                w = Math.abs(p2.z - p1.z);
                h = Math.abs(p2.y - p1.y);
            }
            if (w >= 0.0625 && h >= 0.0625) {
                picW = Math.min(64.0, w);
                picH = Math.min(64.0, h);
            }
        }

        int pw = panelW();
        int px = panelTargetX();
        int bx = px + PAD;
        int bw = pw - PAD * 2;
        int half = (bw - GAP) / 2;
        int contentTop = PANEL_TOP + 34;
        int y = contentTop;

        track(flatButton(bx, y, half, BTN_H, Text.literal("‹"), b -> stepFile(-1)));
        track(flatButton(bx + half + GAP, y, half, BTN_H, Text.literal("›"), b -> stepFile(1)));
        y += BTN_H + GAP;

        // Browse — одноразовый выбор из браузера файлов
        track(flatButton(bx, y, bw, BTN_H, Text.translatable("mogdops-mod.image.browse"), b -> {
            MinecraftClient.getInstance().setScreen(new ImageSelectorScreen(selected -> {
                if (selected != null && !selected.isEmpty()) {
                    int idx = files.indexOf(selected);
                    if (idx < 0) {
                        refreshFiles();
                        idx = files.indexOf(selected);
                    }
                    if (idx >= 0) fileIndex = idx;
                    fitAspect();
                }
                MinecraftClient.getInstance().setScreen(ImageEditorPanelScreen.this);
                ImageEditorPanelScreen.this.init(MinecraftClient.getInstance(),
                        ImageEditorPanelScreen.this.width, ImageEditorPanelScreen.this.height);
            }));
        }));
        y += BTN_H + GAP;

        track(flatButton(bx, y, half, BTN_H, Text.translatable("mogdops-mod.image.open_folder"), b -> {
            File folder = Platform.getConfigFolder().resolve("pics").toFile();
            if (!folder.exists()) folder.mkdirs();
            Util.getOperatingSystem().open(folder);
        }));
        track(flatButton(bx + half + GAP, y, half, BTN_H, Text.translatable("mogdops-mod.image.refresh"), b -> {
            ClientImageTextureManager.clearCache();
            refreshFiles();
            fitAspect();
            this.init(this.client, this.width, this.height);
        }));
        y += BTN_H + GAP + 2;

        y += previewH() + 4;

        widthSlider = new SizeSlider(bx, y, bw, SLIDER_H, Text.translatable("mogdops-mod.image.width"), 0.0625, 64.0, picW, this::setPictureWidth);
        track(widthSlider);
        y += SLIDER_H + GAP;
        heightSlider = new SizeSlider(bx, y, bw, SLIDER_H, Text.translatable("mogdops-mod.image.height"), 0.0625, 64.0, picH, this::setPictureHeight);
        track(heightSlider);
        y += SLIDER_H + GAP;

        track(flatButton(bx, y, bw, BTN_H, Text.translatable("mogdops-mod.image.lock",
                dimensionsLinked ? "ON" : "OFF"), b -> {
            dimensionsLinked = !dimensionsLinked;
            if (dimensionsLinked) linkedAspect = Math.min(32.0, Math.max(1.0 / 32.0, picH / picW));
            b.setMessage(Text.translatable("mogdops-mod.image.lock", dimensionsLinked ? "ON" : "OFF"));
        }));
        y += BTN_H + GAP;

        track(new SizeSlider(bx, y, bw, SLIDER_H, Text.translatable("mogdops-mod.image.offset_x"), -8.0, 8.0, offU, v -> {
            offU = v;
            pushRect();
        }));
        y += SLIDER_H + GAP;
        track(new SizeSlider(bx, y, bw, SLIDER_H, Text.translatable("mogdops-mod.image.offset_y"), -8.0, 8.0, offV, v -> {
            offV = v;
            pushRect();
        }));
        y += SLIDER_H + GAP;

        track(flatButton(bx, y, bw, BTN_H, Text.translatable("mogdops-mod.image.fit_aspect"), b -> {
            fitAspect();
            this.init(this.client, this.width, this.height);
        }));
        y += BTN_H + GAP;

        track(flatButton(bx, y, bw, SAVE_H, Text.translatable("mogdops-mod.image.btn_place"), true, b -> placeAndClose()));
        y += SAVE_H + GAP;
        track(flatButton(bx, y, bw, BTN_H, Text.translatable("mogdops-mod.image.cancel"), b -> this.close()));

        pushRect();
    }

    private void placeAndClose() {
        String file = currentFile();
        Vec3d p1 = MogDopSModClient.imagePos1;
        Vec3d p2 = MogDopSModClient.imagePos2;
        MinecraftClient client = MinecraftClient.getInstance();
        if (file != null && p1 != null && p2 != null) {
            NetworkManager.sendToServer(new SpawnImagePayload(
                    file, p1.x, p1.y, p1.z, p2.x, p2.y, p2.z,
                    MogDopSModClient.imageSide.getId()));
            if (client.player != null) {
                client.player.sendMessage(Text.translatable("mogdops-mod.image.placed_success", file), true);
            }
            this.close();
        } else if (client.player != null) {
            client.player.sendMessage(Text.translatable("mogdops-mod.image.no_points_error"), true);
        }
    }

    private FlatButton flatButton(int x, int y, int w, int h, Text msg, ButtonWidget.PressAction onPress) {
        return new FlatButton(x, y, w, h, msg, false, onPress);
    }

    private FlatButton flatButton(int x, int y, int w, int h, Text msg, boolean primary, ButtonWidget.PressAction onPress) {
        return new FlatButton(x, y, w, h, msg, primary, onPress);
    }

    // ---------- отрисовка ----------

    private void drawCentered(DrawContext context, String text, int cx, int y, int color) {
        context.drawText(this.textRenderer, text, cx - this.textRenderer.getWidth(text) / 2, y, color, false);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int pw = panelW();
        int target = panelTargetX();
        int offset = (int) ((1f - slideProgress()) * (pw + 8));
        int curX = target + offset;

        // Сдвигаем виджеты вместе с панелью на время анимации
        for (var entry : widgetBaseX.entrySet()) {
            entry.getKey().setX(entry.getValue() + offset);
        }

        // Матовая серая панель с едва заметной фактурой
        int panelH = this.height - PANEL_TOP - 6;
        context.fill(curX, PANEL_TOP, curX + pw, PANEL_TOP + panelH, 0xE03D3D3D);
        for (int lineY = PANEL_TOP + 3; lineY < PANEL_TOP + panelH - 2; lineY += 4) {
            context.fill(curX + 1, lineY, curX + pw - 1, lineY + 1, 0x12303030);
        }

        int cx = curX + pw / 2;
        drawCentered(context, this.title.getString(), cx, PANEL_TOP + 5, 0xE5E5E5);

        Vec3d p1 = MogDopSModClient.imagePos1;
        String fname = currentFile();
        String count = files.isEmpty() ? "" : (fileIndex + 1) + "/" + files.size();
        if (fname != null) {
            int maxChars = Math.max(10, pw / 7);
            String shortName = fname.length() > maxChars ? "…" + fname.substring(fname.length() - maxChars + 1) : fname;
            drawCentered(context, count.isEmpty() ? shortName : shortName + " • " + count, cx, PANEL_TOP + 15, 0xC5C5C5);
        } else {
            drawCentered(context, Text.translatable("mogdops-mod.image.no_files").getString(), cx, PANEL_TOP + 15, 0xC0C0C0);
        }
        if (p1 != null) {
            drawCentered(context, String.format(java.util.Locale.ROOT, "%.2f %.2f %.2f", p1.x, p1.y, p1.z),
                    cx, PANEL_TOP + 24, 0xA8A8A8);
        }

        // Превью
        int bx = curX + PAD;
        int bw = pw - PAD * 2;
        int previewTop = PANEL_TOP + 34 + 3 * (BTN_H + GAP) + 2;
        context.fill(bx, previewTop, bx + bw, previewTop + previewH(), 0xFF2F2F2F);
        if (fname != null) {
            ClientImageTextureManager.ImageTextureInfo info = ClientImageTextureManager.getTexture(fname);
            if (info != null) {
                float scale = Math.min(bw / (float) info.width(), previewH() / (float) info.height());
                int dw = Math.max(1, (int) (info.width() * scale));
                int dh = Math.max(1, (int) (info.height() * scale));
                int dx = bx + (bw - dw) / 2;
                int dy = previewTop + (previewH() - dh) / 2;
                context.drawTexture(info.id(), dx, dy, 0, 0, dw, dh, dw, dh);
                context.drawText(this.textRenderer, info.width() + "x" + info.height(),
                        bx + 3, previewTop + previewH() - 10, 0x808080, true);
            } else {
                drawCentered(context, Text.translatable("mogdops-mod.image.load_error").getString(),
                        bx + bw / 2, previewTop + previewH() / 2 - 4, 0xB8B8B8);
            }
        }

        super.render(context, mouseX, mouseY, delta);
    }

    /** Плоская кнопка без ванильных рамок. */
    private static class FlatButton extends ButtonWidget {
        private final boolean primary;

        FlatButton(int x, int y, int w, int h, Text msg, boolean primary, PressAction onPress) {
            super(x, y, w, h, msg, onPress, DEFAULT_NARRATION_SUPPLIER);
            this.primary = primary;
        }

        @Override
        public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
            boolean hovered = this.isHovered();
            int bg;
            if (!this.active) bg = 0xFF1C1C1C;
            else if (primary && hovered) bg = 0xFFCCCCCC;
            else if (primary) bg = 0xFFAAAAAA;
            else if (hovered) bg = 0xFF5A5A5A;
            else bg = 0xFF484848;
            context.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), bg);
            MinecraftClient client = MinecraftClient.getInstance();
            int color;
            if (!this.active) color = 0x606060;
            else if (primary) color = 0x242424;
            else if (hovered) color = 0xF0F0F0;
            else color = 0xD8D8D8;
            String s = this.getMessage().getString();
            context.drawText(client.textRenderer, s,
                    this.getX() + this.getWidth() / 2 - client.textRenderer.getWidth(s) / 2,
                    this.getY() + (this.getHeight() - 8) / 2, color, false);
        }
    }

    /** Плоский слайдер: трек + заливка пропорции. */
    private static class SizeSlider extends SliderWidget {
        private final Text label;
        private final double min;
        private final double max;
        private final java.util.function.DoubleConsumer onChange;

        SizeSlider(int x, int y, int w, int h, Text label, double min, double max, double current,
                   java.util.function.DoubleConsumer onChange) {
            super(x, y, w, h, label, (current - min) / (max - min));
            this.label = label;
            this.min = min;
            this.max = max;
            this.onChange = onChange;
            this.updateMessage();
        }

        void setExternalValue(double actual) {
            this.value = Math.min(1.0, Math.max(0.0, (actual - min) / (max - min)));
            this.updateMessage();
        }

        private double currentValue() {
            return min + this.value * (max - min);
        }

        @Override
        protected void updateMessage() {
            this.setMessage(Text.literal(label.getString() + ": " + String.format(java.util.Locale.ROOT, "%.2f", currentValue())));
        }

        @Override
        protected void applyValue() {
            onChange.accept(currentValue());
        }

        @Override
        public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
            boolean hovered = this.isHovered() || this.isFocused();
            context.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), 0xFF363636);
            int fillW = (int) (this.getWidth() * this.value);
            int fillCol;
            if (!this.active) fillCol = 0xFF2A2A2A;
            else if (hovered) fillCol = 0xFF767676;
            else fillCol = 0xFF5A5A5A;
            if (fillW > 0) context.fill(this.getX(), this.getY(), this.getX() + fillW, this.getY() + this.getHeight(), fillCol);
            int kx = this.getX() + fillW;
            context.fill(kx - 1, this.getY(), kx + 1, this.getY() + this.getHeight(), 0xFFB0B0B0);
            MinecraftClient client = MinecraftClient.getInstance();
            String s = this.getMessage().getString();
            context.drawText(client.textRenderer, s,
                    this.getX() + this.getWidth() / 2 - client.textRenderer.getWidth(s) / 2,
                    this.getY() + (this.getHeight() - 8) / 2, this.active ? 0xD8D8D8 : 0x606060, false);
        }
    }
}
