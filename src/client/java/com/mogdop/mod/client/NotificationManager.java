package com.mogdop.mod.client;

import net.minecraft.text.Text;
import net.minecraft.text.OrderedText;
import net.minecraft.client.MinecraftClient;
import java.util.ArrayList;
import java.util.List;

public class NotificationManager {
    public static class Notification {
        public final Text text;
        public List<OrderedText> lines = new ArrayList<>();
        public final long startTime;
        public float animX = -320f; // Начальная позиция за левым краем экрана
        public float opacity = 0f;
        public int cardHeight = 22;

        public Notification(Text text, int maxWidth) {
            this.text = text;
            this.startTime = System.currentTimeMillis();
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.textRenderer != null) {
                // Автоматический перенос строк под фиксированную ширину карточки
                this.lines = client.textRenderer.wrapLines(text, maxWidth);
                this.cardHeight = 12 + (this.lines.size() * 10);
            } else {
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
        // Прокрутка вверх (amount > 0) уходит в историю (увеличивает scrollOffset)
        // Прокрутка вниз (amount < 0) возвращает к новым сообщениям
        double maxScroll = Math.max(0.0, notifications.size() - 1);
        this.scrollOffset = Math.max(0.0, Math.min(maxScroll, this.scrollOffset + amount * 1.5));
    }

    public synchronized void addNotification(Text text) {
        // Ограничиваем ширину текста в 290 пикселей для аккуратного размещения внутри карточки
        notifications.add(new Notification(text, 290));
        // Храним до 50 сообщений в истории для возможности прокрутки
        if (notifications.size() > 50) {
            notifications.remove(0);
        }
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

        // ВАЖНО: Мы БОЛЬШЕ НЕ УДАЛЯЕМ сообщения из списка по времени жизни!
        // Они навсегда остаются в истории (лимит 50) для возможности прокрутки по нажатию T.
        if (!isChatOpen) {
            this.scrollOffset = 0.0; // Всегда сбрасываем скролл при закрытом чате
        }

        for (Notification n : notifications) {
            long age = now - n.startTime;

            // Плавное выезжание слева
            float targetX = 12f;
            n.animX += (targetX - n.animX) * 0.16f;

            // Настройка прозрачности для фонового затухания на экране
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