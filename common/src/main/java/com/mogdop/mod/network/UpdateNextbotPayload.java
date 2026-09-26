package com.mogdop.mod.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record UpdateNextbotPayload(
        String entityUuidStr,
        String fileName,
        float speed,
        float damage
) implements CustomPayload {

    public static final Id<UpdateNextbotPayload> ID = new Id<>(Identifier.of("mogdopsmod", "update_nextbot"));

    public static final PacketCodec<RegistryByteBuf, UpdateNextbotPayload> CODEC = new PacketCodec<>() {
        @Override
        public void encode(RegistryByteBuf buf, UpdateNextbotPayload value) {
            PacketCodecs.STRING.encode(buf, value.entityUuidStr());
            PacketCodecs.STRING.encode(buf, value.fileName());
            PacketCodecs.FLOAT.encode(buf, value.speed());
            PacketCodecs.FLOAT.encode(buf, value.damage());
        }

        @Override
        public UpdateNextbotPayload decode(RegistryByteBuf buf) {
            return new UpdateNextbotPayload(
                    PacketCodecs.STRING.decode(buf),
                    PacketCodecs.STRING.decode(buf),
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
