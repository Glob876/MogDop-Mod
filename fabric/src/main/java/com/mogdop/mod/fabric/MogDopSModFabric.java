package com.mogdop.mod.fabric;

import com.mogdop.mod.MogDopSMod;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.ItemGroups;

public class MogDopSModFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        // Инициализация общего ядра мода
        MogDopSMod.init();

        // В ванильные вкладки — напрямую через Fabric API.
        // Architectury CreativeTabRegistry.append здесь НЕ используем: на связке
        // Architectury 13.0.8 + Fabric API 0.102 он крашит клиент NoSuchMethodError.
        // Колбэки ленивые: STAFF.get() вызывается при построении вкладок, после регистрации.
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> entries.add(MogDopSMod.STAFF.get()));
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(entries -> entries.add(MogDopSMod.MOB_SPAWNER_SLAB_ITEM.get()));
    }
}