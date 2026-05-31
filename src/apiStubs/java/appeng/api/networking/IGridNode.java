package appeng.api.networking; import appeng.api.networking.storage.IStorageGrid; public interface IGridNode { Grid getGrid(); interface Grid { IStorageGrid getStorageGrid(); } }
