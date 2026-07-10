package com.skyesea.stockexportbus.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;

public final class ClientPacketHandler {
    private ClientPacketHandler() {
    }

    public static void handleStockAmounts(int[] amounts) {
        Screen screen = Minecraft.getInstance().screen;
        if (screen instanceof StockExportBusScreen) {
            ((StockExportBusScreen) screen).applyStockAmounts(amounts);
        }
    }
}
