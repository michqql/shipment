package me.michqql.shipmentplugin.events;

import me.michqql.shipmentplugin.shipment.Shipment;
import me.michqql.shipmentplugin.shipment.TicketSales;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class CrateOpenEvent extends Event {
    private static final HandlerList HANDLER_LIST = new HandlerList();

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

    private final Player player;
    private final Shipment shipment;
    private final TicketSales.Ticket ticket;

    public CrateOpenEvent(Player player, Shipment shipment, TicketSales.Ticket ticket) {
        this.player = player;
        this.shipment = shipment;
        this.ticket = ticket;
    }

    public Player getPlayer() {
        return player;
    }

    public Shipment getShipment() {
        return shipment;
    }

    public TicketSales.Ticket getTicket() {
        return ticket;
    }
}
