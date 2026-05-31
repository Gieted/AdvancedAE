package com.skyesea.stockexportbus;

import appeng.api.config.Upgrades;
import appeng.core.Api;
import appeng.core.definitions.AEItems;
import appeng.items.parts.PartItem;
import net.minecraft.item.Item;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod(StockExportBus.MOD_ID)
@Mod.EventBusSubscriber(modid = StockExportBus.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class StockExportBus {
    public static final String MOD_ID = "stockexportbus";
    public static final String STOCK_EXPORT_BUS_ID = "stock_export_bus_part";

    public static PartItem<StockExportBusPart> STOCK_EXPORT_BUS;

    public StockExportBus() {
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        STOCK_EXPORT_BUS = new PartItem<>(new Item.Properties().group(Api.INSTANCE.getCreativeTab()), StockExportBusPart.class, StockExportBusPart::new);
        STOCK_EXPORT_BUS.setRegistryName(MOD_ID, STOCK_EXPORT_BUS_ID);
        event.getRegistry().register(STOCK_EXPORT_BUS);

        Upgrades.CAPACITY.registerItem(STOCK_EXPORT_BUS, 5);
        Upgrades.SPEED.registerItem(STOCK_EXPORT_BUS, 4);
        Upgrades.REDSTONE.registerItem(STOCK_EXPORT_BUS, 1);
        Upgrades.CRAFTING.registerItem(STOCK_EXPORT_BUS, 1);
        Upgrades.FUZZY.registerItem(STOCK_EXPORT_BUS, 1);
    }
}
