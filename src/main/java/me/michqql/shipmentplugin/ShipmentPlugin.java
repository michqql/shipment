package me.michqql.shipmentplugin;

import me.michqql.shipmentplugin.commands.ShipmentCommand;
import me.michqql.shipmentplugin.data.CommentFile;
import me.michqql.shipmentplugin.gui.InventoryListener;
import me.michqql.shipmentplugin.npc.NPCHandler;
import me.michqql.shipmentplugin.npc.NPCListener;
import me.michqql.shipmentplugin.schematic.CrateListener;
import me.michqql.shipmentplugin.schematic.SchematicHandler;
import me.michqql.shipmentplugin.shipment.ShipmentManager;
import me.michqql.shipmentplugin.utils.MessageUtil;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class ShipmentPlugin extends JavaPlugin {

    // Vault
    private Economy economy = null;

    private SchematicHandler schematicHandler;
    private NPCHandler npcHandler;
    private ShipmentManager shipmentManager;

    @Override
    public void onEnable() {
        startup();
    }

    @Override
    public void onDisable() {
        shutdown(true);
    }

    private void startup() {
        // Loads 'config.yml' using custom implementation
        CommentFile mainConfigurationFile = new CommentFile(this, "", "config");

        // Check dependencies are installed
        if(!areRequiredDependenciesInstalled(mainConfigurationFile.getConfig())) {
            Bukkit.getLogger().severe("[Shipment] Missing required dependency - disabling plugin");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        // Inform user to setup config
        if(mainConfigurationFile.isNewFile())
            Bukkit.getLogger().warning("[Shipment First Time Setup] Please visit the 'config.yml' to setup this plugin!");

        // Vault
        if(!setupEconomy()) {
            Bukkit.getLogger().warning("[Shipment] No Economy plugin linked to Vault found. Please install to use this plugin!");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        final MessageUtil messageUtil = new MessageUtil(new CommentFile(this, "", "lang"));

        // Handlers and Managers
        this.schematicHandler = new SchematicHandler(this, mainConfigurationFile);
        this.npcHandler = new NPCHandler(this, mainConfigurationFile);
        this.shipmentManager = new ShipmentManager(this, schematicHandler, npcHandler, mainConfigurationFile);

        // Commands
        Objects.requireNonNull(getCommand("shipment")).setExecutor(
                new ShipmentCommand(this, messageUtil, schematicHandler, shipmentManager));

        // Events
        new InventoryListener(this);
        Bukkit.getPluginManager().registerEvents(
                new NPCListener(this, mainConfigurationFile, messageUtil, economy, npcHandler, shipmentManager),
                this);
        Bukkit.getPluginManager().registerEvents(
                new CrateListener(this, mainConfigurationFile, messageUtil, schematicHandler, shipmentManager),
                this);
    }

    public void reload() {
        Bukkit.getLogger().info("[Shipment] Reloading Shipment Plugin...");
        shutdown(false);
        startup();
        Bukkit.getLogger().info("[Shipment] Reload complete");
    }

    public void saveData() {
        if(shipmentManager != null)
            this.shipmentManager.save();
    }

    private void shutdown(boolean permanent) {
        // Unregister all listeners as they will be registered again on startup
        HandlerList.unregisterAll(this);

        // Main configuration file does not need to be saved on disable as it is a comment file
        if(shipmentManager != null)
            this.shipmentManager.save();

        if(permanent) {
            if(schematicHandler != null)
                schematicHandler.undoPaste();
        }
    }

    /*
     * Plugin dependencies:
     * - WorldEdit
     * - Citizens
     * - Vault
     * - HolographicDisplays (optional)
     */
    private boolean areRequiredDependenciesInstalled(FileConfiguration config) {
        boolean passed = true;
        boolean usingHolographicDisplays = config.getBoolean("use-holographic-displays");

        final PluginManager pm = Bukkit.getPluginManager();
        if(!pm.isPluginEnabled("WorldEdit")) {
            Bukkit.getLogger().severe("[Shipment] Missing dependency - WorldEdit");
            passed = false;
        }

        if(!pm.isPluginEnabled("Citizens")) {
            Bukkit.getLogger().severe("[Shipment] Missing dependency - Citizens");
            passed = false;
        }

        if(!pm.isPluginEnabled("Vault")) {
            Bukkit.getLogger().severe("[Shipment] Missing dependency - Vault");
            passed = false;
        }

        if(usingHolographicDisplays && !pm.isPluginEnabled("HolographicDisplays")) {
            Bukkit.getLogger().severe("[Shipment] Missing dependency - HolographicDisplays");
            passed = false;
        }

        return passed;
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null)
            return false;

        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null)
            return false;

        economy = rsp.getProvider();
        return true;
    }
}
