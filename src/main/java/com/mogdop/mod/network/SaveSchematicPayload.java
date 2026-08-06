package com.mogdop.mod.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public record SaveSchematicPayload(String filename, List<BlockPos> points, int selectionMode) implements CustomPayload {
    public static final CustomPayload.Id<SaveSchematicPayload> ID = new CustomPayload.Id<>(Identifier.of("mogdops-mod", "save_schematic"));

    public static final PacketCodec<RegistryByteBuf, SaveSchematicPayload> CODEC = new PacketCodec<>() {
        @Override
        public void encode(RegistryByteBuf buf, SaveSchematicPayload value) {
            PacketCodecs.STRING.encode(buf, value.filename());
            PacketCodecs.INTEGER.encode(buf, value.points().size());
            for (BlockPos p : value.points()) {
                BlockPos.PACKET_CODEC.encode(buf, p);
            }
            PacketCodecs.INTEGER.encode(buf, value.selectionMode());
        }

        @Override
        public SaveSchematicPayload decode(RegistryByteBuf buf) {
            String fname = PacketCodecs.STRING.decode(buf);
            int count = PacketCodecs.INTEGER.decode(buf);
            List<BlockPos> list = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                list.add(BlockPos.PACKET_CODEC.decode(buf));
            }
            int mode = PacketCodecs.INTEGER.decode(buf);
            return new SaveSchematicPayload(fname, list, mode);
        }
    };

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}