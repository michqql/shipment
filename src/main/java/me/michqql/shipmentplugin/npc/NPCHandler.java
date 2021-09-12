package me.michqql.shipmentplugin.npc;

import me.michqql.shipmentplugin.ShipmentPlugin;
import me.michqql.shipmentplugin.data.CommentFile;
import me.michqql.shipmentplugin.data.YamlFile;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.MemoryNPCDataStore;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;

import java.util.ArrayList;
import java.util.List;

public class NPCHandler {

    private final CommentFile config;

    private List<NPC> npcs;
    private boolean allowSpawning;

    public NPCHandler(CommentFile config) {
        this.config = config;

        this.allowSpawning = true;

        loadNPC();
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

        List<Integer> ids = f.getIntegerList("npc-ids");
        this.npcs = new ArrayList<>();
        for(int citizensID : ids) {
            if(citizensID == -1) {
                this.allowSpawning = false;
                Bukkit.getLogger().info("[Shipment] NPC disabled (invalid id of -1)");
                break;
            }

            npcs.add(registry.getById(citizensID));
        }
    }

    public boolean canSpawn() {
        return allowSpawning;
    }

    public List<NPC> getNpcs() {
        return npcs;
    }
}
