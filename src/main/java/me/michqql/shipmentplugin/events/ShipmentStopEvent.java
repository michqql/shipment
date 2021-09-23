package me.michqql.shipmentplugin.events;

import me.michqql.shipmentplugin.shipment.Shipment;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class ShipmentStopEvent extends Event {
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

    public ShipmentStopEvent(Shipment shipment, boolean wasForceful) {
        this.shipment = shipment;
        this.wasForceful = wasForceful;
    }

    public Shipment getShipment() {
        return shipment;
    }

    public boolean wasForcefullyStarted() {
        return wasForceful;
    }
}
