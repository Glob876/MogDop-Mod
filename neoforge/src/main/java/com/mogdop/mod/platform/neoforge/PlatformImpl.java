package com.mogdop.mod.platform.neoforge;

import com.mogdop.mod.neoforge.NeoForgeMobSpawnerSlabBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class PlatformImpl {
    public static net.minecraft.world.level.block.Block createSpawnerSlabBlock() {
        return new NeoForgeMobSpawnerSlabBlock(BlockBehaviour.Properties.of().mapColor(Blocks.STONE_SLAB.defaultMapColor()).noOcclusion());
    }
}
