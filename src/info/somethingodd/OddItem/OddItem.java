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
package info.somethingodd.OddItem;

import info.somethingodd.OddItem.configuration.OddItemAliases;
import info.somethingodd.OddItem.configuration.OddItemGroups;
import info.somethingodd.OddItem.util.AlphanumComparator;
import info.somethingodd.OddItem.util.ItemStackComparator;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;

/**
 * @author Gordon Pettey (petteyg359@gmail.com)
 */
public class OddItem {
    protected static OddItemAliases items;
    protected static OddItemGroups groups;
    public static final AlphanumComparator ALPHANUM_COMPARATOR;
    public static final ItemStackComparator ITEM_STACK_COMPARATOR;

    static {
        ALPHANUM_COMPARATOR = new AlphanumComparator();
        ITEM_STACK_COMPARATOR = new ItemStackComparator();
    }

    /**
     * Clears alias and group lists.
     */
    protected static void clear() {
        items = null;
        groups = null;
    }

    /**
     * Gets all aliases for the item represented by an ItemStack
     *
     * @param itemStack the ItemStack to use
     * @return List of aliases
     */
    public static Collection<String> getAliases(ItemStack itemStack) {
        return items.getAliases(itemStack);
    }

    /**
     * Gets all aliases for an item
     *
     * @param query name of item
     * @return <a href="http://download.oracle.com/javase/6/docs/api/java/util/Collection.html?is-external=true">Collection</a>&lt;<a href="http://download.oracle.com/javase/6/docs/api/java/lang/String.html?is-external=true">String</a>&gt; of aliases
     * @throws IllegalArgumentException if no such item exists
     */
    public static Collection<String> getAliases(String query) throws IllegalArgumentException {
        ItemStack itemStack = getItemStack(query);
        return getAliases(itemStack);
    }

    /**
     * Returns an ItemStack of quantity 1 of alias query
     *
     * @param query item name
     * @return <a href="http://jd.bukkit.org/apidocs/org/bukkit/inventory/ItemStack.html?is-external=true">ItemStack</a>
     * @throws IllegalArgumentException exception if item not found, message contains closest match
     */
    public static ItemStack getItemStack(String query) throws IllegalArgumentException {
        return getItemStack(query, 1);
    }

    /**
     * Returns an ItemStack of specific quantity of alias query
     *
     * @param query item name
     * @param quantity quantity
     * @return <a href="http://jd.bukkit.org/apidocs/org/bukkit/inventory/ItemStack.html?is-external=true">ItemStack</a>
     * @throws IllegalArgumentException exception if item not found, message contains closest match
     */
    public static ItemStack getItemStack(String query, int quantity) throws IllegalArgumentException {
        ItemStack i;
        i = items.getItemStack(query);
        if (i == null)
            i = items.getItemStack(query.toLowerCase());
        if (i == null)
            throw new IllegalArgumentException(items.getSuggestions().findBestWordMatch(query));
        i.setAmount(quantity);
        return i;
    }
}
