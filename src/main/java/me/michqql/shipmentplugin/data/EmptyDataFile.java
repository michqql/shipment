package me.michqql.shipmentplugin.data;

import org.bukkit.plugin.Plugin;

import java.io.File;

public abstract class EmptyDataFile {

    protected final Plugin plugin;
    protected final File pluginDataFolder;
    protected File folder, file;

    private final boolean usesFolder;
    protected final String folderName, fileName, extension, path;

    public EmptyDataFile(Plugin plugin, String folder, String file, String extension) {
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
     * @return true if the file was created successfully or already existed
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    protected boolean init() {
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
        return true;
    }

    public boolean exists() {
        return this.file.exists();
    }

    public boolean delete() {
        return this.file.delete();
    }

    protected abstract boolean load();
    public abstract void save();
}
