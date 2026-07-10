package com.skyesea.stockexportbus;

import appeng.api.config.Actionable;
import appeng.api.networking.energy.IEnergyGrid;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.parts.IPartModel;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.core.Api;
import appeng.core.AELog;
import appeng.me.GridAccessException;
import appeng.me.helpers.MachineSource;
import appeng.util.item.AEItemStack;
import appeng.parts.automation.ExportBusPart;
import appeng.parts.PartModel;
import appeng.util.InventoryAdaptor;
import appeng.util.Platform;
import appeng.util.inv.ItemSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;

/**
 * AE2 8.4.4 backport of Advanced AE's Stock Export Bus concept.
 *
 * <p>The part behaves like an export bus, but interprets each filter stack size
 * as a target stock level in the adjacent inventory. During work ticks it first
 * counts matching adjacent items and only exports enough to approach the target.
 */
public class StockExportBusPart extends ExportBusPart {
    private static final ResourceLocation MODEL_BASE =
            new ResourceLocation(StockExportBus.MOD_ID, "part/stock_export_bus");
    private static final IPartModel MODELS_OFF = new PartModel(
            MODEL_BASE,
            new ResourceLocation("appliedenergistics2", "part/export_bus_off"));
    private static final IPartModel MODELS_ON = new PartModel(
            MODEL_BASE,
            new ResourceLocation("appliedenergistics2", "part/export_bus_on"));
    private static final IPartModel MODELS_HAS_CHANNEL = new PartModel(
            MODEL_BASE,
            new ResourceLocation("appliedenergistics2", "part/export_bus_has_channel"));

    private final StockConfigInventory stockConfig = new StockConfigInventory(this, 9);
    private final IActionSource actionSource;

    public StockExportBusPart(ItemStack partItem) {
        super(partItem);
        this.actionSource = new MachineSource(this);
    }

    public static void registerModels() {
        Api.INSTANCE.getPartModels().registerModels(MODELS_OFF.getModels());
        Api.INSTANCE.getPartModels().registerModels(MODELS_ON.getModels());
        Api.INSTANCE.getPartModels().registerModels(MODELS_HAS_CHANNEL.getModels());
    }

    @Override
    public IPartModel getStaticModels() {
        if (this.isActive() && this.isPowered()) {
            return MODELS_HAS_CHANNEL;
        }
        if (this.isPowered()) {
            return MODELS_ON;
        }
        return MODELS_OFF;
    }

    @Override
    protected int getUpgradeSlots() {
        return 4;
    }

    @Override
    public IItemHandler getInventoryByName(String name) {
        if ("config".equals(name)) {
            return this.stockConfig;
        }
        return super.getInventoryByName(name);
    }

    public void setStockFilter(int slot, ItemStack filter) {
        if (slot < 0 || slot >= this.availableSlots()) {
            return;
        }
        if (filter.isEmpty()) {
            return;
        }

        ItemStack configured = filter.copy();
        configured.setCount(Math.max(1, Math.min(64, configured.getCount())));
        this.stockConfig.setStackInSlot(slot, configured);
    }

    public int[] getStockAmounts() {
        int[] amounts = new int[this.stockConfig.getSlots()];
        for (int slot = 0; slot < amounts.length; slot++) {
            amounts[slot] = this.stockConfig.getStackInSlot(slot).getCount();
        }
        return amounts;
    }

    @Override
    public void readFromNBT(CompoundNBT data) {
        super.readFromNBT(data);
        this.stockConfig.readFromNBT(data, "stockConfig");
        IItemHandler legacyConfig = super.getInventoryByName("config");
        if (this.stockConfig.isEmpty()) {
            this.stockConfig.copyFrom(legacyConfig);
        }
        if (legacyConfig instanceof IItemHandlerModifiable) {
            IItemHandlerModifiable modifiableLegacyConfig = (IItemHandlerModifiable) legacyConfig;
            for (int slot = 0; slot < legacyConfig.getSlots(); slot++) {
                modifiableLegacyConfig.setStackInSlot(slot, ItemStack.EMPTY);
            }
        }
    }

    @Override
    public void writeToNBT(CompoundNBT data) {
        super.writeToNBT(data);
        this.stockConfig.writeToNBT(data, "stockConfig");
    }

    @Override
    protected TickRateModulation doBusWork() {
        if (!this.getProxy().isActive() || !this.canDoBusWork()) {
            return TickRateModulation.IDLE;
        }

        long itemsToSend = this.calculateItemsToSend();
        boolean didSomething = false;

        try {
            final InventoryAdaptor destination = this.getHandler();
            final IMEMonitor<IAEItemStack> inv = this.getProxy()
                    .getStorage()
                    .getInventory(Api.instance().storage().getStorageChannel(IItemStorageChannel.class));
            final IEnergyGrid energy = this.getProxy().getEnergy();
            if (destination == null) {
                return TickRateModulation.SLEEP;
            }

            for (int slot = 0; slot < this.availableSlots() && itemsToSend > 0; slot++) {
                final ItemStack filter = this.stockConfig.getStackInSlot(slot);
                if (filter.isEmpty()) {
                    continue;
                }

                final long target = filter.getCount();
                final long stocked = this.getCurrentStock(filter);
                long missing = target - stocked;
                if (missing <= 0) {
                    continue;
                }

                final IAEItemStack request = AEItemStack.fromItemStack(filter);
                if (request == null) {
                    continue;
                }

                final long transferCap = Math.min(itemsToSend, missing);
                request.setStackSize(transferCap);

                final ItemStack simulated = request.createItemStack();
                final ItemStack remainder = destination.simulateAdd(simulated);
                final long canFit = simulated.getCount() - (remainder.isEmpty() ? 0 : remainder.getCount());
                if (canFit <= 0) {
                    continue;
                }

                request.setStackSize(Math.min(transferCap, canFit));
                final IAEItemStack extracted = Platform.poweredExtraction(energy, inv, request, this.actionSource);
                if (extracted == null) {
                    continue;
                }

                itemsToSend -= extracted.getStackSize();

                final ItemStack exported = extracted.createItemStack();
                final int exportedCount = exported.getCount();
                final ItemStack failed = destination.addItems(exported);
                final int failedCount = failed.isEmpty() ? 0 : failed.getCount();
                didSomething |= exportedCount > failedCount;
                if (!failed.isEmpty()) {
                    final IAEItemStack failedAe = AEItemStack.fromItemStack(failed);
                    if (failedAe != null) {
                        inv.injectItems(failedAe, Actionable.MODULATE, this.actionSource);
                    }
                }
            }
        } catch (GridAccessException e) {
            AELog.debug(e);
        }

        return didSomething ? TickRateModulation.FASTER : TickRateModulation.SLOWER;
    }

    private long getCurrentStock(ItemStack filter) {
        final InventoryAdaptor adaptor = this.getHandler();
        if (adaptor == null) {
            return 0;
        }

        long total = 0;
        for (final ItemSlot slot : adaptor) {
            final ItemStack stack = slot.getItemStack();
            if (!stack.isEmpty() && this.matchesFilter(filter, stack)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private boolean matchesFilter(ItemStack filter, ItemStack stack) {
        if (filter.getItem() != stack.getItem()) {
            return false;
        }
        return ItemStack.tagMatches(filter, stack) && filter.getDamageValue() == stack.getDamageValue();
    }
}
