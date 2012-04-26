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
package info.somethingodd.OddItem.util;

import org.bukkit.inventory.ItemStack;

import java.util.Comparator;

/**
 * @author Gordon Pettey (petteyg359@gmail.com)
 */
public class ItemStackComparator implements Comparator<ItemStack> {

    @Override
    public int compare(ItemStack o1, ItemStack o2) {
        if (o1.getTypeId() != o2.getTypeId()) {
            if (o1.getTypeId() < o2.getTypeId()) return -1;
            if (o1.getTypeId() > o2.getTypeId()) return 1;
        }
        if (o1.getDurability() != o2.getDurability()) {
            if (o1.getDurability() < o2.getDurability()) return -1;
            if (o1.getDurability() > o2.getDurability()) return 1;
        }
        return 0;
    }
}
