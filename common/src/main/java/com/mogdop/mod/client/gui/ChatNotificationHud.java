package com.mogdop.mod.client.gui;

import com.mogdop.mod.client.MogDopSModClient;
import com.mogdop.mod.client.NotificationManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.OrderedText;

public class ChatNotificationHud {

    public void render(DrawContext drawContext, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        if (MogDopSModClient.CONFIG.enableCustomNotifications() && MogDopSModClient.CONFIG.hideChatHUD()) {
            java.util.List<NotificationManager.Notification> list = MogDopSModClient.getNotificationManager().getNotifications();
            boolean isChatOpen = client.currentScreen instanceof net.minecraft.client.gui.screen.ChatScreen;

            int y = client.getWindow().getScaledHeight() - 65;
            if (isChatOpen) {
                y -= 25;
            }

            int maxToRender = isChatOpen ? 15 : 8;
            int scrollOffset = isChatOpen ? MogDopSModClient.getNotificationManager().getScrollOffset() : 0;

            int maxScroll = Math.max(0, list.size() - 1);
            if (scrollOffset > maxScroll) {
                scrollOffset = maxScroll;
                MogDopSModClient.getNotificationManager().setScrollOffset(maxScroll);
            }

            int endIndex = list.size() - 1 - scrollOffset;
            int startIndex = Math.max(0, endIndex - maxToRender + 1);

            for (int i = endIndex; i >= startIndex; i--) {
                NotificationManager.Notification n = list.get(i);
                int x = (int) n.animX;
                int h = n.cardHeight;

                float currentOpacity = isChatOpen ? 1.0f : n.opacity;
                int opacityByte = (int) (currentOpacity * 255);
                if (opacityByte <= 0) continue;

                int alphaBG = (int) (0xAA * currentOpacity) << 24;
                int alphaBorder = (int) (0x28 * currentOpacity) << 24;
                int alphaAccent = (int) (0xFF * currentOpacity) << 24;

                drawContext.fill(x, y - h, x + 310, y, alphaBG | 0x101015);
                drawContext.fill(x, y - h, x + 310, y - h + 1, alphaBorder | 0xFFFFFF);
                drawContext.fill(x, y - h, x + 1, y, alphaBorder | 0xFFFFFF);
                drawContext.fill(x, y, x + 310, y + 1, alphaBorder | 0xFFFFFF);
                drawContext.fill(x + 310, y - h, x + 311, y, alphaBorder | 0xFFFFFF);

                drawContext.fill(x + 1, y - h + 1, x + 3, y - 1, alphaAccent | 0xFF00C8FF);

                int lineY = y - h + 6;
                for (OrderedText line : n.lines) {
                    int textColor = (opacityByte << 24) | 0xFFFFFF;
                    drawContext.drawTextWithShadow(client.textRenderer, line, x + 8, lineY, textColor);
                    lineY += 10;
                }

                y -= (h + 4);
            }

            if (isChatOpen && list.size() > 1) {
                int sbX = 12 + 310 + 6;
                int sbYBottom = client.getWindow().getScaledHeight() - 90;
                int sbHeight = 120;
                int sbYTop = sbYBottom - sbHeight;

                drawContext.fill(sbX, sbYTop, sbX + 2, sbYBottom, 0x33FFFFFF);

                int thumbHeight = Math.max(15, sbHeight * Math.min(maxToRender, list.size()) / list.size());
                int scrollSpace = sbHeight - thumbHeight;
                int thumbY = sbYBottom - thumbHeight - (scrollSpace * scrollOffset / maxScroll);

                drawContext.fill(sbX, thumbY, sbX + 2, thumbY + thumbHeight, 0xFF00C8FF);
            }
        }
    }
}