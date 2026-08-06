package com.mogdop.mod.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record SpawnEntityPayload(
        String entityId,
        String customName,
        boolean nameVisible,
        boolean noGravity,
        boolean silent,
        boolean glowing,
        boolean isBaby,
        int slimeSize,
        int fireTicks
) implements CustomPayload {
    
    public static final Id<SpawnEntityPayload> ID = new Id<>(Identifier.of("mogdops-mod", "spawn_entity"));
    
    // ИСПРАВЛЕНИЕ: Ручная запись и чтение буфера в обход лимита в 6 параметров
    public static final PacketCodec<RegistryByteBuf, SpawnEntityPayload> CODEC = new PacketCodec<RegistryByteBuf, SpawnEntityPayload>() {
        @Override
        public void encode(RegistryByteBuf buf, SpawnEntityPayload value) {
            PacketCodecs.STRING.encode(buf, value.entityId());
            PacketCodecs.STRING.encode(buf, value.customName());
            PacketCodecs.BOOL.encode(buf, value.nameVisible());
            PacketCodecs.BOOL.encode(buf, value.noGravity());
            PacketCodecs.BOOL.encode(buf, value.silent());
            PacketCodecs.BOOL.encode(buf, value.glowing());
            PacketCodecs.BOOL.encode(buf, value.isBaby());
            PacketCodecs.INTEGER.encode(buf, value.slimeSize());
            PacketCodecs.INTEGER.encode(buf, value.fireTicks());
        }

        @Override
        public SpawnEntityPayload decode(RegistryByteBuf buf) {
            return new SpawnEntityPayload(
                PacketCodecs.STRING.decode(buf),
                PacketCodecs.STRING.decode(buf),
                PacketCodecs.BOOL.decode(buf),
                PacketCodecs.BOOL.decode(buf),
                PacketCodecs.BOOL.decode(buf),
                PacketCodecs.BOOL.decode(buf),
                PacketCodecs.BOOL.decode(buf),
                PacketCodecs.INTEGER.decode(buf),
                PacketCodecs.INTEGER.decode(buf)
            );
        }
    };

    @Override
    public Id<? extends CustomPayload> getId() { 
        return ID; 
    }
}