package me.michqql.shipmentplugin.gui.guis.admin;

import me.michqql.shipmentplugin.gui.GUI;
import me.michqql.shipmentplugin.gui.GUIManager;
import me.michqql.shipmentplugin.gui.item.ItemBuilder;
import me.michqql.shipmentplugin.shipment.Shipment;
import me.michqql.shipmentplugin.shipment.ShipmentManager;
import me.michqql.shipmentplugin.shipment.TicketSales;
import me.michqql.shipmentplugin.utils.MessageUtil;
import me.michqql.shipmentplugin.utils.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ShipmentGUI extends GUI {

    private final static int MAX_TICKET_SALES_SHOWN = 5;

    // Slots
    private final static int BACK_SLOT = 0, ITEM_SALE_SLOT = 13, TICKET_SALES_SLOT = 15;

    private final static int[] PANE_SLOTS = new int[]{
            1, 2, 3, 4, 5, 6, 7, 8, 9, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26
    };

    private final MessageUtil messageUtil;
    private final ShipmentManager shipmentManager;
    private final Shipment shipment;

    private int ticketPage = 0;

    public ShipmentGUI(Plugin bukkitPlugin, Player player, MessageUtil messageUtil,
                       ShipmentManager shipmentManager, Shipment shipment) {

        super(bukkitPlugin, player);
        this.messageUtil = messageUtil;
        this.shipmentManager = shipmentManager;
        this.shipment = shipment;

        build("&9" + TimeUtil.DATE_FORMATTER.format(shipment.getAsDate()), 3);
    }

    @Override
    protected void createInventory() {
        // Panes
        ItemStack pane = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).displayName("").getItem();
        for(int i : PANE_SLOTS) {
            this.inventory.setItem(i, pane);
        }

        this.inventory.setItem(BACK_SLOT, new ItemBuilder(Material.RED_BED)
                .displayName("&3< Go back").getItem());

        updateInventory();
    }

    @Override
    protected void updateInventory() {
        this.inventory.setItem(ITEM_SALE_SLOT, new ItemBuilder(Material.CHEST)
                .displayName("&3Items for sale")
                .lore(
                        "&bClick to edit",
                        "&bItems on sale: " + getAmountOfItemsForSale()
                ).getItem()
        );

        this.inventory.setItem(TICKET_SALES_SLOT, new ItemBuilder(Material.PAPER)
                .displayName("&3Ticket sales")
                .lore(getTicketSales())
                .getItem());
    }

    @Override
    protected void onCloseEvent() {

    }

    @Override
    protected boolean onClickEvent(int slot, ClickType clickType) {
        switch (slot) {
            case BACK_SLOT:
                GUIManager.openPreviousGUI(player.getUniqueId());
                break;

            case ITEM_SALE_SLOT:
                new ItemsForSaleGUI(bukkitPlugin, player, messageUtil, shipmentManager, null, shipment).openGUI();
                break;

            case TICKET_SALES_SLOT:
                if(clickType == ClickType.LEFT) {
                    ticketPage = Math.max(0, ticketPage - 1);
                } else if(clickType == ClickType.RIGHT) {
                    int maxPages = (int) Math.ceil((double) (shipment.getTicketSales().getUserSales() - 1) / MAX_TICKET_SALES_SHOWN);
                    ticketPage = Math.min(maxPages, ticketPage + 1);
                }
                updateInventory();
                break;
        }
        return true;
    }

    @Override
    protected boolean onPlayerInventoryClickEvent(int slot, ClickType clickType) {
        return false;
    }

    private List<String> getTicketSales() {
        List<String> lines = new ArrayList<>();

        int chronology = shipment.compareShipmentChronology();
        if(chronology > 0)
            lines.add("&cShipment has not gone on sale yet!");
        else if(!shipment.getTicketSales().hasSales())
            lines.add("&cShipment has not made any sales" + (chronology == 0 ? " yet" : "") + "!");
        else {
            lines.add("&bTotal ticket sales: " + shipment.getTicketSales().getTicketSales());
            lines.add("");

            if(ticketPage > 0)
                lines.add(" &b^ Left-Click &fto page up");

            final int sales = ticketPage * MAX_TICKET_SALES_SHOWN;
            int counter = 0;
            int ticketCounter = 0;
            for(Map.Entry<UUID, List<TicketSales.Ticket>> entry : shipment.getTicketSales().getMap().entrySet()) {
                if(ticketCounter >= MAX_TICKET_SALES_SHOWN)
                    break;

                if(counter >= sales) {
                    // Show ticket sale
                    Player buyer = Bukkit.getPlayer(entry.getKey());
                    String name = (buyer != null ? buyer.getName() : "UNKNOWN");
                    lines.add("&b- " + name + " owns " + entry.getValue().size() + " tickets");
                    ticketCounter++;
                }

                counter++; // Must be at end of code block
            }

            if((ticketPage + 1) * MAX_TICKET_SALES_SHOWN < shipment.getTicketSales().getUserSales())
                lines.add(" &bv Right-Click &fto page down");
        }

        return lines;
    }

    private String getAmountOfItemsForSale() {
        int size = shipment.getItemsForSale().getAmountOfItemsForSale();
        return size > 0 ? "&f" + size : "&cnone";
    }
}
