package me.michqql.shipmentplugin.gui.guis.player;

import me.michqql.shipmentplugin.gui.GUI;
import me.michqql.shipmentplugin.shipment.ItemsForSale;
import me.michqql.shipmentplugin.shipment.TicketSales;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.plugin.Plugin;

public class ClaimGUI extends GUI {

    private final TicketSales.Ticket ticket;

    public ClaimGUI(Plugin bukkitPlugin, Player player, TicketSales.Ticket ticket) {
        super(bukkitPlugin, player);
        this.ticket = ticket;

        build("&9Claim items", 1 + (ticket.getItemSize() / 9));
    }

    @Override
    protected void createInventory() {
        int slot = 0;
        for(ItemsForSale.ForSale itemForSale : ticket.getPurchases()) {
            this.inventory.setItem(slot, itemForSale.getItemStack());
            slot++;
        }
    }

    @Override
    protected void updateInventory() {

    }

    @Override
    protected void onCloseEvent() {
        ticket.markAsClaimed();
    }

    @Override
    protected boolean onClickEvent(int slot, ClickType clickType) {
        return false;
    }

    @Override
    protected boolean onPlayerInventoryClickEvent(int slot, ClickType clickType) {
        return false;
    }
}
