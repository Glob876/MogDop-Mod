package com.mogdop.mod.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public record CopyClipboardPayload(List<BlockPos> points, int selectionMode) implements CustomPayload {
    public static final CustomPayload.Id<CopyClipboardPayload> ID = new CustomPayload.Id<>(Identifier.of("mogdops-mod", "copy_clipboard"));

    public static final PacketCodec<RegistryByteBuf, CopyClipboardPayload> CODEC = new PacketCodec<>() {
        @Override
        public void encode(RegistryByteBuf buf, CopyClipboardPayload value) {
            PacketCodecs.INTEGER.encode(buf, value.points().size());
            for (BlockPos p : value.points()) {
                BlockPos.PACKET_CODEC.encode(buf, p);
            }
            PacketCodecs.INTEGER.encode(buf, value.selectionMode());
        }

        @Override
        public CopyClipboardPayload decode(RegistryByteBuf buf) {
            int count = PacketCodecs.INTEGER.decode(buf);
            List<BlockPos> list = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                list.add(BlockPos.PACKET_CODEC.decode(buf));
            }
            int mode = PacketCodecs.INTEGER.decode(buf);
            return new CopyClipboardPayload(list, mode);
        }
    };

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}