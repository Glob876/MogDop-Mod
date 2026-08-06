package com.mogdop.mod.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record SchematicPreviewPayload(int sizeX, int sizeY, int sizeZ, String filename) implements CustomPayload {
    public static final CustomPayload.Id<SchematicPreviewPayload> ID = new CustomPayload.Id<>(Identifier.of("mogdops-mod", "schematic_preview"));

    public static final PacketCodec<RegistryByteBuf, SchematicPreviewPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.INTEGER, SchematicPreviewPayload::sizeX,
            PacketCodecs.INTEGER, SchematicPreviewPayload::sizeY,
            PacketCodecs.INTEGER, SchematicPreviewPayload::sizeZ,
            PacketCodecs.STRING, SchematicPreviewPayload::filename,
            SchematicPreviewPayload::new
    );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}