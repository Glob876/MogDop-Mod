package com.mogdop.mod.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record SpawnNextbotPayload(
        String fileName,
        double x, double y, double z,
        float speed,
        float damage
) implements CustomPayload {

    public static final Id<SpawnNextbotPayload> ID = new Id<>(Identifier.of("mogdopsmod", "spawn_nextbot"));

    public static final PacketCodec<RegistryByteBuf, SpawnNextbotPayload> CODEC = new PacketCodec<>() {
        @Override
        public void encode(RegistryByteBuf buf, SpawnNextbotPayload value) {
            PacketCodecs.STRING.encode(buf, value.fileName());
            PacketCodecs.DOUBLE.encode(buf, value.x());
            PacketCodecs.DOUBLE.encode(buf, value.y());
            PacketCodecs.DOUBLE.encode(buf, value.z());
            PacketCodecs.FLOAT.encode(buf, value.speed());
            PacketCodecs.FLOAT.encode(buf, value.damage());
        }

        @Override
        public SpawnNextbotPayload decode(RegistryByteBuf buf) {
            return new SpawnNextbotPayload(
                    PacketCodecs.STRING.decode(buf),
                    PacketCodecs.DOUBLE.decode(buf),
                    PacketCodecs.DOUBLE.decode(buf),
                    PacketCodecs.DOUBLE.decode(buf),
                    PacketCodecs.FLOAT.decode(buf),
                    PacketCodecs.FLOAT.decode(buf)
            );
        }
    };

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
