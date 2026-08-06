package com.mogdop.mod.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record PasteClipboardPayload(boolean ignoreAir) implements CustomPayload {
    public static final CustomPayload.Id<PasteClipboardPayload> ID = new CustomPayload.Id<>(Identifier.of("mogdops-mod", "paste_clipboard"));

    public static final PacketCodec<RegistryByteBuf, PasteClipboardPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.BOOL, PasteClipboardPayload::ignoreAir,
            PasteClipboardPayload::new
    );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}