package me.michqql.shipmentplugin.gui.guis.admin.presets;

import me.michqql.shipmentplugin.gui.GUI;
import me.michqql.shipmentplugin.gui.GUIManager;
import me.michqql.shipmentplugin.gui.item.ItemBuilder;
import me.michqql.shipmentplugin.preset.PresetHandler;
import me.michqql.shipmentplugin.shipment.ItemsForSale;
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

public class PresetsGUI extends GUI {

    // Slots
    private final static int BACK_SLOT = 0, CREATE_PRESET_SLOT = 1, ENABLED_SLOT = 2, RANDOM_SLOT = 3, PRESET_START_SLOT = 9;

    private final static int[] PANE_SLOTS = new int[]{
            4, 5, 6, 7, 8
    };

    // Prompt
    private final StringPrompt presetNamePrompt;

    private final MessageUtil messageUtil;
    private final PresetHandler presetHandler;

    private boolean readyToDelete;
    private int indexToDelete;

    public PresetsGUI(Plugin bukkitPlugin, Player player, MessageUtil messageUtil, PresetHandler presetHandler) {
        super(bukkitPlugin, player);
        this.messageUtil = messageUtil;
        this.presetHandler = presetHandler;

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

        build("&9Presets", 6);
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

        this.inventory.setItem(CREATE_PRESET_SLOT, new ItemBuilder(Material.WRITABLE_BOOK)
                .displayName("&3Create item preset").getItem());

        updateInventory();
    }

    @Override
    protected void updateInventory() {
        // Enabled/Disabled
        if(presetHandler.isSetupFailed()) {
            this.inventory.setItem(ENABLED_SLOT, new ItemBuilder(Material.RED_CONCRETE)
                    .displayName("&4Failed to load configuration")
                    .lore("&cPlease check console/config.yml", "&c(default preset could not be found)")
                    .getItem());
        } else {
            this.inventory.setItem(ENABLED_SLOT, new ItemBuilder(presetHandler.isEnabled() ? Material.GREEN_CONCRETE : Material.RED_CONCRETE)
                    .displayName("&3Presets: " + (presetHandler.isEnabled() ? "&aENABLED" : "&cDISABLED"))
                    .getItem());
        }

        // Randomised
        boolean isRandom = presetHandler.isRandom();
        this.inventory.setItem(RANDOM_SLOT, new ItemBuilder(Material.ENCHANTING_TABLE)
                .displayName("&3Random Preset: " + (isRandom ? "&aENABLED" : "&cDISABLED"))
                .getItem());

        // Preset slots
        for(int i = PRESET_START_SLOT; i < this.inventory.getSize(); i++) {
            this.inventory.setItem(i, null);
        }

        int slot = PRESET_START_SLOT;
        for(PresetHandler.Preset preset : presetHandler.getPresets()) {
            if(preset == null)
                continue;

            boolean deleteFlag = readyToDelete && (indexToDelete + PRESET_START_SLOT == slot);
            boolean defaultFlag = !isRandom && presetHandler.isPresetDefault(preset);

            this.inventory.setItem(slot, new ItemBuilder(Material.BOOK)
                    .displayName("&f" + preset.getDisplayName() + (defaultFlag ? " &b(default)" : ""))
                    .lore(
                            "&bContains: &f" + preset.getItems().getAmountOfItemsForSale() + " item" +
                                    (preset.getItems().getAmountOfItemsForSale() != 1 ? "s" : ""),
                            "",
                            "&bLeft-Click &fto view items",
                            deleteFlag ? "&cShift-Right-Click again to confirm deletion!" : "&bShift-Right-Click &fto delete!"
                    ).glow(defaultFlag)
                    .getItem());
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
        else if (slot == CREATE_PRESET_SLOT) {
            createPreset();
            return true;
        }
        else if(slot >= PRESET_START_SLOT) {
            int index = slot - PRESET_START_SLOT;
            if(readyToDelete && index != indexToDelete) {
                this.readyToDelete = false;
                this.indexToDelete = -1;
                updateInventory();
            }

            if(!presetHandler.isIndexValid(index))
                return true;

            PresetHandler.Preset preset = presetHandler.getPresets()[index];

            // Left = View Items
            if(clickType == ClickType.LEFT) {
                new PresetGUI(bukkitPlugin, player, messageUtil, preset).openGUI();
                return true;
            }

            // Shift-Right = Delete
            else if(clickType == ClickType.SHIFT_RIGHT) {
                if(presetHandler.isPresetDefault(preset)) {
                    messageUtil.sendList(player, "setup.preset-is-default-cannot-delete");
                    return true;
                }

                if(!readyToDelete) {
                    this.readyToDelete = true;
                    this.indexToDelete = index;
                } else {
                    presetHandler.deletePreset(index);
                    this.readyToDelete = false;
                    this.indexToDelete = -1;
                    messageUtil.sendList(player, "setup.preset-deleted", new HashMap<String, String>(){{
                        put("name", preset.getDisplayName());
                    }});
                }
            }
        }
        updateInventory();
        return true;
    }

    @Override
    protected boolean onPlayerInventoryClickEvent(int slot, ClickType clickType) {
        return false;
    }

    private void createPreset() {
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

                    presetHandler.createPreset(setName, new ItemsForSale());

                    messageUtil.sendList(whom, "setup.preset-created", new HashMap<String, String>(){{
                        put("name", setName);
                    }});

                    GUIManager.loadSavedGUIs(whom.getUniqueId());
                    GUIManager.reopenCurrentGUI(whom.getUniqueId());
                }).buildConversation(player);
        c.begin();
    }
}
