package com.mogdop.mod.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record SpawnImagePayload(
        String imageName,
        double p1x, double p1y, double p1z,
        double p2x, double p2y, double p2z,
        int facingId
) implements CustomPayload {

    public static final Id<SpawnImagePayload> ID = new Id<>(Identifier.of("mogdops-mod", "spawn_image"));

    public static final PacketCodec<RegistryByteBuf, SpawnImagePayload> CODEC = new PacketCodec<>() {
        @Override
        public void encode(RegistryByteBuf buf, SpawnImagePayload value) {
            PacketCodecs.STRING.encode(buf, value.imageName());
            PacketCodecs.DOUBLE.encode(buf, value.p1x());
            PacketCodecs.DOUBLE.encode(buf, value.p1y());
            PacketCodecs.DOUBLE.encode(buf, value.p1z());
            PacketCodecs.DOUBLE.encode(buf, value.p2x());
            PacketCodecs.DOUBLE.encode(buf, value.p2y());
            PacketCodecs.DOUBLE.encode(buf, value.p2z());
            PacketCodecs.INTEGER.encode(buf, value.facingId());
        }

        @Override
        public SpawnImagePayload decode(RegistryByteBuf buf) {
            return new SpawnImagePayload(
                    PacketCodecs.STRING.decode(buf),
                    PacketCodecs.DOUBLE.decode(buf),
                    PacketCodecs.DOUBLE.decode(buf),
                    PacketCodecs.DOUBLE.decode(buf),
                    PacketCodecs.DOUBLE.decode(buf),
                    PacketCodecs.DOUBLE.decode(buf),
                    PacketCodecs.DOUBLE.decode(buf),
                    PacketCodecs.INTEGER.decode(buf)
            );
        }
    };

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}