package me.michqql.shipmentplugin.shipment;

import me.michqql.shipmentplugin.ShipmentPlugin;
import me.michqql.shipmentplugin.data.CommentFile;
import me.michqql.shipmentplugin.npc.NPCHandler;
import me.michqql.shipmentplugin.schematic.SchematicHandler;
import me.michqql.shipmentplugin.utils.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

import java.time.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class ShipmentManager {

    public final static ZoneId ZONE_ID = ZoneId.systemDefault();

    private final Plugin plugin;
    private final SchematicHandler schematicHandler;
    private final NPCHandler npcHandler;
    private final Shipment[] shipments = new Shipment[7]; // first 3 are past, last 3 are future

    // Configuration options
    public DayOfWeek shipmentDayOfWeek = DayOfWeek.SUNDAY;
    private boolean realTimeEditing;
    private int autoSaveTimeInMinutes;

    /**
     * @param config - The main configuration file 'config.yml'
     */
    public ShipmentManager(ShipmentPlugin plugin, SchematicHandler schematicHandler, NPCHandler npcHandler, CommentFile config) {
        this.plugin = plugin;
        this.schematicHandler = schematicHandler;
        this.npcHandler = npcHandler;

        loadConfig(config.getConfig());
        startDailyTimer();
        autoSaveTimer();
    }

    private void loadConfig(FileConfiguration f) {
        // Day of week
        try {
            this.shipmentDayOfWeek = DayOfWeek.of(f.getInt("shipment.day-of-week"));
        } catch(DateTimeException e) {
            Bukkit.getLogger().warning("[Shipment] Invalid day of week. (shipment-day-of-week must be an integer between 1 and 7)");
            Bukkit.getLogger().warning("[Shipment] Invalid day of week. Shipment day of week has been set to SUNDAY (7)");
            this.shipmentDayOfWeek = DayOfWeek.SUNDAY;
        }

        // Saving and editing
        this.realTimeEditing = f.getBoolean("shipment.real-time-editing");
        this.autoSaveTimeInMinutes = f.getInt("shipment.auto-save");
    }

    /**
     * Saves shipments
     */
    public void save() {
        for(Shipment shipment : shipments) {
            if(shipment == null)
                continue;

            shipment.save(plugin);
        }
    }

    /**
     * Starts a timer to save shipments every X minutes
     */
    private void autoSaveTimer() {
        if(autoSaveTimeInMinutes < 1)
            return;

        // Converts time in minutes into time in minecraft ticks
        Bukkit.getScheduler().runTaskTimer(plugin, this::save, 20L, autoSaveTimeInMinutes * 60 * 20L);
    }

    /**
     * Timer to run a task every day at the same time to update shipments
     */
    private void startDailyTimer() {
        // Sets up this task to run at the beginning of a day
        Calendar startOfDay = Calendar.getInstance();
        startOfDay.set(Calendar.HOUR_OF_DAY, 0);
        startOfDay.set(Calendar.MINUTE, 0);
        startOfDay.set(Calendar.SECOND, 0);

        // Timer that executes the update shipment method
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Bukkit.getLogger().info("[Shipment] Daily task timer (update shipments) executing...");
                long now = System.currentTimeMillis();
                updateShipments();
                long timeTaken = System.currentTimeMillis() - now;
                Bukkit.getLogger().info("[Shipment] Executed task timer (update shipments) in " + timeTaken + "ms");
            }
        }, startOfDay.getTime(), TimeUnit.DAYS.toMillis(1));
    }

    /**
     * Method checks and updates array of past, present and future shipments
     *
     * If there is no present shipment, assume the array is entirely empty and
     * initialise it
     *
     * If the present shipment is actually in the past at the time of method
     * call, save the first element and shift the array to the left. Finally
     * generate a new future shipment
     */
    public void updateShipments() {
        // Must check upcoming shipment as past shipments
        // can be null if this plugin has not run for long
        // enough to generate past shipments
        if(shipments[3] == null)
            initialiseShipments();

        // Check if the 'next' shipment is in the past
        if(shipments[3].compareShipmentChronology() == -1) {
            // This element will be overwritten and so should be saved
            if(shipments[0] != null)
                shipments[0].save(plugin);

            // Copy the array, shifting the elements to the left
            // noinspection SuspiciousSystemArraycopy (IntelliJ)
            System.arraycopy(shipments, 1, shipments, 0, shipments.length - 1);

            // Generate new future shipment
            // Apply item preset if applicable
            long timestamp = TimeUtil.getDayTimeStamp(shipmentDayOfWeek, 3);
            shipments[shipments.length - 1] = new Shipment(timestamp); // Actual value of index is always 6
        }

        // Check current day
        updateCurrentShipment();
    }

    /**
     * Checks and updates current shipment via main thread.
     * As this method involves changing the world through
     * the use of WorldEdit schematic pasting, we must ensure
     * this method is called in the main thread to avoid
     * exceptions
     *
     * If the shipment is today, it will spawn in the
     * schematic, otherwise it will paste back the copy
     */
    private void updateCurrentShipment() {
        Bukkit.getScheduler().runTask(plugin, () -> {
            if(shipments[3].isShipmentToday()) {
                schematicHandler.pasteSchematic();
                //npcHandler.spawnNPC(); // dont spawn, we want this always
            } else {
                schematicHandler.undoPaste();
                //npcHandler.despawnNPC(); // don't despawn, we want to keep it
            }
        });
    }

    /**
     * Method initialises shipments and populates the array
     * Will attempt to load any saved data for shipments in
     * the past, present and future.
     */
    private void initialiseShipments() {
        /* PREVIOUS SHIPMENTS */
        // 1. Check file for previous shipments
        // 2. Load previous shipments if applicable
        // 3. Generating next/upcoming shipments
        for(int i = 0; i < 3; i++) { // 3 previous shipments to check
            int weeksAgo = -3 + i;
            long timestamp = TimeUtil.getDayTimeStamp(shipmentDayOfWeek, weeksAgo);

            // Load and read file if exists
            shipments[i] = new Shipment(timestamp, plugin);
        }

        /* CURRENT/NEXT SHIPMENT */
        //  1. Determine next shipment timestamp
        shipments[3] = new Shipment(getNextShipment(), plugin);

        /* FUTURE SHIPMENTS */
        // 1. Determine future shipment timestamps
        for(int i = 0; i < 3; i++) {
            int weeksAhead = 1 + i;
            long timestamp = TimeUtil.getDayTimeStamp(shipmentDayOfWeek, weeksAhead);

            shipments[4 + i] = new Shipment(timestamp, plugin);
        }
    }

    /**
     * Gets the next upcoming shipment
     * If there is a shipment is today, today will be returned
     * @return the epoch ms for the start of the next shipment day
     */
    public long getNextShipment() {
        ZonedDateTime startOfNow = LocalDate.now().atStartOfDay(ZONE_ID);

        return getNextShipment(startOfNow);
    }

    /**
     * Gets the next shipment after the specified date
     * If there is a shipment on the date, the date will be returned
     * @param time of query
     * @return the epoch ms for the start of the next shipment day after the queried date
     */
    private long getNextShipment(ZonedDateTime time) {
        // Calculate the difference between current day of week and the desired day of week
        int dayOfWeekDifference = shipmentDayOfWeek.getValue() - time.getDayOfWeek().getValue();
        if(dayOfWeekDifference < 0)
            dayOfWeekDifference += 7; // Ensure we are getting NEXT day of week

        // Add difference
        time = time.plusDays(dayOfWeekDifference);
        return time.toInstant().toEpochMilli();
    }

    public Shipment getUpcomingShipment() {
        return shipments[3];
    }

    public Shipment getShipmentByTimestamp(long timestamp) {
        for(Shipment shipment : shipments) {
            if(shipment != null && shipment.getShipmentEpochMS() == timestamp)
                return shipment;
        }
        return null;
    }

    public Shipment getTodaysShipment() {
        Shipment today = shipments[3];
        if(today == null)
            return null;

        return today.isShipmentToday() ? today : null;
    }

    /* Presets */
    public boolean doesPresetExist(String name) {
        return false;
    }

    public void createPreset(String name) {

    }
    /* End of Presets */

    public Shipment[] getShipments() {
        return shipments;
    }

    public boolean allowRealTimeEdits() {
        return realTimeEditing;
    }
}
