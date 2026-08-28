package com.mogdop.mod;

import com.mogdop.mod.entity.ImageDisplayEntity;
import com.mogdop.mod.network.*;
import com.mogdop.mod.worldedit.WorldEditIntegration;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import dev.architectury.networking.NetworkManager;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.command.CommandManager;
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

public class MogDopSMod {
    public static final String MOD_ID = "mogdops-mod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    // Архитектурные регистры
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(MOD_ID, RegistryKeys.ITEM);
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(MOD_ID, RegistryKeys.BLOCK);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(MOD_ID, RegistryKeys.BLOCK_ENTITY_TYPE);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(MOD_ID, RegistryKeys.ENTITY_TYPE);

    // 1. Творческий посох
    public static final RegistrySupplier<Item> STAFF = ITEMS.register("staff",
            () -> new Item(new Item.Settings().maxCount(1)));

    // 2. Спавнер-полублок
    public static final RegistrySupplier<Block> MOB_SPAWNER_SLAB = BLOCKS.register("mob_spawner_slab",
            () -> new MobSpawnerSlabBlock(AbstractBlock.Settings.copy(Blocks.STONE_SLAB).nonOpaque()));

    public static final RegistrySupplier<Item> MOB_SPAWNER_SLAB_ITEM = ITEMS.register("mob_spawner_slab",
            () -> new BlockItem(MOB_SPAWNER_SLAB.get(), new Item.Settings()));

    public static final RegistrySupplier<BlockEntityType<MobSpawnerSlabBlockEntity>> MOB_SPAWNER_SLAB_ENTITY = BLOCK_ENTITY_TYPES.register("mob_spawner_slab",
            () -> BlockEntityType.Builder.create(MobSpawnerSlabBlockEntity::new, MOB_SPAWNER_SLAB.get()).build(null));

    // 3. Сущность отображения картинок
    public static final RegistrySupplier<EntityType<ImageDisplayEntity>> IMAGE_DISPLAY_ENTITY = ENTITY_TYPES.register("image_display",
            () -> EntityType.Builder.<ImageDisplayEntity>create(ImageDisplayEntity::new, SpawnGroup.MISC)
                    .dimensions(0.5F, 0.5F)
                    .maxTrackingRange(12)
                    .trackingTickInterval(10)
                    .build("image_display"));

    public static void init() {
        LOGGER.info("MogDop's Mod Initializing on Architectury Multiplatform Engine v0.2.0...");

        // Применяем регистрации
        BLOCKS.register();
        ITEMS.register();
        BLOCK_ENTITY_TYPES.register();
        ENTITY_TYPES.register();

        // Регистрация серверной команды /md staff
        CommandRegistrationEvent.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("md")
                    .then(CommandManager.literal("staff")
                            .executes(ctx -> {
                                ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
                                ItemStack stack = new ItemStack(STAFF.get());
                                if (!player.getInventory().insertStack(stack)) {
                                    player.dropItem(stack, false);
                                }
                                player.currentScreenHandler.syncState();
                                ctx.getSource().sendFeedback(() -> Text.literal("§a[MogDop] Выдан Творческий Посох!"), false);
                                return 1;
                            })
                    )
            );
        });

        // Регистрация сетевых обработчиков
        registerNetworkReceivers();
    }

    private static void registerNetworkReceivers() {
        // Обработчик картинок
        NetworkManager.registerReceiver(NetworkManager.c2s(), SpawnImagePayload.ID, SpawnImagePayload.CODEC, (payload, context) -> {
            ServerPlayerEntity player = (ServerPlayerEntity) context.getPlayer();
            ServerWorld world = (ServerWorld) player.getWorld();
            world.getServer().execute(() -> {
                ImageDisplayEntity entity = IMAGE_DISPLAY_ENTITY.get().create(world);
                if (entity != null) {
                    Vec3d p1 = new Vec3d(payload.p1x(), payload.p1y(), payload.p1z());
                    Vec3d p2 = new Vec3d(payload.p2x(), payload.p2y(), payload.p2z());
                    Direction facing = Direction.byId(payload.facingId());
                    entity.setImageData(payload.imageName(), p1, p2, facing);
                    world.spawnEntity(entity);
                }
            });
        });

        // Обработчик действий инструментов
        NetworkManager.registerReceiver(NetworkManager.c2s(), ToolActionPayload.ID, ToolActionPayload.CODEC, (payload, context) -> {
            ServerPlayerEntity player = (ServerPlayerEntity) context.getPlayer();
            ServerWorld world = (ServerWorld) player.getWorld();
            world.getServer().execute(() -> {
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
                    case "TELEPORT" -> player.teleport(world, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D, player.getYaw(), player.getPitch());
                }
            });
        });

        // Обработчик спавна мобов
        NetworkManager.registerReceiver(NetworkManager.c2s(), SpawnEntityPayload.ID, SpawnEntityPayload.CODEC, (payload, context) -> {
            ServerPlayerEntity player = (ServerPlayerEntity) context.getPlayer();
            ServerWorld world = (ServerWorld) player.getWorld();
            world.getServer().execute(() -> {
                HitResult hitResult = player.raycast(64.0D, 1.0F, false);
                Vec3d spawnPos = hitResult.getPos();

                if (hitResult.getType() == HitResult.Type.BLOCK) {
                    BlockHitResult blockHit = (BlockHitResult) hitResult;
                    Direction side = blockHit.getSide();
                    spawnPos = spawnPos.add(side.getOffsetX() * 0.5, side.getOffsetY() * 0.1, side.getOffsetZ() * 0.5);
                } else {
                    spawnPos = player.getEyePos().add(player.getRotationVec(1.0F).multiply(3.0D));
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
            });
        });

        // Обработчик выдачи предметов
        NetworkManager.registerReceiver(NetworkManager.c2s(), GiveItemPayload.ID, GiveItemPayload.CODEC, (payload, context) -> {
            ServerPlayerEntity player = (ServerPlayerEntity) context.getPlayer();
            ServerWorld world = (ServerWorld) player.getWorld();
            world.getServer().execute(() -> {
                ItemStack stack = payload.stack().copy();
                if (stack.isEmpty()) return;
                if (!player.getInventory().insertStack(stack)) {
                    player.dropItem(stack, false);
                }
                player.currentScreenHandler.syncState();
            });
        });

        // Обработчики WorldEdit API
        NetworkManager.registerReceiver(NetworkManager.c2s(), FillAreaPayload.ID, FillAreaPayload.CODEC, (payload, context) -> {
            ServerPlayerEntity player = (ServerPlayerEntity) context.getPlayer();
            ServerWorld world = (ServerWorld) player.getWorld();
            world.getServer().execute(() -> WorldEditIntegration.fillArea(player, payload.points(), payload.selectionMode(), payload.blockId()));
        });

        NetworkManager.registerReceiver(NetworkManager.c2s(), WallsPayload.ID, WallsPayload.CODEC, (payload, context) -> {
            ServerPlayerEntity player = (ServerPlayerEntity) context.getPlayer();
            ServerWorld world = (ServerWorld) player.getWorld();
            world.getServer().execute(() -> WorldEditIntegration.walls(player, payload.points(), payload.selectionMode(), payload.blockId()));
        });

        NetworkManager.registerReceiver(NetworkManager.c2s(), OutlinePayload.ID, OutlinePayload.CODEC, (payload, context) -> {
            ServerPlayerEntity player = (ServerPlayerEntity) context.getPlayer();
            ServerWorld world = (ServerWorld) player.getWorld();
            world.getServer().execute(() -> WorldEditIntegration.outline(player, payload.points(), payload.selectionMode(), payload.blockId()));
        });

        NetworkManager.registerReceiver(NetworkManager.c2s(), ReplaceAreaPayload.ID, ReplaceAreaPayload.CODEC, (payload, context) -> {
            ServerPlayerEntity player = (ServerPlayerEntity) context.getPlayer();
            ServerWorld world = (ServerWorld) player.getWorld();
            world.getServer().execute(() -> WorldEditIntegration.replaceArea(player, payload.points(), payload.selectionMode(), payload.targetBlockId(), payload.replacementBlockId()));
        });

        NetworkManager.registerReceiver(NetworkManager.c2s(), UndoPayload.ID, UndoPayload.CODEC, (payload, context) -> {
            ServerPlayerEntity player = (ServerPlayerEntity) context.getPlayer();
            ServerWorld world = (ServerWorld) player.getWorld();
            world.getServer().execute(() -> WorldEditIntegration.undo(player));
        });

        NetworkManager.registerReceiver(NetworkManager.c2s(), RedoPayload.ID, RedoPayload.CODEC, (payload, context) -> {
            ServerPlayerEntity player = (ServerPlayerEntity) context.getPlayer();
            ServerWorld world = (ServerWorld) player.getWorld();
            world.getServer().execute(() -> WorldEditIntegration.redo(player));
        });

        NetworkManager.registerReceiver(NetworkManager.c2s(), DrainPayload.ID, DrainPayload.CODEC, (payload, context) -> {
            ServerPlayerEntity player = (ServerPlayerEntity) context.getPlayer();
            ServerWorld world = (ServerWorld) player.getWorld();
            world.getServer().execute(() -> WorldEditIntegration.drain(player, payload.radius()));
        });
    }
}