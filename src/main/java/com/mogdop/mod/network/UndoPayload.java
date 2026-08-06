package com.mogdop.mod.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record UndoPayload() implements CustomPayload {
    public static final Id<UndoPayload> ID = new Id<>(Identifier.of("mogdops-mod", "undo"));
    public static final PacketCodec<RegistryByteBuf, UndoPayload> CODEC = PacketCodec.unit(new UndoPayload());
    @Override public Id<? extends CustomPayload> getId() { return ID; }
}