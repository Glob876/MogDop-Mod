package com.mogdop.mod.platform;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.block.Block;

public class Platform {
    @ExpectPlatform
    public static Block createSpawnerSlabBlock() {
        throw new AssertionError();
    }
}
