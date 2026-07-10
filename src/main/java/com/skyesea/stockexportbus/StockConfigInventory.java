package com.skyesea.stockexportbus;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;

public class StockConfigInventory implements IItemHandlerModifiable {
    private final StockExportBusPart owner;
    private final ItemStack[] stacks;

    public StockConfigInventory(StockExportBusPart owner, int size) {
        this.owner = owner;
        this.stacks = new ItemStack[size];
        for (int i = 0; i < size; i++) {
            this.stacks[i] = ItemStack.EMPTY;
        }
    }

    @Override
    public int getSlots() {
        return this.stacks.length;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        this.validateSlot(slot);
        return this.stacks[slot];
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        this.validateSlot(slot);
        this.stacks[slot] = stack.copy();
        this.owner.saveChanges();
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        this.validateSlot(slot);
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        if (!this.stacks[slot].isEmpty()) {
            return stack;
        }

        int accepted = Math.min(stack.getCount(), this.getSlotLimit(slot));
        if (!simulate) {
            ItemStack configured = stack.copy();
            configured.setCount(accepted);
            this.setStackInSlot(slot, configured);
        }

        if (accepted == stack.getCount()) {
            return ItemStack.EMPTY;
        }

        ItemStack remainder = stack.copy();
        remainder.shrink(accepted);
        return remainder;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        this.validateSlot(slot);
        ItemStack configured = this.stacks[slot];
        if (configured.isEmpty() || amount <= 0) {
            return ItemStack.EMPTY;
        }

        ItemStack extracted = configured.copy();
        extracted.setCount(Math.min(amount, configured.getCount()));
        if (!simulate) {
            if (extracted.getCount() == configured.getCount()) {
                this.setStackInSlot(slot, ItemStack.EMPTY);
            } else {
                ItemStack remainder = configured.copy();
                remainder.shrink(extracted.getCount());
                this.setStackInSlot(slot, remainder);
            }
        }
        return extracted;
    }

    @Override
    public int getSlotLimit(int slot) {
        this.validateSlot(slot);
        return 64;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        this.validateSlot(slot);
        return true;
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
                this.stacks[slot] = ItemStack.of(tag);
            }
        }
    }

    public void writeToNBT(CompoundNBT data, String key) {
        ListNBT list = new ListNBT();
        for (int i = 0; i < this.stacks.length; i++) {
            if (!this.stacks[i].isEmpty()) {
                CompoundNBT tag = new CompoundNBT();
                tag.putByte("Slot", (byte) i);
                this.stacks[i].save(tag);
                list.add(tag);
            }
        }
        data.put(key, list);
    }

    public boolean isEmpty() {
        for (ItemStack stack : this.stacks) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public void copyFrom(IItemHandler source) {
        int slots = Math.min(this.stacks.length, source.getSlots());
        for (int slot = 0; slot < slots; slot++) {
            this.stacks[slot] = source.getStackInSlot(slot).copy();
        }
    }

    private void validateSlot(int slot) {
        if (slot < 0 || slot >= this.stacks.length) {
            throw new IndexOutOfBoundsException("Slot " + slot + " is outside stock config inventory");
        }
    }
}
