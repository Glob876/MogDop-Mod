package com.mogdop.mod.client.audio;

import com.mogdop.mod.client.NextbotHelper;
import com.mogdop.mod.entity.NextbotEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Chase-звук некстботов: .wav из config/audio/ зациклен на клиенте,
 * громкость зависит от дистанции до игрока. Работает независимо от
 * громкости игры (системный микшер) — движок не умеет произвольные файлы.
 */
public class ClientAudioManager {

    private record Loop(Clip clip, String file) {}

    private static final Map<UUID, Loop> LOOPS = new HashMap<>();
    private static final double RANGE = 48.0;

    public static void tick(MinecraftClient client) {
        if (client.world == null || client.player == null) {
            stopAll();
            return;
        }
        Set<UUID> wanted = new HashSet<>();
        Box box = new Box(client.player.getBlockPos()).expand(RANGE);
        for (Entity e : client.world.getOtherEntities(client.player, box)) {
            if (!(e instanceof NextbotEntity nb) || nb.isRemoved()) continue;
            String audio = nb.getAudioName();
            if (audio == null || audio.isEmpty()) continue;
            double dist = nb.distanceTo(client.player);
            if (dist > RANGE) continue;
            UUID id = nb.getUuid();
            wanted.add(id);
            Loop loop = LOOPS.get(id);
            if (loop == null || !loop.file().equals(audio) || !loop.clip().isOpen()) {
                stop(id);
                Clip clip = openLoop(new File(NextbotHelper.getAudioFolder(), audio));
                if (clip != null) {
                    loop = new Loop(clip, audio);
                    LOOPS.put(id, loop);
                } else {
                    continue;
                }
            }
            applyGain(loop.clip(), dist);
        }
        for (UUID id : new HashSet<>(LOOPS.keySet())) {
            if (!wanted.contains(id)) stop(id);
        }
    }

    public static void stopAll() {
        for (UUID id : new HashSet<>(LOOPS.keySet())) stop(id);
    }

    private static void stop(UUID id) {
        Loop loop = LOOPS.remove(id);
        if (loop == null) return;
        try {
            loop.clip().stop();
        } catch (Exception ignored) {}
        try {
            loop.clip().close();
        } catch (Exception ignored) {}
    }

    private static Clip openLoop(File file) {
        if (!file.isFile()) return null;
        try (AudioInputStream src = AudioSystem.getAudioInputStream(file)) {
            AudioFormat base = src.getFormat();
            AudioFormat decoded = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    base.getSampleRate(), 16, base.getChannels(),
                    base.getChannels() * 2, base.getSampleRate(), false);
            try (AudioInputStream pcm = AudioSystem.getAudioInputStream(decoded, src)) {
                Clip clip = AudioSystem.getClip();
                clip.open(pcm);
                clip.loop(Clip.LOOP_CONTINUOUSLY);
                clip.start();
                return clip;
            }
        } catch (Exception e) {
            return null;
        }
    }

    private static void applyGain(Clip clip, double dist) {
        try {
            if (!clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) return;
            FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            // 0 блоков -> +4dB, 48 блоков -> -32dB
            float v = (float) Math.max(gain.getMinimum(), Math.min(4.0F, 4.0F - dist * 0.75F));
            gain.setValue(v);
        } catch (Exception ignored) {}
    }
}
