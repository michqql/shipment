package me.michqql.shipmentplugin.data;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;

public class CommentFile extends YamlFile {

    public CommentFile(Plugin plugin, String folder, String file) {
        super(plugin, folder, file);
    }

    @Override
    protected boolean copy() {
        InputStream in = plugin.getResource(path);
        if(in == null) {
            Bukkit.getLogger().warning("[Shipment] Could not find resource to copy named " + path);
            return false;
        }

        try {
            FileOutputStream out = new FileOutputStream(file);
            byte[] buffer = new byte[1024];

            int length;
            while((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }

            out.close();
            in.close();
        } catch(IOException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[Shipment] Could not copy default resource " + path);
            return false;
        }
        return true;
    }

    @Override
    public void save() {
        // do nothing
    }
}
