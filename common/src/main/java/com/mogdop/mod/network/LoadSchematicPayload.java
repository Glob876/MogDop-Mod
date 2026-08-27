package com.mogdop.mod.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record LoadSchematicPayload(String filename) implements CustomPayload {
    public static final CustomPayload.Id<LoadSchematicPayload> ID = new CustomPayload.Id<>(Identifier.of("mogdops-mod", "load_schematic"));

    public static final PacketCodec<RegistryByteBuf, LoadSchematicPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING, LoadSchematicPayload::filename,
            LoadSchematicPayload::new
    );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}