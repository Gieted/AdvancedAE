package com.skyesea.stockexportbus.network;

import appeng.container.implementations.IOBusContainer;
import com.skyesea.stockexportbus.StockExportBusPart;
import java.util.function.Supplier;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

final class SetStockFilterPacket {
    private final int slot;
    private final ItemStack filter;

    SetStockFilterPacket(int slot, ItemStack filter) {
        this.slot = slot;
        this.filter = filter.copy();
    }

    static void encode(SetStockFilterPacket packet, PacketBuffer buffer) {
        buffer.writeVarInt(packet.slot);
        buffer.writeItem(packet.filter);
    }

    static SetStockFilterPacket decode(PacketBuffer buffer) {
        return new SetStockFilterPacket(buffer.readVarInt(), buffer.readItem());
    }

    static void handle(SetStockFilterPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayerEntity player = context.getSender();
            if (player == null || !(player.containerMenu instanceof IOBusContainer)) {
                return;
            }

            IOBusContainer container = (IOBusContainer) player.containerMenu;
            if (container.getUpgradeable() instanceof StockExportBusPart) {
                StockExportBusPart part = (StockExportBusPart) container.getUpgradeable();
                part.setStockFilter(packet.slot, packet.filter);
                StockExportBusNetwork.syncStockAmounts(player, part);
            }
        });
        context.setPacketHandled(true);
    }
}
