package com.mogdop.mod.client;

import com.mogdop.mod.client.gui.SpawnerScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> new SpawnerScreen(parent, 4); // Напрямую открывает меню на вкладке Настроек (индекс 4)
    }
}