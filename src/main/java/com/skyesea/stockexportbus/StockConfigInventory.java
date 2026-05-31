package com.skyesea.stockexportbus;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;

public class StockConfigInventory {
    private final StockExportBusPart owner;
    private final ItemStack[] stacks;

    public StockConfigInventory(StockExportBusPart owner, int size) {
        this.owner = owner;
        this.stacks = new ItemStack[size];
        for (int i = 0; i < size; i++) {
            this.stacks[i] = ItemStack.EMPTY;
        }
    }

    public int getSlots() {
        return this.stacks.length;
    }

    public ItemStack getStackInSlot(int slot) {
        return this.stacks[slot];
    }

    public void setStackInSlot(int slot, ItemStack stack) {
        this.stacks[slot] = stack.copy();
        this.owner.getHost().markForSave();
    }

    public void readFromNBT(CompoundNBT data, String key) {
        ListNBT list = data.getList(key, 10);
        for (int i = 0; i < this.stacks.length; i++) {
            this.stacks[i] = ItemStack.EMPTY;
        }
        for (int i = 0; i < list.size(); i++) {
            CompoundNBT tag = list.getCompound(i);
            int slot = tag.getByte("Slot") & 255;
            if (slot >= 0 && slot < this.stacks.length) {
                this.stacks[slot] = ItemStack.read(tag);
            }
        }
    }

    public void writeToNBT(CompoundNBT data, String key) {
        ListNBT list = new ListNBT();
        for (int i = 0; i < this.stacks.length; i++) {
            if (!this.stacks[i].isEmpty()) {
                CompoundNBT tag = new CompoundNBT();
                tag.putByte("Slot", (byte) i);
                this.stacks[i].write(tag);
                list.add(tag);
            }
        }
        data.put(key, list);
    }
}
