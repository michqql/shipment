package me.michqql.shipmentplugin.preset;

import me.michqql.shipmentplugin.ShipmentPlugin;
import me.michqql.shipmentplugin.data.CommentFile;
import me.michqql.shipmentplugin.shipment.ItemsForSale;
import me.michqql.shipmentplugin.utils.IOUtil;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;

public class PresetHandler {

    private final ShipmentPlugin plugin;
    private final File presetFolder;

    // Configuration options
    private boolean enabled, random;
    private String defaultPreset;

    public PresetHandler(ShipmentPlugin plugin, CommentFile config) {
        this.plugin = plugin;
        this.presetFolder = IOUtil.makeDirectory(plugin.getDataFolder(), "presets");

        load();
        loadConfig(config.getConfig());
    }

    private void loadConfig(FileConfiguration f) {
        this.enabled = f.getBoolean("presets.enabled");
        this.random = f.getBoolean("presets.random");


        this.defaultPreset = f.getString("presets.default", "");
        if(defaultPreset.isEmpty() && !random) {
            this.enabled = false;
            Bukkit.getLogger().warning("[Shipment] Item Presets disabled. No valid default preset provided");
        }
    }

    private void load() {
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean presetExists(String presetName) {
        return false;
    }

    public void createPreset(String presetName, ItemsForSale items) {

    }
}
