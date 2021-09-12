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
import org.bukkit.ChatColor;
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

    public NPCListener(ShipmentPlugin plugin, CommentFile config, MessageUtil messageUtil, Economy economy, NPCHandler npcHandler,
                       ShipmentManager shipmentManager) {

        this.plugin = plugin;
        this.config = config;
        this.messageUtil = messageUtil;
        this.economy = economy;
        this.npcHandler = npcHandler;
        this.shipmentManager = shipmentManager;
    }

    @EventHandler
    public void onInteractAtNPC(NPCRightClickEvent e) {
        final List<NPC> npcs = npcHandler.getNpcs();
        if(!npcHandler.canSpawn() || npcs.size() == 0 || !npcs.contains(e.getNPC()))
            return;

        e.setCancelled(true);
        Shipment shipment = shipmentManager.getUpcomingShipment();
        if(shipment == null) {
            // TODO: custom message
            e.getClicker().sendMessage(ChatColor.RED + "There is no shipment this week!");
            return;
        }

        new BuyGUI(plugin, e.getClicker(), config, messageUtil, economy, shipment).openGUI();
    }
}
