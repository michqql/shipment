package me.michqql.shipmentplugin.data;

import com.google.gson.*;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.io.*;
import java.util.logging.Level;

public class JsonFile extends DataFile {

    public final static Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public final static JsonParser PARSER = new JsonParser();
    private JsonElement element;

    public JsonFile(Plugin plugin, String folder, String file) {
        super(plugin, folder, file, "json");
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
    protected boolean load() {
        try {
            this.element = PARSER.parse(new FileReader(this.file));
            return true;
        } catch (FileNotFoundException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[Shipment] Could not read json file named " + path);
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void save() {
        try {
            FileWriter fw = new FileWriter(this.file);
            fw.write(GSON.toJson(element));
        } catch (IOException e) {
            Bukkit.getLogger().log(Level.SEVERE, "[Shipment] Could not save json file named " + path);
            e.printStackTrace();
        }
    }

    public JsonObject getJsonObject() {
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }
}
