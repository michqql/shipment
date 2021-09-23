package me.michqql.shipmentplugin.shipment;

import me.michqql.shipmentplugin.gui.item.ItemBuilder;
import me.michqql.shipmentplugin.preset.PresetHandler;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;

public class ItemsForSale {

    private ForSale[] sales;
    private int saleIndex;

    public ItemsForSale() {
        this.sales = new ForSale[50];
    }

    public void addAll(ItemsForSale itemsForSale) {
        int added = 0;

        for(ForSale forSale : itemsForSale.sales) {
            if(forSale == null)
                continue;

            int index = getFirstFreeIndex();
            if(index < 0 || index >= sales.length)
                break;

            this.sales[index] = forSale;
            added++;
        }

        this.saleIndex += added;
    }

    public void addItemForSale(ItemStack item, double price) {
        int index = getFirstFreeIndex();
        if(index == -1)
            return;

        this.sales[index] = new ForSale(saleIndex, item, price);
        this.saleIndex++;
    }

    public ForSale getItemForSale(int index) {
        if(index < 0 || index >= sales.length)
            return null;

        return sales[index];
    }

    public ForSale getItemForSaleBySaleIndex(int saleIndex) {
        for(ForSale sale : sales) {
            if(sale != null && sale.getSaleIndex() == saleIndex)
                return sale;
        }
        return null;
    }

    public void removeItemForSale(int index) {
        if(index < 0 || index >= sales.length)
            return;

        sales[index] = null;
    }

    public boolean isIndexInvalid(int index) {
        return index < 0 || index >= sales.length || sales[index] == null;
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

        // Array is full at this point, grow size
        int maxSize = sales.length;
        sales = Arrays.copyOf(sales, maxSize + 10);
        return maxSize;
    }

    public void save(ConfigurationSection section) {
        section.set("sales-index", saleIndex);

        ConfigurationSection itemSection = section.createSection("items");

        for(int i = 0; i < sales.length; i++) {
            ForSale sale = sales[i];
            if(sale == null)
                continue;

            itemSection.set(i + ".index", sale.saleIndex);
            itemSection.set(i + ".item", ItemBuilder.serializeItem(sale.itemStack));
            itemSection.set(i + ".price", sale.price);
        }
    }

    public void load(ConfigurationSection section) {
        if(section == null)
            return;

        this.saleIndex = section.getInt("sales-index");

        ConfigurationSection itemSection = section.getConfigurationSection("items");
        if(itemSection == null)
            return;

        for(String strIndex : itemSection.getKeys(false)) {
            // We do not care about this strIndex
            int index = getFirstFreeIndex();

            sales[index] = new ForSale(
                    itemSection.getInt(strIndex + ".index"),
                    ItemBuilder.deserializeItem(itemSection.getString(strIndex + ".item")),
                    itemSection.getDouble(strIndex + ".price")
            );
        }
    }

    void loadWithPreset(ConfigurationSection section, PresetHandler.Preset preset) {
        if(preset == null) {
            load(section);
            return;
        }

        if (section == null) {
            addAll(preset.getItems());
            return;
        }

        load(section);
    }

    public static class ForSale {
        final int saleIndex;
        private final ItemStack itemStack;
        private final double price;

        protected ForSale(int saleIndex, ItemStack itemStack, double price) {
            this.saleIndex = saleIndex;
            this.itemStack = itemStack;
            this.price = price;
        }

        public int getSaleIndex() {
            return saleIndex;
        }

        public ItemStack getItemStack() {
            return itemStack;
        }

        public double getPrice() {
            return price;
        }
    }
}
