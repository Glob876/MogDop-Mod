package com.mogdop.mod.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record WorldActionPayload(String action) implements CustomPayload {
    public static final CustomPayload.Id<WorldActionPayload> ID = new CustomPayload.Id<>(Identifier.of("mogdops-mod", "world_action"));
    public static final PacketCodec<RegistryByteBuf, WorldActionPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING, WorldActionPayload::action,
            WorldActionPayload::new
    );
    @Override public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}