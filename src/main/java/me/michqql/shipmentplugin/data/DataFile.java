package me.michqql.shipmentplugin.data;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;

public abstract class DataFile {

    protected final Plugin plugin;
    protected final File pluginDataFolder;
    protected File folder, file;

    private final boolean usesFolder;
    protected final String folderName, fileName, extension, path;
    private boolean isNewFile;

    public DataFile(Plugin plugin, String folder, String file, String extension) {
        this.plugin = plugin;
        this.pluginDataFolder = plugin.getDataFolder();

        this.usesFolder = folder != null && !folder.isEmpty();
        this.folderName = folder;
        this.fileName = file;
        this.extension = extension;
        this.path = (usesFolder ? folder + "/" : "") + file + "." + extension;

        init();
        load();
    }

    /**
     * This method ensures all necessary file objects exist
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void init() {
        if(!pluginDataFolder.exists())
            pluginDataFolder.mkdirs();

        if(usesFolder) {
            this.folder = new File(pluginDataFolder, folderName);
            if(!folder.exists())
                folder.mkdirs();

            this.file = new File(folder, fileName + "." + extension);
        } else {
            this.file = new File(pluginDataFolder, fileName + "." + extension);
        }

        if(!this.file.exists()) {
            try {
                this.file.createNewFile();
                this.isNewFile = true;
                copy();
            } catch(IOException e) {
                Bukkit.getLogger().severe("[Shipment] Could not create file " + path + "!");
                e.printStackTrace();
            }
        }
    }

    /**
     * This method should be overridden to provide specific implementation
     */
    protected void copy() {}

    protected abstract void load();
    public abstract void save();

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    protected boolean delete() {
        return file.delete();
    }

    public boolean isNewFile() {
        return isNewFile;
    }
}
