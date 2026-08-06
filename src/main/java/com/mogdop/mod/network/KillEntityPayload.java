package com.mogdop.mod.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record KillEntityPayload(String entityUuidStr, String entityTypeId, boolean killAll) implements CustomPayload {
    
    public static final CustomPayload.Id<KillEntityPayload> ID = new CustomPayload.Id<>(Identifier.of("mogdops-mod", "kill_entity"));
    
    public static final PacketCodec<RegistryByteBuf, KillEntityPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING, KillEntityPayload::entityUuidStr,
            PacketCodecs.STRING, KillEntityPayload::entityTypeId,
            PacketCodecs.BOOL, KillEntityPayload::killAll,
            KillEntityPayload::new
    );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}