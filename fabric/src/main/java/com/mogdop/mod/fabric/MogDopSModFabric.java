package com.mogdop.mod.fabric;

import com.mogdop.mod.MogDopSMod;
import net.fabricmc.api.ModInitializer;

public class MogDopSModFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        // Инициализация общего ядра мода
        MogDopSMod.init();
    }
}