package me.michqql.shipmentplugin.schematic;

import com.gmail.filoghost.holographicdisplays.api.Hologram;
import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;
import me.michqql.shipmentplugin.ShipmentPlugin;
import me.michqql.shipmentplugin.data.CommentFile;
import me.michqql.shipmentplugin.utils.MessageUtil;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;

public class HologramHandler {

    private final ShipmentPlugin plugin;
    private final List<Hologram> holograms;

    // Configuration options
    private List<String> lines;
    private double xOffset, yOffset, zOffset;

    public HologramHandler(ShipmentPlugin plugin, CommentFile config) {
        this.plugin = plugin;
        this.holograms = new ArrayList<>();

        loadConfig(config.getConfig());
    }

    private void loadConfig(FileConfiguration f) {
        this.lines = f.getStringList("crates.holographic-display");

        try {
            this.xOffset = f.getDouble("crates.hologram-offset.x");
        } catch(NumberFormatException ignore) {}

        try {
            this.yOffset = f.getDouble("crates.hologram-offset.y");
        } catch(NumberFormatException ignore) {}

        try {
            this.zOffset = f.getDouble("crates.hologram-offset.z");
        } catch(NumberFormatException ignore) {}
    }

    void loadHolograms(List<Block> crates) {
        for(Block crate : crates) {
            Hologram hologram = HologramsAPI.createHologram(plugin,
                    crate.getLocation().clone().add(0.5 + xOffset, 0 + yOffset, 0.5 + zOffset));
            for(String line : lines) {
                hologram.appendTextLine(MessageUtil.format(line));
            }
            holograms.add(hologram);
        }
    }

    void unloadHolograms() {
        for(Hologram hologram : holograms) {
            hologram.delete();
        }
    }
}
