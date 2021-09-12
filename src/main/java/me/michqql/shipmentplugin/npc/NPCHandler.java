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

public class NPCHandler {

    private final YamlFile npcFile;

    // Configuration options
    private String npcName;
    private EntityType entityType;
    private Location location;

    // NPC
    private NPCRegistry registry;
    private NPC npc;
    private boolean allowSpawning;

    public NPCHandler(ShipmentPlugin plugin, CommentFile config) {
        this.npcFile = new YamlFile(plugin, "npc", "data");

        this.allowSpawning = true;

        load(config.getConfig());
        loadNPC();
        spawnNPC();
    }

    private void load(FileConfiguration f) {
        // NPC
        this.npcName = f.getString("npc-display-name");
        String type = f.getString("npc-entity-type");
        try {
            this.entityType = EntityType.valueOf(type);
        } catch(IllegalArgumentException e) {
            this.allowSpawning = false;
            Bukkit.getLogger().severe("[Shipment] Invalid NPC entity type - NPC will not spawn");
            return;
        }

        // World
        String worldName = f.getString("npc-world", "");
        World world = Bukkit.getWorld(worldName);

        if(world == null) {
            this.allowSpawning = false;
            Bukkit.getLogger().severe("[Shipment] Invalid NPC world name - NPC will not spawn");
            return;
        }

        // Location
        double x = f.getDouble("npc-location.x");
        double y = f.getDouble("npc-location.y");
        double z = f.getDouble("npc-location.z");
        float yaw = (float) f.getDouble("npc-location.yaw");
        float pitch = (float) f.getDouble("npc-location.pitch");

        this.location = new Location(world, x, y, z, yaw, pitch);

        if(y == -1) {
            this.allowSpawning = false;
        }
    }

    private void loadNPC() {
        // Attempt to get a non-null registry
        this.registry = CitizensAPI.getNPCRegistry();
        if(registry == null) {
            Bukkit.getLogger().info("[Shipment] NPC default registry is null - generating new one");
            this.registry = CitizensAPI.createAnonymousNPCRegistry(new MemoryNPCDataStore());
        }

        // Check a final time to see if registry is still null
        if(registry == null) {
            Bukkit.getLogger().warning("[Shipment] NPC Registry is null - NPC will not spawn");
            this.allowSpawning = false;
        }
    }

    public void spawnNPC() {
        Bukkit.getLogger().info("[Shipment] Attempting to spawn NPC...");
        if(!allowSpawning)
            return;

        FileConfiguration f = npcFile.getConfig();

        // 1. Load the citizens ID of the saved NPC
        // 2. Try to get the NPC belonging to that ID
        int citizensID = f.getInt("citizens-id", -1);
        if(citizensID != -1) {
            Bukkit.getLogger().info("[Shipment] Loaded previous NPC");
            this.npc = registry.getById(citizensID);
        }

        // 3. Check whether the NPC was loaded, if not create a new one
        if(npc == null) {
            Bukkit.getLogger().info("[Shipment] Created new NPC");
            this.npc = registry.createNPC(entityType, ChatColor.translateAlternateColorCodes('&', npcName));
        }

        // 4. Set custom data of NPC
        if(!npc.isSpawned()) {
            npc.spawn(location);
            npc.setProtected(true);
        }

        // 5. Save npc file with data
        f.set("citizens-id", npc.getId());
        npcFile.save();

        Bukkit.getLogger().info("[Shipment] NPC spawned");
    }

    public void despawnNPC() {
        Bukkit.getLogger().info("[Shipment] Attempting to despawn NPC...");
        if(npc == null)
            return;

        npc.despawn();
        Bukkit.getLogger().info("[Shipment] NPC despawned");
    }

    public NPC getNPC() {
        return npc;
    }
}
