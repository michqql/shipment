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
import org.bukkit.event.HandlerList;
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
        // Create persistent objects here
        new InventoryListener(this);

        // Loading non-persistent objects...
        startup();
    }

    @Override
    public void onDisable() {
        shutdown(true);
    }

    private void startup() {
        // Loads 'config.yml' using custom implementation
        CommentFile mainConfigurationFile = new CommentFile(this, "", "config");
        if(mainConfigurationFile.isNewFile())
            Bukkit.getLogger().warning("[Shipment First Time Setup] Please visit the 'config.yml' to setup this plugin!");

        // Vault
        if(!setupEconomy()) {
            Bukkit.getLogger().warning("[Shipment] No Vault or Economy plugin found. Please install to use this plugin!");
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
        Bukkit.getPluginManager().registerEvents(
                new NPCListener(this, mainConfigurationFile, messageUtil, economy, npcHandler, shipmentManager)
                , this);
        Bukkit.getPluginManager().registerEvents(
                new CrateListener(this, schematicHandler, shipmentManager), this);
    }

    public void reload() {
        Bukkit.getLogger().info("[Shipment] Reloading NovaCraft Shipment Plugin...");
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

            if(npcHandler != null)
                npcHandler.despawnNPC();
        }
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
