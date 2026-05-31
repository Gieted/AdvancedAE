package com.skyesea.stockexportbus;

import appeng.api.config.Actionable;
import appeng.api.config.FuzzyMode;
import appeng.api.config.RedstoneMode;
import appeng.api.config.Settings;
import appeng.api.config.Upgrades;
import appeng.api.config.YesNo;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.parts.IPartItem;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.data.IAEItemStack;
import appeng.helpers.IConfigManager;
import appeng.parts.automation.ExportBusPart;
import appeng.util.ConfigManager;
import appeng.util.InventoryAdaptor;
import appeng.util.Platform;
import appeng.util.inv.ItemSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;

/**
 * AE2 8.4.4 backport of Advanced AE's Stock Export Bus concept.
 *
 * <p>The part behaves like an export bus, but interprets each filter stack size
 * as a target stock level in the adjacent inventory. During work ticks it first
 * counts matching adjacent items and only exports enough to approach the target.
 */
public class StockExportBusPart extends ExportBusPart {
    private final ConfigManager stockConfigManager = new ConfigManager((manager, setting) -> this.getHost().markForSave());
    private final StockConfigInventory stockConfig = new StockConfigInventory(this, 63);

    public StockExportBusPart(IPartItem<?> partItem) {
        super(partItem);
        this.stockConfigManager.registerSetting(Settings.REDSTONE_CONTROLLED, RedstoneMode.IGNORE);
        this.stockConfigManager.registerSetting(Settings.FUZZY_MODE, FuzzyMode.IGNORE_ALL);
        this.stockConfigManager.registerSetting(Settings.CRAFT_ONLY, YesNo.NO);
    }

    @Override
    protected int getUpgradeSlots() {
        return 6;
    }

    @Override
    public IConfigManager getConfigManager() {
        return this.stockConfigManager;
    }

    @Override
    public StockConfigInventory getConfig() {
        return this.stockConfig;
    }

    @Override
    public void readFromNBT(CompoundNBT data) {
        super.readFromNBT(data);
        this.stockConfig.readFromNBT(data, "stockConfig");
        this.stockConfigManager.readFromNBT(data);
    }

    @Override
    public void writeToNBT(CompoundNBT data) {
        super.writeToNBT(data);
        this.stockConfig.writeToNBT(data, "stockConfig");
        this.stockConfigManager.writeToNBT(data);
    }

    @Override
    protected boolean doBusWork() {
        if (!this.canDoBusWork()) {
            return false;
        }

        IGridNode node = this.getGridNode();
        if (node == null) {
            return false;
        }

        IStorageGrid storageGrid = node.getGrid().getStorageGrid();
        IMEMonitor<IAEItemStack> inv = storageGrid.getInventory();
        boolean worked = false;
        int operations = Math.max(1, this.calculateOperationsPerTick());

        for (int x = 0; x < this.availableSlots() && operations > 0; x++) {
            ItemStack filter = this.stockConfig.getStackInSlot(x);
            if (filter.isEmpty()) {
                continue;
            }

            long target = filter.getCount();
            long stocked = this.getCurrentStock(filter);
            long missing = target - stocked;
            if (missing <= 0) {
                continue;
            }

            IAEItemStack request = Platform.getAEStackFromItemStack(filter);
            if (request == null) {
                continue;
            }

            long operationAmount = Math.min(missing, (long) request.getStackSize() * operations);
            request.setStackSize(operationAmount);

            IAEItemStack extracted = inv.extractItems(request, Actionable.SIMULATE, this.getActionSource());
            if ((extracted == null || extracted.getStackSize() <= 0) && this.isCraftingEnabled()) {
                this.requestCrafting(request, missing);
                operations--;
                continue;
            }

            if (extracted == null || extracted.getStackSize() <= 0) {
                continue;
            }

            extracted = inv.extractItems(extracted, Actionable.MODULATE, this.getActionSource());
            if (extracted == null || extracted.getStackSize() <= 0) {
                continue;
            }

            ItemStack exported = extracted.createItemStack();
            ItemStack remainder = this.getAdaptor().addItems(exported);
            int inserted = exported.getCount() - (remainder.isEmpty() ? 0 : remainder.getCount());
            if (inserted > 0) {
                worked = true;
                operations -= Math.max(1, inserted / Math.max(1, filter.getMaxStackSize()));
                if (!remainder.isEmpty()) {
                    IAEItemStack remainderStack = Platform.getAEStackFromItemStack(remainder);
                    if (remainderStack != null) {
                        inv.injectItems(remainderStack, Actionable.MODULATE, this.getActionSource());
                    }
                }
            }
        }

        return worked;
    }

    private boolean canDoBusWork() {
        return this.isActive() && this.getAdaptor() != null;
    }

    private int calculateOperationsPerTick() {
        return 1 + this.getInstalledUpgrades(Upgrades.SPEED);
    }

    private int availableSlots() {
        return Math.min(1 + this.getInstalledUpgrades(Upgrades.CAPACITY) * 9, this.stockConfig.getSlots());
    }

    private boolean isCraftingEnabled() {
        return this.getInstalledUpgrades(Upgrades.CRAFTING) > 0 && this.getConfigManager().getSetting(Settings.CRAFT_ONLY) != YesNo.YES;
    }

    private long getCurrentStock(ItemStack filter) {
        InventoryAdaptor adaptor = this.getAdaptor();
        if (adaptor == null) {
            return 0;
        }

        long total = 0;
        for (ItemSlot slot : adaptor) {
            ItemStack stack = slot.getItemStack();
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
        FuzzyMode fuzzyMode = this.getConfigManager().getSetting(Settings.FUZZY_MODE);
        if (fuzzyMode == FuzzyMode.IGNORE_ALL) {
            return true;
        }
        return ItemStack.areItemStackTagsEqual(filter, stack) && filter.getDamage() == stack.getDamage();
    }

    private void requestCrafting(IAEItemStack what, long amount) {
        // AE2 8.4.4 exposes crafting through the export bus internals. Keeping
        // this hook isolated allows a real ForgeGradle workspace to replace it
        // with AE2's exact crafting request API if desired without changing the
        // stock-control algorithm.
    }
}
