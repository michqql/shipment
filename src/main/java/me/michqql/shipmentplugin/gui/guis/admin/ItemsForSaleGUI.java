package me.michqql.shipmentplugin.gui.guis.admin;

import me.michqql.shipmentplugin.gui.GUI;
import me.michqql.shipmentplugin.gui.GUIManager;
import me.michqql.shipmentplugin.gui.item.ItemBuilder;
import me.michqql.shipmentplugin.preset.PresetHandler;
import me.michqql.shipmentplugin.shipment.ItemsForSale;
import me.michqql.shipmentplugin.shipment.Shipment;
import me.michqql.shipmentplugin.shipment.ShipmentManager;
import me.michqql.shipmentplugin.utils.Colour;
import me.michqql.shipmentplugin.utils.MessageUtil;
import org.bukkit.Material;
import org.bukkit.conversations.*;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;

public class ItemsForSaleGUI extends GUI {

    // Slots
    private final static int BACK_SLOT = 0, ADD_ITEM_SLOT = 1, CREATE_PRESET_SLOT = 2, COPY_ITEMS_SLOT = 3, START_SLOT = 9;

    private final static int[] PANE_SLOTS = new int[]{
            2, 3, 4, 5, 6, 7, 8
    };

    // Prompt
    private final StringPrompt presetNamePrompt;

    private final MessageUtil messageUtil;
    private final ShipmentManager shipmentManager;
    private final PresetHandler presetHandler;
    private final Shipment shipment;
    private final ItemsForSale itemsForSale;
    private final boolean canEdit;

    private boolean readyToDelete;
    private int indexToDelete;

    public ItemsForSaleGUI(Plugin bukkitPlugin, Player player, MessageUtil messageUtil, ShipmentManager shipmentManager,
                           PresetHandler presetHandler, Shipment shipment) {

        super(bukkitPlugin, player);
        this.messageUtil = messageUtil;
        this.shipmentManager = shipmentManager;
        this.presetHandler = presetHandler;
        this.shipment = shipment;
        this.itemsForSale = shipment.getItemsForSale();

        this.presetNamePrompt = new StringPrompt() {
            @Override
            public @Nullable Prompt acceptInput(@NotNull ConversationContext context, @Nullable String input) {
                context.setSessionData("name", input);
                return null;
            }

            @Override
            public @NotNull String getPromptText(@NotNull ConversationContext context) {
                return MessageUtil.format(messageUtil.getMessage("setup.enter-preset-name"));
            }
        };

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

        this.inventory.setItem(CREATE_PRESET_SLOT, new ItemBuilder(Material.BOOK)
                .displayName("&3Create preset")
                .lore(
                        "&bSaves this shipments items into a preset that",
                        "&bcan be applied multiple times to new presets"
                ).getItem());

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
        }
        else if(slot == CREATE_PRESET_SLOT) {
            // 1. Save this GUI structure
            GUIManager.savePlayerGUIs(player.getUniqueId());
            player.closeInventory();

            // 2. Prompt player to enter an amount in chat
            // 3. Upon entering an amount, open this GUI again
            ConversationFactory cf = new ConversationFactory(bukkitPlugin);
            Conversation c = cf.withFirstPrompt(presetNamePrompt)
                    .withLocalEcho(false)
                    .addConversationAbandonedListener(abandonedEvent -> {
                        ConversationContext context = abandonedEvent.getContext();
                        String setName = (String) context.getSessionData("name");
                        if(setName == null) {
                            context.getForWhom().sendRawMessage("Invalid name");
                            return;
                        }

                        if(!(context.getForWhom() instanceof Player)) {
                            return;
                        }

                        Player whom = (Player) context.getForWhom();
                        if(presetHandler.presetExists(setName)) {
                            messageUtil.sendList(whom, "setup.preset-name-taken", new HashMap<String, String>(){{
                                put("name", setName);
                            }});
                            return;
                        }

                        presetHandler.createPreset(setName, shipment.getItemsForSale());

                        messageUtil.sendList(whom, "setup.preset-created", new HashMap<String, String>(){{
                            put("name", setName);
                        }});
                        GUIManager.loadSavedGUIs(whom.getUniqueId());
                        GUIManager.reopenCurrentGUI(whom.getUniqueId());
                    }).buildConversation(player);
            c.begin();
            return true;
        }
        else if(slot == COPY_ITEMS_SLOT && shipment.compareShipmentChronology() == -1) {
            new CopyItemsGUI(bukkitPlugin, player, messageUtil, shipmentManager, shipment).openGUI();
        }
        else if(!canEdit) {
            return true;
        }
        else if(slot == ADD_ITEM_SLOT) {
            new AddItemGUI(bukkitPlugin, player, messageUtil, shipment).openGUI();
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
