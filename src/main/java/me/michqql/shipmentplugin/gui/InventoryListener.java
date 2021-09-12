package me.michqql.shipmentplugin.gui;

import me.michqql.shipmentplugin.ShipmentPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public class InventoryListener implements Listener {

    public InventoryListener(ShipmentPlugin plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent e) {
        if(!(e.getWhoClicked() instanceof Player) || e.getClickedInventory() == null)
            return;

        final Player player = (Player) e.getWhoClicked();
        final GUI gui = GUIManager.getCurrentGUI(player.getUniqueId());
        if(gui == null)
            return;

        boolean playerInventory = (e.getClickedInventory().equals(player.getInventory()));
        e.setCancelled(gui.internalClick(playerInventory, e.getSlot(), e.getClick()));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClose(InventoryCloseEvent e) {
        if(!(e.getPlayer() instanceof Player))
            return;

        final Player player = (Player) e.getPlayer();
        final UUID uuid = player.getUniqueId();
        final GUI current = GUIManager.getCurrentGUI(uuid);
        final GUI past = GUIManager.getPreviousGUI(uuid);

        // Neither GUI's exist (the player doesn't have any of our GUI's open)
        // -> They closed a creative inventory, crafting table inventory, chest inventory etc
        // -> Nothing to do
        if(current == null && past == null)
            return;

        /*
         * The player only had 1 GUI open, their current GUI
         * Therefore, we know that when they close this,
         * we can completely remove this entry from our list
         */
        if(past == null) {
            current.internalClose();
            GUIManager.removeEntry(uuid);
            return;
        }

        // Only past GUI exists
        if(current == null) {
            GUIManager.removeCurrentGUI(uuid);
            return;
        }

        // Both exist, current GUI is closed and past GUI is open
        // -> Close the current GUI and remove it from the list
        if(past.internalIsOpen() && !current.internalIsOpen()) {
            current.internalClose();
            GUIManager.removeCurrentGUI(uuid);
            return;
        }

        // Both exist, both are open
        // -> Close the past GUI but keep it
        if(past.internalIsOpen()) {
            past.internalClose();
            return;
        }

        // Edge case, remove player's entry (all guis)
        // Allows for normal inventory function in the case of a logic error
        GUIManager.removeEntry(uuid);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        GUIManager.removeEntry(e.getPlayer().getUniqueId());
    }
}
