package com.mogdop.mod.client.gui;

import com.mogdop.mod.client.MogDopSModClient;
import com.mogdop.mod.client.NotificationManager;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.OrderedText;

public class ChatNotificationHud implements HudRenderCallback {

    @Override
    public void onHudRender(DrawContext drawContext, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        // Рендер кастомных всплывающих уведомлений чата слева
        if (MogDopSModClient.CONFIG.enableCustomNotifications() && MogDopSModClient.CONFIG.hideChatHUD()) {
            java.util.List<NotificationManager.Notification> list = MogDopSModClient.getNotificationManager().getNotifications();
            boolean isChatOpen = client.currentScreen instanceof net.minecraft.client.gui.screen.ChatScreen;

            int y = client.getWindow().getScaledHeight() - 65; // Отступ снизу
            if (isChatOpen) {
                y -= 25; // Сдвигаем вверх, освобождая место под строку ввода
            }

            int maxToRender = isChatOpen ? 15 : 8;
            int scrollOffset = isChatOpen ? MogDopSModClient.getNotificationManager().getScrollOffset() : 0;

            // Убедимся, что scrollOffset не выходит за рамки текущего размера списка
            int maxScroll = Math.max(0, list.size() - 1);
            if (scrollOffset > maxScroll) {
                scrollOffset = maxScroll;
                MogDopSModClient.getNotificationManager().setScrollOffset(maxScroll);
            }

            // Вычисляем диапазон индексов сообщений, которые нужно отрисовать
            int endIndex = list.size() - 1 - scrollOffset;
            int startIndex = Math.max(0, endIndex - maxToRender + 1);

            for (int i = endIndex; i >= startIndex; i--) {
                NotificationManager.Notification n = list.get(i);
                int x = (int) n.animX;
                int h = n.cardHeight;

                // Если чат открыт — игнорируем время жизни и рисуем со 100% непрозрачностью
                float currentOpacity = isChatOpen ? 1.0f : n.opacity;
                int opacityByte = (int) (currentOpacity * 255);
                if (opacityByte <= 0) continue;

                // Применяем альфа-канал ко всем слоям
                int alphaBG = (int) (0xAA * currentOpacity) << 24;
                int alphaBorder = (int) (0x28 * currentOpacity) << 24;
                int alphaAccent = (int) (0xFF * currentOpacity) << 24;

                // 1. Отрисовка подложки карточки (Glassmorphism)
                drawContext.fill(x, y - h, x + 310, y, alphaBG | 0x101015);

                // 2. Отрисовка тонкой полупрозрачной обводки
                drawContext.fill(x, y - h, x + 310, y - h + 1, alphaBorder | 0xFFFFFF); // Верх
                drawContext.fill(x, y - h, x + 1, y, alphaBorder | 0xFFFFFF); // Лево
                drawContext.fill(x, y, x + 310, y + 1, alphaBorder | 0xFFFFFF); // Низ
                drawContext.fill(x + 310, y - h, x + 311, y, alphaBorder | 0xFFFFFF); // Право

                // 3. Неоновый левый акцент (Cyan)
                drawContext.fill(x + 1, y - h + 1, x + 3, y - 1, alphaAccent | 0xFF00C8FF);

                // 4. Отрисовка строк текста сообщения
                int lineY = y - h + 6;
                for (OrderedText line : n.lines) {
                    int textColor = (opacityByte << 24) | 0xFFFFFF;
                    drawContext.drawTextWithShadow(client.textRenderer, line, x + 8, lineY, textColor);
                    lineY += 10;
                }

                y -= (h + 4);
            }

            // Отрисовываем тонкую современную полосу прокрутки (скроллбар) справа от сообщений
            if (isChatOpen && list.size() > 1) {
                int sbX = 12 + 310 + 6; // Справа от карточек
                int sbYBottom = client.getWindow().getScaledHeight() - 90;
                int sbHeight = 120; // Фиксированная высота полосы скроллбара
                int sbYTop = sbYBottom - sbHeight;

                // Фоновая полоса скроллбара
                drawContext.fill(sbX, sbYTop, sbX + 2, sbYBottom, 0x33FFFFFF);

                // Бегунок скроллбара
                int thumbHeight = Math.max(15, sbHeight * Math.min(maxToRender, list.size()) / list.size());
                int scrollSpace = sbHeight - thumbHeight;
                int thumbY = sbYBottom - thumbHeight - (scrollSpace * scrollOffset / maxScroll);

                drawContext.fill(sbX, thumbY, sbX + 2, thumbY + thumbHeight, 0xFF00C8FF);
            }
        }
    }
}