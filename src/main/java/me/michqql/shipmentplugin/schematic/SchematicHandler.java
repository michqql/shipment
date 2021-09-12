package me.michqql.shipmentplugin.schematic;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.session.ClipboardHolder;
import me.michqql.shipmentplugin.ShipmentPlugin;
import me.michqql.shipmentplugin.data.CommentFile;
import me.michqql.shipmentplugin.data.type.ExceptionFile;
import me.michqql.shipmentplugin.data.type.SchematicFile;
import me.michqql.shipmentplugin.utils.IOUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SchematicHandler {

    private final ShipmentPlugin plugin;

    // Configuration options
    private String schematicName;
    private String schematicExtension;
    private World world;
    private Location location;

    // Schematic
    private boolean spawned;
    private boolean allowSpawning;
    private SchematicFile schematicFile;

    // Schematic Boundary Viewing
    private final Set<Player> viewers = new HashSet<>();

    public SchematicHandler(ShipmentPlugin plugin, CommentFile config) {
        this.plugin = plugin;
        this.allowSpawning = true;

        // Attempts to create the folder if it does not exist
        IOUtil.createFolder(plugin.getDataFolder(), "schematics");

        // Configuration options
        load(config.getConfig());
        loadSchematic();

        runView();
    }

    private void load(FileConfiguration f) {
        // Schematic
        this.schematicName = f.getString("ship-schematic-name");
        this.schematicExtension = f.getString("ship-schematic-extension");

        // World
        String worldName = f.getString("ship-world", "");
        this.world = Bukkit.getWorld(worldName);

        if(world == null) {
            this.allowSpawning = false;
            Bukkit.getLogger().severe("[Shipment] Invalid ship world name - ship will not spawn");
            return;
        }

        // Location
        int x = f.getInt("ship-location.x");
        int y = f.getInt("ship-location.y");
        int z = f.getInt("ship-location.z");

        this.location = new Location(world, x, y, z);

        if(y == -1) {
            this.allowSpawning = false;
        }
    }

    private void loadSchematic() {
        this.schematicFile = new SchematicFile(plugin, schematicName, schematicExtension);

        if(!schematicFile.exists()) {
            this.allowSpawning = false;
            Bukkit.getLogger().severe("[Shipment] Missing ship schematic file named '" + schematicName + "." + schematicExtension + "'");
        }
    }

    // Pasting and undoing paste
    public void pasteSchematic() {
        Bukkit.getLogger().info("[Shipment] Attempting to paste schematic...");
        if(!schematicFile.exists() || !allowSpawning)
            return;

        final Clipboard schematic = schematicFile.getSchematic();

        // 1. Copy region first
        CuboidRegion region = new CuboidRegion(
                BukkitAdapter.asBlockVector(location),
                BukkitAdapter.asBlockVector(location.clone().add(
                        schematic.getDimensions().getX(),
                        schematic.getDimensions().getY(),
                        schematic.getDimensions().getZ()
                ))
        );
        BlockArrayClipboard clipboard = new BlockArrayClipboard(region);

        try (EditSession editSession = WorldEdit.getInstance().getEditSessionFactory().getEditSession(BukkitAdapter.adapt(world), -1)) {
            ForwardExtentCopy forwardExtentCopy = new ForwardExtentCopy(
                    editSession, region, clipboard, region.getMinimumPoint()
            );
            Operations.complete(forwardExtentCopy);

            new SchematicFile(plugin, "copy", "schem", clipboard); // automatically saves
        } catch (WorldEditException e) {
            Bukkit.getLogger().severe("[Shipment] Could not copy shipment region");
            e.printStackTrace();
            this.allowSpawning = false;
            return;
        }

        // 2. Paste schematic
        schematic.setOrigin(schematic.getMinimumPoint()); // Ensures schematic does not paste based on copy position
        try (EditSession editSession = WorldEdit.getInstance().getEditSessionFactory().getEditSession(BukkitAdapter.adapt(world), -1)) {
            Operation operation = new ClipboardHolder(schematic)
                    .createPaste(editSession)
                    .to(BlockVector3.at(location.getX(), location.getY(), location.getZ()))
                    .build();
            Operations.complete(operation);
        } catch (WorldEditException e) {
            Bukkit.getLogger().severe("[Shipment] Could not paste schematic named '" + schematicName + "." + schematicExtension + "'");
            Bukkit.getLogger().severe("[Shipment] Exception stack trace saved to 'exceptions/world-edit-exception.txt'");
            new ExceptionFile(plugin, "exceptions", "world-edit-exception").write(e);
            this.allowSpawning = false;
            return;
        }

        this.spawned = true;
        Bukkit.getLogger().info("[Shipment] Schematic pasted");
    }

    public void undoPaste() {
        Bukkit.getLogger().info("[Shipment] Attempting to undo schematic paste...");
        if(!allowSpawning)
            return;

        final SchematicFile file = new SchematicFile(plugin, "copy", "schem");
        if(!file.exists()) {
            Bukkit.getLogger().warning("[Shipment] Missing copy of region schematic file");
            Bukkit.getLogger().warning("[Shipment] This is not always a bug. Only report if schematic hasn't been removed");
            return;
        }

        final Clipboard schematic = file.getSchematic();

        // 1. Paste schematic
        schematic.setOrigin(schematic.getMinimumPoint()); // Ensures schematic does not paste based on copy position
        try (EditSession editSession = WorldEdit.getInstance().getEditSessionFactory().getEditSession(BukkitAdapter.adapt(world), -1)) {
            Operation operation = new ClipboardHolder(schematic)
                    .createPaste(editSession)
                    .to(BlockVector3.at(location.getX(), location.getY(), location.getZ()))
                    .build();
            Operations.complete(operation);
        } catch (WorldEditException e) {
            Bukkit.getLogger().severe("[Shipment] Could not paste copy of region schematic");
            e.printStackTrace();
        }

        // 2. Delete copy of region after use
        file.delete();
        this.spawned = false;
        Bukkit.getLogger().info("[Shipment] Schematic undone");
    }

    public boolean isSpawned() {
        return spawned;
    }

    public Location[] getRegionMinMax() {
        Clipboard clipboard = schematicFile.getClipboard();
        BlockVector3 dimensions = clipboard.getDimensions();
        int xMin = location.getBlockX();
        int yMin = location.getBlockY();
        int zMin = location.getBlockZ();

        int xMax = xMin + dimensions.getX();
        int yMax = yMin + dimensions.getY();
        int zMax = zMin + dimensions.getZ();
        return new Location[] {
                new Location(world, xMin, yMin, zMin),
                new Location(world, xMax, yMax, zMax)
        };
    }

    // Viewing boundaries
    public void viewSchematicBoundaries(Player player) {
        viewers.add(player);
    }

    public void stopViewingSchematicBoundaries(Player player) {
        viewers.remove(player);
    }

    public boolean toggleViewingSchematicBoundaries(Player player) {
        if(!viewers.remove(player)) {
            viewers.add(player);
            return true;
        }
        return false;
    }

    private void runView() {
        if(!schematicFile.exists() || !allowSpawning)
            return;

        // 1. Compute boundaries outside of runnable as this only needs to be done once
        // 2. Check if there are viewers, if not return
        // 3. For each viewer, show boundaries
        new BukkitRunnable() {
            @Override
            public void run() {
                if(viewers.size() == 0)
                    return;

                List<Location> particleLocations = getParticleBoundaryLocations();

                for(Player player : viewers) {
                    if(!player.isOnline())
                        continue;

                    for(Location loc : particleLocations) {
                        player.spawnParticle(Particle.END_ROD, loc, 0, 0, 0, 0);
                    }
                }
            }
        }.runTaskTimerAsynchronously(plugin, 20L, 20L);
    }

    private List<Location> getParticleBoundaryLocations() {
        List<Location> locations = new ArrayList<>();

        Clipboard clipboard = schematicFile.getClipboard();
        BlockVector3 dimensions = clipboard.getDimensions();
        int xMin = location.getBlockX();
        int yMin = location.getBlockY();
        int zMin = location.getBlockZ();

        int xMax = xMin + dimensions.getX();
        int yMax = yMin + dimensions.getY();
        int zMax = zMin + dimensions.getZ();

        for(int x = xMin; x < xMax; x++) {
            locations.add(new Location(world, x, yMin, zMin));
            locations.add(new Location(world, x, yMin, zMax));
            locations.add(new Location(world, x, yMax, zMin));
            locations.add(new Location(world, x, yMax, zMax));
        }

        for(int y = yMin; y < yMax; y++) {
            locations.add(new Location(world, xMin, y, zMin));
            locations.add(new Location(world, xMin, y, zMax));
            locations.add(new Location(world, xMax, y, zMin));
            locations.add(new Location(world, xMax, y, zMax));
        }

        for(int z = zMin; z < zMax; z++) {
            locations.add(new Location(world, xMin, yMin, z));
            locations.add(new Location(world, xMin, yMax, z));
            locations.add(new Location(world, xMax, yMin, z));
            locations.add(new Location(world, xMax, yMax, z));
        }

        return locations;
    }
}
