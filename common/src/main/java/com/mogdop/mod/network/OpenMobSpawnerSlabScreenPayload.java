package com.mogdop.mod.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record OpenMobSpawnerSlabScreenPayload(
        BlockPos pos, 
        String mobId, 
        int spawnInterval, 
        int maxMobs, 
        boolean active, 
        int spawnRange
) implements CustomPayload {
    
    public static final Id<OpenMobSpawnerSlabScreenPayload> ID = new Id<>(Identifier.of("mogdops-mod", "open_mob_spawner_slab_screen"));
    
    public static final PacketCodec<RegistryByteBuf, OpenMobSpawnerSlabScreenPayload> CODEC = new PacketCodec<RegistryByteBuf, OpenMobSpawnerSlabScreenPayload>() {
        @Override
        public void encode(RegistryByteBuf buf, OpenMobSpawnerSlabScreenPayload value) {
            BlockPos.PACKET_CODEC.encode(buf, value.pos());
            PacketCodecs.STRING.encode(buf, value.mobId());
            PacketCodecs.INTEGER.encode(buf, value.spawnInterval());
            PacketCodecs.INTEGER.encode(buf, value.maxMobs());
            PacketCodecs.BOOL.encode(buf, value.active());
            PacketCodecs.INTEGER.encode(buf, value.spawnRange());
        }

        @Override
        public OpenMobSpawnerSlabScreenPayload decode(RegistryByteBuf buf) {
            return new OpenMobSpawnerSlabScreenPayload(
                BlockPos.PACKET_CODEC.decode(buf),
                PacketCodecs.STRING.decode(buf),
                PacketCodecs.INTEGER.decode(buf),
                PacketCodecs.INTEGER.decode(buf),
                PacketCodecs.BOOL.decode(buf),
                PacketCodecs.INTEGER.decode(buf)
            );
        }
    };

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}