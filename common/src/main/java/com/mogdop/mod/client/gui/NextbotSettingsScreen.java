package com.mogdop.mod.client.gui;

import com.mogdop.mod.client.MogDopSModClient;
import com.mogdop.mod.client.NextbotHelper;
import com.mogdop.mod.client.render.ClientImageTextureManager;
import com.mogdop.mod.entity.NextbotEntity;
import com.mogdop.mod.network.SpawnNextbotPayload;
import com.mogdop.mod.network.UpdateNextbotPayload;
import dev.architectury.networking.NetworkManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

import java.util.Locale;

/**
 * Ванильный редактор пресета NextBot. Основной редактор живёт во вкладке
 * NextBots главного меню (SpawnerScreen) — этот экран оставлен для прямого
 * открытия и чинится без перекрытий: превью сверху, все контролы ниже.
 */
public class NextbotSettingsScreen extends Screen {

    private String fileName;
    private float speed;
    private float damage;
    private float size;
    private String audioName;
    private boolean forceCrop;
    private final String editingUuid;

    private String statusMessage = "";
    private int statusColor = 0xFFFF5555;

    private CheckboxWidget forceCropBox;

    public NextbotSettingsScreen() {
        this(null);
    }

    public NextbotSettingsScreen(String editingUuid) {
        super(Text.translatable("mogdops-mod.nextbot.title"));
        this.editingUuid = editingUuid;
        this.fileName = MogDopSModClient.nextbotFileName != null ? MogDopSModClient.nextbotFileName : "";
        this.speed = MogDopSModClient.nextbotSpeed <= 0 ? 0.3F : MogDopSModClient.nextbotSpeed;
        this.damage = MogDopSModClient.nextbotDamage <= 0 ? 100.0F : MogDopSModClient.nextbotDamage;
        this.size = MogDopSModClient.nextbotSize <= 0 ? 1.0F : MogDopSModClient.nextbotSize;
        this.audioName = MogDopSModClient.nextbotAudio != null ? MogDopSModClient.nextbotAudio : "";
        this.forceCrop = MogDopSModClient.nextbotForceCrop;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        // Превью 96px занимает y 44..~160 — контролы начинаются с y=168, перекрытий нет.
        int y = 168;

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("mogdops-mod.image.browse"), b -> {
            MinecraftClient client = MinecraftClient.getInstance();
            client.setScreen(new ImageSelectorScreen(selected -> {
                if (selected != null) {
                    this.fileName = selected;
                    ClientImageTextureManager.clearCache();
                }
                client.setScreen(this);
            }));
        }).dimensions(cx - 150, y, 145, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("mogdops-mod.image.open_folder"), b -> {
            if (!NextbotHelper.openFolder(NextbotHelper.getPicsFolder())) {
                fail("mogdops-mod.nextbot.folder_failed", NextbotHelper.getPicsFolder().getAbsolutePath());
            } else {
                ok();
            }
        }).dimensions(cx + 5, y, 145, 20).build());
        y += 26;

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("mogdops-mod.nextbot.browse_audio"), b -> {
            MinecraftClient client = MinecraftClient.getInstance();
            client.setScreen(new AudioSelectorScreen(selected -> {
                if (selected != null) this.audioName = selected;
                client.setScreen(this);
            }));
        }).dimensions(cx - 150, y, 145, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("mogdops-mod.nextbot.open_audio_folder"), b -> {
            if (!NextbotHelper.openFolder(NextbotHelper.getAudioFolder())) {
                fail("mogdops-mod.nextbot.folder_failed", NextbotHelper.getAudioFolder().getAbsolutePath());
            } else {
                ok();
            }
        }).dimensions(cx + 5, y, 145, 20).build());
        y += 26;

        // Слайдер скорости 0.05..1.0
        this.addDrawableChild(new SliderWidget(cx - 150, y, 300, 20,
                Text.literal(String.format(Locale.ROOT, "%s: %.2f", Text.translatable("mogdops-mod.nextbot.speed").getString(), speed)),
                (speed - 0.05) / (1.0 - 0.05)) {
            @Override
            protected void updateMessage() {
                float v = 0.05F + (float) this.value * (1.0F - 0.05F);
                this.setMessage(Text.literal(String.format(Locale.ROOT, "%s: %.2f",
                        Text.translatable("mogdops-mod.nextbot.speed").getString(), v)));
            }

            @Override
            protected void applyValue() {
                speed = 0.05F + (float) this.value * (1.0F - 0.05F);
            }
        });
        y += 26;

        // Слайдер урона 1..1000 (дефолт 100 = ваншот)
        this.addDrawableChild(new SliderWidget(cx - 150, y, 300, 20,
                Text.literal(String.format(Locale.ROOT, "%s: %.0f", Text.translatable("mogdops-mod.nextbot.damage").getString(), damage)),
                (damage - 1.0) / (1000.0 - 1.0)) {
            @Override
            protected void updateMessage() {
                float v = 1.0F + (float) this.value * (1000.0F - 1.0F);
                this.setMessage(Text.literal(String.format(Locale.ROOT, "%s: %.0f",
                        Text.translatable("mogdops-mod.nextbot.damage").getString(), v)));
            }

            @Override
            protected void applyValue() {
                damage = 1.0F + (float) this.value * (1000.0F - 1.0F);
            }
        });
        y += 26;

        // Слайдер размера 0.25..3.0
        this.addDrawableChild(new SliderWidget(cx - 150, y, 300, 20,
                Text.literal(String.format(Locale.ROOT, "%s: %.2f", Text.translatable("mogdops-mod.nextbot.size").getString(), size)),
                (size - NextbotEntity.MIN_SIZE) / (NextbotEntity.MAX_SIZE - NextbotEntity.MIN_SIZE)) {
            @Override
            protected void updateMessage() {
                float v = NextbotEntity.MIN_SIZE + (float) this.value * (NextbotEntity.MAX_SIZE - NextbotEntity.MIN_SIZE);
                this.setMessage(Text.literal(String.format(Locale.ROOT, "%s: %.2f",
                        Text.translatable("mogdops-mod.nextbot.size").getString(), v)));
            }

            @Override
            protected void applyValue() {
                size = NextbotEntity.MIN_SIZE + (float) this.value * (NextbotEntity.MAX_SIZE - NextbotEntity.MIN_SIZE);
            }
        });
        y += 26;

        forceCropBox = CheckboxWidget.builder(Text.translatable("mogdops-mod.nextbot.force_crop"), this.textRenderer)
                .checked(forceCrop)
                .callback((checkbox, checked) -> forceCrop = checked)
                .pos(cx - 150, y)
                .build();
        this.addDrawableChild(forceCropBox);
        y += 28;

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("mogdops-mod.nextbot.spawn"), b -> spawnPressed()).dimensions(cx - 150, y, 145, 20).build());
        if (editingUuid != null && !editingUuid.isEmpty()) {
            this.addDrawableChild(ButtonWidget.builder(Text.translatable("mogdops-mod.nextbot.apply"), b -> applyPressed()).dimensions(cx + 5, y, 145, 20).build());
        } else {
            this.addDrawableChild(ButtonWidget.builder(Text.translatable("mogdops-mod.image.cancel"), b -> this.close()).dimensions(cx + 5, y, 145, 20).build());
        }
    }

    private void saveDraft(String resolved) {
        MogDopSModClient.nextbotFileName = resolved;
        MogDopSModClient.nextbotSpeed = speed;
        MogDopSModClient.nextbotDamage = damage;
        MogDopSModClient.nextbotSize = size;
        MogDopSModClient.nextbotAudio = audioName;
        MogDopSModClient.nextbotForceCrop = forceCropBoxChecked();
    }

    private void spawnPressed() {
        String resolved = validateAndResolve();
        if (resolved == null) return;
        saveDraft(resolved);
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        var spawnPos = NextbotHelper.computeSpawnPos(client);
        NetworkManager.sendToServer(new SpawnNextbotPayload(resolved, spawnPos.x, spawnPos.y, spawnPos.z, speed, damage, size, audioName));
        client.player.sendMessage(Text.translatable("mogdops-mod.nextbot.spawned", resolved), true);
        this.close();
    }

    private void applyPressed() {
        String resolved = validateAndResolve();
        if (resolved == null) return;
        saveDraft(resolved);
        NetworkManager.sendToServer(new UpdateNextbotPayload(editingUuid, resolved, speed, damage, size, audioName));
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.sendMessage(Text.translatable("mogdops-mod.nextbot.applied"), true);
        }
        this.close();
    }

    /** Проверка 1:1 + существование аудио; при forceCrop неквадрат режется в новый файл. */
    private String validateAndResolve() {
        String resolved = NextbotHelper.resolveTexture(fileName, forceCropBoxChecked(), err -> {
            if (err.startsWith("mogdops-mod.nextbot.err_notsquare|")) {
                String[] parts = err.split("\\|");
                fail("mogdops-mod.nextbot.err_notsquare", parts.length > 1 ? parts[1] : "?", parts.length > 2 ? parts[2] : "?");
            } else {
                fail(err);
            }
        });
        if (resolved == null) return null;
        this.fileName = resolved;
        if (audioName != null && !audioName.isEmpty() && !NextbotHelper.audioExists(audioName)) {
            fail("mogdops-mod.nextbot.err_noaudio", audioName);
            return null;
        }
        ok();
        return resolved;
    }

    private boolean forceCropBoxChecked() {
        if (forceCropBox != null) {
            forceCrop = forceCropBox.isChecked();
        }
        return forceCrop;
    }

    private void fail(String key, Object... args) {
        statusMessage = Text.translatable(key, args).getString();
        statusColor = 0xFFFF5555;
    }

    private void ok() {
        statusMessage = "";
    }

    /** Совместимость: кроп теперь в NextbotHelper. */
    public static String cropCenterSquare(String relPath) {
        return NextbotHelper.cropCenterSquare(relPath);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        int cx = this.width / 2;
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, cx, 10, 0xFF00C8FF);
        String fileLabel = Text.translatable("mogdops-mod.nextbot.file", fileName == null || fileName.isEmpty() ? "—" : fileName).getString();
        context.drawCenteredTextWithShadow(this.textRenderer, fileLabel, cx, 22, 0xFFFFAA00);
        String audioLabel = Text.translatable("mogdops-mod.nextbot.audio",
                audioName == null || audioName.isEmpty() ? Text.translatable("mogdops-mod.nextbot.audio_none").getString() : audioName).getString();
        context.drawCenteredTextWithShadow(this.textRenderer, audioLabel, cx, 32, 0xFF55FFFF);

        // Превью 96x96: y 44..140 + подпись — выше всех кнопок (кнопки с y=168).
        if (fileName != null && !fileName.isEmpty()) {
            ClientImageTextureManager.ImageTextureInfo info = ClientImageTextureManager.getTexture(fileName);
            if (info != null) {
                int box = 96;
                int px = cx - box / 2;
                int py = 44;
                context.fill(px - 1, py - 1, px + box + 1, py + box + 1, 0xFF000000);
                context.drawTexture(info.id(), px, py, 0, 0, box, box, box, box);
                String dims = info.width() + "x" + info.height();
                boolean square = info.width() == info.height();
                context.drawCenteredTextWithShadow(this.textRenderer, dims, cx, py + box + 4, square ? 0xFF55FF55 : 0xFFFF5555);
                if (!square) {
                    context.drawCenteredTextWithShadow(this.textRenderer,
                            Text.translatable("mogdops-mod.nextbot.need_square").getString(), cx, py + box + 14, 0xFFFF5555);
                }
            }
        }
        if (!statusMessage.isEmpty()) {
            context.drawCenteredTextWithShadow(this.textRenderer, statusMessage, cx, this.height - 20, statusColor);
        }
    }
}
