package com.mogdop.mod.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class NotificationManager {
    public static class Notification {
        public final Text text;
        public List<OrderedText> lines = new ArrayList<>();
        public final long startTime;
        public float animX = -320f;
        public float opacity = 0f;
        public int cardHeight = 22;
        private int lastWrapWidth = -1;

        public Notification(Text text, int maxWidth) {
            this.text = text;
            this.startTime = System.currentTimeMillis();
            rewrap(maxWidth);
        }

        /** Переразбивка строк под текущую ширину подложки (wrapWidth = bgW - padding*2). */
        public void rewrap(int maxWidth) {
            if (maxWidth == lastWrapWidth && !lines.isEmpty()) return;
            lastWrapWidth = maxWidth;
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.textRenderer != null) {
                this.lines = client.textRenderer.wrapLines(text, Math.max(50, maxWidth));
                this.cardHeight = 12 + (this.lines.size() * 10);
            } else {
                this.lines.clear();
                this.lines.add(text.asOrderedText());
                this.cardHeight = 22;
            }
        }
    }

    private final List<Notification> notifications = new ArrayList<>();
    private double scrollOffset = 0.0;

    public synchronized int getScrollOffset() {
        return (int) Math.round(this.scrollOffset);
    }

    public synchronized void setScrollOffset(double scrollOffset) {
        this.scrollOffset = scrollOffset;
    }

    public synchronized void scroll(double amount) {
        double maxScroll = Math.max(0.0, notifications.size() - 1);
        this.scrollOffset = Math.max(0.0, Math.min(maxScroll, this.scrollOffset + amount * 1.5));
    }

    public synchronized void addNotification(Text text) {
        notifications.add(new Notification(text, currentWrapWidth()));
        if (notifications.size() > 50) {
            notifications.remove(0);
        }
    }

    /** Текущая ширина переноса из конфига чата: widthPx - padding*2. */
    public static int currentWrapWidth() {
        try {
            int w = MogDopSModClient.CONFIG.chat().widthPx;
            int p = MogDopSModClient.CONFIG.chat().padding;
            return Math.max(50, w - p * 2);
        } catch (Throwable t) {
            return 290;
        }
    }

    /** Переразбить все уведомления под текущую ширину (вызывать каждый кадр перед layout). */
    public synchronized void rewrapAll() {
        int w = currentWrapWidth();
        for (Notification n : notifications) n.rewrap(w);
    }

    public synchronized void clear() {
        notifications.clear();
    }

    public synchronized List<Notification> getNotifications() {
        return new ArrayList<>(notifications);
    }

    public synchronized void update() {
        long now = System.currentTimeMillis();
        MinecraftClient client = MinecraftClient.getInstance();
        boolean isChatOpen = client.currentScreen instanceof net.minecraft.client.gui.screen.ChatScreen;

        if (!isChatOpen) {
            this.scrollOffset = 0.0;
        }

        for (Notification n : notifications) {
            long age = now - n.startTime;
            float targetX = 12f;
            n.animX += (targetX - n.animX) * 0.16f;

            if (age < 300) {
                n.opacity = age / 300f;
            } else if (age > 7200) {
                n.opacity = Math.max(0f, 1f - ((age - 7200) / 800f));
            } else {
                n.opacity = 1f;
            }
        }
    }
}