package com.mogdop.mod.neoforge;

import com.mogdop.mod.MogDopSMod;
import com.mogdop.mod.client.gui.SpawnerScreen;
import com.mogdop.mod.neoforge.client.MogDopSModNeoForgeClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(MogDopSMod.MOD_ID)
public class MogDopSModNeoForge {

    public MogDopSModNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        // 1. Инициализация общего ядра мода
        MogDopSMod.init();

        // 2. Клиентская инициализация под NeoForge
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modContainer.registerExtensionPoint(IConfigScreenFactory.class,
                    (container, screen) -> new SpawnerScreen(screen, 4));

            MogDopSModNeoForgeClient.init(modEventBus);
        }
    }
}