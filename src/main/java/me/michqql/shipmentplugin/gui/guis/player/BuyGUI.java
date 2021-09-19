package me.michqql.shipmentplugin.gui.guis.player;

import me.michqql.shipmentplugin.data.CommentFile;
import me.michqql.shipmentplugin.data.TextFile;
import me.michqql.shipmentplugin.gui.GUI;
import me.michqql.shipmentplugin.gui.item.ItemBuilder;
import me.michqql.shipmentplugin.shipment.ItemsForSale;
import me.michqql.shipmentplugin.shipment.Shipment;
import me.michqql.shipmentplugin.utils.MessageUtil;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class BuyGUI extends GUI {

    private final static SimpleDateFormat DATE_TIME_FORMATTER = new SimpleDateFormat("EEEE MMMM dd yyyy");

    // Slots
    private final static int TICKET_PURCHASE_SLOT = 4, ITEM_START_SLOT = 9;

    private final MessageUtil messageUtil;

    private final Economy economy;
    private final Shipment shipment;
    private final ItemsForSale itemsForSale;

    // Configuration options
    private int maxTicketsPerPlayer;
    private boolean outputPurchases;
    private Material material;
    private String displayName;
    private List<String> lore;

    private List<ItemsForSale.ForSale> itemBasket;

    public BuyGUI(Plugin bukkitPlugin, Player player, CommentFile config, MessageUtil messageUtil, Economy economy, Shipment shipment) {
        super(bukkitPlugin, player);
        this.messageUtil = messageUtil;
        this.economy = economy;
        this.shipment = shipment;
        this.itemsForSale = shipment.getItemsForSale();

        loadConfig(config.getConfig());
        loadTicket(new CommentFile(bukkitPlugin, "", "ticket_item").getConfig());

        this.itemBasket = new ArrayList<>();

        build("&9Market", 6);
    }

    private void loadConfig(FileConfiguration config) {
        this.maxTicketsPerPlayer = config.getInt("max-tickets-per-player-per-shipment");
        this.outputPurchases = config.getBoolean("output-ticket-purchases");
    }

    private void loadTicket(FileConfiguration ticket) {
        String matName = ticket.getString("material");
        try {
            this.material = Material.valueOf(matName);
        } catch(IllegalArgumentException e) {
            this.material = Material.PAPER;
            Bukkit.getLogger().warning("[Shipment] Ticket Item has invalid material " + matName);
        }

        this.displayName = ticket.getString("display-name");
        this.lore = ticket.getStringList("lore");
    }

    @Override
    protected void createInventory() {
        updateInventory();
    }

    @Override
    protected void updateInventory() {
        if (itemBasket.size() == 0) {
            // Player has not selected any items yet
            this.inventory.setItem(TICKET_PURCHASE_SLOT, new ItemBuilder(Material.MAP)
                    .displayName("&3&mPurchase Ticket")
                    .lore("&bPlease select items to purchase first")
                    .getItem());
        } else {
            // Player has selected items
            this.inventory.setItem(TICKET_PURCHASE_SLOT, new ItemBuilder(Material.FILLED_MAP)
                    .displayName("&3Purchase Ticket")
                    .lore("&bThis ticket will cost: &f$" + calculateTotalCost())
                    .getItem());
        }

        for (int i = ITEM_START_SLOT; i < this.inventory.getSize(); i++) {
            this.inventory.setItem(i, null);
        }

        int slot = ITEM_START_SLOT;
        for (ItemsForSale.ForSale item : itemsForSale.getSales()) {
            if (item == null)
                continue;

            boolean inBasket = itemBasket.contains(item);

            ItemStack copy = new ItemStack(item.getItemStack());
            this.inventory.setItem(slot, new ItemBuilder(copy)
                    .addLore(
                            "",
                            "&bPrice: $" + item.getPrice(),
                            (inBasket) ? "&cClick to remove item from basket!" : "&bClick to add item to basket!"
                    ).getItem());
            slot++;
        }
    }

    @Override
    protected void onCloseEvent() {

    }

    @Override
    protected boolean onClickEvent(int slot, ClickType clickType) {
        if(slot == TICKET_PURCHASE_SLOT) {
            purchase();
        } else if(slot >= ITEM_START_SLOT) {
            int index = slot - ITEM_START_SLOT;

            // 1. Check index is valid
            if(!itemsForSale.isIndexValid(index))
                return true;

            // 2. If item not in basket, add
            // 3. Otherwise, remove
            ItemsForSale.ForSale item = itemsForSale.getItemForSale(index); // item should not be null
            if(!itemBasket.remove(item))
                itemBasket.add(item);
            updateInventory();

        }
        return true;
    }

    @Override
    protected boolean onPlayerInventoryClickEvent(int slot, ClickType clickType) {
        return false;
    }

    private double calculateTotalCost() {
        double cost = 0.0D;
        for(ItemsForSale.ForSale forSale : itemBasket) {
            cost += forSale.getPrice();
        }
        return cost;
    }

    private void purchase() {
        if(itemBasket.size() == 0)
            return;

        final double totalCost = calculateTotalCost();

        // 1. Check if player has reached max purchases
        if(maxTicketsPerPlayer > 0 && (shipment.getTicketSales().getUserPurchases(player.getUniqueId()) == maxTicketsPerPlayer)) {
            player.closeInventory();
            messageUtil.sendList(player, "purchase.reached-max-purchases");
            return;
        }

        // 2. Check player has inventory space
        if(player.getInventory().firstEmpty() == -1) {
            player.closeInventory();
            messageUtil.sendList(player, "purchase.no-inventory-space");
            return;
        }

        // 3. Check player has funds
        final double balance = economy.getBalance(player);
        if(balance < totalCost) {
            player.closeInventory();
            messageUtil.sendList(player, "purchase.insufficient-funds", new HashMap<String, String>(){{
                put("cost", String.valueOf(totalCost));
                put("balance", String.valueOf(balance));
            }});
            return;
        }

        // 4. Subtract funds
        EconomyResponse response = economy.withdrawPlayer(player, totalCost);
        if(!response.transactionSuccess()) {
            player.closeInventory();
            messageUtil.sendList(player, "purchase.unexpected-error", new HashMap<String, String>(){{
                put("issue", response.errorMessage);
            }});
            return;
        }

        String date = DATE_TIME_FORMATTER.format(shipment.getAsDate());

        if(outputPurchases) Bukkit.getLogger().info(player.getName() + ": Purchased ticket (shipment " + date + ")");

        // 5. Add ticket to players inventory
        final int ticketID = shipment.getTicketSales().buyTicket(player.getUniqueId(), itemBasket);
        if(outputPurchases) Bukkit.getLogger().info(player.getName() + ": Ticket processed successfully (shipment " + date + ")");
        itemBasket = new ArrayList<>();

        HashMap<String, String> placeholders = new HashMap<String, String>(){{
            put("id", String.valueOf(ticketID));
            put("player", player.getName());
            put("cost", String.valueOf(totalCost));
            put("date", date);
        }};

        // Generate ticket item
        ItemBuilder ticketItem = new ItemBuilder(material)
                .specifyPlaceholders(placeholders)
                .displayName(displayName)
                .lore(lore)
                .persistentData(bukkitPlugin, "ticketID", PersistentDataType.INTEGER, ticketID)
                .persistentData(bukkitPlugin, "timestamp", PersistentDataType.LONG, shipment.getShipmentEpochMS())
                .persistentData(bukkitPlugin, "playerUUID", PersistentDataType.STRING, player.getUniqueId().toString());

        player.getInventory().addItem(ticketItem.getItem());
        if(outputPurchases) Bukkit.getLogger().info(player.getName() + ": Ticket added to inventory (shipment " + date + ")");
        messageUtil.sendList(player, "purchase.successful", placeholders);

        player.closeInventory();
    }
}
