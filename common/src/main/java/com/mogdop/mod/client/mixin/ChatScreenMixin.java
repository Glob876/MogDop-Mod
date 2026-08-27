package com.mogdop.mod.client.mixin;

import com.mogdop.mod.client.MogDopSModClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatScreen.class)
public class ChatScreenMixin {

    @Shadow
    protected TextFieldWidget chatField;

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        if (this.chatField != null) {
            // Отключаем стандартный черный фон текстового поля
            this.chatField.setDrawsBackground(false);
            
            // Кастим к Screen для надежного доступа к ширине и высоте родительского класса Screen
            Screen screen = (Screen) (Object) this;
            int screenWidth = screen.width;
            int screenHeight = screen.height;
            
            // Задаем идеально симметричные отступы по 14 пикселей слева и справа
            int targetX = 14;
            int targetY = screenHeight - 14;
            int targetWidth = screenWidth - 28;
            int targetHeight = 12;
            
            this.chatField.setDimensionsAndPosition(targetWidth, targetHeight, targetX, targetY);
            
            // Установка красивого современного плейсхолдера при пустом поле ввода
            this.chatField.setPlaceholder(Text.translatable("mogdops-mod.chat.placeholder"));
        }
    }

    // Сбрасываем позицию прокрутки при закрытии экрана чата
    @Inject(method = "removed", at = @At("HEAD"))
    private void onRemoved(CallbackInfo ci) {
        MogDopSModClient.getNotificationManager().setScrollOffset(0.0);
    }

    // Удаляем стандартную темно-серую полосу внизу экрана ChatScreen
    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;fill(IIIII)V"))
    private void onDrawContextFill(DrawContext instance, int x1, int y1, int x2, int y2, int color) {
        // Делаем метод пустым, чтобы предотвратить отрисовку стандартной серой полосы внизу экрана
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/widget/TextFieldWidget;render(Lnet/minecraft/client/gui/DrawContext;IIF)V", shift = At.Shift.BEFORE))
    private void onRenderChatFieldBackground(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (this.chatField != null) {
            // Рамка будет идеально симметрична и находиться в 8 пикселях от краев экрана
            int x = this.chatField.getX() - 6;
            int y = this.chatField.getY() - 4;
            int w = this.chatField.getWidth() + 12;
            int h = 18;

            // Рисуем стильный полупрозрачный фон с закруглением/рамкой
            // Задний фон (Glassmorphism):
            context.fill(x, y, x + w, y + h, 0xCC14141A);
            
            // Тонкие рамки вокруг инпута:
            context.fill(x, y, x + w, y + 1, 0x22FFFFFF); // верхняя
            context.fill(x, y, x + 1, y + h, 0x22FFFFFF); // левая
            context.fill(x, y + h - 1, x + w, y + h, 0x22FFFFFF); // нижняя
            context.fill(x + w - 1, y, x + w, y + h, 0x22FFFFFF); // правая

            // Неоновый индикатор активности ввода слева (Cyan):
            context.fill(x + 1, y + 1, x + 3, y + h - 1, 0xFF00C8FF);
        }
    }
}