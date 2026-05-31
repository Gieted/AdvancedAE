package appeng.api.storage.data; import net.minecraft.item.ItemStack; public interface IAEItemStack { long getStackSize(); IAEItemStack setStackSize(long size); ItemStack createItemStack(); }
