package me.michqql.shipmentplugin.gui.guis.admin;

import me.michqql.shipmentplugin.gui.GUI;
import me.michqql.shipmentplugin.gui.guis.admin.presets.PresetsGUI;
import me.michqql.shipmentplugin.gui.item.ItemBuilder;
import me.michqql.shipmentplugin.preset.PresetHandler;
import me.michqql.shipmentplugin.shipment.Shipment;
import me.michqql.shipmentplugin.shipment.ShipmentManager;
import me.michqql.shipmentplugin.utils.MessageUtil;
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
    private final static int NEXT_SHIPMENT_SLOT = 4, PRESETS_SLOT = 8;

    private final static int[] PANE_SLOTS = new int[]{
            0, 1, 2, 3, 5, 6, 7, 9, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26
    };

    private final static int[] SHIPMENT_SLOTS = new int[]{
            10, 11, 12, 13, 14, 15, 16
    };

    private final MessageUtil messageUtil;
    private final ShipmentManager shipmentManager;
    private final PresetHandler presetHandler;

    public MainOverviewGUI(Plugin bukkitPlugin, Player player, MessageUtil messageUtil, ShipmentManager shipmentManager,
                           PresetHandler presetHandler) {

        super(bukkitPlugin, player);
        this.messageUtil = messageUtil;
        this.shipmentManager = shipmentManager;
        this.presetHandler = presetHandler;

        build("&9Shipments", 3);
    }

    @Override
    protected void createInventory() {
        // Panes
        ItemStack pane = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).displayName("").getItem();
        for(int i : PANE_SLOTS) {
            this.inventory.setItem(i, pane);
        }

        updateInventory();
    }

    @Override
    protected void updateInventory() {
        // Presets
        this.inventory.setItem(PRESETS_SLOT, new ItemBuilder(Material.BOOKSHELF)
                .displayName("&3Item Presets")
                .lore("&f" + presetHandler.getPresetsSize() + "&b preset" + (presetHandler.getPresetsSize() != 1 ? "s" : ""))
                .getItem());

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
            if (i >= shipments.length)
                break;

            Shipment shipment = shipments[i];
            if (shipment == null)
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
    protected void onCloseEvent() {

    }

    @Override
    protected boolean onClickEvent(int slot, ClickType clickType) {
        if(slot == PRESETS_SLOT) {
            new PresetsGUI(bukkitPlugin, player, messageUtil, presetHandler).openGUI();
            return true;
        }

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

            new ShipmentGUI(bukkitPlugin, player, messageUtil, shipmentManager, presetHandler, shipment).openGUI();
        }
        return true;
    }

    @Override
    protected boolean onPlayerInventoryClickEvent(int slot, ClickType clickType) {
        return false;
    }
}
