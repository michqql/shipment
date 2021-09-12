package me.michqql.shipmentplugin.data.type;

import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.*;
import me.michqql.shipmentplugin.data.EmptyDataFile;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class SchematicFile extends EmptyDataFile {

    protected Clipboard clipboard;
    private final boolean preExist;

    public SchematicFile(Plugin plugin, String file, String extension) {
        super(plugin, "schematics", file, extension);
        this.preExist = false;
    }

    public SchematicFile(Plugin plugin, String file, String extension, Clipboard clipboard) {
        super(plugin, "schematics", file, extension);
        this.clipboard = clipboard;
        this.preExist = true;

        if(exists())
            delete();

        createFile();
        save();
    }

    @Override
    protected boolean load() {
        if(!exists())
            return false;

        ClipboardFormat format = ClipboardFormats.findByFile(super.file);
        if(format == null)
            return false;

        try(ClipboardReader reader = format.getReader(new FileInputStream(file))) {
            clipboard = reader.read();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void createFile() {
        if(!this.file.exists()) {
            try {
                this.file.createNewFile();
            } catch(IOException e) {
                Bukkit.getLogger().severe("[Shipment] Could not create file " + path + "!");
                e.printStackTrace();
            }
        }
    }

    @Override
    public void save() {
        if(!preExist)
            return;

        try (ClipboardWriter writer = getFromExtension().getWriter(new FileOutputStream(file))) {
            writer.write(clipboard);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Clipboard getClipboard() {
        return clipboard;
    }

    public Clipboard getSchematic() {
        return clipboard;
    }

    private ClipboardFormat getFromExtension() {
        return ClipboardFormats.findByAlias(extension);
    }
}
