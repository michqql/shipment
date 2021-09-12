package me.michqql.shipmentplugin.schematic;

import me.michqql.shipmentplugin.ShipmentPlugin;
import me.michqql.shipmentplugin.gui.guis.player.ClaimGUI;
import me.michqql.shipmentplugin.gui.item.ItemBuilder;
import me.michqql.shipmentplugin.shipment.Shipment;
import me.michqql.shipmentplugin.shipment.ShipmentManager;
import me.michqql.shipmentplugin.shipment.TicketSales;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

public class CrateListener implements Listener {

    private final ShipmentPlugin plugin;
    private final SchematicHandler schematicHandler;
    private final ShipmentManager shipmentManager;

    public CrateListener(ShipmentPlugin plugin, SchematicHandler schematicHandler, ShipmentManager shipmentManager) {
        this.plugin = plugin;
        this.schematicHandler = schematicHandler;
        this.shipmentManager = shipmentManager;
    }

    @EventHandler
    public void onCrateInteract(PlayerInteractEvent e) {
        // We only care about this if there is a shipment today
        if(shipmentManager.getTodaysShipment() == null)
            return;

        // 1. Check clicked block is a chest
        if(!e.hasBlock())
            return;

        Block block = e.getClickedBlock();
        assert block != null; // Checked by e.hasBlock()
        if(block.getType() != Material.CHEST)
            return;

        // 2. Check clicked block is within shipment region
        Location[] minMax = schematicHandler.getRegionMinMax();
        if(!isInsideRegion(block.getLocation(), minMax[0], minMax[1]))
            return;

        e.setCancelled(true); // stop player from opening chest

        // 3. Check player is holding a ticket (first main, then off hand)
        //    Then remove the ticket from the players inventory
        Player player = e.getPlayer();
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();
        boolean main = false;

        TicketSales.Ticket ticket = getTicketFromItem(mainHand);
        if(ticket == null)
            ticket = getTicketFromItem(offHand);
        else
            main = true;

        if(ticket == null)
            return;
        else {
            if(main)
                player.getInventory().setItemInMainHand(null);
            else
                player.getInventory().setItemInOffHand(null);
        }

        if(ticket.isClaimed()) {
            Bukkit.getLogger().warning("[Shipment] " + player.getName() + " (" + player.getUniqueId() + ") had duplicate ticket!");
            return;
        }

        // 4. Open ClaimGUI corresponding to held ticket
        new ClaimGUI(plugin, player, ticket).openGUI();
    }

    private boolean isInsideRegion(Location location, Location min, Location max) {
        return (location.getBlockX() >= min.getBlockX() && location.getBlockX() <= max.getBlockX()) &&
                (location.getBlockY() >= min.getBlockY() && location.getBlockY() <= max.getBlockY()) &&
                (location.getBlockZ() >= min.getBlockZ() && location.getBlockZ() <= max.getBlockZ());
    }

    private TicketSales.Ticket getTicketFromItem(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if(meta == null)
            return null;

        PersistentDataContainer dataContainer = meta.getPersistentDataContainer();
        Integer ticketID = ItemBuilder.getPersistentData(plugin, dataContainer, "ticketID", PersistentDataType.INTEGER);
        Long timestamp = ItemBuilder.getPersistentData(plugin, dataContainer, "timestamp", PersistentDataType.LONG);
        String strUUID = ItemBuilder.getPersistentData(plugin, dataContainer, "playerUUID", PersistentDataType.STRING);

        if(ticketID == null || timestamp == null || strUUID == null)
            return null;

        UUID uuid;
        try {
            uuid = UUID.fromString(strUUID);
        } catch (IllegalArgumentException e) {
            return null;
        }

        Shipment shipment = shipmentManager.getTodaysShipment();
        if(shipment == null || shipment.getShipmentEpochMS() != timestamp)
            return null;

        return shipment.getTicketSales().getTicket(uuid, ticketID);
    }
}
