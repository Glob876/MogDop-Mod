package com.mogdop.mod.network;

import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record GiveItemPayload(ItemStack stack) implements CustomPayload {
    
    public static final CustomPayload.Id<GiveItemPayload> ID = new CustomPayload.Id<>(Identifier.of("mogdops-mod", "give_item"));
    
    // В 1.21.1 ItemStack.PACKET_CODEC сам сериализует все чары, имена и свойства!
    public static final PacketCodec<RegistryByteBuf, GiveItemPayload> CODEC = PacketCodec.tuple(
            ItemStack.PACKET_CODEC, GiveItemPayload::stack,
            GiveItemPayload::new
    );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}