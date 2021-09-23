package me.michqql.shipmentplugin.data.type;

import me.michqql.shipmentplugin.data.YamlFile;
import me.michqql.shipmentplugin.shipment.ItemsForSale;
import me.michqql.shipmentplugin.utils.IOUtil;
import org.bukkit.plugin.Plugin;

import java.io.File;

public class PresetFile extends YamlFile {

    public PresetFile(Plugin plugin, File file) {
        super(plugin, "presets", IOUtil.filenameWithoutExtension(file.getName()));
    }

    public PresetFile(Plugin plugin, String identifier) {
        super(plugin, "presets", identifier);
    }

    public String getPresetName() {
        return fileName;
    }

    public String getDisplayName() {
        return fileConfiguration.getString("display-name");
    }

    public void setDisplayName(String string) {
        fileConfiguration.set("display-name", string);
    }

    public ItemsForSale getItems() {
        ItemsForSale items = new ItemsForSale();
        items.load(fileConfiguration.getConfigurationSection("items"));
        return items;
    }

    public void saveItems(ItemsForSale items) {
        items.save(fileConfiguration.createSection("items"));
        save();
    }

    @Override
    public boolean delete() {
        return super.delete();
    }
}
