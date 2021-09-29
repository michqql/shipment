package me.michqql.shipmentplugin.gui;

import java.util.*;

public class GUIManager {

    private static final HashMap<UUID, LinkedList<GUI>> PLAYER_GUIS = new HashMap<>();
    private static final HashMap<UUID, LinkedList<GUI>> SAVED_GUIS = new HashMap<>();

    /**
     * Registers a new GUI, storing it in the hashmap
     * @param player - the owning player's uuid
     * @param gui  - the GUI
     */
    static void registerGUI(final UUID player, final GUI gui) {
        Objects.requireNonNull(player, "Could not register GUI due to null player");
        Objects.requireNonNull(gui, "Could not register null GUI");

        PLAYER_GUIS.compute(player, (uuid, guis) -> {
            if(guis == null)
                guis = new LinkedList<>();

            guis.addLast(gui);
            return guis;
        });
    }

    public static GUI getCurrentGUI(final UUID uuid) {
        final LinkedList<GUI> localGuis = PLAYER_GUIS.get(uuid);
        return (localGuis != null) ? localGuis.getLast() : null;
    }

    static GUI getPreviousGUI(final UUID uuid) {
        final LinkedList<GUI> localGuis = PLAYER_GUIS.get(uuid);
        return (localGuis != null && localGuis.size() >= 2) ? localGuis.get(localGuis.size() - 2) : null;
    }

    static void removeEntry(final UUID uuid) {
        PLAYER_GUIS.remove(uuid);
    }

    static void removeCurrentGUI(final UUID uuid) {
        final LinkedList<GUI> localGuis = PLAYER_GUIS.get(uuid);
        if(localGuis.size() >= 1)
            localGuis.removeLast().internalClose();

        PLAYER_GUIS.put(uuid, localGuis);
    }

    public static void openPreviousGUI(final UUID uuid) {
        GUI current = getCurrentGUI(uuid);
        GUI past = getPreviousGUI(uuid);

        if(current != null)
            current.internalSetOpen(false);

        if(past != null) {
            past.updateInventory();
            past.openGUI();
        }
    }

    public static void reopenCurrentGUI(final UUID uuid) {
        GUI current = getCurrentGUI(uuid);
        if(current != null) {
            current.internalSetOpen(false);
            current.updateInventory();
            current.openGUI();
        }
    }

    public static void savePlayerGUIs(UUID uuid) {
        SAVED_GUIS.put(uuid, PLAYER_GUIS.get(uuid));
    }

    public static void loadSavedGUIs(UUID uuid) {
        PLAYER_GUIS.put(uuid, SAVED_GUIS.remove(uuid));
    }
}
