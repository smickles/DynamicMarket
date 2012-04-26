package info.somethingodd.OddItem.bktree;


import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.language.*;

import java.util.HashMap;

/**
 * This class in an implementation of a Burkhard-Keller tree in Java.
 * The BK-Tree is a tree structure to quickly finding close matches to
 * any defined object.
 *
 * The BK-Tree was first described in the paper:
 * "Some Approaches to Best-Match File Searching" by W. A. Burkhard and R. M. Keller
 * It is available in the ACM archives.
 *
 * Another good explanation can be found here:
 * http://blog.notdot.net/2007/4/Damn-Cool-Algorithms-Part-1-BK-Trees
 *
 * Searching the tree yields O(logn), which is a huge upgrade over brute force
 *
 * @author Josh Clemm
 * @see https://code.google.com/p/java-bk-tree/
 */

/**
 * Add resetability and Distance wrapper to support Apache commons-codec
 *
 * @author Gordon Pettey
 */


public class BKTree <E> {

	private Node root = null;
    private E bestTerm;
    private final Distance distance;

	public BKTree(String comparator) {
		this.distance = new Distance(comparator);
	}

    public void add(E term) {
		if(root != null) {
			root.add(term);
		}
		else {
			root = new Node(term);
		}
	}

    public void clear() {
        root.children.clear();
        root = null;
    }

	/**
	 * This method will find all the close matching Objects within
	 * a certain threshold.  For instance, for search for similar
	 * strings, threshold set to 1 will return all the strings that
	 * are off by 1 edit distance.
	 * @param searchObject
	 * @param threshold
	 * @return matching objects
	 */
	public HashMap<E, Integer> query(E searchObject, int threshold) {
        HashMap<E, Integer> matches = new HashMap<E, Integer>();
		root.query(searchObject, threshold, matches);
		return matches;
	}

	/**
	 * Attempts to find the closest match to the search term.
	 * @param term
	 * @return the edit distance of the best match
	 */
	public int find(E term) {
		return root.findBestMatch(term, Integer.MAX_VALUE);
	}

	/**
	 * Attempts to find the closest match to the search term.
	 * @param term
	 * @return a match that is within the best edit distance of the search term.
	 */
	public E findBestWordMatch(E term) {
		root.findBestMatch(term, Integer.MAX_VALUE);
		return root.getBestTerm();
	}

	/**
	 * Attempts to find the closest match to the search term.
	 * @param term
	 * @return a match that is within the best edit distance of the search term.
	 */
	public HashMap<E,Integer> findBestWordMatchWithDistance(E term) {
		int distance = root.findBestMatch(term, Integer.MAX_VALUE);
		HashMap<E, Integer> returnMap = new HashMap<E, Integer>();
		returnMap.put(root.getBestTerm(), distance);
		return returnMap;
	}

    private class Distance {
        private Caverphone2 c;
        private ColognePhonetic k;
        private final LevenshteinDistance l = new LevenshteinDistance();
        private Metaphone m;
        private RefinedSoundex r;
        private Soundex s;

        public Distance(String comparator) {
            if (comparator.equals("c"))
                this.c = new Caverphone2();
            else if (comparator.equals("k"))
                this.k = new ColognePhonetic();
            else if (comparator.equals("m"))
                this.m = new Metaphone();
            else if (comparator.equals("s"))
                this.s = new Soundex();
            else
                this.r = new RefinedSoundex();

        }

        public int distance(E a, E b) {
            String x = (String) a;
            String y = (String) b;
            if (c != null) {
                return l.distance(c.encode(x), c.encode(y));
            } else if (k != null) {
                return l.distance(k.encode(x), k.encode(y));
            } else if (m != null) {
                return l.distance(m.encode(x), m.encode(y));
            } else if (r != null) {
                try {
                    return -r.difference(x, y);
                } catch (EncoderException e) {
                    e.printStackTrace();
                }
            } else if (s != null) {
                try {
                    return -s.difference(x, y);
                } catch (EncoderException e) {
                    e.printStackTrace();
                }
            }
            return l.distance(x, y);
        }
    }

	private class Node {

		final E term;
		final HashMap<Integer, Node> children;

		public Node(E term) {
			this.term = term;
			children = new HashMap<Integer, Node>();
		}

		public void add(E term) {
			int score = distance.distance(term, this.term);

			Node child = children.get(score);
			if(child != null) {
				child.add(term);
			}
			else {
				children.put(score, new Node(term));
			}
		}

		public int findBestMatch(E term, int bestDistance) {
			int distanceAtNode = distance.distance(term, this.term);

//			System.out.println("term = " + term + ", this.term = " + this.term + ", distance = " + distanceAtNode);

//			if(distanceAtNode == 1) {
//				return distanceAtNode;
//			}

			if(distanceAtNode < bestDistance) {
				bestDistance = distanceAtNode;
				bestTerm = this.term;
			}

			int possibleBest = bestDistance;

			for (Integer score : children.keySet()) {
				if(score < distanceAtNode + bestDistance ) {
					possibleBest = children.get(score).findBestMatch(term, bestDistance);
					if(possibleBest < bestDistance) {
						bestDistance = possibleBest;
					}
				}
			}
			return bestDistance;
		}

		public E getBestTerm() {
			return bestTerm;
		}

		public void query(E term, int threshold, HashMap<E, Integer> collected) {
			int distanceAtNode = distance.distance(term, this.term);

			if(distanceAtNode == threshold) {
				collected.put(this.term, distanceAtNode);
				return;
			}

			if(distanceAtNode < threshold) {
				collected.put(this.term, distanceAtNode);
			}

			for (int score = distanceAtNode-threshold; score <= threshold+distanceAtNode; score++) {
				Node child = children.get(score);
				if(child != null) {
					child.query(term, threshold, collected);
				}
			}
		}
	}
}
