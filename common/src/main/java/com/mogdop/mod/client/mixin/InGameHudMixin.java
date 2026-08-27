package com.mogdop.mod.client.mixin;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class InGameHudMixin {

    @Shadow
    private Text overlayMessage;

    @Shadow
    private int overlayRemaining;

    @Shadow
    private boolean overlayTinted;

    @Inject(method = "renderOverlayMessage", at = @At("HEAD"), cancellable = true)
    private void onRenderOverlayMessage(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (this.overlayMessage != null && this.overlayRemaining > 0) {
            String str = this.overlayMessage.getString();
            if (str.contains("MogDopWE") || str.contains("[MogDopWE]")) {
                ci.cancel(); // Перехватываем стандартный рендер снизу для сообщений WorldEdit

                InGameHud hud = (InGameHud) (Object) this;
                TextRenderer font = hud.getTextRenderer();

                int opacity = (int) ((float) this.overlayRemaining * 255.0F / 20.0F);
                if (opacity > 255) opacity = 255;

                if (opacity > 8) {
                    int textWidth = font.getWidth(this.overlayMessage);
                    int centerX = context.getScaledWindowWidth() / 2;
                    int y = 15; // РЕНДЕР СТРОГО ПО ЦЕНТРУ ЭКРАНА ВВЕРХУ

                    int bgWidth = textWidth + 20;
                    int bgHeight = 18;
                    int x1 = centerX - bgWidth / 2;
                    int x2 = centerX + bgWidth / 2;
                    int y1 = y - 4;
                    int y2 = y + bgHeight - 4;

                    int alphaBG = (int) (0xCC * (opacity / 255.0f)) << 24;
                    int alphaBorder = (int) (0x44 * (opacity / 255.0f)) << 24;
                    int alphaAccent = (int) (0xFF * (opacity / 255.0f)) << 24;

                    // Полупрозрачная стеклянная карточка по центру вверху
                    context.fill(x1, y1, x2, y2, alphaBG | 0x101018);
                    context.fill(x1, y1, x2, y1 + 1, alphaBorder | 0xFFFFFF);
                    context.fill(x1, y2 - 1, x2, y2, alphaBorder | 0xFFFFFF);
                    context.fill(x1, y1, x1 + 1, y2, alphaBorder | 0xFFFFFF);
                    context.fill(x2 - 1, y1, x2, y2, alphaBorder | 0xFFFFFF);

                    // Верхний неоновый акцент
                    context.fill(x1 + 2, y1, x2 - 2, y1 + 2, alphaAccent | 0x00C8FF);

                    int color = (opacity << 24) | 0xFFFFFF;
                    context.drawTextWithShadow(font, this.overlayMessage, centerX - textWidth / 2, y + 1, color);
                }
            }
        }
    }
}