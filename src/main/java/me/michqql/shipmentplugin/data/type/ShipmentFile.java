package me.michqql.shipmentplugin.data.type;

import me.michqql.shipmentplugin.data.YamlFile;
import org.bukkit.plugin.Plugin;

public class ShipmentFile extends YamlFile {

    private boolean exists = true;

    public ShipmentFile(Plugin plugin, long timestamp) {
        super(plugin, "shipments", String.valueOf(timestamp));
    }

    public boolean exists() {
        return exists;
    }

    @Override
    protected void copy() {
        this.exists = false;
    }

    public boolean existsOtherwiseDelete() {
        if(exists)
            return true;

        super.delete();
        return false;
    }
}
