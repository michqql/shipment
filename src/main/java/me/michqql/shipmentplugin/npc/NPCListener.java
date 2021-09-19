package me.michqql.shipmentplugin.npc;

import me.michqql.shipmentplugin.ShipmentPlugin;
import me.michqql.shipmentplugin.data.CommentFile;
import me.michqql.shipmentplugin.gui.guis.player.BuyGUI;
import me.michqql.shipmentplugin.shipment.Shipment;
import me.michqql.shipmentplugin.shipment.ShipmentManager;
import me.michqql.shipmentplugin.utils.MessageUtil;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import net.citizensnpcs.api.npc.NPC;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.List;

public class NPCListener implements Listener {

    private final ShipmentPlugin plugin;
    private final CommentFile config;
    private final MessageUtil messageUtil;
    private final Economy economy;

    private final NPCHandler npcHandler;
    private final ShipmentManager shipmentManager;

    // Configuration options
    private boolean skipEmptyShipments;

    public NPCListener(ShipmentPlugin plugin, CommentFile config, MessageUtil messageUtil, Economy economy, NPCHandler npcHandler,
                       ShipmentManager shipmentManager) {

        this.plugin = plugin;
        this.config = config;
        this.messageUtil = messageUtil;
        this.economy = economy;
        this.npcHandler = npcHandler;
        this.shipmentManager = shipmentManager;

        loadConfig();
    }

    private void loadConfig() {
        this.skipEmptyShipments = config.getConfig().getBoolean("npc-skip-empty-shipments", true);
    }
    @EventHandler
    public void onInteractAtNPC(NPCRightClickEvent e) {
        final List<NPC> npcs = npcHandler.getNpcs();
        if(!npcHandler.canSpawn() || npcs.size() == 0 || !npcs.contains(e.getNPC()))
            return;

        e.setCancelled(true);
        Shipment shipment = shipmentManager.getUpcomingShipment();
        if(shipment == null || (skipEmptyShipments && shipment.getItemsForSale().getAmountOfItemsForSale() == 0)) {
            messageUtil.sendList(e.getClicker(), "npc-no-shipment");
            return;
        }

        new BuyGUI(plugin, e.getClicker(), config, messageUtil, economy, shipment).openGUI();
    }
}
