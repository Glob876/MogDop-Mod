package com.mogdop.mod.neoforge;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * NeoForge-нативная версия спавнер-полублока (Mojang-маппинги).
 * Отдельный FQN, чтобы не конфликтовать с common-Yarn классом
 * com.mogdop.mod.MobSpawnerSlabBlock (он едет в shadowJar, но на NeoForge не используется).
 * Прямых ссылок на common-классы с Minecraft-типами в сигнатурах НЕТ —
 * только рефлексия, иначе NeoForge (Mojang) не скомпилируется против common (Yarn).
 * Логика зеркалит common/MobSpawnerSlabBlock: открытие GUI, тикер, half-slab форма.
 */
public class NeoForgeMobSpawnerSlabBlock extends BaseEntityBlock {
    public static final MapCodec<NeoForgeMobSpawnerSlabBlock> CODEC = simpleCodec(NeoForgeMobSpawnerSlabBlock::new);
    private static final VoxelShape SHAPE = box(0.0D, 0.0D, 0.0D, 16.0D, 8.0D, 16.0D);

    public NeoForgeMobSpawnerSlabBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        try {
            Class<?> beClass = Class.forName("com.mogdop.mod.MobSpawnerSlabBlockEntity");
            return (BlockEntity) beClass.getConstructor(BlockPos.class, BlockState.class).newInstance(pos, state);
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger("mogdopsmod").error("newBlockEntity failed", e);
            return null;
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (!world.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            try {
                BlockEntity be = world.getBlockEntity(pos);
                if (be != null && be.getClass().getName().equals("com.mogdop.mod.MobSpawnerSlabBlockEntity")) {
                    String mobId = (String) be.getClass().getMethod("getMobId").invoke(be);
                    int interval = (int) be.getClass().getMethod("getSpawnInterval").invoke(be);
                    int maxMobs = (int) be.getClass().getMethod("getMaxMobs").invoke(be);
                    boolean active = (boolean) be.getClass().getMethod("isActive").invoke(be);
                    int range = (int) be.getClass().getMethod("getSpawnRange").invoke(be);
                    Class<?> payloadClass = Class.forName("com.mogdop.mod.network.OpenMobSpawnerSlabScreenPayload");
                    Object payload = payloadClass.getConstructors()[0].newInstance(pos, mobId, interval, maxMobs, active, range);
                    dev.architectury.networking.NetworkManager.sendToPlayer(serverPlayer,
                            (net.minecraft.network.protocol.common.custom.CustomPacketPayload) payload);
                }
            } catch (Exception e) {
                org.slf4j.LoggerFactory.getLogger("mogdopsmod").error("useWithoutItem failed", e);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Nullable
    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type) {
        try {
            Object beType = Class.forName("com.mogdop.mod.MogDopSMod")
                    .getField("MOB_SPAWNER_SLAB_ENTITY").get(null);
            Object validType = beType.getClass().getMethod("get").invoke(beType);
            if (validType != type) return null;
            Class<?> beClass = Class.forName("com.mogdop.mod.MobSpawnerSlabBlockEntity");
            java.lang.reflect.Method tick = beClass.getMethod("tick", Level.class, BlockPos.class, BlockState.class, beClass);
            return (w, p, s, be) -> {
                try {
                    tick.invoke(null, w, p, s, be);
                } catch (Exception e) {
                    org.slf4j.LoggerFactory.getLogger("mogdopsmod").error("tick failed", e);
                }
            };
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger("mogdopsmod").error("getTicker failed", e);
            return null;
        }
    }
}
