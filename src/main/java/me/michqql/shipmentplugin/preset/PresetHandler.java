package me.michqql.shipmentplugin.preset;

import me.michqql.shipmentplugin.ShipmentPlugin;
import me.michqql.shipmentplugin.data.CommentFile;
import me.michqql.shipmentplugin.data.type.PresetFile;
import me.michqql.shipmentplugin.shipment.ItemsForSale;
import me.michqql.shipmentplugin.utils.IOUtil;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.util.*;

public class PresetHandler {

    private final static Random RANDOM = new Random();

    private final ShipmentPlugin plugin;
    private final File presetsFolder;

    private Preset[] presets;

    // Configuration options
    private boolean failedSetup, enabled, random;
    private Preset defaultPreset;

    public PresetHandler(ShipmentPlugin plugin, CommentFile config) {
        this.plugin = plugin;
        this.presetsFolder = IOUtil.makeDirectory(plugin.getDataFolder(), "presets");

        this.presets = new Preset[50];

        load();
        loadConfig(config.getConfig());
    }

    private void loadConfig(FileConfiguration f) {
        this.enabled = f.getBoolean("presets.enabled");
        this.random = f.getBoolean("presets.random");

        String defaultIdentifier = f.getString("presets.default-identifier", "");
        if(enabled) {
            if(defaultIdentifier.isEmpty() && !random) {
                Bukkit.getLogger().warning("[Shipment] Item presets disabled - please specify a default");
                return;
            }

            this.defaultPreset = getById(defaultIdentifier);
            if(defaultPreset == null && !random) {
                this.enabled = false;
                this.failedSetup = true;
                Bukkit.getLogger().warning("[Shipment] Item presets disabled - invalid preset identifier: " + defaultIdentifier);
            }
        }
    }

    private void load() {
        int index = 0;
        for(File file : IOUtil.getFiles(presetsFolder)) {
            PresetFile preset = new PresetFile(plugin, file);
            presets[index] = new Preset(preset);
            index++;
        }
    }

    public void save() {
        for(Preset preset : presets) {
            if(preset == null)
                continue;

            PresetFile file = new PresetFile(plugin, preset.getIdentifier());
            file.setDisplayName(preset.getDisplayName());
            file.saveItems(preset.getItems());
        }
    }

    public boolean isSetupFailed() {
        return failedSetup;
    }

    public boolean isEnabled() {
        return !failedSetup && enabled;
    }

    public boolean isRandom() {
        return random;
    }

    public Preset getById(String id) {
        id = id.toLowerCase();
        for(Preset preset : presets) {
            if (preset != null && preset.getIdentifier().equals(id))
                return preset;
        }
        return null;
    }

    public Preset getDefaultPreset() {
        if(!enabled)
            return null;

        if(random) {
            ArrayList<Integer> validIndexes = new ArrayList<>();
            for(int i = 0; i < presets.length; i++) {
                if(presets[i] != null)
                    validIndexes.add(i);
            }

            int randomIndex = validIndexes.get(RANDOM.nextInt(validIndexes.size()));
            return presets[randomIndex];
        }

        return defaultPreset;
    }

    public boolean isPresetDefault(Preset preset) {
        return preset.equals(defaultPreset);
    }

    public int getPresetsSize() {
        int size = 0;
        for(Preset preset : presets) {
            if (preset != null)
                size++;
        }
        return size;
    }

    public boolean isIndexValid(int index) {
        return index >= 0 && index < presets.length && presets[index] != null;
    }

    public Preset[] getPresets() {
        return presets;
    }

    public boolean presetExists(String id) {
        return getById(id) != null;
    }

    public void createPreset(String presetName, ItemsForSale items) {
        final String id = presetName.replaceAll(" ", "_").toLowerCase();

        if(presetExists(id))
            return;

        presets[getFirstFreeIndex()] = new Preset(id, presetName, items);
        PresetFile file = new PresetFile(plugin, id);
        file.setDisplayName(presetName);
        file.saveItems(items);
    }

    public void deletePreset(int index) {
        Preset preset = presets[index];
        if(preset == null)
            return;

        if(isPresetDefault(preset) && !random) {
            this.enabled = false;
            this.failedSetup = true;
            this.defaultPreset = null;
            Bukkit.getLogger().warning("[Shipment] Default preset deleted. Item Presets disabled");
        }

        presets[index] = null;
        new PresetFile(plugin, preset.getIdentifier()).delete();
    }

    private int getFirstFreeIndex() {
        for(int i = 0; i < presets.length; i++) {
            if(presets[i] == null)
                return i;
        }

        // Array is full at this point, grow size
        int maxSize = presets.length;
        presets = Arrays.copyOf(presets, maxSize + 10);
        return maxSize;
    }

    public static class Preset {
        private final String identifier;
        private final String displayName;

        private final ItemsForSale items;

        Preset(PresetFile file) {
            this.identifier = file.getPresetName().toLowerCase();
            this.displayName = file.getDisplayName();
            this.items = file.getItems();
        }

        Preset(String identifier, String displayName, ItemsForSale items) {
            this.identifier = identifier.toLowerCase();
            this.displayName = displayName;
            this.items = items;
        }

        public String getIdentifier() {
            return identifier;
        }

        public String getDisplayName() {
            return displayName == null ? identifier : displayName;
        }

        public ItemsForSale getItems() {
            return items;
        }
    }
}
