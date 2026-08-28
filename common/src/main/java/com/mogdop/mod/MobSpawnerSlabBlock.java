package com.mogdop.mod;

import com.mogdop.mod.network.OpenMobSpawnerSlabScreenPayload;
import com.mojang.serialization.MapCodec;
import dev.architectury.networking.NetworkManager;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class MobSpawnerSlabBlock extends BlockWithEntity {

    public static final MapCodec<MobSpawnerSlabBlock> CODEC = createCodec(MobSpawnerSlabBlock::new);
    private static final VoxelShape SHAPE = Block.createCuboidShape(0.0D, 0.0D, 0.0D, 16.0D, 8.0D, 16.0D);

    public MobSpawnerSlabBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return VoxelShapes.empty();
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new MobSpawnerSlabBlockEntity(pos, state);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (!world.isClient()) {
            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof MobSpawnerSlabBlockEntity slabBe) {
                NetworkManager.sendToPlayer((ServerPlayerEntity) player, new OpenMobSpawnerSlabScreenPayload(
                        pos, slabBe.getMobId(), slabBe.getSpawnInterval(), slabBe.getMaxMobs(), slabBe.isActive(), slabBe.getSpawnRange()
                ));
            }
        }
        return ActionResult.SUCCESS;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return validateTicker(type, MogDopSMod.MOB_SPAWNER_SLAB_ENTITY.get(), MobSpawnerSlabBlockEntity::tick);
    }
}