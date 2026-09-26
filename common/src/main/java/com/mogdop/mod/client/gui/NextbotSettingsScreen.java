package com.mogdop.mod.client.gui;

import com.mogdop.mod.client.MogDopSModClient;
import com.mogdop.mod.client.render.ClientImageTextureManager;
import com.mogdop.mod.network.SpawnNextbotPayload;
import com.mogdop.mod.network.UpdateNextbotPayload;
import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Locale;

public class NextbotSettingsScreen extends Screen {

    private String fileName;
    private float speed;
    private float damage;
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
    }

    public NextbotSettingsScreen(String editingUuid, String fileName, float speed, float damage) {
        super(Text.translatable("mogdops-mod.nextbot.title"));
        this.editingUuid = editingUuid;
        this.fileName = fileName != null ? fileName : "";
        this.speed = speed;
        this.damage = damage;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int y = 40;

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("mogdops-mod.image.browse"), b -> {
            MinecraftClient client = MinecraftClient.getInstance();
            client.setScreen(new ImageSelectorScreen(selected -> {
                this.fileName = selected;
                ClientImageTextureManager.clearCache();
                client.setScreen(this);
            }));
        }).dimensions(cx - 150, y, 120, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("mogdops-mod.image.open_folder"), b -> {
            File folder = Platform.getConfigFolder().resolve("pics").toFile();
            if (!folder.exists()) folder.mkdirs();
            Util.getOperatingSystem().open(folder);
        }).dimensions(cx + 30, y, 120, 20).build());
        y += 28;

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

    private void spawnPressed() {
        String resolved = validateAndResolve();
        if (resolved == null) return;
        MogDopSModClient.nextbotFileName = resolved;
        MogDopSModClient.nextbotSpeed = speed;
        MogDopSModClient.nextbotDamage = damage;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        Vec3d spawnPos = computeSpawnPos();
        NetworkManager.sendToServer(new SpawnNextbotPayload(resolved, spawnPos.x, spawnPos.y, spawnPos.z, speed, damage));
        client.player.sendMessage(Text.translatable("mogdops-mod.nextbot.spawned", resolved), true);
        this.close();
    }

    private void applyPressed() {
        String resolved = validateAndResolve();
        if (resolved == null) return;
        MogDopSModClient.nextbotFileName = resolved;
        MogDopSModClient.nextbotSpeed = speed;
        MogDopSModClient.nextbotDamage = damage;
        NetworkManager.sendToServer(new UpdateNextbotPayload(editingUuid, resolved, speed, damage));
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.sendMessage(Text.translatable("mogdops-mod.nextbot.applied"), true);
        }
        this.close();
    }

    private Vec3d computeSpawnPos() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return new Vec3d(0, 64, 0);
        HitResult hit = client.player.raycast(64.0, 1.0F, false);
        if (hit.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockHit = (BlockHitResult) hit;
            Direction side = blockHit.getSide();
            Vec3d p = hit.getPos();
            return p.add(side.getOffsetX() * 0.5, side.getOffsetY() * 0.1 + 0.1, side.getOffsetZ() * 0.5);
        }
        return client.player.getEyePos().add(client.player.getRotationVec(1.0F).multiply(3.0));
    }

    /** Проверка 1:1 обязательна; при forceCrop — центр-квадрат в новый файл, оригинал не трогаем. */
    private String validateAndResolve() {
        if (fileName == null || fileName.isEmpty()) {
            fail("mogdops-mod.nextbot.err_nofile");
            return null;
        }
        ClientImageTextureManager.ImageTextureInfo info = ClientImageTextureManager.getTexture(fileName);
        if (info == null) {
            fail("mogdops-mod.nextbot.err_load");
            return null;
        }
        if (info.width() != info.height()) {
            if (!forceCropBoxChecked()) {
                fail("mogdops-mod.nextbot.err_notsquare", info.width(), info.height());
                return null;
            }
            String cropped = cropCenterSquare(fileName);
            if (cropped == null) {
                fail("mogdops-mod.nextbot.err_crop");
                return null;
            }
            this.fileName = cropped;
            ClientImageTextureManager.clearCache();
            return cropped;
        }
        ok();
        return fileName;
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

    /** Центр-квадрат: читаем оригинал, вырезаем квадрат по min(w,h) из центра, пишем рядом cropped_*.png. */
    public static String cropCenterSquare(String relPath) {
        try {
            File base = Platform.getConfigFolder().resolve("pics").toFile();
            File src = new File(base, relPath);
            if (!src.exists()) return null;
            BufferedImage img = ImageIO.read(src);
            if (img == null) return null;
            int w = img.getWidth();
            int h = img.getHeight();
            int side = Math.min(w, h);
            if (side <= 0) return null;
            int x = (w - side) / 2;
            int y = (h - side) / 2;
            BufferedImage square = img.getSubimage(x, y, side, side);
            String srcName = src.getName();
            String outName = "cropped_" + srcName.replaceAll("(?i)\\.(jpg|jpeg|webp|bmp)$", ".png");
            if (!outName.endsWith(".png")) outName = outName + ".png";
            File parent = src.getParentFile();
            File out = new File(parent, outName);
            ImageIO.write(square, "png", out);
            String rel = base.toURI().relativize(out.toURI()).getPath();
            return rel;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        int cx = this.width / 2;
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, cx, 12, 0xFF00C8FF);
        String fileLabel = Text.translatable("mogdops-mod.nextbot.file", fileName == null || fileName.isEmpty() ? "—" : fileName).getString();
        context.drawCenteredTextWithShadow(this.textRenderer, fileLabel, cx, 30, 0xFFFFAA00);

        // Превью 96x96 по центру выше кнопок
        if (fileName != null && !fileName.isEmpty()) {
            ClientImageTextureManager.ImageTextureInfo info = ClientImageTextureManager.getTexture(fileName);
            if (info != null) {
                int box = 96;
                int px = cx - box / 2;
                int py = 68;
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
            context.drawCenteredTextWithShadow(this.textRenderer, statusMessage, cx, this.height - 24, statusColor);
        }
    }
}
