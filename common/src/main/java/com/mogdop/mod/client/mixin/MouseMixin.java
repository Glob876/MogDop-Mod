package com.mogdop.mod.client.mixin;

import com.mogdop.mod.client.MogDopSModClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class MouseMixin {

    // Внедряемся в базовый метод скроллинга мыши GLFW
    @Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true)
    private void onScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            // 1. Если открыт чат (ChatScreen) — напрямую прокручиваем историю сообщений
            if (client.currentScreen instanceof ChatScreen) {
                if (vertical != 0) {
                    MogDopSModClient.getNotificationManager().scroll(vertical);
                    ci.cancel(); // Отменяем стандартную прокрутку
                }
            }
            // 2. Если экраны закрыты, игрок держит Топор
            else if (client.currentScreen == null) {
                if (MogDopSModClient.isSelectionAxe(client.player.getMainHandStack())) {

                    // АЛЬТ + СКРОЛЛ: Изменение размера выделения в сторону направленной грани!
                    if (Screen.hasAltDown() && MogDopSModClient.currentToolMode == 0) {
                        Direction face = MogDopSModClient.getTargetedSelectionFace();
                        if (face != null && vertical != 0) {
                            int delta = (int) Math.signum(vertical);
                            MogDopSModClient.expandSelectionFace(face, delta);
                            ci.cancel(); // Отменяем смену слотов в хотбаре
                            return;
                        }
                    }

                    // СТРЛ + СКРОЛЛ: Переключение режимов мультитула
                    if (Screen.hasControlDown()) {
                        if (vertical > 0) {
                            MogDopSModClient.cycleToolMode(1); // Листаем вперед
                        } else if (vertical < 0) {
                            MogDopSModClient.cycleToolMode(-1); // Листаем назад
                        }
                        ci.cancel(); // Отменяем смену слотов в хотбаре
                    }
                }
            }
        }
    }
}