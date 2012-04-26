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
import info.somethingodd.OddItem.bktree.BKTree;

import org.bukkit.Material;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * @author Gordon Pettey (petteyg359@gmail.com)
 * @author Michael Carver (xiberphim@gmail.com) some deletions and 
 * 	modifications to work with DynaMark 26 APR 2012
 */
public class OddItemAliases implements ConfigurationSerializable {
    private final BKTree<String> suggestions;
    private final Map<String, ItemStack> items;
    private final Map<ItemStack, Set<String>> aliases;

    public OddItemAliases(Map<String, Object> serialized) {
        suggestions = new BKTree<String>("r");
        items = Collections.synchronizedMap(new TreeMap<String, ItemStack>(OddItem.ALPHANUM_COMPARATOR));
        aliases = Collections.synchronizedMap(new TreeMap<ItemStack, Set<String>>(OddItem.ITEM_STACK_COMPARATOR));
        for (String key : serialized.keySet()) {
            ItemStack itemStack = stringToItemStack(key);
            @SuppressWarnings("unchecked")
			Collection<String> names = (Collection<String>) serialized.get(key);
            if (aliases.get(itemStack) == null)
                aliases.put(itemStack, new TreeSet<String>(OddItem.ALPHANUM_COMPARATOR));
            
            if (itemStack == null)
            	System.out.println(key);
            if (names == null)
            	System.out.println(key);
            for (String n : names) {
            	if (n == null)
            		System.out.println(key);
            }
            
            aliases.get(itemStack).addAll(names);
            for (String alias : names) {
                items.put(alias, itemStack);
                suggestions.add(alias);
            }
        }
    }

    private String itemStackToString(ItemStack itemStack) {
        return new StringBuilder().append(itemStack.getTypeId() + ";" + itemStack.getDurability()).toString();
    }

    private ItemStack stringToItemStack(String string) {
    	String[] split = string.split(":");
        Material type = Material.getMaterial(Integer.valueOf(split[0]));
        short damage = 0;
        int amount = 1;
        Byte data = Byte.valueOf(split[1]);
       
        return new ItemStack(type, amount, damage, data);
    }

    public Collection<String> getAliases(ItemStack itemStack) {
        return aliases.get(itemStack);
    }

    public ItemStack getItemStack(String query) {
        return items.get(query);
    }

    /**
     * @return number of aliases loaded
     */
    public int aliasCount() {
        return items.size();
    }

    /**
     * @return number of items loaded
     */
    public int itemCount() {
        return aliases.size();
    }

    protected Map<ItemStack, Set<String>> getAliases() {
        return Collections.synchronizedMap(Collections.unmodifiableMap(aliases));
    }

    protected Map<String, ItemStack> getItems() {
        return Collections.synchronizedMap(Collections.unmodifiableMap(items));
    }

    public BKTree<String> getSuggestions() {
        return suggestions;
    }

    public String toString() {
        StringBuilder str = new StringBuilder("OddItemAliases");
        str.append("{");
        str.append("aliases=").append(aliases.toString());
        str.append(",");
        str.append("items=").append(items.toString());
        str.append("}\n");
        return str.toString();
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> serialized = new TreeMap<String, Object>(OddItem.ALPHANUM_COMPARATOR);
        for (ItemStack itemStack : aliases.keySet()) {
            serialized.put(itemStackToString(itemStack), aliases.get(itemStack).toArray());
        }
        return serialized;
    }

    public static OddItemAliases deserialize(Map<String, Object> serialized) {
        return new OddItemAliases(serialized);
    }

    public static OddItemAliases valueOf(Map<String, Object> serialized) {
        return new OddItemAliases(serialized);
    }

    public int hashCode() {
        int hash = 17;
        hash += items.hashCode();
        hash += aliases.hashCode();
        return hash;
    }

    public boolean equals(Object other) {
        if (!(other instanceof OddItemAliases)) return false;
        if (this == other) return true;
        if (!getItems().equals(((OddItemAliases) other).getItems())) return false;
        return getAliases().equals(((OddItemAliases) other).getAliases());
    }
}