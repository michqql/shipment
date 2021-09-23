package me.michqql.shipmentplugin.npc;

import me.michqql.shipmentplugin.ShipmentPlugin;
import me.michqql.shipmentplugin.data.CommentFile;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.MemoryNPCDataStore;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;

public class NPCHandler {

    private final CommentFile config;

    private final List<NPC> npcs;
    private boolean allowSpawning;

    public NPCHandler(ShipmentPlugin plugin, CommentFile config) {
        this.config = config;
        this.allowSpawning = true;

        this.npcs = new ArrayList<>();
        // Delay loading NPCs due to the delay in Citizens (delayed by 10s)
        Bukkit.getScheduler().runTaskLater(plugin, this::loadNPC, 200);
    }

    private void loadNPC() {
        // Attempt to get a non-null registry
        // NPC
        NPCRegistry registry = CitizensAPI.getNPCRegistry();
        if(registry == null) {
            Bukkit.getLogger().info("[Shipment] NPC default registry is null - generating new one");
            registry = CitizensAPI.createAnonymousNPCRegistry(new MemoryNPCDataStore());
        }

        // Check a final time to see if registry is still null
        if(registry == null) {
            Bukkit.getLogger().warning("[Shipment] NPC Registry is null - NPC disabled");
            this.allowSpawning = false;
            return;
        }

        FileConfiguration f = config.getConfig();

        List<Integer> ids = f.getIntegerList("npc.ids");
        for(int citizensID : ids) {
            if(citizensID == -1) {
                this.allowSpawning = false;
                Bukkit.getLogger().warning("[Shipment] NPC disabled (invalid id of -1)");
                break;
            }

            NPC npc = registry.getById(citizensID);
            if(npc == null) {
                Bukkit.getLogger().warning("[Shipment] Invalid NPC registration of id " + citizensID);
            } else {
                npcs.add(registry.getById(citizensID));
            }
        }
        Bukkit.getLogger().info("[Shipment] " + npcs.size() + " NPCs have been registered");
    }

    public boolean canSpawn() {
        return allowSpawning;
    }

    public List<NPC> getNpcs() {
        return npcs;
    }
}
