package appeng.helpers; import appeng.api.config.Settings; public interface IConfigManager { <T extends Enum<T>> T getSetting(Settings setting); }
