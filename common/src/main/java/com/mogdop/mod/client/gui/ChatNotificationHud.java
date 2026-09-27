package com.mogdop.mod.client.gui;

import com.mogdop.mod.client.MogDopSModClient;
import com.mogdop.mod.client.NotificationManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.OrderedText;

public class ChatNotificationHud {

    /** Длительность анимации открытия чата, мс. */
    private static final long OPEN_ANIM_MS = 260L;

    private boolean wasChatOpen = false;
    private long chatOpenedAt = 0L;

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

    private static int withAlpha(int argb, float mul) {
        int a = (int) (((argb >>> 24) & 0xFF) * Math.max(0f, Math.min(1f, mul)));
        return (a << 24) | (argb & 0xFFFFFF);
    }

    public void render(DrawContext drawContext, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        if (!(MogDopSModClient.CONFIG.enableCustomNotifications() && MogDopSModClient.CONFIG.hideChatHUD())) {
            wasChatOpen = false;
            return;
        }
        NotificationManager manager = MogDopSModClient.getNotificationManager();
        manager.rewrapAll();
        java.util.List<NotificationManager.Notification> list = manager.getNotifications();
        if (list.isEmpty()) {
            wasChatOpen = false;
            return;
        }
        boolean isChatOpen = client.currentScreen instanceof net.minecraft.client.gui.screen.ChatScreen;

        // Анимация открытия: при переходе в ChatScreen стартует easeOutCubic 0→1
        long now = System.currentTimeMillis();
        if (isChatOpen && !wasChatOpen) chatOpenedAt = now;
        wasChatOpen = isChatOpen;
        float openEase = 1f;
        if (isChatOpen) {
            float t = Math.min(1f, (now - chatOpenedAt) / (float) OPEN_ANIM_MS);
            openEase = 1f - (1f - t) * (1f - t) * (1f - t);
        }

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

        // Видимость каждого сообщения: в открытом чате все видны, иначе — по opacity.
        // Высота слота масштабируется видимостью, поэтому подложка плавно
        // уменьшается вслед за затухающими сообщениями, а не оставляет пустоту.
        float bgFade = isChatOpen ? 1.0f : 0.0f;
        int contentH = padding * 2;
        for (int i = endIndex; i >= startIndex; i--) {
            float vis = isChatOpen ? 1f : list.get(i).opacity;
            if (!isChatOpen) bgFade = Math.max(bgFade, vis);
            contentH += Math.round(list.get(i).cardHeight * vis);
            if (i > startIndex) contentH += Math.round(4 * vis);
        }
        if (!isChatOpen && bgFade <= 0.01f) return;

        int bgHFull = Math.min(bgHMax, contentH);
        // При открытии подложка вырастает снизу вверх от нижнего края
        int screenH = client.getWindow().getScaledHeight();
        int bottomEdge = screenH - 25;
        int bgH = isChatOpen ? Math.round(bgHFull * openEase) : bgHFull;
        int y = bottomEdge - bgH;

        if (isBgEnabled() && bgH > 0) {
            float alphaMul = isChatOpen ? openEase : bgFade;
            int alpha = (int) (getBgOpacity() * alphaMul);
            if (alpha > 0) {
                drawContext.fill(x, y, x + bgW, y + bgH, (alpha << 24) | 0x101015);
            }
        }

        // Сообщения поверх подложки, снизу вверх (новые — внизу, край прибит к низу)
        float alphaMul = isChatOpen ? openEase : 1f;
        int curBottom = y + bgH - padding;
        for (int i = endIndex; i >= startIndex; i--) {
            NotificationManager.Notification n = list.get(i);
            int h = n.cardHeight;
            float vis = isChatOpen ? 1f : n.opacity;
            int effH = Math.round(h * vis);
            if (effH <= 0) continue;

            float currentOpacity = isChatOpen ? 1.0f : vis;
            // Плавное проявление при раскрытии: строка гаснет, пока слот за краем подложки
            float reveal = Math.max(0f, Math.min(1f, (curBottom - y) / (float) Math.max(1, h)));
            int a = (int) (currentOpacity * 255 * alphaMul * reveal);
            // Поддерживаем слайд-анимацию появления сдвигом текста (без отдельных карточек)
            float slide = Math.max(0f, Math.min(1f, (n.animX + 320f) / 332f));
            int textX = x + padding - (int) ((1f - slide) * 20);
            int cardTop = curBottom - effH;

            if (a > 0 && cardTop + effH > y && cardTop < y + bgH) {
                // Текстовый блок прибит к низу слота: при схлопывании текст стоит
                // на месте, а пустота сверху исчезает
                int lineY = curBottom - 6 - n.lines.size() * 10;
                for (OrderedText line : n.lines) {
                    int textColor = (a << 24) | 0xFFFFFF;
                    drawContext.drawTextWithShadow(client.textRenderer, line, textX, lineY, textColor);
                    lineY += 10;
                }
            }
            curBottom -= (effH + (i > startIndex ? Math.round(4 * vis) : 0));
            if (curBottom < y) break;
        }

        if (isChatOpen && list.size() > 1 && openEase > 0.05f) {
            int sbX = x + bgW + 6;
            int sbYBottom = y + bgH;
            int sbHeight = Math.min(120, bgH);
            int sbYTop = sbYBottom - sbHeight;

            drawContext.fill(sbX, sbYTop, sbX + 2, sbYBottom, withAlpha(0x33FFFFFF, openEase));

            int thumbHeight = Math.max(15, sbHeight * Math.min(maxToRender, list.size()) / list.size());
            int scrollSpace = sbHeight - thumbHeight;
            int thumbY = maxScroll > 0
                    ? sbYBottom - thumbHeight - (scrollSpace * scrollOffset / maxScroll)
                    : sbYBottom - thumbHeight;

            drawContext.fill(sbX, thumbY, sbX + 2, thumbY + thumbHeight, withAlpha(parseAccent(), openEase));
        }
    }
}
