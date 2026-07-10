package com.skyesea.stockexportbus.network;

import com.skyesea.stockexportbus.client.ClientPacketHandler;
import java.util.function.Supplier;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkEvent;

final class StockAmountsPacket {
    private final int[] amounts;

    StockAmountsPacket(int[] amounts) {
        this.amounts = amounts;
    }

    static void encode(StockAmountsPacket packet, PacketBuffer buffer) {
        buffer.writeVarInt(packet.amounts.length);
        for (int amount : packet.amounts) {
            buffer.writeVarInt(amount);
        }
    }

    static StockAmountsPacket decode(PacketBuffer buffer) {
        int length = buffer.readVarInt();
        if (length < 0 || length > 9) {
            throw new IllegalArgumentException("Invalid stock amount count: " + length);
        }
        int[] amounts = new int[length];
        for (int i = 0; i < length; i++) {
            amounts[i] = buffer.readVarInt();
        }
        return new StockAmountsPacket(amounts);
    }

    static void handle(StockAmountsPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientPacketHandler.handleStockAmounts(packet.amounts)));
        context.setPacketHandled(true);
    }
}
