package com.mogdop.mod.client.gui;

import net.minecraft.client.gui.screen.Screen;
import org.jetbrains.annotations.Nullable;

public class MogDopConfigScreen extends SpawnerScreen {
    public MogDopConfigScreen(@Nullable Screen parent) {
        super(parent, 4); // Открывает SpawnerScreen напрямую на вкладке Настройки (индекс 4)
    }
}