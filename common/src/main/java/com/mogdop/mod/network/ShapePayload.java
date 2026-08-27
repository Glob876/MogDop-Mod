package com.mogdop.mod.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record ShapePayload(String shapeType, BlockPos pos, String blockId, int radius, int height, boolean hollow) implements CustomPayload {
    public static final CustomPayload.Id<ShapePayload> ID = new CustomPayload.Id<>(Identifier.of("mogdops-mod", "shape"));

    public static final PacketCodec<RegistryByteBuf, ShapePayload> CODEC = new PacketCodec<>() {
        @Override
        public void encode(RegistryByteBuf buf, ShapePayload value) {
            PacketCodecs.STRING.encode(buf, value.shapeType());
            BlockPos.PACKET_CODEC.encode(buf, value.pos());
            PacketCodecs.STRING.encode(buf, value.blockId());
            PacketCodecs.INTEGER.encode(buf, value.radius());
            PacketCodecs.INTEGER.encode(buf, value.height());
            PacketCodecs.BOOL.encode(buf, value.hollow());
        }

        @Override
        public ShapePayload decode(RegistryByteBuf buf) {
            return new ShapePayload(
                    PacketCodecs.STRING.decode(buf),
                    BlockPos.PACKET_CODEC.decode(buf),
                    PacketCodecs.STRING.decode(buf),
                    PacketCodecs.INTEGER.decode(buf),
                    PacketCodecs.INTEGER.decode(buf),
                    PacketCodecs.BOOL.decode(buf)
            );
        }
    };

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}