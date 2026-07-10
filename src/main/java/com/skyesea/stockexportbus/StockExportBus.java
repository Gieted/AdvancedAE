package com.skyesea.stockexportbus;

import appeng.api.config.Upgrades;
import appeng.core.CreativeTab;
import appeng.items.parts.PartItem;
import com.skyesea.stockexportbus.network.StockExportBusNetwork;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod(StockExportBus.MOD_ID)
@Mod.EventBusSubscriber(modid = StockExportBus.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class StockExportBus {
    public static final String MOD_ID = "stockexportbus";
    public static final String STOCK_EXPORT_BUS_ID = "stock_export_bus";
    private static final ResourceLocation OLD_STOCK_EXPORT_BUS_ID =
            new ResourceLocation(MOD_ID, "stock_export_bus_part");

    public static PartItem<StockExportBusPart> STOCK_EXPORT_BUS;

    public StockExportBus() {
        StockExportBusNetwork.initialize();
        StockExportBusPart.registerModels();
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        STOCK_EXPORT_BUS = new PartItem<>(new Item.Properties().tab(CreativeTab.INSTANCE), StockExportBusPart::new);
        STOCK_EXPORT_BUS.setRegistryName(MOD_ID, STOCK_EXPORT_BUS_ID);
        event.getRegistry().register(STOCK_EXPORT_BUS);

        Upgrades.CAPACITY.registerItem(STOCK_EXPORT_BUS, 2);
        Upgrades.SPEED.registerItem(STOCK_EXPORT_BUS, 4);
        Upgrades.REDSTONE.registerItem(STOCK_EXPORT_BUS, 1);
    }

    @SubscribeEvent
    public static void remapOldItemId(RegistryEvent.MissingMappings<Item> event) {
        for (RegistryEvent.MissingMappings.Mapping<Item> mapping : event.getMappings(MOD_ID)) {
            if (OLD_STOCK_EXPORT_BUS_ID.equals(mapping.key)) {
                mapping.remap(STOCK_EXPORT_BUS);
            }
        }
    }
}
