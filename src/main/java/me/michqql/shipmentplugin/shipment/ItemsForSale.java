package me.michqql.shipmentplugin.shipment;

import me.michqql.shipmentplugin.gui.item.ItemBuilder;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;

public class ItemsForSale {

    private final int itemLimit;
    private final ForSale[] sales;

    public ItemsForSale() {
        this.itemLimit = 45;
        this.sales = new ForSale[itemLimit];
    }

    public int addAll(ItemsForSale itemsForSale) {
        int added = 0;

        for(ForSale forSale : itemsForSale.sales) {
            int index = getFirstFreeIndex();
            if(index == -1 || index >= sales.length)
                break;

            this.sales[index] = forSale;
            added++;
        }

        return added;
    }

    public boolean addItemForSale(ItemStack item, double price) {
        int index = getFirstFreeIndex();
        if(index == -1)
            return false;

        this.sales[index] = new ForSale(index, item, price);
        return true;
    }

    public ForSale getItemForSale(int index) {
        if(index < 0 || index >= sales.length)
            return null;

        return sales[index];
    }

    public boolean removeItemForSale(int index) {
        if(index < 0 || index >= sales.length)
            return false;

        sales[index] = null;
        return true;
    }

    public boolean isIndexValid(int index) {
        return index >= 0 && index < sales.length && sales[index] != null;
    }

    public int getItemLimit() {
        return itemLimit;
    }

    public int getAmountOfItemsForSale() {
        int amount = 0;
        for (ForSale sale : sales) {
            if (sale != null)
                amount++;
        }
        return amount;
    }

    public ForSale[] getSales() {
        return Arrays.copyOf(sales, sales.length);
    }

    private int getFirstFreeIndex() {
        for(int i = 0; i < sales.length; i++) {
            if (sales[i] == null)
                return i;
        }
        return -1;
    }

    void save(ConfigurationSection section) {
        for(int i = 0; i < sales.length; i++) {
            ForSale sale = sales[i];
            if(sale == null)
                continue;

            section.set(i + ".item", ItemBuilder.serializeItem(sale.itemStack));
            section.set(i + ".price", sale.price);
        }
    }

    void load(ConfigurationSection section) {
        if(section == null)
            return;

        for(String strIndex : section.getKeys(false)) {
            int index;
            try {
                index = Integer.parseInt(strIndex);
            } catch(NumberFormatException e) {
                continue;
            }

            sales[index] = new ForSale(
                    index,
                    ItemBuilder.deserializeItem(section.getString(strIndex + ".item")),
                    section.getDouble(strIndex + ".price")
            );
        }
    }

    public static class ForSale {
        final int index;
        private final ItemStack itemStack;
        private final double price;

        protected ForSale(int index, ItemStack itemStack, double price) {
            this.index = index;
            this.itemStack = itemStack;
            this.price = price;
        }

        public int getIndex() {
            return index;
        }

        public ItemStack getItemStack() {
            return itemStack;
        }

        public double getPrice() {
            return price;
        }
    }
}
