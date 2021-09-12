package me.michqql.shipmentplugin.shipment;

import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

public class TicketSales {

    private final ItemsForSale itemsForSale;
    private final HashMap<UUID, List<Ticket>> playerToTicketMap = new HashMap<>();
    private int ticketSales;

    public TicketSales(ItemsForSale itemsForSale) {
        this.itemsForSale = itemsForSale;
    }

    public Ticket getTicket(UUID uuid, int ticketID) {
        List<Ticket> tickets = playerToTicketMap.getOrDefault(uuid, new ArrayList<>());
        for(Ticket ticket : tickets) {
            if(ticket.ticketId == ticketID)
                return ticket;
        }
        return null;
    }

    public int buyTicket(UUID uuid, List<ItemsForSale.ForSale> purchases) {
        final int ticketId = ticketSales++;
        final Ticket ticket = new Ticket(uuid, ticketId, purchases);

        playerToTicketMap.compute(uuid, (uuid1, tickets) -> {
            if(tickets == null)
                tickets = new ArrayList<>();

            tickets.add(ticket);
            return tickets;
        });
        return ticketId;
    }

    public boolean hasSales() {
        return ticketSales > 0;
    }

    public int getTicketSales() {
        return ticketSales;
    }

    public int getUserSales() {
        return playerToTicketMap.size();
    }

    public HashMap<UUID, List<Ticket>> getMap() {
        return playerToTicketMap;
    }

    void save(ConfigurationSection section) {
        section.set("ticket-sales", ticketSales);

        // 1. Loop through all players
        // 2. Loop through each player's tickets
        // 3. Loop through each ticket's ForSale, adding index to an array
        playerToTicketMap.forEach((uuid, tickets) -> tickets.forEach(ticket -> {
            List<Integer> forSaleIndexes = new ArrayList<>();
            ticket.purchases.forEach(sale -> forSaleIndexes.add(sale.index));

            section.set("players." + uuid + "." + ticket.ticketId + ".claimed", ticket.isClaimed());
            section.set("players." + uuid + "." + ticket.ticketId + ".items", forSaleIndexes);
        }));
    }

    void load(ConfigurationSection section) {
        if(section == null)
            return;

        this.ticketSales = section.getInt("ticket-sales");

        // 1. Loop through all uuid's
        // 2. Loop through all ticket ID's
        // 3. Loop through all ForSale indexes
        ConfigurationSection playerSection = section.getConfigurationSection("players");
        if(playerSection == null)
            return;

        for(String strUUID : playerSection.getKeys(false)) {
            UUID uuid;
            List<Ticket> tickets = new ArrayList<>();
            try {
                uuid = UUID.fromString(strUUID);
            } catch(IllegalArgumentException e) {
                continue;
            }

            ConfigurationSection ticketSection = playerSection.getConfigurationSection(strUUID);
            if(ticketSection == null)
                continue;

            // Loop through all ticket ID's
            for(String strTicketID : ticketSection.getKeys(false)) {
                int ticketID;
                try {
                    ticketID = Integer.parseInt(strTicketID);
                } catch(NumberFormatException e) {
                    continue;
                }

                List<ItemsForSale.ForSale> purchases = new ArrayList<>();
                boolean claimed = ticketSection.getBoolean(strTicketID + ".claimed");
                List<Integer> indexes = ticketSection.getIntegerList(strTicketID + ".items");
                for(int index : indexes) {
                    purchases.add(itemsForSale.getItemForSale(index));
                }

                tickets.add(new Ticket(uuid, ticketID, purchases, claimed));
            }

            // Store tickets in map
            playerToTicketMap.put(uuid, tickets);
        }
    }

    public static class Ticket {
        private final UUID player;
        private final int ticketId;
        private final List<ItemsForSale.ForSale> purchases;
        private boolean claimed;

        public Ticket(UUID player, int ticketId, List<ItemsForSale.ForSale> purchases) {
            this.player = player;
            this.ticketId = ticketId;
            this.purchases = purchases;
            this.claimed = false;
        }

        public Ticket(UUID player, int ticketId, List<ItemsForSale.ForSale> purchases, boolean claimed) {
            this.player = player;
            this.ticketId = ticketId;
            this.purchases = purchases;
            this.claimed = claimed;
        }

        public UUID getPlayer() {
            return player;
        }

        public int getTicketId() {
            return ticketId;
        }

        public List<ItemsForSale.ForSale> getPurchases() {
            return purchases;
        }

        public int getItemSize() {
            return purchases.size();
        }

        public boolean isClaimed() {
            return claimed;
        }

        public void markAsClaimed() {
            this.claimed = true;
        }
    }
}
