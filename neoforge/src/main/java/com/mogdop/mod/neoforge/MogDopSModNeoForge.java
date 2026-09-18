package com.mogdop.mod.neoforge;

import com.mogdop.mod.neoforge.client.MogDopSModNeoForgeClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod("mogdopsmod")
public class MogDopSModNeoForge {

    public MogDopSModNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        // Common-ядро вызываем через рефлексию: прямой import com.mogdop.mod.MogDopSMod
        // невозможен — common скомпилирован в Yarn, NeoForge в Mojang (class_xxx не резолвится).
        // На рантайме common-байткод уже ремапнут transformProductionNeoForge в Mojang.
        try {
            Class.forName("com.mogdop.mod.MogDopSMod").getMethod("init").invoke(null);
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger("mogdopsmod").error("Common MogDopSMod.init failed", e);
            throw new RuntimeException(e);
        }

        // В ванильные вкладки — через NeoForge-событие. Предметы берём рефлексией:
        // прямой import common MogDopSMod невозможен (Yarn vs Mojang).
        // Architectury CreativeTabRegistry.append НЕ используем (см. комментарий в common).
        modEventBus.addListener((net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent event) -> {
            try {
                Class<?> modClass = Class.forName("com.mogdop.mod.MogDopSMod");
                Object staffSupplier = modClass.getField("STAFF").get(null);
                Object slabSupplier = modClass.getField("MOB_SPAWNER_SLAB_ITEM").get(null);
                Object staffItem = staffSupplier.getClass().getMethod("get").invoke(staffSupplier);
                Object slabItem = slabSupplier.getClass().getMethod("get").invoke(slabSupplier);
                if (event.getTabKey().equals(net.minecraft.world.item.CreativeModeTabs.TOOLS_AND_UTILITIES)) {
                    event.accept((net.minecraft.world.item.Item) staffItem);
                } else if (event.getTabKey().equals(net.minecraft.world.item.CreativeModeTabs.FUNCTIONAL_BLOCKS)) {
                    event.accept((net.minecraft.world.item.Item) slabItem);
                }
            } catch (Exception e) {
                org.slf4j.LoggerFactory.getLogger("mogdopsmod").error("BuildCreativeModeTabContents failed", e);
            }
        });

        // 2. Клиентский init
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modContainer.registerExtensionPoint(IConfigScreenFactory.class,
                    (container, screen) -> {
                        try {
                            Class<?> cls = Class.forName("com.mogdop.mod.client.gui.SpawnerScreen");
                            // Используем рефлексию чтобы обойти Yarn vs Mojang несоответствие Screen типа
                            for (var ctor : cls.getConstructors()) {
                                if (ctor.getParameterCount() == 2) {
                                    return (net.minecraft.client.gui.screens.Screen) ctor.newInstance(screen, 4);
                                }
                            }
                            return screen;
                        } catch (Exception e) {
                            org.slf4j.LoggerFactory.getLogger("mogdopsmod").error("IConfigScreenFactory failed", e);
                            return screen;
                        }
                    });

            MogDopSModNeoForgeClient.init(modEventBus);
        }
    }
}
