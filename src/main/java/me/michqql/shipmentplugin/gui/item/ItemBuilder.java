package me.michqql.shipmentplugin.gui.item;

import me.michqql.shipmentplugin.utils.MessageUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ItemBuilder {

    private final ItemStack item;
    private final ItemMeta meta;
    private final PersistentDataContainer dataContainer;

    private Map<String, String> placeholders;

    public ItemBuilder(Material material) {
        this.item = new ItemStack(material);
        this.meta = item.getItemMeta();
        this.dataContainer = meta.getPersistentDataContainer();
    }

    public ItemBuilder(ItemStack itemStack) {
        this.item = itemStack;
        this.meta = item.getItemMeta();
        this.dataContainer = meta.getPersistentDataContainer();
    }

    public ItemBuilder specifyPlaceholders(Map<String, String> placeholders) {
        this.placeholders = placeholders;
        return this;
    }

    public ItemBuilder displayName(String string) {
        meta.displayName(text(string));
        return this;
    }

    public ItemBuilder lore(String... lore) {
        return lore(Arrays.asList(lore));
    }

    public ItemBuilder lore(List<String> strings) {
        List<Component> lore = new ArrayList<>();
        for(String line : strings) {
            lore.add(text(line));
        }
        meta.lore(lore);
        return this;
    }

    public ItemBuilder addLore(String... lore) {
        return addLore(Arrays.asList(lore));
    }

    public ItemBuilder addLore(List<String> strings) {
        List<Component> lore = meta.hasLore() ? meta.lore() : new ArrayList<>();
        assert lore != null; // Checked by meta.hasLore()

        for(String line : strings) {
            lore.add(text(line));
        }
        meta.lore(lore);
        return this;
    }

    public ItemBuilder amount(int amount) {
        item.setAmount(amount);
        return this;
    }

    public <T,Z> ItemBuilder persistentData(Plugin plugin, String key, PersistentDataType<T,Z> type, Z value) {
        dataContainer.set(new NamespacedKey(plugin, key), type, value);
        return this;
    }

    public ItemStack getItem() {
        this.item.setItemMeta(meta);
        return item;
    }

    private Component text(String text) {
        text = MessageUtil.replacePlaceholdersStatic(text, placeholders); // first replace placeholders
        return Component.text(text);
    }

    /**
     * Method uses bukkit objects IO and does not depend on NMS
     * Avoids GZIP format exception
     * @param item the item to serialize
     * @return a serialized string
     */
    public static String serializeItem(ItemStack item) {
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            BukkitObjectOutputStream data = new BukkitObjectOutputStream(os);

            data.writeObject(item);
            data.close();
            return Base64Coder.encodeLines(os.toByteArray());
        } catch(IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    /**
     * Method uses bukkit objects IO and does not depend on NMS
     * Avoids GZIP format exception
     * @param data the string to deserialize
     * @return an item
     */
    public static ItemStack deserializeItem(String data) {
        try {
            ByteArrayInputStream is = new ByteArrayInputStream(Base64Coder.decodeLines(data));
            BukkitObjectInputStream stream = new BukkitObjectInputStream(is);

            return (ItemStack) stream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static <T, Z> Z getPersistentData(Plugin plugin, PersistentDataContainer dataContainer, String key, PersistentDataType<T, Z> type) {
        NamespacedKey namedKey = new NamespacedKey(plugin, key);
        return dataContainer.has(namedKey, type) ? dataContainer.get(namedKey, type) : null;
    }

    public static Component getComponentFromText(String text) {
        return Component.text(MessageUtil.format(text));
    }
}
