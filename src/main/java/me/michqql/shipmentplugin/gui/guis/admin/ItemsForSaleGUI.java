package me.michqql.shipmentplugin.gui.guis.admin;

import me.michqql.shipmentplugin.gui.GUI;
import me.michqql.shipmentplugin.gui.GUIManager;
import me.michqql.shipmentplugin.gui.item.ItemBuilder;
import me.michqql.shipmentplugin.shipment.ItemsForSale;
import me.michqql.shipmentplugin.shipment.Shipment;
import me.michqql.shipmentplugin.shipment.ShipmentManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public class ItemsForSaleGUI extends GUI {

    // Slots
    private final static int BACK_SLOT = 0, ADD_ITEM_SLOT = 1, COPY_ITEMS_SLOT = 2, START_SLOT = 9;

    private final static int[] PANE_SLOTS = new int[]{
            2, 3, 4, 5, 6, 7, 8
    };

    private final ShipmentManager shipmentManager;
    private final Shipment shipment;
    private final ItemsForSale itemsForSale;
    private final boolean canEdit;

    private boolean readyToDelete;
    private int indexToDelete;

    public ItemsForSaleGUI(Plugin bukkitPlugin, Player player, ShipmentManager shipmentManager, Shipment shipment) {
        super(bukkitPlugin, player);
        this.shipmentManager = shipmentManager;
        this.shipment = shipment;
        this.itemsForSale = shipment.getItemsForSale();

        this.canEdit = shipment.compareShipmentChronology() == 1 ||
                (shipment.compareShipmentChronology() == 0 && shipmentManager.allowRealTimeEdits());

        build("&9Items for sale", 6);
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

        if(canEdit) {
            this.inventory.setItem(ADD_ITEM_SLOT, new ItemBuilder(Material.ITEM_FRAME)
                    .displayName("&3Add item")
                    .lore(
                            "&bClick to add new item for sale",
                            "&b- Add item, set amount, set price"
                    ).getItem());
        } else {
            this.inventory.setItem(ADD_ITEM_SLOT, new ItemBuilder(Material.BARRIER)
                    .displayName("&3Items cannot be altered now").getItem());
        }

        if(shipment.compareShipmentChronology() == -1) {
            this.inventory.setItem(COPY_ITEMS_SLOT, new ItemBuilder(Material.REPEATER)
                    .displayName("&3Copy items")
                    .lore("&bAllows you to copy items to an upcoming shipment")
                    .getItem());
        }

        updateInventory();
    }

    @Override
    protected void updateInventory() {
        for(int i = START_SLOT; i < this.inventory.getSize(); i++) {
            this.inventory.setItem(i, null);
        }

        int slot = START_SLOT;
        for (ItemsForSale.ForSale sale : itemsForSale.getSales()) {
            if(sale == null)
                continue;

            boolean flag = readyToDelete && (indexToDelete + START_SLOT == slot);

            ItemStack copy = new ItemStack(sale.getItemStack());
            this.inventory.setItem(slot, new ItemBuilder(copy)
                    .addLore(
                            "",
                            "&bPrice: $" + sale.getPrice(),
                            (flag) ? "&cClick again to confirm deletion!" : "&bClick to delete!"
                    ).getItem());
            slot++;
        }
    }

    @Override
    protected void onCloseEvent() {

    }

    @Override
    protected boolean onClickEvent(int slot, ClickType clickType) {
        if(slot == BACK_SLOT) {
            GUIManager.openPreviousGUI(player.getUniqueId());
            return true;
        } else if(slot == COPY_ITEMS_SLOT && shipment.compareShipmentChronology() == -1) {
            new CopyItemsGUI(bukkitPlugin, player, shipmentManager, shipment).openGUI();
        } else if(!canEdit) {
            return true;
        } else if(slot == ADD_ITEM_SLOT) {
            new AddItemGUI(bukkitPlugin, player, shipment).openGUI();
            return true;
        } else if(slot >= START_SLOT) {
            int index = slot - START_SLOT;
            if(readyToDelete && index != indexToDelete) {
                this.readyToDelete = false;
                this.indexToDelete = -1;
                updateInventory();
            }

            // 1. Check index is valid
            if(!itemsForSale.isIndexValid(index))
                return true;

            // 2. If not ready to delete, make player confirm
            // 3. Otherwise, delete
            if(!readyToDelete) {
                this.readyToDelete = true;
                this.indexToDelete = index;
            } else {
                itemsForSale.removeItemForSale(index);
                this.readyToDelete = false;
                this.indexToDelete = -1;
            }
            updateInventory();
        }

        return true;
    }

    @Override
    protected boolean onPlayerInventoryClickEvent(int slot, ClickType clickType) {
        return false;
    }
}