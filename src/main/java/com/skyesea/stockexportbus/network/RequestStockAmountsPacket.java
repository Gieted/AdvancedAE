package com.skyesea.stockexportbus.network;

import appeng.container.implementations.IOBusContainer;
import com.skyesea.stockexportbus.StockExportBusPart;
import java.util.function.Supplier;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

final class RequestStockAmountsPacket {
    static void encode(RequestStockAmountsPacket packet, PacketBuffer buffer) {
    }

    static RequestStockAmountsPacket decode(PacketBuffer buffer) {
        return new RequestStockAmountsPacket();
    }

    static void handle(RequestStockAmountsPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayerEntity player = context.getSender();
            if (player == null || !(player.containerMenu instanceof IOBusContainer)) {
                return;
            }

            IOBusContainer container = (IOBusContainer) player.containerMenu;
            if (container.getUpgradeable() instanceof StockExportBusPart) {
                StockExportBusNetwork.syncStockAmounts(
                        player,
                        (StockExportBusPart) container.getUpgradeable());
            }
        });
        context.setPacketHandled(true);
    }
}
