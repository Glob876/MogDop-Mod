package com.mogdop.mod.client.gui;

import com.mogdop.mod.client.MogDopSModClient;
import com.mogdop.mod.client.NotificationManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.OrderedText;

public class ChatNotificationHud {

    public static int getBgW() {
        try {
            return MogDopSModClient.CONFIG.chat().widthPx;
        } catch (Throwable t) {
            return 310;
        }
    }

    public static int getBgHMax() {
        try {
            return MogDopSModClient.CONFIG.chat().heightPx;
        } catch (Throwable t) {
            return 180;
        }
    }

    public static int getPadding() {
        try {
            int p = MogDopSModClient.CONFIG.chat().padding;
            return Math.max(4, Math.min(7, p));
        } catch (Throwable t) {
            return 6;
        }
    }

    public static int getBgOpacity() {
        try {
            return MogDopSModClient.CONFIG.chat().bgOpacity;
        } catch (Throwable t) {
            return 0xAA;
        }
    }

    public static boolean isBgEnabled() {
        try {
            return MogDopSModClient.CONFIG.chat().bgEnabled;
        } catch (Throwable t) {
            return true;
        }
    }

    public static int parseAccent() {
        try {
            String hex = MogDopSModClient.CONFIG.chat().accentColor;
            if (hex.startsWith("#")) hex = hex.substring(1);
            return (0xFF << 24) | Integer.parseInt(hex, 16);
        } catch (Throwable t) {
            return 0xFF00C8FF;
        }
    }

    public void render(DrawContext drawContext, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        if (MogDopSModClient.CONFIG.enableCustomNotifications() && MogDopSModClient.CONFIG.hideChatHUD()) {
            NotificationManager manager = MogDopSModClient.getNotificationManager();
            manager.rewrapAll();
            java.util.List<NotificationManager.Notification> list = manager.getNotifications();
            if (list.isEmpty()) return;
            boolean isChatOpen = client.currentScreen instanceof net.minecraft.client.gui.screen.ChatScreen;

            int bgW = getBgW();
            int bgHMax = getBgHMax();
            int padding = getPadding();
            int x = 12;

            int maxToRender = isChatOpen ? 15 : 8;
            int scrollOffset = isChatOpen ? manager.getScrollOffset() : 0;

            int maxScroll = Math.max(0, list.size() - 1);
            if (scrollOffset > maxScroll) {
                scrollOffset = maxScroll;
                manager.setScrollOffset(maxScroll);
            }

            int endIndex = list.size() - 1 - scrollOffset;
            int startIndex = Math.max(0, endIndex - maxToRender + 1);

            // Одна общая подложка: высота по контенту, но не выше лимита из конфига
            int contentH = padding * 2;
            for (int i = endIndex; i >= startIndex; i--) {
                contentH += list.get(i).cardHeight + 4;
            }
            if (endIndex >= startIndex) contentH -= 4;
            int bgH = Math.min(bgHMax, contentH);
            int screenH = client.getWindow().getScaledHeight();
            int y = screenH - bgH - 25;

            // Альфа подложки: из конфига, при затухании — по максимальной среди видимых
            float bgFade = isChatOpen ? 1.0f : 0.0f;
            if (!isChatOpen) {
                for (int i = endIndex; i >= startIndex; i--) {
                    bgFade = Math.max(bgFade, list.get(i).opacity);
                }
            }
            if (isBgEnabled() && bgFade > 0.01f) {
                int alpha = (int) (getBgOpacity() * bgFade);
                if (alpha > 0) {
                    drawContext.fill(x, y, x + bgW, y + bgH, (alpha << 24) | 0x101015);
                }
            }

            // Сообщения поверх подложки, снизу вверх (новые — внизу)
            int curBottom = y + bgH - padding;
            for (int i = endIndex; i >= startIndex; i--) {
                NotificationManager.Notification n = list.get(i);
                int h = n.cardHeight;

                float currentOpacity = isChatOpen ? 1.0f : n.opacity;
                int opacityByte = (int) (currentOpacity * 255);
                // Поддерживаем слайд-анимацию появления сдвигом текста (без отдельных карточек)
                float slide = Math.max(0f, Math.min(1f, (n.animX + 320f) / 332f));
                int textX = x + padding - (int) ((1f - slide) * 20);
                int cardTop = curBottom - h;

                if (opacityByte > 0 && cardTop + h > y && cardTop < y + bgH) {
                    int lineY = cardTop + 6;
                    for (OrderedText line : n.lines) {
                        int textColor = (opacityByte << 24) | 0xFFFFFF;
                        drawContext.drawTextWithShadow(client.textRenderer, line, textX, lineY, textColor);
                        lineY += 10;
                    }
                }
                curBottom -= (h + 4);
                if (curBottom < y) break;
            }

            if (isChatOpen && list.size() > 1) {
                int sbX = x + bgW + 6;
                int sbYBottom = y + bgH;
                int sbHeight = Math.min(120, bgH);
                int sbYTop = sbYBottom - sbHeight;

                drawContext.fill(sbX, sbYTop, sbX + 2, sbYBottom, 0x33FFFFFF);

                int thumbHeight = Math.max(15, sbHeight * Math.min(maxToRender, list.size()) / list.size());
                int scrollSpace = sbHeight - thumbHeight;
                int thumbY = maxScroll > 0
                        ? sbYBottom - thumbHeight - (scrollSpace * scrollOffset / maxScroll)
                        : sbYBottom - thumbHeight;

                drawContext.fill(sbX, thumbY, sbX + 2, thumbY + thumbHeight, parseAccent());
            }
        }
    }
}
