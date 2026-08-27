package com.mogdop.mod.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record DrainPayload(int radius) implements CustomPayload {
    public static final CustomPayload.Id<DrainPayload> ID = new CustomPayload.Id<>(Identifier.of("mogdops-mod", "drain"));

    public static final PacketCodec<RegistryByteBuf, DrainPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.INTEGER, DrainPayload::radius,
            DrainPayload::new
    );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}