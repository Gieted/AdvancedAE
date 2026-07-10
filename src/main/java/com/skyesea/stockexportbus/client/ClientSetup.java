package com.skyesea.stockexportbus.client;

import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.style.StyleManager;
import appeng.container.implementations.IOBusContainer;
import com.skyesea.stockexportbus.StockExportBus;
import java.io.IOException;
import net.minecraft.client.gui.ScreenManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = StockExportBus.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ScreenManager.<IOBusContainer, StockExportBusScreen>register(
                IOBusContainer.EXPORT_TYPE,
                (container, playerInventory, title) -> new StockExportBusScreen(
                        container,
                        playerInventory,
                        title,
                        loadExportBusStyle())));
    }

    private static ScreenStyle loadExportBusStyle() {
        try {
            return StyleManager.loadStyleDoc("/screens/export_bus.json");
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load the AE2 export bus screen style", e);
        }
    }
}
