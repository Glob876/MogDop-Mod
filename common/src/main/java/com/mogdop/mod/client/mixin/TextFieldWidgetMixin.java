package com.mogdop.mod.client.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TextFieldWidget.class)
public class TextFieldWidgetMixin {

    @Shadow
    private int editableColor;

    @Shadow
    private int uneditableColor;

    @Shadow
    private int firstCharacterIndex;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        // По умолчанию неактивные поля окрашиваются в темно-серый цвет (0x707070).
        // Принудительно устанавливаем белый цвет для обоих состояний!
        this.editableColor = 0xFFFFFF;
        this.uneditableColor = 0xFFFFFF;
    }

    @Inject(method = "renderWidget", at = @At("HEAD"))
    private void onRenderWidget(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        TextFieldWidget self = (TextFieldWidget) (Object) this;
        // Если текстовое поле не в фокусе, сбрасываем смещение прокрутки на 0,
        // чтобы текст всегда был прижат к левому краю и не улетал за границы видимости из-за нулевой ширины при инициализации.
        if (!self.isFocused() && this.firstCharacterIndex > 0) {
            this.firstCharacterIndex = 0;
        }
    }
}