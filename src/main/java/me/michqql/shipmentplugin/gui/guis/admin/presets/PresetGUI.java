package me.michqql.shipmentplugin.gui.guis.admin.presets;

import me.michqql.shipmentplugin.gui.GUI;
import me.michqql.shipmentplugin.gui.GUIManager;
import me.michqql.shipmentplugin.gui.item.ItemBuilder;
import me.michqql.shipmentplugin.preset.PresetHandler;
import me.michqql.shipmentplugin.shipment.ItemsForSale;
import me.michqql.shipmentplugin.utils.MessageUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public class PresetGUI extends GUI {

    // Slots
    private final static int BACK_SLOT = 0, ADD_ITEM_SLOT = 1, START_SLOT = 9;

    private final static int[] PANE_SLOTS = new int[]{
            2, 3, 4, 5, 6, 7, 8
    };

    private final MessageUtil messageUtil;
    private final PresetHandler.Preset preset;
    private final ItemsForSale itemsForSale;

    private boolean readyToDelete;
    private int indexToDelete;

    public PresetGUI(Plugin bukkitPlugin, Player player, MessageUtil messageUtil,
                     PresetHandler.Preset preset) {

        super(bukkitPlugin, player);
        this.messageUtil = messageUtil;
        this.preset = preset;
        this.itemsForSale = preset.getItems();

        build("&9Preset Items", 6);
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

        this.inventory.setItem(ADD_ITEM_SLOT, new ItemBuilder(Material.ITEM_FRAME)
                .displayName("&3Add item")
                .lore(
                        "&bClick to add new item to this preset",
                        "&b- Add item, set amount, set price"
                ).getItem());

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
        }
        else if(slot == ADD_ITEM_SLOT) {
            new AddItemToPresetGUI(bukkitPlugin, player, messageUtil, preset).openGUI();
            return true;
        }
        else if(slot >= START_SLOT) {
            int index = slot - START_SLOT;
            if(readyToDelete && index != indexToDelete) {
                this.readyToDelete = false;
                this.indexToDelete = -1;
                updateInventory();
            }

            // 1. Check index is valid
            if(itemsForSale.isIndexInvalid(index))
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
