package me.michqql.shipmentplugin.gui.guis.admin;

import me.michqql.shipmentplugin.gui.GUI;
import me.michqql.shipmentplugin.gui.item.ItemBuilder;
import me.michqql.shipmentplugin.shipment.Shipment;
import me.michqql.shipmentplugin.shipment.ShipmentManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainOverviewGUI extends GUI {

    private final static SimpleDateFormat DATE_TIME_FORMATTER = new SimpleDateFormat("EEEE MMMM dd yyyy");

    // Item slots
    private final static int
            NEXT_SHIPMENT_SLOT = 4;

    private final static int[] PANE_SLOTS = new int[]{
            0, 1, 2, 3, 5, 6, 7, 8, 9, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26
    };

    private final static int[] SHIPMENT_SLOTS = new int[]{
            10, 11, 12, 13, 14, 15, 16
    };

    private final ShipmentManager shipmentManager;

    public MainOverviewGUI(Plugin bukkitPlugin, Player player, ShipmentManager shipmentManager) {
        super(bukkitPlugin, player);
        this.shipmentManager = shipmentManager;

        build("&9Shipments", 3);
    }

    @Override
    protected void createInventory() {
        // Panes
        ItemStack pane = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).displayName("").getItem();
        for(int i : PANE_SLOTS) {
            this.inventory.setItem(i, pane);
        }

        // The upcoming shipment
        long nextShipmentMs = shipmentManager.getNextShipment();
        boolean shipmentToday = nextShipmentMs <= System.currentTimeMillis();

        this.inventory.setItem(NEXT_SHIPMENT_SLOT,
                new ItemBuilder(Material.CLOCK)
                        .displayName("&3Next shipment date:")
                        .lore(
                                "&b-> " +  DATE_TIME_FORMATTER.format(new Date(nextShipmentMs)) + (shipmentToday ? " &c(today)" : "")
                        ).getItem()
        );

        // Shipments
        Shipment[] shipments = shipmentManager.getShipments();
        for(int i = 0; i < SHIPMENT_SLOTS.length; i++) {
            if(i >= shipments.length)
                break;

            Shipment shipment = shipments[i];
            if(shipment == null)
                continue;

            int chronology = shipment.compareShipmentChronology();

            Material material = chronology < 0 ? Material.RED_CONCRETE : (chronology > 0 ? Material.YELLOW_CONCRETE : Material.GREEN_CONCRETE);
            String timeline = chronology < 0 ? "Past" : (chronology > 0 ? "Upcoming" : (shipmentToday ? "Today's" : "Next"));

            this.inventory.setItem(SHIPMENT_SLOTS[i], new ItemBuilder(material)
                    .displayName("&3" + timeline + " shipment")
                    .lore(
                            "&b" + DATE_TIME_FORMATTER.format(new Date(shipment.getShipmentEpochMS())),
                            "",
                            "&bClick for more"
                    ).getItem());
        }
    }

    @Override
    protected void updateInventory() {

    }

    @Override
    protected void onCloseEvent() {

    }

    @Override
    protected boolean onClickEvent(int slot, ClickType clickType) {
        int index = -1;
        for(int i = 0; i < SHIPMENT_SLOTS.length; i++) {
            if(slot == SHIPMENT_SLOTS[i]) {
                index = i;
                break;
            }
        }

        if(index >= 0) {
            Shipment[] shipments = shipmentManager.getShipments();
            if(index >= shipments.length)
                return true;

            Shipment shipment = shipments[index];
            if(shipment == null)
                return true;

            new ShipmentGUI(bukkitPlugin, player, shipmentManager, shipment).openGUI();
        }
        return true;
    }

    @Override
    protected boolean onPlayerInventoryClickEvent(int slot, ClickType clickType) {
        return false;
    }
}
