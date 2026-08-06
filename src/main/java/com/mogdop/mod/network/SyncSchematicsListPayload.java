package com.mogdop.mod.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public record SyncSchematicsListPayload(List<String> files) implements CustomPayload {
    public static final CustomPayload.Id<SyncSchematicsListPayload> ID = new CustomPayload.Id<>(Identifier.of("mogdops-mod", "sync_schematics"));

    public static final PacketCodec<RegistryByteBuf, SyncSchematicsListPayload> CODEC = new PacketCodec<>() {
        @Override
        public void encode(RegistryByteBuf buf, SyncSchematicsListPayload value) {
            PacketCodecs.INTEGER.encode(buf, value.files().size());
            for (String f : value.files()) {
                PacketCodecs.STRING.encode(buf, f);
            }
        }

        @Override
        public SyncSchematicsListPayload decode(RegistryByteBuf buf) {
            int count = PacketCodecs.INTEGER.decode(buf);
            List<String> list = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                list.add(PacketCodecs.STRING.decode(buf));
            }
            return new SyncSchematicsListPayload(list);
        }
    };

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}