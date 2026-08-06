package com.mogdop.mod.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record RedoPayload() implements CustomPayload {
    public static final Id<RedoPayload> ID = new Id<>(Identifier.of("mogdops-mod", "redo"));
    public static final PacketCodec<RegistryByteBuf, RedoPayload> CODEC = PacketCodec.unit(new RedoPayload());
    @Override public Id<? extends CustomPayload> getId() { return ID; }
}