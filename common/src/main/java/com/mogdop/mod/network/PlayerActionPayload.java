package com.mogdop.mod.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record PlayerActionPayload(String action) implements CustomPayload {
    public static final CustomPayload.Id<PlayerActionPayload> ID = new CustomPayload.Id<>(Identifier.of("mogdops-mod", "player_action"));
    public static final PacketCodec<RegistryByteBuf, PlayerActionPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING, PlayerActionPayload::action,
            PlayerActionPayload::new
    );
    @Override public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}