/* This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package info.somethingodd.OddItem.configuration;

import info.somethingodd.OddItem.OddItem;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemorySection;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * @author Gordon Pettey (petteyg359@gmail.com)
 */
public class OddItemGroup implements ConfigurationSerializable, Iterable<ItemStack> {
    private List<ItemStack> itemStacks;
    private List<String> items;
    private Map<String, Object> data;
    private Set<String> aliases;

    @SuppressWarnings("unchecked")
	public OddItemGroup(Map<String, Object> serialized) {
        data = ((ConfigurationSection) serialized.get("data")).getValues(false);
        items = (List<String>) serialized.get("items");
        itemStacks = new ArrayList<ItemStack>();
        aliases = new TreeSet<String>(OddItem.ALPHANUM_COMPARATOR);
        if (serialized.containsKey("aliases")) {
            aliases.addAll((Collection<String>) serialized.get("aliases"));
        }
        for (String item : items) {
            ItemStack itemStack;
            try {
                if (item.contains(",")) {
                    itemStack = OddItem.getItemStack(item.substring(0, item.indexOf(",")));
                    itemStack.setAmount(Integer.valueOf(item.substring(item.indexOf(",") + 1)));
                } else {
                    itemStack = OddItem.getItemStack(item);
                    itemStack.setAmount(1);
                }
                itemStacks.add(itemStack);
            } catch (IllegalArgumentException e2) {
                Bukkit.getLogger().warning("Invalid item \"" + item + "\" in groups.yml (" + items.toString() + ")");
            }
        }
    }

    /**
     * Gets all aliases for the group
     * @return Collection of aliases
     */
    public Collection<String> getAliases() {
        return Collections.unmodifiableCollection(aliases);
    }

    /**
     * Gets group data
     * @return @{code Map} of data
     */
    public Map<String, Object> getData() {
        return data;
    }

    /**
     * Get data by key
     * @param key key to retrieve
     * @return data
     */
    public Object getData(String key) {
        if (!data.containsKey(key))
            return null;
        return data.get(key);
    }

    /**
     * Get data by second-level key
     * @param key top-level key
     * @param key2 second-level key
     * @return data
     */
    public Object getData(String key, String key2) {
        ConfigurationSection configurationSection = (ConfigurationSection) getData(key);
        if (configurationSection == null)
            return null;
        return configurationSection.get(key2);
    }

    /**
     * Get {@code int} by key
     * @param key top-level key
     * @return data
     */
    public Integer getInt(String key) {
        return (Integer) getData(key);
    }

    /**
     * Get {@code int} by second-level key
     * @param key top-level key
     * @param key2 second-level key
     * @return data
     */
    public Integer getInt(String key, String key2) {
        return (Integer) getData(key, key2);
    }

    /**
     * Get {@code String} by key
     * @param key top-level key
     * @return data
     */
    public String getString(String key) {
        return (String) data.get(key);
    }

    /**
     * Get {@code String} by second-level key
     * @param key top-level key
     * @param key2 second-level key
     * @return data
     */
    public String getString(String key, String key2) {
        return (String) getData(key, key2);
    }

    /**
     * Get {@code List&lt;String&gt;} by key
     * @param key top-level key
     * @return data
     */
    @SuppressWarnings("unchecked")
	public List<String> getStringList(String key) {
        return (List<String>) data.get(key);
    }

    /**
     * Get {@code List&lt;String&gt;} by second-level key
     * @param key top-level key
     * @param key2 second-level key
     * @return data
     */
    @SuppressWarnings("unchecked")
	public List<String> getStringList(String key, String key2) {
        return (List<String>) getData(key, key2);
    }

    /**
     * Get {@code double} by key
     * @param key top-level key
     * @return data
     */
    public double getDouble(String key) {
        return (Double) getData(key);
    }

    /**
     * Get {@code double} by second-level key
     * @param key top-level key
     * @param key2 second-level key
     * @return data
     */
    public double getDouble(String key, String key2) {
        return (Double) getData(key, key2);
    }

    /**
     * Get {@code ConfigurationSection} by second-level key
     * @param key top-level key
     * @return data
     */
    public ConfigurationSection getConfigurationSection(String key) {
        return (ConfigurationSection) getData(key);
    }

    /**
     * Checks for top-level data key
     * @param key key to check
     * @return whether group contains data key
     */
    public boolean match(String key) {
        for (String k : data.keySet())
            if (k.equals(key))
                return true;
        return false;
    }

    /**
     * Checks for second-level data key
     * @param key top-level key to check
     * @param key2 second-level key to check
     * @return whether group contains key2 inside key1 in data
     */
    @SuppressWarnings("rawtypes")
	public boolean match(String key, String key2) {
        for (Object x : data.values()) {
            if (x instanceof MemorySection)
                return ((MemorySection) x).getValues(false).containsKey("key2");
            if (x instanceof Map)
                return ((Map) x).containsKey(key2);
            if (x instanceof List)
                return ((List) x).contains(key2);
        }
        return false;
    }

    /**
     * OddItemGroup as String
     * @return OddItemGroup{items=[items],data=[data]}
     */
    public String toString() {
        StringBuilder str = new StringBuilder("OddItemGroup");
        str.append("{");
        str.append("items=").append(items.toString());
        str.append(",");
        str.append("data=").append(data.toString());
        str.append("}\n");
        return str.toString();
    }

    /**
     * {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> serialized = new TreeMap<String, Object>(OddItem.ALPHANUM_COMPARATOR);
        serialized.put("items", items);
        serialized.put("data", data);
        return serialized;
    }

    public static OddItemGroup deserialize(Map<String, Object> serialized) {
        return new OddItemGroup(serialized);
    }

    public static OddItemGroup valueOf(Map<String, Object> serialized) {
        return new OddItemGroup(serialized);
    }

    public Collection<ItemStack> getItems() {
        return Collections.unmodifiableCollection(itemStacks);
    }

    public ItemStack get(int index) {
        return itemStacks.get(index);
    }

    /**
     * {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public Iterator<ItemStack> iterator() {
        return Collections.unmodifiableList(itemStacks).iterator();
    }
}
