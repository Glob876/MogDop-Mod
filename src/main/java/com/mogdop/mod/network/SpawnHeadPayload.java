package com.mogdop.mod.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record SpawnHeadPayload(String headName, String uuidStr, String textureValue, boolean isCustomTexture) implements CustomPayload {
    
    public static final CustomPayload.Id<SpawnHeadPayload> ID = new CustomPayload.Id<>(Identifier.of("mogdops-mod", "spawn_head"));
    
    public static final PacketCodec<RegistryByteBuf, SpawnHeadPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING, SpawnHeadPayload::headName,
            PacketCodecs.STRING, SpawnHeadPayload::uuidStr,
            PacketCodecs.STRING, SpawnHeadPayload::textureValue,
            PacketCodecs.BOOL, SpawnHeadPayload::isCustomTexture,
            SpawnHeadPayload::new
    );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { 
        return ID; 
    }
}