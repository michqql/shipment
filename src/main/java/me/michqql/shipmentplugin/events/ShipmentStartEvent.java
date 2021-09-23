package me.michqql.shipmentplugin.events;

import me.michqql.shipmentplugin.shipment.Shipment;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class ShipmentStartEvent extends Event implements Cancellable {
    private static final HandlerList HANDLER_LIST = new HandlerList();

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

    private final Shipment shipment;
    private final boolean wasForceful;

    private boolean cancelled;

    public ShipmentStartEvent(Shipment shipment, boolean wasForceful) {
        this.shipment = shipment;
        this.wasForceful = wasForceful;
    }

    public Shipment getShipment() {
        return shipment;
    }

    public boolean wasForcefullyStarted() {
        return wasForceful;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }
}
