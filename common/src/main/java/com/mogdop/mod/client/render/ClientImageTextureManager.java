package com.mogdop.mod.client.render;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class ClientImageTextureManager {

    public record ImageTextureInfo(Identifier id, int width, int height) {}

    private static final Map<String, ImageTextureInfo> TEXTURE_CACHE = new HashMap<>();

    public static void clearCache() {
        MinecraftClient client = MinecraftClient.getInstance();
        for (ImageTextureInfo info : TEXTURE_CACHE.values()) {
            if (client.getTextureManager() != null) {
                client.getTextureManager().destroyTexture(info.id());
            }
        }
        TEXTURE_CACHE.clear();
    }

    public static ImageTextureInfo getTexture(String fileName) {
        if (fileName == null || fileName.isEmpty()) return null;
        if (TEXTURE_CACHE.containsKey(fileName)) {
            return TEXTURE_CACHE.get(fileName);
        }

        File folder = FabricLoader.getInstance().getConfigDir().resolve("pics").toFile();
        if (!folder.exists()) folder.mkdirs();

        File file = new File(folder, fileName);
        if (!file.exists()) return null;

        NativeImage image = null;

        // 1. Попытка прямой быстрой загрузки через нативный STB загрузчик Minecraft
        try (InputStream in = new FileInputStream(file)) {
            image = NativeImage.read(in);
        } catch (Exception ignored) {
            // STB не поддерживает Progressive JPEG и некоторые варианты JPG
        }

        // 2. Универсальный декодер через ImageIO (поддерживает любые Progressive JPEG, JPG, BMP и т.д.)
        if (image == null) {
            try {
                BufferedImage bImg = ImageIO.read(file);
                if (bImg != null) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ImageIO.write(bImg, "png", baos);
                    image = NativeImage.read(new ByteArrayInputStream(baos.toByteArray()));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (image == null) {
            return null;
        }

        try {
            int width = image.getWidth();
            int height = image.getHeight();

            String sanitized = fileName.toLowerCase().replaceAll("[^a-z0-9_.-]", "_");
            Identifier id = Identifier.of("mogdops-mod", "custom_img_" + Math.abs(sanitized.hashCode()));

            NativeImageBackedTexture texture = new NativeImageBackedTexture(image);
            MinecraftClient.getInstance().getTextureManager().registerTexture(id, texture);

            ImageTextureInfo info = new ImageTextureInfo(id, width, height);
            TEXTURE_CACHE.put(fileName, info);
            return info;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}