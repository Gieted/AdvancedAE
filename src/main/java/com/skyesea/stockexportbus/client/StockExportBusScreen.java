package com.skyesea.stockexportbus.client;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.implementations.IOBusScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.container.SlotSemantic;
import appeng.container.slot.IOptionalSlot;
import appeng.container.implementations.IOBusContainer;
import com.skyesea.stockexportbus.StockExportBusPart;
import com.skyesea.stockexportbus.network.StockExportBusNetwork;
import java.util.Arrays;
import java.util.List;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

public final class StockExportBusScreen extends IOBusScreen {
    private final IOBusContainer stockContainer;
    private final PlayerInventory playerInventory;
    private final int[] targetAmounts = new int[9];
    private final boolean stockBus;

    StockExportBusScreen(
            IOBusContainer container,
            PlayerInventory playerInventory,
            ITextComponent title,
            ScreenStyle style) {
        super(container, playerInventory, title, style);
        this.stockContainer = container;
        this.playerInventory = playerInventory;
        this.stockBus = container.getUpgradeable() instanceof StockExportBusPart;

        if (this.stockBus) {
            this.setTextContent(
                    AEBaseScreen.TEXT_ID_DIALOG_TITLE,
                    new TranslationTextComponent("item.stockexportbus.stock_export_bus"));
        }
    }

    @Override
    protected void init() {
        super.init();
        if (this.stockBus) {
            StockExportBusNetwork.requestStockAmounts();
        }
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        if (!this.stockBus) {
            return;
        }

        List<Slot> configSlots = this.stockContainer.getSlots(SlotSemantic.CONFIG);
        for (int slot = 0; slot < configSlots.size() && slot < this.targetAmounts.length; slot++) {
            ItemStack configured = configSlots.get(slot).getItem();
            if (!configured.isEmpty() && this.targetAmounts[slot] > 0) {
                configured.setCount(this.targetAmounts[slot]);
            }
        }
    }

    @Override
    protected void slotClicked(Slot slot, int slotId, int mouseButton, ClickType clickType) {
        if (!this.stockBus) {
            super.slotClicked(slot, slotId, mouseButton, clickType);
            return;
        }

        List<Slot> configSlots = this.stockContainer.getSlots(SlotSemantic.CONFIG);
        int configSlot = configSlots.indexOf(slot);
        ItemStack cursorStack = this.playerInventory.getCarried().copy();

        if (clickType == ClickType.QUICK_MOVE && this.isPlayerSlot(slot) && !slot.getItem().isEmpty()) {
            int emptyConfigSlot = this.findEmptyConfigSlot(configSlots);
            if (emptyConfigSlot >= 0) {
                this.configureStockSlot(configSlots.get(emptyConfigSlot), emptyConfigSlot, slot.getItem());
            }
            return;
        }

        super.slotClicked(slot, slotId, mouseButton, clickType);

        if (configSlot >= 0 && !cursorStack.isEmpty()) {
            this.configureStockSlot(slot, configSlot, cursorStack);
        } else if (configSlot >= 0) {
            this.targetAmounts[configSlot] = 0;
        }
    }

    private boolean isPlayerSlot(Slot slot) {
        return this.stockContainer.getSlots(SlotSemantic.PLAYER_INVENTORY).contains(slot)
                || this.stockContainer.getSlots(SlotSemantic.PLAYER_HOTBAR).contains(slot);
    }

    private int findEmptyConfigSlot(List<Slot> configSlots) {
        for (int index = 0; index < configSlots.size(); index++) {
            Slot configSlot = configSlots.get(index);
            if (configSlot instanceof IOptionalSlot && !((IOptionalSlot) configSlot).isSlotEnabled()) {
                continue;
            }
            if (configSlot.getItem().isEmpty()) {
                return index;
            }
        }
        return -1;
    }

    private void configureStockSlot(Slot slot, int configSlot, ItemStack source) {
        ItemStack configured = source.copy();
        int amount = Math.max(1, Math.min(64, configured.getCount()));
        configured.setCount(amount);

        slot.set(configured.copy());
        slot.getItem().setCount(amount);
        this.targetAmounts[configSlot] = amount;
        StockExportBusNetwork.setStockFilter(configSlot, configured);
    }

    public void applyStockAmounts(int[] amounts) {
        Arrays.fill(this.targetAmounts, 0);
        int length = Math.min(this.targetAmounts.length, amounts.length);
        System.arraycopy(amounts, 0, this.targetAmounts, 0, length);
    }
}
