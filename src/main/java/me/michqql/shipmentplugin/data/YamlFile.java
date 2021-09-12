package me.michqql.shipmentplugin.data;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.IOException;

public class YamlFile extends DataFile {

    protected FileConfiguration fileConfiguration;

    public YamlFile(Plugin plugin, String folder, String file) {
        super(plugin, folder, file, "yml");
    }

    public FileConfiguration getConfig() {
        return fileConfiguration;
    }

    @Override
    protected boolean load() {
        this.fileConfiguration = YamlConfiguration.loadConfiguration(super.file);
        return true;
    }

    @Override
    public void save() {
        try {
            this.fileConfiguration.save(super.file);
        } catch(IOException e) {
            Bukkit.getLogger().severe("[Shipment] Could not save file " + super.path);
            e.printStackTrace();
        }
    }
}
