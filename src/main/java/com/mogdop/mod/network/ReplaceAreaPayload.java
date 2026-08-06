package com.mogdop.mod.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public record ReplaceAreaPayload(List<BlockPos> points, int selectionMode, String targetBlockId, String replacementBlockId) implements CustomPayload {
    public static final CustomPayload.Id<ReplaceAreaPayload> ID = new CustomPayload.Id<>(Identifier.of("mogdops-mod", "replace_area"));

    public static final PacketCodec<RegistryByteBuf, ReplaceAreaPayload> CODEC = new PacketCodec<>() {
        @Override
        public void encode(RegistryByteBuf buf, ReplaceAreaPayload value) {
            PacketCodecs.INTEGER.encode(buf, value.points().size());
            for (BlockPos p : value.points()) {
                BlockPos.PACKET_CODEC.encode(buf, p);
            }
            PacketCodecs.INTEGER.encode(buf, value.selectionMode());
            PacketCodecs.STRING.encode(buf, value.targetBlockId());
            PacketCodecs.STRING.encode(buf, value.replacementBlockId());
        }

        @Override
        public ReplaceAreaPayload decode(RegistryByteBuf buf) {
            int count = PacketCodecs.INTEGER.decode(buf);
            List<BlockPos> list = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                list.add(BlockPos.PACKET_CODEC.decode(buf));
            }
            int mode = PacketCodecs.INTEGER.decode(buf);
            String targetId = PacketCodecs.STRING.decode(buf);
            String replaceId = PacketCodecs.STRING.decode(buf);
            return new ReplaceAreaPayload(list, mode, targetId, replaceId);
        }
    };

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}