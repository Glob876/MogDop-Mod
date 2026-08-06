package com.mogdop.mod.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlayerBlockHistoryManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File FILE = FabricLoader.getInstance().getConfigDir().resolve("mogdops-mod-block-history.json").toFile();

    private static Map<String, List<String>> historyMap = new HashMap<>();

    public static void load() {
        if (!FILE.exists()) return;
        try (FileReader reader = new FileReader(FILE)) {
            Type type = new TypeToken<Map<String, List<String>>>() {}.getType();
            Map<String, List<String>> loaded = GSON.fromJson(reader, type);
            if (loaded != null) {
                historyMap = loaded;
            }
        } catch (Exception ignored) {}
    }

    public static void save() {
        try {
            if (!FILE.getParentFile().exists()) {
                FILE.getParentFile().mkdirs();
            }
            try (FileWriter writer = new FileWriter(FILE)) {
                GSON.toJson(historyMap, writer);
            }
        } catch (Exception ignored) {}
    }

    public static String getCurrentPlayerKey() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.player != null) {
            return client.player.getUuidAsString();
        }
        return "default_player";
    }

    public static List<String> getHistory() {
        String key = getCurrentPlayerKey();
        return historyMap.computeIfAbsent(key, k -> new ArrayList<>());
    }

    public static void pushToHistory(String blockId) {
        if (blockId == null || blockId.isEmpty() || blockId.equals("minecraft:air")) return;
        List<String> list = getHistory();
        list.remove(blockId);
        list.add(0, blockId);
        if (list.size() > 10) {
            list.remove(list.size() - 1);
        }
        save();
    }
}