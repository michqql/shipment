package me.michqql.shipmentplugin.data;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.sql.Timestamp;

public class TextFile extends DataFile {

    private PrintWriter w;

    public TextFile(Plugin plugin, String folder, String file) {
        super(plugin, folder, file, "txt");
    }

    @Override
    protected boolean load() {
        try {
            this.w = new PrintWriter(file);
            return true;
        } catch (FileNotFoundException e) {
            Bukkit.getLogger().severe("[Shipment] Could not load text file " + path + "!");
            e.printStackTrace();
            return false;
        }
    }

    public void write(String line) {
        w.write("[" + new Timestamp(System.currentTimeMillis()) + "] " + line);
    }

    @Override
    public void save() {}
}
