package com.skyesea.stockexportbus.network;

import com.skyesea.stockexportbus.StockExportBus;
import com.skyesea.stockexportbus.StockExportBusPart;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.fml.network.simple.SimpleChannel;

public final class StockExportBusNetwork {
    private static final String PROTOCOL_VERSION = "3";

    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(StockExportBus.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    private static boolean initialized;

    private StockExportBusNetwork() {
    }

    public static void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;

        CHANNEL.registerMessage(
                0,
                SetStockFilterPacket.class,
                SetStockFilterPacket::encode,
                SetStockFilterPacket::decode,
                SetStockFilterPacket::handle);
        CHANNEL.registerMessage(
                1,
                RequestStockAmountsPacket.class,
                RequestStockAmountsPacket::encode,
                RequestStockAmountsPacket::decode,
                RequestStockAmountsPacket::handle);
        CHANNEL.registerMessage(
                2,
                StockAmountsPacket.class,
                StockAmountsPacket::encode,
                StockAmountsPacket::decode,
                StockAmountsPacket::handle);
    }

    public static void setStockFilter(int slot, net.minecraft.item.ItemStack filter) {
        CHANNEL.sendToServer(new SetStockFilterPacket(slot, filter));
    }

    public static void requestStockAmounts() {
        CHANNEL.sendToServer(new RequestStockAmountsPacket());
    }

    static void syncStockAmounts(ServerPlayerEntity player, StockExportBusPart part) {
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new StockAmountsPacket(part.getStockAmounts()));
    }
}
