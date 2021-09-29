package me.michqql.shipmentplugin.events;

import me.michqql.shipmentplugin.shipment.ItemsForSale;
import me.michqql.shipmentplugin.shipment.Shipment;
import me.michqql.shipmentplugin.shipment.TicketSales;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.List;

@SuppressWarnings("unused")
public class TicketPurchaseEvent extends Event {
    private static final HandlerList HANDLER_LIST = new HandlerList();

    @SuppressWarnings("NullableProblems")
    @Override
    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

    private final Player player;
    private final Shipment shipment;
    private final List<ItemsForSale.ForSale> itemBasket;
    private final double totalCost;
    private final Response response;
    private final TicketSales.Ticket ticket;

    public TicketPurchaseEvent(Player player, Shipment shipment, List<ItemsForSale.ForSale> itemBasket, double totalCost,
                               Response response, TicketSales.Ticket ticket) {

        this.player = player;
        this.shipment = shipment;
        this.itemBasket = itemBasket;
        this.totalCost = totalCost;
        this.response = response;
        this.ticket = ticket;
    }

    public Player getPlayer() {
        return player;
    }

    public Shipment getShipment() {
        return shipment;
    }

    public List<ItemsForSale.ForSale> getItemBasket() {
        return itemBasket;
    }

    public double getTotalCost() {
        return totalCost;
    }

    public Response getResponse() {
        return response;
    }

    public TicketSales.Ticket getTicket() {
        return ticket;
    }

    public enum Response {
        MAX_TICKETS_REACHED, NO_INVENTORY_SPACE, INSUFFICIENT_FUNDS, UNEXPECTED_ERROR, SUCCESS
    }
}
