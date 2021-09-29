package me.michqql.shipmentplugin.gui;

import me.michqql.shipmentplugin.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;

public abstract class GUI {

    protected final static BukkitScheduler SCHEDULER = Bukkit.getScheduler();

    protected final Plugin bukkitPlugin;
    protected final Player player;

    protected Inventory inventory;
    private boolean inventoryOpen, finishedBuilding;

    public GUI(Plugin bukkitPlugin, Player player) {
        this.bukkitPlugin = bukkitPlugin;
        this.player = player;
        GUIManager.registerGUI(player.getUniqueId(), this);
    }

    /**
     * Opens the inventory this GUI class represents
     * for the owning player
     *
     * If the inventory has not been built fully yet,
     * a BukkitRunnable will be created to ensure the inventory
     * can be opened as soon as it is built.
     */
    public final void openGUI() {
        if(!inventoryOpen) {
            this.internalSetOpen(true);
            if(!finishedBuilding) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if(finishedBuilding) {
                            internalSetOpen(true);
                            player.openInventory(inventory);
                            this.cancel();
                        }
                    }
                }.runTaskTimer(bukkitPlugin, 1L, 1L);
            } else {
                player.openInventory(inventory);
            }
        }
    }

    /**
     * Creates the Inventory object via Bukkit's API
     * @param title - the title to call the inventory
     * @param rows  - the amount of rows to give this inventory
     */
    protected final void build(String title, int rows) {
        this.inventory = Bukkit.createInventory(null, rows * 9, MessageUtil.format(title));
        SCHEDULER.runTaskAsynchronously(bukkitPlugin, () -> {
            this.createInventory();
            this.finishedBuilding = true;
        });
    }

    /**
     * Creates the Inventory object via Bukkit's API
     * @param title - the title to call the inventory
     * @param type  - the inventory type to use
     */
    protected final void build(String title, InventoryType type) {
        this.inventory = Bukkit.createInventory(null, type, MessageUtil.format(title));
        SCHEDULER.runTaskAsynchronously(bukkitPlugin, () -> {
            this.createInventory();
            this.finishedBuilding = true;
        });
    }

    /*
     * Abstract methods that any extending/sub class will need to implement.
     * Provides functionality to this class.
     */
    /**
     * Method called only once
     */
    protected abstract void createInventory();

    /**
     * Method called multiple times
     * Usually to tell the sub class to update the inventory
     */
    protected abstract void updateInventory();

    /**
     * Method called when the inventory this GUI represents has been closed
     */
    protected abstract void onCloseEvent();

    /**
     * Method called when the player has clicked on the inventory (the top half)
     * @param slot      - the slot that the player has clicked on
     * @param clickType - the way the player clicked (left, right, shift+left, drop, etc)
     * @return {@code true} to cancel the event (stop the player taking the item),
     *         {@code false} to allow the player to take the item
     */
    protected abstract boolean onClickEvent(final int slot, final ClickType clickType);

    /**
     * Method called when the player has clicked in their inventory (the bottom half)
     * @param slot      - the slot that the player has clicked on
     * @param clickType - the way the player clicked (left, right, shift+left, drop, etc)
     * @return {@code true} to cancel the event (stop the player taking the item),
     *         {@code false} to allow the player to take the item
     */
    protected abstract boolean onPlayerInventoryClickEvent(final int slot, final ClickType clickType);

    /*
     * Internal methods
     */
    final void internalClose() {
        if(inventoryOpen) {
            internalSetOpen(false);
            onCloseEvent();
        }
    }

    final boolean internalClick(final boolean playerInventory, final int slot, final ClickType clickType) {
        return playerInventory ? onPlayerInventoryClickEvent(slot, clickType) : onClickEvent(slot, clickType);
    }

    final void internalSetOpen(boolean open) {
        this.inventoryOpen = open;
    }

    final boolean internalIsOpen() {
        return inventoryOpen;
    }
}
