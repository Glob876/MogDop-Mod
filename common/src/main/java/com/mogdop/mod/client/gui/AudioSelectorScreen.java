package com.mogdop.mod.client.gui;

import com.mogdop.mod.client.NextbotHelper;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

import java.io.File;
import java.util.List;
import java.util.function.Consumer;

/** Браузер .wav из config/audio/: список с пагинацией, одноразовый выбор через pickCallback. */
public class AudioSelectorScreen extends Screen {

    private static final int PER_PAGE = 7;

    private final Consumer<String> pickCallback;
    private List<String> files = List.of();
    private int page = 0;
    private String statusMessage = "";

    public AudioSelectorScreen(Consumer<String> pickCallback) {
        super(Text.translatable("mogdops-mod.nextbot.audio_title"));
        this.pickCallback = pickCallback;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    protected void init() {
        files = NextbotHelper.listAudioFiles();
        int maxPage = Math.max(0, (files.size() - 1) / PER_PAGE);
        if (page > maxPage) page = maxPage;

        int cx = this.width / 2;
        int y = 44;
        int from = page * PER_PAGE;
        int to = Math.min(files.size(), from + PER_PAGE);
        for (int i = from; i < to; i++) {
            String name = files.get(i);
            this.addDrawableChild(ButtonWidget.builder(Text.literal("♪ " + name), b -> {
                if (pickCallback != null) pickCallback.accept(name);
            }).dimensions(cx - 150, y, 300, 20).build());
            y += 24;
        }

        int navY = 44 + PER_PAGE * 24 + 8;
        if (page > 0) {
            this.addDrawableChild(ButtonWidget.builder(Text.literal("←"), b -> {
                page--;
                this.clearChildren();
                this.init();
            }).dimensions(cx - 150, navY, 60, 20).build());
        }
        if (to < files.size()) {
            this.addDrawableChild(ButtonWidget.builder(Text.literal("→"), b -> {
                page++;
                this.clearChildren();
                this.init();
            }).dimensions(cx + 90, navY, 60, 20).build());
        }

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("mogdops-mod.image.open_folder"), b -> {
            File folder = NextbotHelper.getAudioFolder();
            Util.getOperatingSystem().open(folder);
        }).dimensions(cx - 150, navY + 26, 145, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("mogdops-mod.image.refresh"), b -> {
            statusMessage = "";
            this.clearChildren();
            this.init();
        }).dimensions(cx + 5, navY + 26, 145, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("mogdops-mod.nextbot.audio_none"), b -> {
            if (pickCallback != null) pickCallback.accept("");
        }).dimensions(cx - 150, navY + 52, 145, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("mogdops-mod.image.cancel"), b -> {
            if (pickCallback != null) pickCallback.accept(null);
            else this.close();
        }).dimensions(cx + 5, navY + 52, 145, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        int cx = this.width / 2;
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, cx, 14, 0xFF00C8FF);
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.translatable("mogdops-mod.nextbot.audio_folder_hint"), cx, 26, 0xFFAAAAAA);
        if (files.isEmpty()) {
            context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.translatable("mogdops-mod.nextbot.no_audio_files").getString(), cx, 60, 0xFFFF5555);
        }
        if (!statusMessage.isEmpty()) {
            context.drawCenteredTextWithShadow(this.textRenderer, statusMessage, cx, this.height - 20, 0xFFFF5555);
        }
    }
}
