package me.michqql.shipmentplugin.data.type;

import me.michqql.shipmentplugin.data.DataFile;
import org.bukkit.plugin.Plugin;

import java.io.FileNotFoundException;
import java.io.PrintStream;

public class ExceptionFile extends DataFile {

    public ExceptionFile(Plugin plugin, String folder, String file) {
        super(plugin, folder, file, "txt");
    }

    @Override
    protected boolean load() {
        return true;
    }

    public void write(Exception e) {
        try {
            e.printStackTrace(new PrintStream(file));
        } catch (FileNotFoundException fileNotFoundException) {
            fileNotFoundException.printStackTrace();
        }
    }

    @Override
    public void save() {
        // do nothing
    }
}
