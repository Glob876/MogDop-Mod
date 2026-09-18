package com.mogdop.mod.platform.fabric;

import com.mogdop.mod.MobSpawnerSlabBlock;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Blocks;

public class PlatformImpl {
    public static net.minecraft.block.Block createSpawnerSlabBlock() {
        return new MobSpawnerSlabBlock(AbstractBlock.Settings.copy(Blocks.STONE_SLAB).nonOpaque());
    }
}
