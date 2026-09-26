package com.mogdop.mod.client;

import com.mogdop.mod.client.render.ClientImageTextureManager;
import dev.architectury.platform.Platform;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Общие клиентские утилиты NextBots: папки pics/audio, кроп квадрата, точка спавна. */
public class NextbotHelper {

    public static File getPicsFolder() {
        File folder = Platform.getConfigFolder().resolve("pics").toFile();
        if (!folder.exists()) folder.mkdirs();
        return folder;
    }

    public static File getAudioFolder() {
        File folder = Platform.getConfigFolder().resolve("audio").toFile();
        if (!folder.exists()) folder.mkdirs();
        return folder;
    }

    /** Рекурсивный плоский список .wav из config/audio (относительные пути). */
    public static List<String> listAudioFiles() {
        List<String> out = new ArrayList<>();
        File base = getAudioFolder();
        collectAudio(base, base, out);
        out.sort(String.CASE_INSENSITIVE_ORDER);
        return out;
    }

    private static void collectAudio(File base, File dir, List<String> out) {
        File[] list = dir.listFiles();
        if (list == null) return;
        for (File f : list) {
            if (f.isDirectory()) {
                collectAudio(base, f, out);
            } else if (f.getName().toLowerCase().endsWith(".wav")) {
                out.add(base.toURI().relativize(f.toURI()).getPath());
            }
        }
    }

    public static boolean audioExists(String relPath) {
        if (relPath == null || relPath.isEmpty()) return false;
        File f = new File(getAudioFolder(), relPath);
        return f.isFile();
    }

    /**
     * Проверка картинки 1:1. При forceCrop неквадрат обрезается в центр-квадрат
     * (новый файл cropped_*, оригинал не трогаем). Возвращает итоговое имя файла
     * либо null (ошибка уже передана в errorSink как lang-ключ с format-аргументами).
     */
    public static String resolveTexture(String fileName, boolean forceCrop, Consumer<String> errorSink) {
        if (fileName == null || fileName.isEmpty()) {
            errorSink.accept("mogdops-mod.nextbot.err_nofile");
            return null;
        }
        ClientImageTextureManager.ImageTextureInfo info = ClientImageTextureManager.getTexture(fileName);
        if (info == null) {
            errorSink.accept("mogdops-mod.nextbot.err_load");
            return null;
        }
        if (info.width() != info.height()) {
            if (!forceCrop) {
                errorSink.accept("mogdops-mod.nextbot.err_notsquare|" + info.width() + "|" + info.height());
                return null;
            }
            String cropped = cropCenterSquare(fileName);
            if (cropped == null) {
                errorSink.accept("mogdops-mod.nextbot.err_crop");
                return null;
            }
            ClientImageTextureManager.clearCache();
            return cropped;
        }
        return fileName;
    }

    /** Центр-квадрат: читаем оригинал, вырезаем квадрат по min(w,h) из центра, пишем рядом cropped_*.png. */
    public static String cropCenterSquare(String relPath) {
        try {
            File base = getPicsFolder();
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
            File out = new File(src.getParentFile(), outName);
            ImageIO.write(square, "png", out);
            return base.toURI().relativize(out.toURI()).getPath();
        } catch (Exception e) {
            return null;
        }
    }

    /** Точка спавна: грань блока под прицелом либо 3 блока перед глазами. */
    public static Vec3d computeSpawnPos(MinecraftClient client) {
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
}
