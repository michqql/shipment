package me.michqql.shipmentplugin.schematic;

import me.michqql.shipmentplugin.ShipmentPlugin;
import me.michqql.shipmentplugin.data.CommentFile;
import me.michqql.shipmentplugin.events.CrateOpenEvent;
import me.michqql.shipmentplugin.gui.guis.player.ClaimGUI;
import me.michqql.shipmentplugin.gui.item.ItemBuilder;
import me.michqql.shipmentplugin.shipment.Shipment;
import me.michqql.shipmentplugin.shipment.ShipmentManager;
import me.michqql.shipmentplugin.shipment.TicketSales;
import me.michqql.shipmentplugin.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.checkerframework.checker.units.qual.min;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class CrateListener implements Listener {

    private final ShipmentPlugin plugin;
    private final MessageUtil messageUtil;
    private final SchematicHandler schematicHandler;
    private final ShipmentManager shipmentManager;

    // Configurable options
    private boolean warnDuplicateTickets;
    private boolean onlyPurchaserCanOpen;
    private boolean playSound;
    private Sound sound;
    private float volume, pitch;

    public CrateListener(ShipmentPlugin plugin, CommentFile config, MessageUtil messageUtil, SchematicHandler schematicHandler, ShipmentManager shipmentManager) {
        this.plugin = plugin;
        this.messageUtil = messageUtil;
        this.schematicHandler = schematicHandler;
        this.shipmentManager = shipmentManager;

        loadConfig(config.getConfig());
    }

    private void loadConfig(FileConfiguration f) {
        // Ticket restrictions
        this.warnDuplicateTickets = f.getBoolean("ticket.warn-duplicate-tickets", true);
        this.onlyPurchaserCanOpen = f.getBoolean("ticket.only-purchaser-can-claim");

        // Sound
        this.playSound = f.getBoolean("crates.effects.sound.play");
        this.volume = (float) f.getDouble("crates.effects.sound.volume");
        this.pitch = (float) f.getDouble("crates.effects.sound.pitch");
        String soundString = f.getString("crates.effects.sound.on-open", "");
        try {
            this.sound = Sound.valueOf(soundString);
        } catch (IllegalArgumentException | NullPointerException e) {
            this.playSound = false;
        }
    }

    @EventHandler
    public void onCrateInteract(PlayerInteractEvent e) {
        // We only care about this if there is a shipment today
        if(shipmentManager.getTodaysShipment() == null)
            return;

        // 1. Check clicked block is a valid type
        if(!e.hasBlock())
            return;

        Block block = e.getClickedBlock();
        assert block != null; // Checked by e.hasBlock()
        if(!schematicHandler.getCrateMaterials().contains(block.getType()))
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

        if(ticket == null) {
            messageUtil.sendList(player, "claim.must-hold-ticket");
            return;
        }

        // Check if ticket is a duplicate
        if(ticket.isClaimed()) {
            if(main)
                player.getInventory().setItemInMainHand(null);
            else
                player.getInventory().setItemInOffHand(null);

            if(warnDuplicateTickets) {
                Bukkit.getLogger().warning("[Shipment] " + player.getName() + " (" + player.getUniqueId() + ") had duplicate ticket!");
                Bukkit.getLogger().warning("[Shipment] Purchaser: " + ticket.getPlayer() + ", TicketID: " + ticket.getTicketId());

                final TicketSales.Ticket finalTicket = ticket;
                HashMap<String, String> placeholders = new HashMap<String, String>(){{
                    put("opener.name", player.getName());
                    put("opener.uuid", player.getUniqueId().toString());
                    put("purchaser.uuid", finalTicket.getPlayer().toString());
                    put("ticket-id", String.valueOf(finalTicket.getTicketId()));
                }};

                for(Player online : Bukkit.getOnlinePlayers()) {
                    if(online.hasPermission("shipment.warn")) {
                        messageUtil.sendList(online, "purchase.staff-warn-duplicate", placeholders);
                    }
                }
            }
            return;
        }

        Shipment shipment = shipmentManager.getTodaysShipment();
        if(shipment == null)
            return;

        // Check player is purchaser and config option enabled
        if(onlyPurchaserCanOpen && !ticket.getPlayer().equals(player.getUniqueId())) {
            messageUtil.sendList(player, "claim.not-purchaser");
            return;
        }

        // Remove ticket item from players inventory
        if(main)
            player.getInventory().setItemInMainHand(null);
        else
            player.getInventory().setItemInOffHand(null);

        if(playSound)
            player.playSound(block.getLocation(), sound, volume, pitch);

        // 4. Open ClaimGUI corresponding to held ticket
        Bukkit.getPluginManager().callEvent(new CrateOpenEvent(player, shipment, ticket));
        new ClaimGUI(plugin, player, shipment.getItemsForSale(), ticket).openGUI();
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
