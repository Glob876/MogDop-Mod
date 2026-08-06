package com.mogdop.mod.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record RequestSchematicsListPayload() implements CustomPayload {
    public static final CustomPayload.Id<RequestSchematicsListPayload> ID = new CustomPayload.Id<>(Identifier.of("mogdops-mod", "request_schematics"));
    public static final PacketCodec<RegistryByteBuf, RequestSchematicsListPayload> CODEC = PacketCodec.unit(new RequestSchematicsListPayload());

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
