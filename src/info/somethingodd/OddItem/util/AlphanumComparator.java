/**
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package info.somethingodd.OddItem.util;

import java.util.Comparator;

/**
 * The Alphanum Algorithm is an improved sorting algorithm for strings
 * containing numbers.  Instead of sorting numbers in ASCII order like
 * a standard sort, this algorithm sorts numbers in numeric order.
 * <p/>
 * The Alphanum Algorithm is discussed at http://www.DaveKoelle.com
 * <p/>
 * This is an updated version with enhancements made by Daniel Migowski,
 * Andre Bogus, and David Koelle
 */
public class AlphanumComparator implements Comparator<String> {

    /**
     * Determines if character is a numerical digit.
     *
     * @param ch character to check
     * @return whether ch is a numerical digit
     */
    private final boolean isDigit(char ch) {
        return ch >= 48 && ch <= 57;
    }

    /**
     * Gets a numerical or alphabetical chunk for comparison
     *
     * @param s mixed String
     * @param marker starting position
     * @return chunk of s
     */
    private final String getChunk(String s, int marker) {
        StringBuilder chunk = new StringBuilder();
        char c = s.charAt(marker);
        chunk.append(c);
        marker++;
        if (isDigit(c)) {
            while (marker < s.length()) {
                c = s.charAt(marker);
                if (!isDigit(c)) {
                    break;
                }
                chunk.append(c);
                marker++;
            }
        } else {
            while (marker < s.length()) {
                c = s.charAt(marker);
                if (isDigit(c)) {
                    break;
                }
                chunk.append(c);
                marker++;
            }
        }
        return chunk.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compare(String o1, String o2) {
        int thisMarker = 0;
        int thatMarker = 0;
        while (thisMarker < o1.length() && thatMarker < o2.length()) {
            String thisChunk = getChunk(o1, thisMarker);
            thisMarker += thisChunk.length();
            String thatChunk = getChunk(o2, thatMarker);
            thatMarker += thatChunk.length();
            // If both chunks contain numeric characters, sort them numerically
            int result = 0;
            if (isDigit(thisChunk.charAt(0)) && isDigit(thatChunk.charAt(0))) {
                // Simple chunk comparison by length.
                int thisChunkLength = thisChunk.length();
                result = thisChunkLength - thatChunk.length();
                // If equal, the first different number counts
                if (result == 0) {
                    for (int i = 0; i < thisChunkLength; i++) {
                        result = thisChunk.charAt(i) - thatChunk.charAt(i);
                        if (result != 0) {
                            return result;
                        }
                    }
                }
            } else {
                result = thisChunk.compareTo(thatChunk);
            }
            if (result != 0) {
                return result;
            }
        }
        return o1.length() - o2.length();
    }
}
