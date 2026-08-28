package com.mogdop.mod;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MobSpawnerSlabBlockEntity extends BlockEntity {

    private String mobId = "minecraft:zombie";
    private int spawnInterval = 100;
    private int maxMobs = 5;
    private int currentSpawnTimer = 0;
    private boolean active = false;
    private int spawnRange = 1;

    private final List<UUID> spawnedUuids = new ArrayList<>();

    public MobSpawnerSlabBlockEntity(BlockPos pos, BlockState state) {
        super(MogDopSMod.MOB_SPAWNER_SLAB_ENTITY.get(), pos, state);
    }

    public String getMobId() { return mobId; }
    public void setMobId(String mobId) { this.mobId = mobId; }

    public int getSpawnInterval() { return spawnInterval; }
    public void setSpawnInterval(int spawnInterval) { this.spawnInterval = spawnInterval; }

    public int getMaxMobs() { return maxMobs; }
    public void setMaxMobs(int maxMobs) { this.maxMobs = maxMobs; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public int getSpawnRange() { return spawnRange; }
    public void setSpawnRange(int spawnRange) { this.spawnRange = spawnRange; }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        nbt.putString("MobId", mobId);
        nbt.putInt("SpawnInterval", spawnInterval);
        nbt.putInt("MaxMobs", maxMobs);
        nbt.putInt("SpawnTimer", currentSpawnTimer);
        nbt.putBoolean("Active", active);
        nbt.putInt("SpawnRange", spawnRange);

        NbtList uuidList = new NbtList();
        for (UUID uuid : spawnedUuids) {
            uuidList.add(NbtHelper.fromUuid(uuid));
        }
        nbt.put("SpawnedUuids", uuidList);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        this.mobId = nbt.getString("MobId");
        if (this.mobId.isEmpty()) this.mobId = "minecraft:zombie";
        this.spawnInterval = nbt.getInt("SpawnInterval");
        if (this.spawnInterval <= 0) this.spawnInterval = 100;
        this.maxMobs = nbt.getInt("MaxMobs");
        if (this.maxMobs <= 0) this.maxMobs = 5;
        this.currentSpawnTimer = nbt.getInt("SpawnTimer");
        this.active = nbt.getBoolean("Active");
        this.spawnRange = nbt.getInt("SpawnRange");
        if (this.spawnRange <= 0) this.spawnRange = 1;

        this.spawnedUuids.clear();
        if (nbt.contains("SpawnedUuids", NbtElement.LIST_TYPE)) {
            NbtList uuidList = nbt.getList("SpawnedUuids", NbtElement.INT_ARRAY_TYPE);
            for (int i = 0; i < uuidList.size(); i++) {
                try {
                    this.spawnedUuids.add(NbtHelper.toUuid(uuidList.get(i)));
                } catch (Exception ignored) {}
            }
        }
    }

    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registryLookup) {
        NbtCompound nbt = new NbtCompound();
        writeNbt(nbt, registryLookup);
        return nbt;
    }

    public static void tick(World world, BlockPos pos, BlockState state, MobSpawnerSlabBlockEntity blockEntity) {
        if (world.isClient()) return;
        if (!blockEntity.active) return;

        blockEntity.spawnedUuids.removeIf(uuid -> {
            Entity entity = ((ServerWorld) world).getEntity(uuid);
            return entity == null || !entity.isAlive();
        });

        blockEntity.currentSpawnTimer++;
        if (blockEntity.currentSpawnTimer >= blockEntity.spawnInterval) {
            blockEntity.currentSpawnTimer = 0;

            if (blockEntity.spawnedUuids.size() < blockEntity.maxMobs) {
                EntityType<?> type = Registries.ENTITY_TYPE.get(Identifier.tryParse(blockEntity.mobId));
                if (type != null) {
                    Entity entity = type.create(world);
                    if (entity != null) {
                        double offsetLimit = blockEntity.spawnRange - 1;
                        double spawnX = pos.getX() + 0.5D + (world.random.nextDouble() * 2.0D - 1.0D) * offsetLimit;
                        double spawnY = pos.getY() + 1.0D;
                        double spawnZ = pos.getZ() + 0.5D + (world.random.nextDouble() * 2.0D - 1.0D) * offsetLimit;

                        entity.refreshPositionAndAngles(spawnX, spawnY, spawnZ, world.random.nextFloat() * 360F, 0.0F);
                        world.spawnEntity(entity);

                        blockEntity.spawnedUuids.add(entity.getUuid());
                        blockEntity.markDirty();
                    }
                }
            }
        }
    }
}