package com.mogdop.mod;

import com.mogdop.mod.entity.ImageDisplayEntity;
import com.mogdop.mod.network.*;
import com.mogdop.mod.worldedit.WorldEditIntegration;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

public class MogDopSMod implements ModInitializer {
    public static final String MOD_ID = "mogdops-mod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Block MOB_SPAWNER_SLAB;
    public static BlockEntityType<MobSpawnerSlabBlockEntity> MOB_SPAWNER_SLAB_ENTITY;
    public static EntityType<ImageDisplayEntity> IMAGE_DISPLAY_ENTITY;

    @Override
    public void onInitialize() {
        LOGGER.info("MogDop's Mod Initializing v0.2.0 with Schematics & WorldEdit API & Image Entities...");

        MOB_SPAWNER_SLAB = Registry.register(Registries.BLOCK, Identifier.of(MOD_ID, "mob_spawner_slab"),
                new MobSpawnerSlabBlock(AbstractBlock.Settings.copy(Blocks.STONE_SLAB).nonOpaque()));

        Registry.register(Registries.ITEM, Identifier.of(MOD_ID, "mob_spawner_slab"),
                new BlockItem(MOB_SPAWNER_SLAB, new Item.Settings()));

        MOB_SPAWNER_SLAB_ENTITY = Registry.register(Registries.BLOCK_ENTITY_TYPE, Identifier.of(MOD_ID, "mob_spawner_slab"),
                BlockEntityType.Builder.create(MobSpawnerSlabBlockEntity::new, MOB_SPAWNER_SLAB).build());

        IMAGE_DISPLAY_ENTITY = Registry.register(Registries.ENTITY_TYPE, Identifier.of(MOD_ID, "image_display"),
                EntityType.Builder.<ImageDisplayEntity>create(ImageDisplayEntity::new, SpawnGroup.MISC)
                        .dimensions(0.5F, 0.5F)
                        .maxTrackingRange(12)
                        .trackingTickInterval(10)
                        .build());

        // Регистрация C2S Пакетов
        PayloadTypeRegistry.playC2S().register(SpawnEntityPayload.ID, SpawnEntityPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(KillEntityPayload.ID, KillEntityPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(GiveItemPayload.ID, GiveItemPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(WorldActionPayload.ID, WorldActionPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(PlayerActionPayload.ID, PlayerActionPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(FillAreaPayload.ID, FillAreaPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ReplaceAreaPayload.ID, ReplaceAreaPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(UndoPayload.ID, UndoPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(RedoPayload.ID, RedoPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ToolActionPayload.ID, ToolActionPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(UpdateMobSpawnerSlabPayload.ID, UpdateMobSpawnerSlabPayload.CODEC);

        PayloadTypeRegistry.playC2S().register(WallsPayload.ID, WallsPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(OutlinePayload.ID, OutlinePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(StackPayload.ID, StackPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(DrainPayload.ID, DrainPayload.CODEC);

        // C2S Пакеты Схематик
        PayloadTypeRegistry.playC2S().register(CopyClipboardPayload.ID, CopyClipboardPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(PasteClipboardPayload.ID, PasteClipboardPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(RotateClipboardPayload.ID, RotateClipboardPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(SaveSchematicPayload.ID, SaveSchematicPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(LoadSchematicPayload.ID, LoadSchematicPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(RequestSchematicsListPayload.ID, RequestSchematicsListPayload.CODEC);

        // C2S Пакет Изображений
        PayloadTypeRegistry.playC2S().register(SpawnImagePayload.ID, SpawnImagePayload.CODEC);

        // S2C Пакеты
        PayloadTypeRegistry.playS2C().register(OpenMobSpawnerSlabScreenPayload.ID, OpenMobSpawnerSlabScreenPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SyncSchematicsListPayload.ID, SyncSchematicsListPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SchematicPreviewPayload.ID, SchematicPreviewPayload.CODEC);

        // ОБРАБОТЧИК ИЗОБРАЖЕНИЙ
        ServerPlayNetworking.registerGlobalReceiver(SpawnImagePayload.ID, (payload, context) -> context.server().execute(() -> {
            ServerWorld world = context.player().getServerWorld();
            ImageDisplayEntity entity = IMAGE_DISPLAY_ENTITY.create(world);
            if (entity != null) {
                Vec3d p1 = new Vec3d(payload.p1x(), payload.p1y(), payload.p1z());
                Vec3d p2 = new Vec3d(payload.p2x(), payload.p2y(), payload.p2z());
                Direction facing = Direction.byId(payload.facingId());
                entity.setImageData(payload.imageName(), p1, p2, facing);
                world.spawnEntity(entity);
            }
        }));

        // ОБРАБОТЧИКИ СХЕМАТИК И БУФЕРА ОБМЕНА
        ServerPlayNetworking.registerGlobalReceiver(CopyClipboardPayload.ID, (CopyClipboardPayload payload, ServerPlayNetworking.Context context) -> context.server().execute(() -> {
            WorldEditIntegration.copyClipboard(context.player(), payload.points(), payload.selectionMode());
        }));

        ServerPlayNetworking.registerGlobalReceiver(PasteClipboardPayload.ID, (PasteClipboardPayload payload, ServerPlayNetworking.Context context) -> context.server().execute(() -> {
            WorldEditIntegration.pasteClipboard(context.player(), payload.ignoreAir());
        }));

        ServerPlayNetworking.registerGlobalReceiver(RotateClipboardPayload.ID, (RotateClipboardPayload payload, ServerPlayNetworking.Context context) -> context.server().execute(() -> {
            WorldEditIntegration.rotateClipboard(context.player(), payload.degrees());
        }));

        ServerPlayNetworking.registerGlobalReceiver(SaveSchematicPayload.ID, (SaveSchematicPayload payload, ServerPlayNetworking.Context context) -> context.server().execute(() -> {
            if (!payload.points().isEmpty()) {
                WorldEditIntegration.copyClipboard(context.player(), payload.points(), payload.selectionMode());
            }
            WorldEditIntegration.saveSchematic(context.player(), payload.filename());
            List<String> files = WorldEditIntegration.getSchematicsList();
            ServerPlayNetworking.send(context.player(), new SyncSchematicsListPayload(files));
        }));

        ServerPlayNetworking.registerGlobalReceiver(LoadSchematicPayload.ID, (LoadSchematicPayload payload, ServerPlayNetworking.Context context) -> context.server().execute(() -> {
            Vec3i dims = WorldEditIntegration.loadSchematic(context.player(), payload.filename());
            if (dims != null) {
                ServerPlayNetworking.send(context.player(), new SchematicPreviewPayload(dims.getX(), dims.getY(), dims.getZ(), payload.filename()));
            }
        }));

        ServerPlayNetworking.registerGlobalReceiver(RequestSchematicsListPayload.ID, (RequestSchematicsListPayload payload, ServerPlayNetworking.Context context) -> context.server().execute(() -> {
            List<String> files = WorldEditIntegration.getSchematicsList();
            ServerPlayNetworking.send(context.player(), new SyncSchematicsListPayload(files));
        }));

        // ДЕЛЕГИРОВАНИЕ ВСЕХ ЗАПРОСОВ WORLDEDIT В ОФИЦИАЛЬНЫЙ WORLDEDIT API
        ServerPlayNetworking.registerGlobalReceiver(WallsPayload.ID, (WallsPayload payload, ServerPlayNetworking.Context context) -> context.server().execute(() -> {
            WorldEditIntegration.walls(context.player(), payload.points(), payload.selectionMode(), payload.blockId());
        }));

        ServerPlayNetworking.registerGlobalReceiver(OutlinePayload.ID, (OutlinePayload payload, ServerPlayNetworking.Context context) -> context.server().execute(() -> {
            WorldEditIntegration.outline(context.player(), payload.points(), payload.selectionMode(), payload.blockId());
        }));

        ServerPlayNetworking.registerGlobalReceiver(ReplaceAreaPayload.ID, (ReplaceAreaPayload payload, ServerPlayNetworking.Context context) -> context.server().execute(() -> {
            WorldEditIntegration.replaceArea(context.player(), payload.points(), payload.selectionMode(), payload.targetBlockId(), payload.replacementBlockId());
        }));

        ServerPlayNetworking.registerGlobalReceiver(FillAreaPayload.ID, (FillAreaPayload payload, ServerPlayNetworking.Context context) -> context.server().execute(() -> {
            WorldEditIntegration.fillArea(context.player(), payload.points(), payload.selectionMode(), payload.blockId());
        }));

        ServerPlayNetworking.registerGlobalReceiver(UndoPayload.ID, (UndoPayload payload, ServerPlayNetworking.Context context) -> context.server().execute(() -> {
            WorldEditIntegration.undo(context.player());
        }));

        ServerPlayNetworking.registerGlobalReceiver(RedoPayload.ID, (RedoPayload payload, ServerPlayNetworking.Context context) -> context.server().execute(() -> {
            WorldEditIntegration.redo(context.player());
        }));

        ServerPlayNetworking.registerGlobalReceiver(DrainPayload.ID, (DrainPayload payload, ServerPlayNetworking.Context context) -> context.server().execute(() -> {
            WorldEditIntegration.drain(context.player(), payload.radius());
        }));

        ServerPlayNetworking.registerGlobalReceiver(UpdateMobSpawnerSlabPayload.ID, (UpdateMobSpawnerSlabPayload payload, ServerPlayNetworking.Context context) -> context.server().execute(() -> {
            ServerWorld world = context.player().getServerWorld();
            BlockPos pos = payload.pos();
            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof MobSpawnerSlabBlockEntity slabBe) {
                slabBe.setMobId(payload.mobId());
                slabBe.setSpawnInterval(payload.spawnInterval());
                slabBe.setMaxMobs(payload.maxMobs());
                slabBe.setActive(payload.active());
                slabBe.setSpawnRange(payload.spawnRange());
                slabBe.markDirty();
                world.getChunkManager().markForUpdate(pos);
            }
        }));

        ServerPlayNetworking.registerGlobalReceiver(ToolActionPayload.ID, (ToolActionPayload payload, ServerPlayNetworking.Context context) -> context.server().execute(() -> {
            ServerPlayerEntity player = context.player();
            ServerWorld world = player.getServerWorld();
            BlockPos pos = payload.pos();
            String action = payload.action();

            switch (action) {
                case "REMOVER" -> {
                    int r = payload.removerRadius();
                    if (r <= 1) {
                        world.setBlockState(pos, Blocks.AIR.getDefaultState());
                    } else {
                        int minX = pos.getX() - r + 1;
                        int maxX = pos.getX() + r - 1;
                        int minY = pos.getY() - r + 1;
                        int maxY = pos.getY() + r - 1;
                        int minZ = pos.getZ() - r + 1;
                        int maxZ = pos.getZ() + r - 1;
                        for (int x = minX; x <= maxX; x++) {
                            for (int y = minY; y <= maxY; y++) {
                                for (int z = minZ; z <= maxZ; z++) {
                                    world.setBlockState(new BlockPos(x, y, z), Blocks.AIR.getDefaultState());
                                }
                            }
                        }
                    }
                }
                case "EXPLOSION" -> {
                    float power = payload.explosionPower();
                    boolean fire = payload.explosionFire();
                    world.createExplosion(player, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, power, fire, net.minecraft.world.World.ExplosionSourceType.TNT);
                }
                case "TELEPORT" -> player.teleport(pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D, true);
            }
        }));

        ServerPlayNetworking.registerGlobalReceiver(WorldActionPayload.ID, (WorldActionPayload payload, ServerPlayNetworking.Context context) -> context.server().execute(() -> {
            ServerWorld world = context.player().getServerWorld();
            String act = payload.action();
            switch (act) {
                case "DAY" -> world.setTimeOfDay(1000);
                case "NIGHT" -> world.setTimeOfDay(13000);
                case "SUN" -> world.setWeather(12000, 0, false, false);
                case "RAIN" -> world.setWeather(0, 12000, true, false);
            }
        }));

        ServerPlayNetworking.registerGlobalReceiver(PlayerActionPayload.ID, (PlayerActionPayload payload, ServerPlayNetworking.Context context) -> context.server().execute(() -> {
            ServerPlayerEntity player = context.player();
            String act = payload.action();
            switch (act) {
                case "FLY" -> {
                    boolean current = player.getAbilities().allowFlying;
                    player.getAbilities().allowFlying = !current;
                    if (!player.getAbilities().allowFlying) player.getAbilities().flying = false;
                    player.sendAbilitiesUpdate();
                }
                case "SPEED" -> player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 1200, 2));
                case "STRENGTH" -> player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 1200, 2));
                case "CLEAR" -> player.clearStatusEffects();
            }
        }));

        ServerPlayNetworking.registerGlobalReceiver(SpawnEntityPayload.ID, (SpawnEntityPayload payload, ServerPlayNetworking.Context context) -> context.server().execute(() -> {
            ServerPlayerEntity player = context.player();
            ServerWorld world = player.getServerWorld();
            HitResult hitResult = player.raycast(50.0D, 1.0F, false);
            Vec3d spawnPos = hitResult.getPos();

            if (hitResult.getType() == HitResult.Type.BLOCK) {
                BlockHitResult blockHit = (BlockHitResult) hitResult;
                Direction side = blockHit.getSide();
                spawnPos = spawnPos.add(side.getOffsetX() * 0.5, side.getOffsetY() * 0.1, side.getOffsetZ() * 0.5);
            }

            Identifier id = Identifier.of(payload.entityId());
            EntityType<?> entityType = Registries.ENTITY_TYPE.get(id);

            Entity entity = entityType.create(world);
            if (entity != null) {
                if (!payload.customName().isEmpty()) {
                    entity.setCustomName(Text.literal(payload.customName()));
                    entity.setCustomNameVisible(payload.nameVisible());
                }
                entity.setNoGravity(payload.noGravity());
                entity.setSilent(payload.silent());
                entity.setGlowing(payload.glowing());

                if (payload.isBaby()) {
                    if (entity instanceof net.minecraft.entity.passive.PassiveEntity passive) {
                        passive.setBaby(true);
                    } else if (entity instanceof net.minecraft.entity.mob.ZombieEntity zombie) {
                        zombie.setBaby(true);
                    }
                }

                if (entity instanceof net.minecraft.entity.mob.SlimeEntity slime && payload.slimeSize() > 0) {
                    slime.setSize(payload.slimeSize(), true);
                }

                if (payload.fireTicks() > 0) {
                    entity.setOnFireFor(payload.fireTicks());
                }

                entity.refreshPositionAndAngles(spawnPos.x, spawnPos.y, spawnPos.z, player.getYaw(), 0.0F);
                world.spawnEntity(entity);
            }
        }));

        ServerPlayNetworking.registerGlobalReceiver(GiveItemPayload.ID, (GiveItemPayload payload, ServerPlayNetworking.Context context) -> context.server().execute(() -> context.player().getInventory().offerOrDrop(payload.stack())));

        ServerPlayNetworking.registerGlobalReceiver(KillEntityPayload.ID, (KillEntityPayload payload, ServerPlayNetworking.Context context) -> context.server().execute(() -> {
            ServerPlayerEntity player = context.player();
            ServerWorld world = player.getServerWorld();
            if (payload.killAll()) {
                Identifier id = Identifier.of(payload.entityTypeId());
                EntityType<?> type = Registries.ENTITY_TYPE.get(id);

                Box box = player.getBoundingBox().expand(50.0D);
                for (Entity entity : world.getOtherEntities(player, box, e -> e.getType() == type)) {
                    entity.discard();
                }
            } else {
                try {
                    Entity entity = world.getEntity(UUID.fromString(payload.entityUuidStr()));
                    if (entity != null) entity.discard();
                } catch (Exception ignored) {}
            }
        }));
    }
}