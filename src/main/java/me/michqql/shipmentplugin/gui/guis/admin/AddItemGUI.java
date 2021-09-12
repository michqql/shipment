package me.michqql.shipmentplugin.gui.guis.admin;

import me.michqql.shipmentplugin.gui.GUI;
import me.michqql.shipmentplugin.gui.GUIManager;
import me.michqql.shipmentplugin.gui.item.ItemBuilder;
import me.michqql.shipmentplugin.shipment.Shipment;
import me.michqql.shipmentplugin.utils.Colour;
import org.bukkit.Material;
import org.bukkit.conversations.*;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AddItemGUI extends GUI {

    // Slots
    private final static int BACK_SLOT = 0, ITEM_SLOT = 3, AMOUNT_SLOT = 4, PRICE_SLOT = 5, SAVE_SLOT = 8;

    private final static int[] PANE_SLOTS = new int[]{
            1, 2, 6, 7
    };

    // Prompt
    private final static NumericPrompt PRICE_PROMPT = new NumericPrompt() {
        @Override
        protected @Nullable Prompt acceptValidatedInput(@NotNull ConversationContext context, @NotNull Number input) {
            context.setSessionData("price", input.doubleValue());
            return null;
        }

        @Override
        public @NotNull String getPromptText(@NotNull ConversationContext context) {
            return Colour.format("&3Please enter a price!");
        }
    };

    private final Shipment shipment;

    private ItemStack originalItem;
    //private ItemStack item;
    private int amount;
    private double price;

    public AddItemGUI(Plugin bukkitPlugin, Player player, Shipment shipment) {
        super(bukkitPlugin, player);
        this.shipment = shipment;

        this.originalItem = null;
        this.amount = 1;
        this.price = 0;

        build("&9Add item", 1);
    }

    @Override
    protected void createInventory() {
        // Panes
        ItemStack pane = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).displayName("").getItem();
        for(int i : PANE_SLOTS) {
            this.inventory.setItem(i, pane);
        }

        this.inventory.setItem(BACK_SLOT, new ItemBuilder(Material.RED_BED)
                .displayName("&3< Go back").lore("&cDoes not save item!").getItem());

        updateInventory();
    }

    @Override
    protected void updateInventory() {
        if(originalItem == null) {
            this.inventory.setItem(ITEM_SLOT, new ItemBuilder(Material.ITEM_FRAME)
                    .displayName("&3No item")
                    .lore("&bYou have not yet added an item!")
                    .getItem());
        } else {
            ItemStack copy = new ItemStack(originalItem);
            this.inventory.setItem(ITEM_SLOT, new ItemBuilder(copy)
                    .addLore(
                            "",
                            "&bClick to remove!"
                    ).amount(amount).getItem());
        }

        this.inventory.setItem(AMOUNT_SLOT, new ItemBuilder(Material.OAK_SIGN)
                .displayName("&3Amount: &b" + amount)
                .lore(
                        "&bLeft-Click&f increase by 1",
                        "&bShift-Left-Click&f increase by 10",
                        "",
                        "&bRight-Click&f decrease by 1",
                        "&bShift-Right-Click&f decrease by 10"
                )
                .getItem());

        this.inventory.setItem(PRICE_SLOT, new ItemBuilder(Material.GOLD_INGOT)
                .displayName("&3Price: &b$" + price)
                .lore("&bClick to enter a price").getItem());

        this.inventory.setItem(SAVE_SLOT, new ItemBuilder(Material.EMERALD_BLOCK)
                .displayName("&3Save")
                .lore(
                        "&bSaves item and goes back",
                        "",
                        "&bItem: " + (originalItem == null ? "&cnone" : "&f" + originalItem.getI18NDisplayName()),
                        "&bAmount: &f" + amount,
                        "&bPrice: &f$" + price
                ).getItem());
    }

    @Override
    protected void onCloseEvent() {

    }

    @Override
    protected boolean onClickEvent(int slot, ClickType clickType) {
        switch (slot) {
            case BACK_SLOT:
                GUIManager.openPreviousGUI(player.getUniqueId());
                return true;

            case ITEM_SLOT:
                boolean nullItem = originalItem == null;
                boolean emptyItem = player.getItemOnCursor().getType().isAir();

                if(nullItem && emptyItem)
                    break;

                if(emptyItem) {
                    // Remove item
                    player.setItemOnCursor(originalItem);
                    this.originalItem = null;
                } else if(nullItem) {
                    // Set item
                    ItemStack cursor = player.getItemOnCursor();
                    player.setItemOnCursor(new ItemStack(Material.AIR));

                    this.originalItem = cursor;
                    this.amount = originalItem.getAmount();
                } else {
                    // Swap item
                    ItemStack cursor = player.getItemOnCursor();
                    player.setItemOnCursor(originalItem);

                    this.originalItem = cursor;
                    this.amount = originalItem.getAmount();
                }
                break;

            case AMOUNT_SLOT:
                if(originalItem == null)
                    break;

                if(clickType == ClickType.LEFT)
                    this.amount += 1;
                else if(clickType == ClickType.SHIFT_LEFT)
                    this.amount += 10;
                else if(clickType == ClickType.RIGHT)
                    this.amount -= 1;
                else if(clickType == ClickType.SHIFT_RIGHT)
                    this.amount -= 10;

                this.amount = Math.max(1, Math.min(64, amount)); // Bound amount to 1-64
                break;

            case PRICE_SLOT:
                if(originalItem == null)
                    break;

                // 1. Save this GUI structure
                GUIManager.savePlayerGUIs(player.getUniqueId());
                player.closeInventory();

                // 2. Prompt player to enter an amount in chat
                // 3. Upon entering an amount, open this GUI again
                ConversationFactory cf = new ConversationFactory(bukkitPlugin);
                Conversation c = cf.withFirstPrompt(PRICE_PROMPT)
                        .withLocalEcho(false)
                        .addConversationAbandonedListener(abandonedEvent -> {
                            ConversationContext context = abandonedEvent.getContext();
                            Double setPrice = (Double) context.getSessionData("price");
                            if(setPrice != null)
                                price = setPrice;

                            if(context.getForWhom() instanceof Player) {
                                Player cp = (Player) context.getForWhom();

                                cp.sendMessage(Colour.format("&3Price has been set as: $" + price));
                                GUIManager.loadSavedGUIs(cp.getUniqueId());
                                GUIManager.reopenCurrentGUI(cp.getUniqueId());
                            }
                        }).buildConversation(player);
                c.begin();
                return true;

            case SAVE_SLOT:
                if(originalItem == null)
                    return true;

                ItemStack toSave = new ItemStack(originalItem);
                toSave.setAmount(amount);
                shipment.getItemsForSale().addItemForSale(toSave, price);
                GUIManager.openPreviousGUI(player.getUniqueId());
                return true;
        }

        updateInventory();
        return true;
    }

    @Override
    protected boolean onPlayerInventoryClickEvent(int slot, ClickType clickType) {
        return false;
    }
}
