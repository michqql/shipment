package me.michqql.shipmentplugin.shipment;

import me.michqql.shipmentplugin.data.type.ShipmentFile;
import me.michqql.shipmentplugin.utils.TimeUtil;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

import java.util.Date;

public class Shipment {

    // All shipment epoch milliseconds should be at the start of the day this shipment represents
    private final long shipmentEpochMS;
    private final ItemsForSale itemsForSale;
    private final TicketSales ticketSales;

    Shipment(long shipmentEpochMS) {
        this.shipmentEpochMS = shipmentEpochMS;

        this.itemsForSale = new ItemsForSale(45);
        this.ticketSales = new TicketSales(itemsForSale);
    }

    Shipment(long shipmentEpochMS, Plugin plugin) {
        this(shipmentEpochMS);
        load(plugin);
    }

    public long getShipmentEpochMS() {
        return shipmentEpochMS;
    }

    public ItemsForSale getItemsForSale() {
        return itemsForSale;
    }

    public TicketSales getTicketSales() {
        return ticketSales;
    }

    public Date getAsDate() {
        return new Date(shipmentEpochMS);
    }

    public boolean isShipmentToday() {
        // Is the epoch millisecond of this shipment equal to the epoch ms of today
        return shipmentEpochMS == TimeUtil.getStartOfToday();
    }

    public int compareShipmentChronology() {
        return Long.compare(shipmentEpochMS, TimeUtil.getStartOfToday());
    }

    /**
     * Package private method that should only be called by ShipmentManager
     * Saves this shipment object to file
     * @param plugin bukkit plugin
     */
    void save(Plugin plugin) {
        ShipmentFile file = new ShipmentFile(plugin, shipmentEpochMS);
        FileConfiguration f = file.getConfig();

        // Save here...
        itemsForSale.save(f.createSection("items-for-sale"));
        ticketSales.save(f.createSection("ticket-sales"));

        file.save();
    }

    private void load(Plugin plugin) {
        ShipmentFile file = new ShipmentFile(plugin, shipmentEpochMS);

        if(!file.existsOtherwiseDelete())
            return;

        FileConfiguration f = file.getConfig();

        // Load here...
        itemsForSale.load(f.getConfigurationSection("items-for-sale"));
        ticketSales.load(f.getConfigurationSection("ticket-sales"));
    }
}
