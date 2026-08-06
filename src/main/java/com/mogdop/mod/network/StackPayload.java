package com.mogdop.mod.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record StackPayload(BlockPos pos1, BlockPos pos2, int count, String directionStr) implements CustomPayload {
    public static final CustomPayload.Id<StackPayload> ID = new CustomPayload.Id<>(Identifier.of("mogdops-mod", "stack"));

    public static final PacketCodec<RegistryByteBuf, StackPayload> CODEC = new PacketCodec<>() {
        @Override
        public void encode(RegistryByteBuf buf, StackPayload value) {
            BlockPos.PACKET_CODEC.encode(buf, value.pos1());
            BlockPos.PACKET_CODEC.encode(buf, value.pos2());
            PacketCodecs.INTEGER.encode(buf, value.count());
            PacketCodecs.STRING.encode(buf, value.directionStr());
        }

        @Override
        public StackPayload decode(RegistryByteBuf buf) {
            return new StackPayload(
                    BlockPos.PACKET_CODEC.decode(buf),
                    BlockPos.PACKET_CODEC.decode(buf),
                    PacketCodecs.INTEGER.decode(buf),
                    PacketCodecs.STRING.decode(buf)
            );
        }
    };

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}