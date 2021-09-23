package me.michqql.shipmentplugin.gui.guis.player;

import me.michqql.shipmentplugin.gui.GUI;
import me.michqql.shipmentplugin.shipment.ItemsForSale;
import me.michqql.shipmentplugin.shipment.TicketSales;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.plugin.Plugin;

public class ClaimGUI extends GUI {

    private final ItemsForSale itemsForSale;
    private final TicketSales.Ticket ticket;

    public ClaimGUI(Plugin bukkitPlugin, Player player, ItemsForSale itemsForSale, TicketSales.Ticket ticket) {
        super(bukkitPlugin, player);
        this.itemsForSale = itemsForSale;
        this.ticket = ticket;

        build("&9Claim items", 1 + ((ticket.getPurchaseSize() - 1) / 9));
    }

    @Override
    protected void createInventory() {
        int slot = 0;
        for(ItemsForSale.ForSale item : ticket.getPurchases()) {
            ItemsForSale.ForSale cached = itemsForSale.getItemForSaleBySaleIndex(item.getSaleIndex());
            if(cached == null)
                continue;

            this.inventory.setItem(slot, cached.getItemStack());
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
