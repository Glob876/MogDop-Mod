package com.mogdop.mod.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record RotateClipboardPayload(int degrees) implements CustomPayload {
    public static final CustomPayload.Id<RotateClipboardPayload> ID = new CustomPayload.Id<>(Identifier.of("mogdops-mod", "rotate_clipboard"));

    public static final PacketCodec<RegistryByteBuf, RotateClipboardPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.INTEGER, RotateClipboardPayload::degrees,
            RotateClipboardPayload::new
    );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}