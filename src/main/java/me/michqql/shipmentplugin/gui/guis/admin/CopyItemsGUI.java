package me.michqql.shipmentplugin.gui.guis.admin;

import me.michqql.shipmentplugin.gui.GUI;
import me.michqql.shipmentplugin.gui.GUIManager;
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

public class CopyItemsGUI extends GUI {

    private final static SimpleDateFormat DATE_TIME_FORMATTER = new SimpleDateFormat("EEEE MMMM dd yyyy");

    // Item slots
    private final static int BACK_SLOT = 0, COPIED_ITEMS_SLOT = 4, NEXT_SHIPMENT_SLOT = 11;

    private final static int[] PANE_SLOTS = new int[]{
            1, 2, 3, 5, 6, 7, 8, 9, 10, 11, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26
    };

    private final static int[] SHIPMENT_SLOTS = new int[]{
            12, 13, 14
    };

    private final MessageUtil messageUtil;
    private final ShipmentManager shipmentManager;
    private final PresetHandler presetHandler;
    private final Shipment toCopy;

    public CopyItemsGUI(Plugin bukkitPlugin, Player player, MessageUtil messageUtil,
                        ShipmentManager shipmentManager, PresetHandler presetHandler, Shipment toCopy) {

        super(bukkitPlugin, player);
        this.messageUtil = messageUtil;
        this.shipmentManager = shipmentManager;
        this.presetHandler = presetHandler;
        this.toCopy = toCopy;

        build("&9Copy items", 3);
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

        // Items to copy
        this.inventory.setItem(COPIED_ITEMS_SLOT,
                new ItemBuilder(Material.CLOCK)
                        .displayName("&3" + toCopy.getItemsForSale().getAmountOfItemsForSale() + " items to copy")
                        .getItem()
        );

        // Upcoming Shipments
        Shipment[] shipments = shipmentManager.getShipments();
        Shipment next = shipments[3];
        if(next != null && next.compareShipmentChronology() == 1) {
            this.inventory.setItem(NEXT_SHIPMENT_SLOT, new ItemBuilder(Material.OAK_BOAT)
                    .displayName("&3Upcoming shipment")
                    .lore(
                            "&b" + DATE_TIME_FORMATTER.format(new Date(next.getShipmentEpochMS())),
                            "&bCurrently has " + next.getItemsForSale().getAmountOfItemsForSale() + " items for sale",
                            "",
                            "&bLeft-Click &fto copy items"
                    ).getItem());
        }

        // Future shipments
        for(int i = 0; i < SHIPMENT_SLOTS.length; i++) {
            if(i >= shipments.length)
                break;

            Shipment shipment = shipments[i + 4]; // We want future shipments only
            if(shipment == null)
                continue;

            this.inventory.setItem(SHIPMENT_SLOTS[i], new ItemBuilder(Material.OAK_BOAT)
                    .displayName("&3Upcoming shipment")
                    .lore(
                            "&b" + DATE_TIME_FORMATTER.format(new Date(shipment.getShipmentEpochMS())),
                            "&bCurrently has " + shipment.getItemsForSale().getAmountOfItemsForSale() + " items for sale",
                            "",
                            "&bLeft-Click &fto copy items"
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
        if(slot == BACK_SLOT) {
            GUIManager.openPreviousGUI(player.getUniqueId());
            return true;
        }

        // Upcoming shipments
        Shipment[] shipments = shipmentManager.getShipments();
        Shipment next = shipments[3];
        if(slot == NEXT_SHIPMENT_SLOT && next != null && next.compareShipmentChronology() == 1) {
            next.getItemsForSale().addAll(this.toCopy.getItemsForSale());
            new MainOverviewGUI(bukkitPlugin, player, messageUtil, shipmentManager, presetHandler).openGUI();
            return true;
        }

        // Future shipments
        int index = -1;
        for(int i = 0; i < SHIPMENT_SLOTS.length; i++) {
            if(slot == SHIPMENT_SLOTS[i]) {
                index = i;
                break;
            }
        }

        if(index >= 0) {
            index += 4; // We want future shipments only

            if(index >= shipments.length)
                return true;

            Shipment shipment = shipments[index];
            if(shipment == null)
                return true;

            shipment.getItemsForSale().addAll(this.toCopy.getItemsForSale());
            new MainOverviewGUI(bukkitPlugin, player, messageUtil, shipmentManager, presetHandler).openGUI();
        }
        return true;
    }

    @Override
    protected boolean onPlayerInventoryClickEvent(int slot, ClickType clickType) {
        return false;
    }
}
