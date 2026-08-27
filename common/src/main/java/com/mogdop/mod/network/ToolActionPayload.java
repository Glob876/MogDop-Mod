package com.mogdop.mod.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record ToolActionPayload(
        String action,
        BlockPos pos,
        float explosionPower,
        boolean explosionFire,
        int removerRadius
) implements CustomPayload {
    
    public static final Id<ToolActionPayload> ID = new Id<>(Identifier.of("mogdops-mod", "tool_action"));
    
    public static final PacketCodec<RegistryByteBuf, ToolActionPayload> CODEC = new PacketCodec<RegistryByteBuf, ToolActionPayload>() {
        @Override
        public void encode(RegistryByteBuf buf, ToolActionPayload value) {
            net.minecraft.network.codec.PacketCodecs.STRING.encode(buf, value.action());
            BlockPos.PACKET_CODEC.encode(buf, value.pos());
            net.minecraft.network.codec.PacketCodecs.FLOAT.encode(buf, value.explosionPower());
            net.minecraft.network.codec.PacketCodecs.BOOL.encode(buf, value.explosionFire());
            net.minecraft.network.codec.PacketCodecs.INTEGER.encode(buf, value.removerRadius());
        }

        @Override
        public ToolActionPayload decode(RegistryByteBuf buf) {
            return new ToolActionPayload(
                net.minecraft.network.codec.PacketCodecs.STRING.decode(buf),
                BlockPos.PACKET_CODEC.decode(buf),
                net.minecraft.network.codec.PacketCodecs.FLOAT.decode(buf),
                net.minecraft.network.codec.PacketCodecs.BOOL.decode(buf),
                net.minecraft.network.codec.PacketCodecs.INTEGER.decode(buf)
            );
        }
    };

    @Override
    public Id<? extends CustomPayload> getId() { 
        return ID; 
    }
}